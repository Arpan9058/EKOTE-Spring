#!/usr/bin/env python3
"""Compare the resolved CycloneDX Maven inventory with its reviewed snapshot."""

import argparse
import difflib
import json
from pathlib import Path
import sys


def inventory(document):
    if document.get('bomFormat') != 'CycloneDX':
        raise ValueError('Expected a CycloneDX SBOM')
    components = document.get('components')
    if not isinstance(components, list) or not components:
        raise ValueError('SBOM must contain resolved dependencies')
    coordinates = set()
    for component in components:
        purl = component.get('purl', '')
        version = component.get('version', '')
        if not isinstance(purl, str) or not purl.startswith('pkg:maven/') or '@' not in purl:
            raise ValueError('Every dependency must have a versioned Maven package URL')
        if not isinstance(version, str) or not version or 'SNAPSHOT' in version.upper():
            raise ValueError('Dependency versions must be fixed releases')
        if purl in coordinates:
            raise ValueError(f'Duplicate dependency coordinate: {purl}')
        coordinates.add(purl)
    return {'schemaVersion': 1, 'dependencies': sorted(coordinates)}


def render(document):
    return json.dumps(document, indent=2) + '\n'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('sbom', type=Path)
    parser.add_argument('snapshot', type=Path)
    parser.add_argument('--update', action='store_true', help='Write a snapshot for review')
    args = parser.parse_args()
    try:
        current = render(inventory(json.loads(args.sbom.read_text())))
        if args.update:
            args.snapshot.write_text(current)
            print(f'Updated {args.snapshot}; review and commit the dependency changes')
            return 0
        expected = args.snapshot.read_text()
        if current != expected:
            sys.stderr.writelines(difflib.unified_diff(
                expected.splitlines(keepends=True), current.splitlines(keepends=True),
                fromfile=str(args.snapshot), tofile='resolved Maven dependencies'))
            print('Dependency drift: regenerate the snapshot only after reviewing the update', file=sys.stderr)
            return 1
        print(f'Resolved Maven dependencies match {args.snapshot}')
        return 0
    except (OSError, ValueError, TypeError, AttributeError) as error:
        print(f'Dependency snapshot check failed: {error}', file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
