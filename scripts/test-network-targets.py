#!/usr/bin/env python3
"""Regression checks for target selection during Kubernetes readiness recovery."""
import importlib.util
import pathlib
import unittest
from unittest.mock import Mock, patch

spec = importlib.util.spec_from_file_location('network', pathlib.Path(__file__).with_name('verify-network-policies.py'))
network = importlib.util.module_from_spec(spec)
spec.loader.exec_module(network)


def pod(ready=True, owner='ReplicaSet'):
    return {'metadata': {'ownerReferences': [{'kind': owner}]}, 'status': {
        'phase': 'Running', 'podIP': '192.0.2.1',
        'conditions': [{'type': 'Ready', 'status': 'True' if ready else 'False'}],
        'containerStatuses': [{'ready': ready}]}}


class TargetTest(unittest.TestCase):
    def test_unready_missing_terminating_or_probe_endpoints_are_rejected(self):
        missing = pod()
        missing['status']['containerStatuses'] = []
        terminating = pod()
        terminating['metadata']['deletionTimestamp'] = '2026-10-16T00:00:00Z'
        probe = pod(owner='')
        self.assertIsNone(network.ready_target_ip([pod(False), missing, terminating, probe], 'ReplicaSet'))
        self.assertEqual('192.0.2.1', network.ready_target_ip([pod(False), pod()], 'ReplicaSet'))

    def test_recovery_waits_for_a_new_ready_status(self):
        load = Mock(side_effect=[[pod(False)], [pod()]])
        with patch.object(network.time, 'sleep') as sleep:
            self.assertEqual('192.0.2.1', network.wait_for_ready_target(load, 'app', 'ReplicaSet', attempts=2))
            sleep.assert_called_once_with(1)
        self.assertEqual(2, load.call_count)

    def test_a_permanently_unavailable_target_fails_with_a_bounded_error(self):
        load = Mock(return_value=[pod(False)])
        with patch.object(network.time, 'sleep'), self.assertRaisesRegex(RuntimeError, 'No ready app'):
            network.wait_for_ready_target(load, 'app', 'ReplicaSet', attempts=3)
        self.assertEqual(3, load.call_count)


if __name__ == '__main__':
    unittest.main()
