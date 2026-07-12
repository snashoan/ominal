#!/usr/bin/env python3
"""Rewrite a rootfs tarball so hard links become regular files.

Android app-private filesystems reject hard-link creation. PRoot's
link2symlink support handles guest links after extraction, but the bootstrap
archive itself must be extractable by Android's tar implementation.
"""

import argparse
import copy
import io
import tarfile


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("source")
    parser.add_argument("output")
    return parser.parse_args()


def main():
    args = parse_args()
    hardlinks = 0
    regular_files = 0

    with tarfile.open(args.source, "r:*") as source, tarfile.open(args.output, "w:gz") as output:
        for member in source:
            if member.islnk():
                source_file = source.extractfile(member)
                if source_file is None:
                    raise RuntimeError("could not resolve hard link: " + member.name)
                payload = source_file.read()
                replacement = copy.copy(member)
                replacement.type = tarfile.REGTYPE
                replacement.linkname = ""
                replacement.size = len(payload)
                output.addfile(replacement, io.BytesIO(payload))
                hardlinks += 1
            elif member.isfile():
                source_file = source.extractfile(member)
                output.addfile(member, source_file)
                regular_files += 1
            else:
                output.addfile(member)

    print("regular_files=" + str(regular_files))
    print("materialized_hardlinks=" + str(hardlinks))


if __name__ == "__main__":
    main()
