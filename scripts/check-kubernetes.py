#!/usr/bin/env python3
"""Check rendered kubectl JSON without third-party Python packages."""
import json
import sys


def resources(text):
    decoder = json.JSONDecoder()
    while text.strip():
        obj, end = decoder.raw_decode(text.lstrip())
        yield from obj.get('items', []) if obj.get('kind') == 'List' else [obj]
        text = text.lstrip()[end:]


def check(objects):
    errors = []
    kinds = {o['kind'] for o in objects}
    for required in ('Deployment', 'StatefulSet', 'NetworkPolicy', 'Namespace'):
        if required not in kinds:
            errors.append(f'missing {required}')
    for obj in objects:
        name = obj['metadata']['name']
        kind = obj['kind']
        if kind == 'Secret':
            errors.append(f'{name}: secrets must be provisioned separately')
        if kind == 'Service' and obj['spec'].get('type', 'ClusterIP') != 'ClusterIP':
            errors.append(f'{name}: public services require a separate reviewed overlay')
        if kind not in ('Deployment', 'StatefulSet'):
            continue
        spec = obj['spec']['template']['spec']
        security = spec.get('securityContext', {})
        if spec.get('automountServiceAccountToken') is not False:
            errors.append(f'{name}: service-account token must be disabled')
        if security.get('runAsNonRoot') is not True or security.get('runAsUser', 0) <= 0:
            errors.append(f'{name}: non-root UID required')
        if security.get('seccompProfile', {}).get('type') != 'RuntimeDefault':
            errors.append(f'{name}: RuntimeDefault seccomp required')
        for container in spec['containers']:
            identity = f'{name}/{container["name"]}'
            image = container.get('image', '')
            if ':' not in image or image.endswith(':latest'):
                errors.append(f'{identity}: image must have an explicit tag or digest')
            security = container.get('securityContext', {})
            if security.get('allowPrivilegeEscalation') is not False or security.get('readOnlyRootFilesystem') is not True:
                errors.append(f'{identity}: read-only root and no privilege escalation required')
            if 'ALL' not in security.get('capabilities', {}).get('drop', []):
                errors.append(f'{identity}: drop all capabilities')
            for group in ('requests', 'limits'):
                for resource in ('cpu', 'memory'):
                    if not container.get('resources', {}).get(group, {}).get(resource):
                        errors.append(f'{identity}: {group}.{resource} required')
            for probe in ('startupProbe', 'readinessProbe', 'livenessProbe'):
                if not container.get(probe):
                    errors.append(f'{identity}: {probe} required')
    return errors


if __name__ == '__main__':
    failures = check(list(resources(sys.stdin.read())))
    for failure in failures:
        print(failure, file=sys.stderr)
    if failures:
        sys.exit(1)
    print('Kubernetes workload security and reliability checks passed')
