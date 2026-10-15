#!/usr/bin/env python3
"""Preserve all image findings and fail on any HIGH/CRITICAL OS advisory."""
import collections
import json
import sys
report = json.load(open(sys.argv[1]))
if report.get('SchemaVersion') != 2 or not report.get('Results'):
    raise SystemExit('Missing or unsupported image scan; refusing an empty success')
results = [r for r in report['Results'] if r.get('Class') == 'os-pkgs']
if not results or not report.get('Metadata', {}).get('OS'):
    raise SystemExit('No operating-system package scan found')
findings = [v for r in results for v in r.get('Vulnerabilities', [])]
print('OS advisory counts:', dict(collections.Counter(v['Severity'] for v in findings)))
blocking = [v for v in findings if v['Severity'] in ('HIGH', 'CRITICAL')]
for vulnerability in blocking:
    print(vulnerability['VulnerabilityID'], vulnerability['PkgName'], vulnerability.get('FixedVersion') or 'vendor fix unavailable')
raise SystemExit(1 if blocking else 0)
