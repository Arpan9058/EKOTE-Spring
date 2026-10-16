#!/usr/bin/env python3
"""Validate CodeQL SARIF and reject high/critical or unscored error findings."""
import json
import math
import pathlib
import sys


def check(directory):
    paths = sorted(pathlib.Path(directory).glob('*.sarif'))
    if not paths:
        raise ValueError('No CodeQL SARIF reports found')
    total = blocking = 0
    for path in paths:
        report = json.loads(path.read_text())
        if report.get('version') != '2.1.0' or not report.get('runs'):
            raise ValueError(f'{path}: missing supported SARIF runs')
        for run in report['runs']:
            driver = run.get('tool', {}).get('driver', {})
            rules = {rule['id']: rule for rule in driver.get('rules', [])}
            if driver.get('name') != 'CodeQL' or not rules or not isinstance(run.get('results'), list):
                raise ValueError(f'{path}: incomplete CodeQL analysis')
            for invocation in run.get('invocations', []):
                if invocation.get('executionSuccessful') is False:
                    raise ValueError(f'{path}: analysis execution failed')
            for result in run['results']:
                rule = rules.get(result.get('ruleId'))
                if rule is None:
                    raise ValueError(f'{path}: finding has no rule metadata')
                score = rule.get('properties', {}).get('security-severity')
                if score is not None:
                    score = float(score)
                    if not math.isfinite(score) or not 0 <= score <= 10:
                        raise ValueError(f'{path}: invalid security severity')
                level = result.get('level', rule.get('defaultConfiguration', {}).get('level', 'warning'))
                blocked = score >= 7 if score is not None else level == 'error'
                total += 1
                blocking += int(blocked)
                # Avoid reproducing source snippets or potential secrets from messages.
                print(f'{result["ruleId"]}: severity={score if score is not None else level}; blocking={blocked}')
    print(f'CodeQL findings: {total}; blocking: {blocking}')
    return 1 if blocking else 0


if __name__ == '__main__':
    try:
        sys.exit(check(sys.argv[1]))
    except (ValueError, KeyError, TypeError, OSError, IndexError) as error:
        raise SystemExit(f'Invalid or incomplete CodeQL report: {error}')
