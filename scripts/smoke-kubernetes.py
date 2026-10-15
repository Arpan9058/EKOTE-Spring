#!/usr/bin/env python3
"""Exercise the isolated local deployment; never targets an external application URL."""
import argparse
import http.cookiejar
import re
import subprocess
import time
import urllib.parse
import urllib.request
import uuid

arguments = argparse.ArgumentParser()
arguments.add_argument('--verify-database-recovery', action='store_true', help='Temporarily stop MySQL in this disposable namespace and verify probe recovery')
options = arguments.parse_args()

forward = subprocess.Popen(['kubectl', '-n', 'ekote', 'port-forward', 'deployment/ekote', '18080:8080', '18081:8081'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
jar = http.cookiejar.CookieJar()
client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))

def get(path, port=18080):
    with client.open(f'http://127.0.0.1:{port}{path}', timeout=45) as response:
        return response.read().decode()

try:
    for attempt in range(60):
        try:
            form = get('/register')
            break
        except OSError:
            if forward.poll() is not None:
                raise RuntimeError('Port forward stopped unexpectedly')
            time.sleep(0.5)
    else:
        raise RuntimeError('Application did not respond')
    token = re.search(r'name="_csrf"[^>]*value="([^"]+)"', form)
    assert token, 'Registration must include a CSRF token'
    suffix = uuid.uuid4().hex[:10]
    data = urllib.parse.urlencode({'_csrf': token.group(1), 'user_name': 'Cluster smoke', 'user_mail': f'cluster-{suffix}@example.test', 'user_pass': 'Smoke-password-42', 'user_id': str(int(suffix, 16)), 'user_role': 'ADMIN'}).encode()
    with client.open('http://127.0.0.1:18080/register', data=data, timeout=15) as response:
        assert 'registration_successful' in response.url, response.url
    login = get('/login')
    token = re.search(r'name="_csrf"[^>]*value="([^"]+)"', login)
    data = urllib.parse.urlencode({'_csrf': token.group(1), 'username': f'cluster-{suffix}@example.test', 'password': 'Smoke-password-42'}).encode()
    with client.open('http://127.0.0.1:18080/login', data=data, timeout=15) as response:
        assert 'error' not in response.url
    try:
        get('/api/inventory/summary')
        raise AssertionError('Public registration must not grant ADMIN')
    except urllib.error.HTTPError as error:
        assert error.code == 403
    assert 'UP' in get('/actuator/health/readiness', 18081)
    assert 'UP' in get('/actuator/health/liveness', 18081)
    assert 'jvm_memory' in get('/actuator/prometheus', 18081)
    try:
        get('/register', 18081)
        raise AssertionError('Management listener must deny application routes')
    except urllib.error.HTTPError as error:
        assert error.code in (403, 404)
    print('Cluster registration, login, role restriction, readiness, liveness and Prometheus checks passed', flush=True)
    if options.verify_database_recovery:
        try:
            subprocess.run(['kubectl', '-n', 'ekote', 'scale', 'statefulset/mysql', '--replicas=0'], check=True)
            subprocess.run(['kubectl', '-n', 'ekote', 'wait', '--for=delete', 'pod/mysql-0', '--timeout=90s'], check=True)
            try:
                get('/actuator/health/readiness', 18081)
                raise AssertionError('Database outage must fail readiness')
            except urllib.error.HTTPError as error:
                assert error.code == 503
            assert 'UP' in get('/actuator/health/liveness', 18081), 'Database outage must not fail liveness'
            print('Database outage removes readiness while liveness stays UP', flush=True)
        finally:
            subprocess.run(['kubectl', '-n', 'ekote', 'scale', 'statefulset/mysql', '--replicas=1'], check=True)
            subprocess.run(['kubectl', '-n', 'ekote', 'rollout', 'status', 'statefulset/mysql', '--timeout=180s'], check=True)
        for attempt in range(20):
            try:
                assert 'UP' in get('/actuator/health/readiness', 18081)
                break
            except urllib.error.HTTPError as error:
                if error.code != 503:
                    raise
                time.sleep(1)
        else:
            raise AssertionError('Readiness did not recover after MySQL restart')
        # The previously registered account must persist through the database restart.
        login = get('/login')
        token = re.search(r'name="_csrf"[^>]*value="([^\"]+)"', login)
        data = urllib.parse.urlencode({'_csrf': token.group(1), 'username': f'cluster-{suffix}@example.test', 'password': 'Smoke-password-42'}).encode()
        with client.open('http://127.0.0.1:18080/login', data=data, timeout=15) as response:
            assert 'error' not in response.url
        print('Database recovery restores readiness and preserves the registered account', flush=True)
finally:
    forward.terminate()
    forward.wait(timeout=10)
