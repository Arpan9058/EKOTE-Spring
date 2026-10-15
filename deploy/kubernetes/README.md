# Local Kubernetes deployment

This setup runs the actual application and MySQL in an isolated Kubernetes namespace.
It is a development/evaluation deployment, with two application replicas sharing
JDBC sessions. RollingUpdate keeps zero unavailable app pods and a disruption budget
retains one replica during voluntary disruption. This does not provide node/database
HA: the local cluster is a single node and the MySQL StatefulSet is a single instance.
Use supported managed infrastructure across failure domains for production.

Prerequisites: Docker, kind 0.27.0, kubectl compatible with Kubernetes 1.32, Python 3,
and enough capacity for a control plane, a 1 GiB application and a 1 GiB database.
Run from the repository root:

```sh
docker build -t ekote-spring:local demo
kind create cluster --name ekote-dev --config deploy/testing/kind.yaml --image kindest/node:v1.32.2@sha256:142f543559cc55d64e1ab9341df08e5ced84bd2e893736da8f51320f26f5950b
python3 scripts/install-test-network-policy.py
kind load docker-image ekote-spring:local --name ekote-dev
python3 scripts/create-kubernetes-secret.py
kubectl apply --dry-run=server -k deploy/kubernetes -o json > /tmp/ekote-manifests.json
python3 scripts/check-kubernetes.py < /tmp/ekote-manifests.json
python3 scripts/test-kubernetes-policy.py < /tmp/ekote-manifests.json
kubectl apply -k deploy/kubernetes
kubectl -n ekote rollout status statefulset/mysql --timeout=300s
kubectl -n ekote rollout status deployment/ekote --timeout=300s
python3 scripts/smoke-kubernetes.py --verify-database-recovery
python3 scripts/verify-network-policies.py
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
to your cluster. The default kind CNI does not enforce NetworkPolicy, so the testing configuration
explicitly disables it and installs a checksum-pinned Calico policy-only manifest
with digest-pinned images. A minimal additional CNI discovery role fixes the upstream
policy-only manifest's missing read permission; it does not grant cluster-admin.
This compatibility-pinned Kubernetes/Calico test lab is not a supported production
cluster template. Use maintained provider/CNI releases on the actual production target.

Traffic tests prove app→MySQL and DNS, ingress→HTTP and monitoring→metrics are allowed;
untrusted traffic and five disallowed flow combinations time out. A deny→allow→deny
control proves failures come from policy enforcement rather than unavailable targets.
Probe pods are non-root and unready so app-labelled probes never become Service
endpoints. They and their newly created test namespaces are cleaned up afterward.
Run the traffic script only in `kind-ekote-dev` or `kind-ekote-ci`.

The smoke registers a unique test user, logs in, checks that registration cannot grant
ADMIN, checks readiness/liveness/Prometheus, and verifies that management denies app
routes. With `--verify-database-recovery`, it temporarily scales MySQL to zero,
verifies readiness fails while liveness stays up, then restores MySQL and verifies
readiness and the persisted account recover. It intentionally leaves the test user in the disposable cluster. Do not run
it against a persistent production namespace. CI creates and deletes its own cluster.

The local-path provisioner/PVC is for development, not a backup or HA guarantee. Take
verified backups before schema updates. Flyway applies versioned migrations and Hibernate validates the result.
Nonempty legacy databases require the reviewed adoption procedure in
[database operations](../../docs/DATABASE_OPERATIONS.md). Audit records start with new stock mutations; old history is not
backfilled or fabricated. Database administrators can modify audit rows; it is not a
cryptographically tamper-proof ledger.

Delete only your disposable cluster when finished:

```sh
kind delete cluster --name ekote-dev
```

Deleting the cluster destroys its local MySQL volume. Namespace deletion and PVC
removal are destructive and are never part of the repeat-deploy path.
