#!/usr/bin/env python3
"""Regression checks for the static-analysis release gate."""
import contextlib
import importlib.util
import io
import json
import pathlib
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('gate', pathlib.Path(__file__).with_name('check-codeql.py'))
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)


class GateTest(unittest.TestCase):
    def report(self, severity='8.0', results=True):
        return {'version': '2.1.0', 'runs': [{'tool': {'driver': {'name': 'CodeQL', 'rules': [
            {'id': 'java/test-security-rule', 'properties': {'security-severity': severity}}]}},
            'results': [{'ruleId': 'java/test-security-rule'}] if results else []}]}

    def evaluate(self, report):
        with tempfile.TemporaryDirectory() as directory, contextlib.redirect_stdout(io.StringIO()):
            pathlib.Path(directory, 'java.sarif').write_text(json.dumps(report))
            return gate.check(directory)

    def test_high_blocks_and_medium_remains_reported(self):
        self.assertEqual(1, self.evaluate(self.report()))
        self.assertEqual(0, self.evaluate(self.report('6.9')))

    def test_complete_analysis_with_no_findings_passes(self):
        self.assertEqual(0, self.evaluate(self.report(results=False)))

    def test_missing_empty_failed_or_invalid_scans_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(ValueError): gate.check(directory)
        for report in ({}, {'version': '2.1.0', 'runs': []}, self.report('nan')):
            with self.assertRaises(ValueError): self.evaluate(report)
        report = self.report()
        report['runs'][0]['invocations'] = [{'executionSuccessful': False}]
        with self.assertRaises(ValueError): self.evaluate(report)

    def test_rules_in_codeql_query_pack_extensions_are_checked(self):
        report = self.report()
        tool = report['runs'][0]['tool']
        tool['extensions'] = [{'name': 'codeql/java-queries', 'rules': tool['driver'].pop('rules')}]
        self.assertEqual(1, self.evaluate(report))
        report['runs'][0]['results'] = []
        self.assertEqual(0, self.evaluate(report))

    def test_unscored_error_cannot_pass(self):
        report = self.report()
        del report['runs'][0]['tool']['driver']['rules'][0]['properties']['security-severity']
        report['runs'][0]['results'][0]['level'] = 'error'
        self.assertEqual(1, self.evaluate(report))


if __name__ == '__main__':
    unittest.main()
