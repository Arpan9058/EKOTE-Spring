#!/usr/bin/env python3
"""Prove allow/deny behavior in the disposable kind-ekote-ci/dev cluster."""
import json
import subprocess
import time
import uuid

IMAGE = 'busybox@sha256:bdf57e528e45e4433820e045b29b4597825a1c9e38353532d90a01445013f82e'
suffix = uuid.uuid4().hex[:8]
created_namespaces = []
created_pods = []
policy_name = 'traffic-proof-' + suffix

def kubectl(*args, **kwargs):
    return subprocess.run(['kubectl', *args], check=True, capture_output=True, text=True, **kwargs).stdout

def apply(obj):
    kubectl('apply', '-f', '-', input=json.dumps(obj))

def namespace(name):
    if not kubectl('get', 'namespace', name, '--ignore-not-found', '-o', 'name').strip():
        apply({'apiVersion': 'v1', 'kind': 'Namespace', 'metadata': {'name': name}})
        created_namespaces.append(name)

def probe(namespace_name, label):
    name = 'traffic-probe-' + suffix
    apply({'apiVersion': 'v1', 'kind': 'Pod', 'metadata': {'name': name, 'namespace': namespace_name, 'labels': {'app': label}}, 'spec': {
        'automountServiceAccountToken': False,
        'securityContext': {'runAsNonRoot': True, 'runAsUser': 10001, 'seccompProfile': {'type': 'RuntimeDefault'}},
        'containers': [{'name': 'probe', 'image': IMAGE, 'command': ['sleep', '600'],
            'securityContext': {'allowPrivilegeEscalation': False, 'readOnlyRootFilesystem': True, 'capabilities': {'drop': ['ALL']}},
            'resources': {'requests': {'cpu': '5m', 'memory': '8Mi'}, 'limits': {'cpu': '100m', 'memory': '32Mi'}},
            # App-labelled probes must never become Service endpoints.
            'readinessProbe': {'exec': {'command': ['false']}, 'periodSeconds': 2}}]}})
    created_pods.append((namespace_name, name))
    for _ in range(60):
        data = json.loads(kubectl('-n', namespace_name, 'get', 'pod', name, '-o', 'json'))
        if any(c.get('state', {}).get('running') for c in data.get('status', {}).get('containerStatuses', [])):
            return namespace_name, name
        time.sleep(1)
    raise RuntimeError('Probe container did not start')

def connection(source, address, port, allowed):
    ns, pod = source
    kubectl('-n', ns, 'exec', pod, '--', 'true')
    result = subprocess.run(['kubectl', '-n', ns, 'exec', pod, '--', 'nc', '-z', '-v', '-w', '3', address, str(port)], capture_output=True, text=True, timeout=12)
    if allowed:
        assert result.returncode == 0, f'Expected allowed TCP {ns} -> {address}:{port}: {result.stderr}'
    else:
        assert result.returncode != 0 and 'timed out' in result.stderr.lower(), f'Expected a policy timeout, not an unavailable probe/target: {result.stderr}'

context = kubectl('config', 'current-context').strip()
assert context in ('kind-ekote-ci', 'kind-ekote-dev'), 'Use only the explicitly disposable test clusters'
apps = json.loads(kubectl('-n', 'ekote', 'get', 'pods', '-l', 'app=ekote', '-o', 'json'))['items']
app_ip = next(p['status']['podIP'] for p in apps if any(o['kind'] == 'ReplicaSet' for o in p['metadata'].get('ownerReferences', [])) and all(c.get('ready') for c in p['status'].get('containerStatuses', [])))
mysql_ip = json.loads(kubectl('-n', 'ekote', 'get', 'pod', 'mysql-0', '-o', 'json'))['status']['podIP']
untrusted_namespace = 'untrusted-' + suffix
try:
    for ns in ('ingress-nginx', 'monitoring', untrusted_namespace):
        namespace(ns)
    app = probe('ekote', 'ekote')
    ingress = probe('ingress-nginx', 'probe')
    monitoring = probe('monitoring', 'probe')
    outsider = probe(untrusted_namespace, 'probe')
    connection(app, mysql_ip, 3306, True)
    kubectl('-n', app[0], 'exec', app[1], '--', 'nslookup', 'mysql.ekote.svc.cluster.local')
    connection(ingress, app_ip, 8080, True)
    connection(monitoring, app_ip, 8081, True)
    connection(outsider, mysql_ip, 3306, False)
    connection(outsider, app_ip, 8080, False)
    connection(monitoring, app_ip, 8080, False)
    connection(ingress, app_ip, 8081, False)
    connection(app, app_ip, 8080, False)
    # Control: open one previously denied flow, prove it works, remove permission and prove denial.
    apply({'apiVersion': 'networking.k8s.io/v1', 'kind': 'NetworkPolicy', 'metadata': {'name': policy_name, 'namespace': 'ekote'}, 'spec': {
        'podSelector': {'matchLabels': {'app': 'ekote'}}, 'policyTypes': ['Ingress'],
        'ingress': [{'from': [{'namespaceSelector': {'matchLabels': {'kubernetes.io/metadata.name': untrusted_namespace}}}], 'ports': [{'protocol': 'TCP', 'port': 8080}]}]}})
    time.sleep(2)
    connection(outsider, app_ip, 8080, True)
    kubectl('-n', 'ekote', 'delete', 'networkpolicy', policy_name)
    time.sleep(2)
    connection(outsider, app_ip, 8080, False)
    print('Network enforcement proved: DB/DNS/HTTP/metrics allows, five denied flows, and deny→allow→deny control')
finally:
    kubectl('-n', 'ekote', 'delete', 'networkpolicy', policy_name, '--ignore-not-found')
    for ns, name in created_pods:
        kubectl('-n', ns, 'delete', 'pod', name, '--ignore-not-found', '--wait=false')
    for ns in created_namespaces:
        kubectl('delete', 'namespace', ns, '--ignore-not-found', '--wait=false')
