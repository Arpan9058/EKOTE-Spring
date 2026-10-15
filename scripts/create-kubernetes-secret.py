#!/usr/bin/env python3
"""Provision random local database credentials without printing or committing them."""
import os
import secrets
import subprocess
import tempfile

subprocess.run(['kubectl', 'apply', '-f', 'deploy/kubernetes/namespace.yml'], check=True)
# Existing credentials must survive repeat deploys; rotating them needs a database rotation procedure.
exists = subprocess.run(['kubectl', '-n', 'ekote', 'get', 'secret', 'database', '--ignore-not-found', '-o', 'name'], check=True, capture_output=True, text=True)
if exists.stdout.strip():
    print('Existing database secret retained')
else:
    path = None
    try:
        with tempfile.NamedTemporaryFile(mode='w', prefix='ekote-secret-', delete=False) as file:
            path = file.name
            os.chmod(path, 0o600)
            for key in ('MYSQL_PASSWORD', 'MYSQL_ROOT_PASSWORD'):
                file.write(f'{key}={secrets.token_urlsafe(32)}\n')
        subprocess.run(['kubectl', '-n', 'ekote', 'create', 'secret', 'generic', 'database', f'--from-env-file={path}'], check=True)
    finally:
        if path:
            os.unlink(path)
