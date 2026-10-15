#!/usr/bin/env python3
"""Restore a backup into an owned disposable MySQL container and compare every data row."""
import argparse
import json
import os
from pathlib import Path
import secrets
import stat
import subprocess
import sys
import tempfile
import time
import uuid

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source-container', required=True)
args = parser.parse_args()
target = 'ekote-restore-' + uuid.uuid4().hex[:12]
image = 'mysql:8.4@sha256:24989179f58856db763f418010c1169fecc944e3ec1ea7e921572d83eb070f70'
backup_tool = str(Path(__file__).with_name('database-backup.py'))
created = False

with tempfile.TemporaryDirectory(prefix='ekote-recovery-') as directory:
    root = Path(directory)
    backup = root / 'backup.sql.gz'
    env = root / 'database.env'
    env.write_text('MYSQL_DATABASE=ekote\nMYSQL_USER=ekote\nMYSQL_PASSWORD=' + secrets.token_urlsafe(32) + '\nMYSQL_ROOT_PASSWORD=' + secrets.token_urlsafe(32) + '\n')
    env.chmod(0o600)
    try:
        subprocess.run([sys.executable, backup_tool, 'backup', '--container', args.source_container, '--file', str(backup)], check=True)
        for file in (backup, Path(str(backup) + '.metadata.json')):
            assert stat.S_IMODE(file.stat().st_mode) == 0o600
        subprocess.run(['docker', 'run', '-d', '--name', target, '--env-file', str(env), image], check=True, stdout=subprocess.DEVNULL)
        created = True
        for attempt in range(90):
            result = subprocess.run(['docker', 'exec', target, 'sh', '-c', 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -Nse "SELECT 1"'], capture_output=True)
            if result.returncode == 0:
                break
            time.sleep(1)
        else:
            raise RuntimeError('Disposable restore target did not become ready')
        subprocess.run([sys.executable, backup_tool, 'restore', '--container', target, '--file', str(backup), '--allow-restore'], check=True)
        export = 'export MYSQL_PWD="$MYSQL_PASSWORD"; exec mysqldump -u"$MYSQL_USER" --no-create-info --skip-comments --skip-extended-insert --order-by-primary --skip-add-locks --skip-disable-keys --no-tablespaces --set-gtid-purged=OFF "$MYSQL_DATABASE"'
        source_rows = subprocess.check_output(['docker', 'exec', args.source_container, 'sh', '-c', export])
        restored_rows = subprocess.check_output(['docker', 'exec', target, 'sh', '-c', export])
        assert source_rows == restored_rows, 'Restored rows differ from the source; quiesce writes while verifying'
        rejected = subprocess.run([sys.executable, backup_tool, 'restore', '--container', target, '--file', str(backup), '--allow-restore'], capture_output=True, text=True)
        assert rejected.returncode != 0 and 'nonempty' in rejected.stderr
        corrupted = root / 'corrupt.sql.gz'
        corrupted.write_bytes(backup.read_bytes() + b'corruption')
        corrupted.chmod(0o600)
        Path(str(corrupted) + '.metadata.json').write_bytes(Path(str(backup) + '.metadata.json').read_bytes())
        rejected = subprocess.run([sys.executable, backup_tool, 'restore', '--container', target, '--file', str(corrupted), '--allow-restore'], capture_output=True, text=True)
        assert rejected.returncode != 0 and 'integrity' in rejected.stderr
        print('Recovery verified: exact row match, private permissions, nonempty-target and corruption rejection')
    finally:
        if created:
            subprocess.run(['docker', 'rm', '-fv', target], check=True, stdout=subprocess.DEVNULL)
