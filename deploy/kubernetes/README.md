# Local Kubernetes deployment

This setup runs the actual application and MySQL in an isolated Kubernetes namespace.
It is a development/evaluation deployment, with one application replica because
HTTP sessions currently live in the JVM. Deployment uses Recreate; updates interrupt
service and invalidate sessions. Add shared sessions, tested migrations and managed
HA database/storage before claiming production availability or enabling autoscaling.

Prerequisites: Docker, kind 0.27.0, kubectl compatible with Kubernetes 1.32, Python 3,
and enough capacity for a control plane, a 1 GiB application and a 1 GiB database.
Run from the repository root:

```sh
docker build -t ekote-spring:local demo
kind create cluster --name ekote-dev --image kindest/node:v1.32.2@sha256:142f543559cc55d64e1ab9341df08e5ced84bd2e893736da8f51320f26f5950b
kind load docker-image ekote-spring:local --name ekote-dev
python3 scripts/create-kubernetes-secret.py
kubectl apply --dry-run=server -k deploy/kubernetes -o json > /tmp/ekote-manifests.json
python3 scripts/check-kubernetes.py < /tmp/ekote-manifests.json
python3 scripts/test-kubernetes-policy.py < /tmp/ekote-manifests.json
kubectl apply -k deploy/kubernetes
kubectl -n ekote rollout status statefulset/mysql --timeout=300s
kubectl -n ekote rollout status deployment/ekote --timeout=300s
python3 scripts/smoke-kubernetes.py --verify-database-recovery
kubectl -n ekote port-forward service/ekote 8080:8080
```

Nodes need registry access to pull the pinned MySQL image, or a preloaded image with
the exact digest reference. Proxy DNS/trust and containerd snapshotter settings may
need environment-specific configuration in nested cloud runtimes.

The secret helper generates random passwords in a mode-0600 temporary file, removes
the file, and retains existing Secrets across repeat deployments. It does not print
credentials. Do not rotate the Secret without also rotating MySQL accounts. Kubernetes
Secrets need encryption at rest and restricted RBAC on a shared cluster.

All services are ClusterIP. There is no public ingress or external database exposure.
The app uses a read-only root filesystem, non-root UID, dropped capabilities,
RuntimeDefault seccomp, a bounded temporary volume, no service-account token, resource
requests/limits and separate startup, readiness and liveness probes. MySQL uses the
same security restrictions with writable data/socket/temp mounts and a 2 GiB PVC.
The namespace enforces the Kubernetes restricted Pod Security standard.

Management runs on a separate port 8081. Only health/probes and Prometheus are allowed
there. This listener intentionally permits unauthenticated scraping inside its trusted
network; never publish it through an ingress or public service. The normal listener
requires ADMIN for metrics. Readiness includes MySQL; liveness excludes the database,
so a database outage removes readiness without causing repeated JVM restarts.

NetworkPolicies deny traffic by default, allow DNS, app-to-MySQL, HTTP from `ekote`
and `ingress-nginx`, and management scraping from `monitoring`. Adapt namespace labels
to your cluster. **kind's default CNI does not enforce NetworkPolicy.** The local smoke
verifies app behavior, schema acceptance and policy structure, not network isolation.
Use a policy-enforcing CNI and verify allowed/denied flows before shared deployment.

The smoke registers a unique test user, logs in, checks that registration cannot grant
ADMIN, checks readiness/liveness/Prometheus, and verifies that management denies app
routes. With `--verify-database-recovery`, it temporarily scales MySQL to zero,
verifies readiness fails while liveness stays up, then restores MySQL and verifies
readiness and the persisted account recover. It intentionally leaves the test user in the disposable cluster. Do not run
it against a persistent production namespace. CI creates and deletes its own cluster.

The local-path provisioner/PVC is for development, not a backup or HA guarantee. Take
verified backups before schema updates. Hibernate `ddl-auto=update` is still used;
existing duplicate `(gun_name, model)` rows must be reconciled before adding the new
unique constraint. Audit records start with new stock mutations; old history is not
backfilled or fabricated. Database administrators can modify audit rows; it is not a
cryptographically tamper-proof ledger.

Delete only your disposable cluster when finished:

```sh
kind delete cluster --name ekote-dev
```

Deleting the cluster destroys its local MySQL volume. Namespace deletion and PVC
removal are destructive and are never part of the repeat-deploy path.
