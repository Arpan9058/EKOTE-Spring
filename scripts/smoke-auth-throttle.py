#!/usr/bin/env python3
"""Prove a shared authentication quota against two disposable local app instances.

Use fresh test instances/buckets: this deliberately exhausts the local login quota.
Never run against a user's production service.
"""
import argparse
import http.cookiejar
import re
import urllib.error
import urllib.parse
import urllib.request
import uuid

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--node-a', default='http://127.0.0.1:18082')
parser.add_argument('--node-b', default='http://127.0.0.1:18083')
args = parser.parse_args()
nodes = (args.node_a, args.node_b)
for node in nodes:
    if urllib.parse.urlparse(node).hostname not in ('localhost', '127.0.0.1'):
        parser.error('Use only local disposable app instances')
client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
with client.open(nodes[0] + '/login', timeout=15) as response:
    form = response.read().decode()
csrf = re.search(r'name="_csrf"[^>]*value="([^"]+)"', form)
assert csrf, 'Login must include a CSRF token'
body = urllib.parse.urlencode({'_csrf': csrf.group(1), 'username': f'throttle-{uuid.uuid4().hex}@example.test', 'password': 'Deliberately-incorrect-42'}).encode()
allowed = 0
for attempt in range(40):
    request = urllib.request.Request(nodes[attempt % 2] + '/login', data=body,
        headers={'X-Forwarded-For': f'192.0.2.{attempt + 1}'})
    try:
        with client.open(request, timeout=15) as response:
            assert 'error' in response.url, 'Incorrect credentials must never authenticate'
            allowed += 1
    except urllib.error.HTTPError as error:
        assert error.code == 429, error.code
        assert error.headers['Retry-After'] == '60'
        assert error.headers['Cache-Control'] == 'no-store'
assert 1 <= allowed <= 20, f'Shared quota must permit at most 20 total attempts, got {allowed}'
for node in nodes:
    # The servlet's decoded login path must not evade the exhausted bucket.
    try:
        client.open(urllib.request.Request(node + '/%6cogin', data=body), timeout=15)
        raise AssertionError('Encoded login path bypassed the exhausted shared limit')
    except urllib.error.HTTPError as error:
        assert error.code == 429, error.code
print(f'Shared login quota proved across two containers: {allowed} allowed, remaining attempts HTTP 429; spoofed forwarding headers and encoded paths cannot bypass it')
