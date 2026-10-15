#!/usr/bin/env python3
"""Install the checksum-pinned policy engine in an owned disposable kind cluster."""
import hashlib
import json
import subprocess
import urllib.request
URL = 'https://raw.githubusercontent.com/projectcalico/calico/v3.29.3/manifests/calico-policy-only.yaml'
EXPECTED = '060a538848847deb8ac8085bd974f976ee28ec9ce54c0ad8801e764ee70c4230'
PINS = {'calico/cni:v3.29.3': 'calico/cni@sha256:53f826d3f565a6635b4d58ea4fcfdc0e7ea418ffd4dbb495b4c801074e6eb99c', 'calico/node:v3.29.3': 'calico/node@sha256:eed399f2a727cfc1f374ab5c9cda6123c207e794ed8dc66c7eb6d8db412669e1', 'calico/kube-controllers:v3.29.3': 'calico/kube-controllers@sha256:5516ab776c38525fcc985a3030b4f2fd472da68b4170601c23bf6887bfdce703', 'calico/typha:v3.29.3': 'calico/typha@sha256:27d6bead88904d3f72e6d2f8023399dbfbe0af14b01fc3e30aa458ac98d839b9'}
with urllib.request.urlopen(URL, timeout=60) as response:
    payload = response.read()
if hashlib.sha256(payload).hexdigest() != EXPECTED:
    raise RuntimeError('Upstream manifest checksum mismatch')
manifest = payload.decode()
for tag, digest in PINS.items():
    manifest = manifest.replace('docker.io/' + tag, 'docker.io/' + digest)
subprocess.run(['kubectl', 'apply', '-f', '-'], input=manifest, text=True, check=True)
subprocess.run(['kubectl', 'apply', '-f', 'deploy/testing/calico-cni-discovery.yaml'], check=True)
subprocess.run(['kubectl', '-n', 'kube-system', 'rollout', 'status', 'daemonset/calico-node', '--timeout=180s'], check=True)
subprocess.run(['kubectl', 'wait', '--for=condition=Ready', 'nodes', '--all', '--timeout=180s'], check=True)
print('Calico policy engine is ready; traffic tests must still establish enforcement')
