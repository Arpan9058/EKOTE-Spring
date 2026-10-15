#!/usr/bin/env python3
"""Ensure representative policy violations are rejected, using rendered manifests."""
import copy
import importlib.util
import sys

spec = importlib.util.spec_from_file_location('policy', 'scripts/check-kubernetes.py')
policy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(policy)
objects = list(policy.resources(sys.stdin.read()))
assert not policy.check(objects), 'Base manifests must satisfy policy'
for mutation in ('root', 'writable', 'public', 'probe', 'limit', 'token', 'capability', 'latest'):
    changed = copy.deepcopy(objects)
    workload = next(o for o in changed if o['kind'] == 'Deployment')['spec']['template']['spec']
    container = workload['containers'][0]
    if mutation == 'root':
        workload['securityContext']['runAsUser'] = 0
    elif mutation == 'writable':
        container['securityContext']['readOnlyRootFilesystem'] = False
    elif mutation == 'public':
        next(o for o in changed if o['kind'] == 'Service')['spec']['type'] = 'LoadBalancer'
    elif mutation == 'probe':
        del container['readinessProbe']
    elif mutation == 'limit':
        del container['resources']['limits']['memory']
    elif mutation == 'token':
        workload['automountServiceAccountToken'] = True
    elif mutation == 'capability':
        container['securityContext']['capabilities']['drop'] = []
    elif mutation == 'latest':
        container['image'] = 'ekote-spring:latest'
    assert policy.check(changed), f'Policy failed to reject {mutation}'
print('Eight unsafe manifest mutations rejected')
