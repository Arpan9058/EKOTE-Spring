#!/usr/bin/env python3
"""Regression checks for the resolved dependency drift gate."""

import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).with_name('check-dependency-snapshot.py')


class DependencySnapshotTest(unittest.TestCase):
    def test_inventory_is_stable_but_version_changes_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            sbom = Path(directory) / 'bom.json'
            snapshot = Path(directory) / 'dependencies.json'

            def write(versions):
                sbom.write_text(json.dumps({
                    'bomFormat': 'CycloneDX', 'serialNumber': 'volatile build metadata',
                    'components': [{'purl': f'pkg:maven/example/{name}@{version}?type=jar',
                                    'version': version} for name, version in versions]}))

            def run(*args):
                return subprocess.run([sys.executable, str(SCRIPT), str(sbom), str(snapshot), *args],
                                      capture_output=True, text=True)

            write([('a', '1.0'), ('b', '2.0')])
            self.assertEqual(0, run('--update').returncode)
            write([('b', '2.0'), ('a', '1.0')])
            self.assertEqual(0, run().returncode)
            write([('b', '2.1'), ('a', '1.0')])
            self.assertEqual(1, run().returncode)
            self.assertIn('Dependency drift', run().stderr)
            write([('a', '1.0-SNAPSHOT')])
            self.assertEqual(1, run('--update').returncode)
            for invalid in ({}, {'bomFormat': 'CycloneDX', 'components': []},
                            {'bomFormat': 'CycloneDX', 'components': [{'version': '1'}]}):
                sbom.write_text(json.dumps(invalid))
                self.assertEqual(1, run('--update').returncode)
            write([('a', '1.0'), ('a', '1.0')])
            self.assertEqual(1, run('--update').returncode)
            sbom.write_text('invalid JSON')
            self.assertEqual(1, run().returncode)
            sbom.unlink()
            self.assertEqual(1, run().returncode)


if __name__ == '__main__':
    unittest.main()
