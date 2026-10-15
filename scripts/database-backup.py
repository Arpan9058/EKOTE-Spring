#!/usr/bin/env python3
"""Back up MySQL consistently; restore only into an explicitly selected empty database."""
import argparse
import datetime
import gzip
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('operation', choices=['backup', 'restore'])
parser.add_argument('--container', required=True, help='Owned MySQL Docker container with MYSQL_USER/PASSWORD/DATABASE')
parser.add_argument('--file', required=True, type=Path)
parser.add_argument('--allow-restore', action='store_true', help='Explicitly permit import into the selected empty database')
args = parser.parse_args()
if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]*', args.container):
    parser.error('Invalid container name')
metadata = Path(str(args.file) + '.metadata.json')

def command(script, stdin=None, stdout=None):
    return subprocess.run(['docker', 'exec', '-i', args.container, 'sh', '-c', script], stdin=stdin, stdout=stdout, stderr=subprocess.PIPE, check=False)

def checksum(path):
    digest = hashlib.sha256()
    with path.open('rb') as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()

if args.operation == 'backup':
    if metadata.exists():
        parser.error('Metadata already exists; choose a new backup filename')
    descriptor = os.open(args.file, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    process = None
    try:
        with os.fdopen(descriptor, 'wb') as destination:
            process = subprocess.Popen(['docker', 'exec', args.container, 'sh', '-c',
                'export MYSQL_PWD="$MYSQL_PASSWORD"; exec mysqldump -u"$MYSQL_USER" --single-transaction --quick --routines --events --triggers --hex-blob --no-tablespaces --set-gtid-purged=OFF "$MYSQL_DATABASE"'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            with gzip.GzipFile(fileobj=destination, mode='wb', mtime=0) as compressed:
                shutil.copyfileobj(process.stdout, compressed)
            process.stdout.close()
            error = process.stderr.read()
            if process.wait() != 0:
                raise RuntimeError('Database export failed; no usable backup was produced')
        details = {'format': 'ekote-mysql-gzip-v1', 'created_at': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'sha256': checksum(args.file), 'bytes': args.file.stat().st_size}
        with os.fdopen(os.open(metadata, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as file:
            json.dump(details, file, indent=2)
        print('Consistent backup and SHA-256 metadata created with private file permissions')
    except BaseException:
        if process is not None and process.poll() is None:
            process.terminate()
            process.wait(timeout=10)
        args.file.unlink(missing_ok=True)
        raise
else:
    if not args.allow_restore:
        parser.error('Restore requires --allow-restore and an empty target database')
    details = json.loads(metadata.read_text())
    if details.get('format') != 'ekote-mysql-gzip-v1' or checksum(args.file) != details.get('sha256'):
        parser.error('Backup integrity check failed')
    # Validate the entire gzip stream (including CRC) before any import can occur.
    with gzip.open(args.file, 'rb') as source:
        while source.read(1024 * 1024):
            pass
    result = command('export MYSQL_PWD="$MYSQL_PASSWORD"; exec mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()"', stdout=subprocess.PIPE)
    if result.returncode != 0 or result.stdout.strip() != b'0':
        parser.error('Target is unavailable or nonempty; refusing destructive restore')
    process = subprocess.Popen(['docker', 'exec', '-i', args.container, 'sh', '-c',
        'export MYSQL_PWD="$MYSQL_PASSWORD"; exec mysql -u"$MYSQL_USER" "$MYSQL_DATABASE"'], stdin=subprocess.PIPE, stderr=subprocess.PIPE)
    try:
        with gzip.open(args.file, 'rb') as source:
            shutil.copyfileobj(source, process.stdin)
        process.stdin.close()
        error = process.stderr.read()
        if process.wait() != 0:
            raise RuntimeError('Restore failed; discard the incomplete target and investigate')
    finally:
        if process.poll() is None:
            process.terminate()
            process.wait(timeout=10)
    print('Backup restored into the previously empty database')
