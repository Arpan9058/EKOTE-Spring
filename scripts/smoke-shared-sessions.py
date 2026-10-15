#!/usr/bin/env python3
"""Verify login and logout across two local app processes sharing a database."""
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
for base in (args.node_a, args.node_b):
    if urllib.parse.urlparse(base).hostname not in ('localhost', '127.0.0.1'):
        parser.error('Use only local disposable app instances')
client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

def get(base, path):
    with client.open(base + path, timeout=15) as response:
        return response.url, response.read().decode()

def token(html):
    result = re.search(r'name="_csrf"[^>]*value="([^"]+)"', html)
    assert result, 'CSRF token missing'
    return result.group(1)

def post(base, path, data):
    with client.open(base + path, data=urllib.parse.urlencode(data).encode(), timeout=15) as response:
        return response.url

suffix = uuid.uuid4().hex[:10]
mail = f'shared-{suffix}@example.test'
_, form = get(args.node_a, '/register')
result = post(args.node_a, '/register', {'_csrf': token(form), 'user_name': 'Shared session test', 'user_mail': mail, 'user_pass': 'Shared-password-42', 'user_id': str(int(suffix, 16))})
assert 'registration_successful' in result
_, form = get(args.node_a, '/login')
assert 'error' not in post(args.node_a, '/login', {'_csrf': token(form), 'username': mail, 'password': 'Shared-password-42'})
# A redirect to login would prove the second process lost the authentication session.
url, html = get(args.node_b, '/inventory_management')
assert '/login' not in url and 'inventory' in html.lower()
try:
    get(args.node_b, '/api/inventory/summary')
    raise AssertionError('USER must not gain administrative privileges on a second process')
except urllib.error.HTTPError as error:
    assert error.code == 403
_, form = get(args.node_b, '/login')
assert '/login' in post(args.node_b, '/logout', {'_csrf': token(form)})
url, _ = get(args.node_a, '/inventory_management')
assert '/login' in url, 'Logout on process B must revoke the session on process A'
print('Login on A works on B; role restrictions persist; logout on B revokes access on A')
