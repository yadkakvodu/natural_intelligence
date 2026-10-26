#!/usr/bin/env python3
"""Repair a pg_dump plain-SQL file damaged by PowerShell redirection.

`pg_dump ... > backup.sql` in Windows PowerShell 5.1 decodes the UTF-8 output with
the console code page (CP866 on a Russian Windows) and writes UTF-16LE with BOM.
The result is unreadable for psql ("invalid byte sequence for encoding UTF8").

This script reverses both steps losslessly:
    UTF-16LE text -> CP866 bytes -> UTF-8 text

Usage:
    python3 fix_backup.py broken.sql fixed.sql
"""

import argparse
import sys


def repair(data: bytes) -> bytes:
    text = data.decode("utf-16")  # BOM-aware; raises on invalid input
    text = text.replace("\r\n", "\n")

    repaired = []
    skipped = 0
    for line in text.split("\n"):
        if line.isascii():
            repaired.append(line)
            continue
        try:
            repaired.append(line.encode("cp866").decode("utf-8"))
        except (UnicodeEncodeError, UnicodeDecodeError):
            # Already valid UTF-8 text (not CP866 mojibake) - keep as is.
            repaired.append(line)
            skipped += 1

    if skipped:
        print(f"[warn] {skipped} non-ASCII lines were kept as-is", file=sys.stderr)
    return "\n".join(repaired).encode("utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("src", help="broken pg_dump file (UTF-16LE + CP866 mojibake)")
    parser.add_argument("dst", help="output file (UTF-8, LF)")
    args = parser.parse_args()

    with open(args.src, "rb") as fh:
        data = fh.read()

    if not data.startswith(b"\xff\xfe"):
        print(f"[warn] {args.src} does not start with a UTF-16LE BOM", file=sys.stderr)

    fixed = repair(data)
    with open(args.dst, "wb") as fh:
        fh.write(fixed)

    print(f"fixed: {args.src} -> {args.dst} ({len(data)} -> {len(fixed)} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
