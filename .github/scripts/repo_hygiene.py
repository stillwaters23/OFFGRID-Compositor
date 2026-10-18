#!/usr/bin/env python3
"""Fail the build on new voice weights and on obvious secrets.

Allowlist, by exact path, of the only WAV files already in git:
  06 Prompts and Skills/Rex Voice/Rex 01.wav
  06 Prompts and Skills/Rex Voice/Rex 02.wav
  06 Prompts and Skills/Rex Voice/Rex 03.wav
They move to the private OFFGRID_Voice_Assets repo after Royalty decides
about history. Do not add a path here to land a new recording or a model.
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

# Allowlist, by exact path, of the only WAV files already in git.
# They move to the private OFFGRID_Voice_Assets repo after Royalty decides about history.
ALLOWED_WAV_PATHS = frozenset(
    {
        "06 Prompts and Skills/Rex Voice/Rex 01.wav",
        "06 Prompts and Skills/Rex Voice/Rex 02.wav",
        "06 Prompts and Skills/Rex Voice/Rex 03.wav",
    }
)

BLOCKED_EXTENSIONS = frozenset({".wav", ".gguf", ".onnx", ".pt", ".safetensors"})

BINARY_EXTENSIONS = frozenset(
    {
        ".wav",
        ".ttf",
        ".otf",
        ".png",
        ".jpg",
        ".jpeg",
        ".webp",
        ".gif",
        ".tflite",
        ".apk",
        ".so",
        ".jar",
        ".zip",
        ".aar",
        ".mp4",
        ".mp3",
        ".pdf",
        ".ico",
        ".bin",
        ".gguf",
        ".onnx",
        ".pt",
        ".safetensors",
        ".woff",
        ".woff2",
    }
)

# Headers are split so this file does not contain a contiguous private-key block.
_PEM_HEADER = "-----BEGIN " + "(?:ENCRYPTED |RSA |DSA |EC |OPENSSH )?PRIVATE KEY-----"
_PGP_HEADER = "-----BEGIN " + "PGP PRIVATE KEY BLOCK-----"

SECRET_PATTERNS = (
    ("private key", re.compile(_PEM_HEADER + "|" + _PGP_HEADER)),
    ("aws access key id", re.compile(r"\bAKIA[0-9A-Z]{16}\b")),
    ("github token", re.compile(r"\b(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{36}\b")),
    ("github fine-grained token", re.compile(r"\bgithub_pat_[A-Za-z0-9_]{22,}\b")),
    ("slack token", re.compile(r"\bxox[baprs]-[A-Za-z0-9-]{10,}\b")),
    ("google api key", re.compile(r"\bAIza[0-9A-Za-z\-_]{35}\b")),
    ("stripe secret key", re.compile(r"\bsk_(?:live|test)_[A-Za-z0-9]{16,}\b")),
    ("openai-style api key", re.compile(r"\bsk-[A-Za-z0-9]{20,}\b")),
    (
        "quoted api key or token",
        re.compile(
            r"(?i)\b(?:api[_-]?key|apikey|secret[_-]?key|access[_-]?token|"
            r"private[_-]?key|stream[_-]?key|password|passwd)\b"
            r"\s*[:=]\s*['\"][^'\"]{8,}['\"]"
        ),
    ),
)


def repo_root() -> Path:
    out = subprocess.check_output(
        ["git", "rev-parse", "--show-toplevel"],
        text=True,
    )
    return Path(out.strip())


def tracked_files(root: Path) -> list[str]:
    # Cached files plus untracked files that are not ignored, so a local run sees
    # the same set CI will see once the change is committed.
    out = subprocess.check_output(
        ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"],
        cwd=root,
    )
    return [name.decode() for name in out.split(b"\0") if name]


def asset_findings(paths: list[str]) -> list[str]:
    findings: list[str] = []
    wavs: list[str] = []
    for path in paths:
        extension = Path(path).suffix.lower()
        if extension not in BLOCKED_EXTENSIONS:
            continue
        if extension == ".wav":
            wavs.append(path)
            if path not in ALLOWED_WAV_PATHS:
                findings.append(f"New WAV file is not allowlisted: {path}")
            continue
        findings.append(f"Model file is not allowed in git: {path}")
    for path in sorted(ALLOWED_WAV_PATHS.difference(wavs)):
        findings.append(f"Allowlisted WAV is no longer tracked: {path}")
    return findings


def looks_binary(path: Path) -> bool:
    if path.suffix.lower() in BINARY_EXTENSIONS:
        return True
    try:
        sample = path.open("rb").read(4096)
    except OSError:
        return True
    return b"\0" in sample


def findings_in_lines(lines: list[str], label: str) -> list[str]:
    findings: list[str] = []
    for number, line in enumerate(lines, 1):
        for kind, pattern in SECRET_PATTERNS:
            if pattern.search(line):
                findings.append(f"{label}:{number}: {kind}")
                break
    return findings


def secret_findings(root: Path, paths: list[str]) -> list[str]:
    findings: list[str] = []
    for relative in paths:
        path = root / relative
        if not path.is_file() or looks_binary(path):
            continue
        try:
            lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
        except OSError:
            continue
        findings.extend(findings_in_lines(lines, relative))
    return findings


def detector_self_test() -> list[str]:
    """The detector has to fire on built samples, and stay quiet on the workflow regex."""
    problems: list[str] = []
    sample_header = "-----BEGIN " + "PRIVATE KEY-----"
    aws_key = "AKIA" + ("A1B2C3D4" * 2)
    quoted = "api" + "_key = " + '"' + ("a" * 16) + '"'
    benign = "private var secret = SecretKeySpec(keyBytes, " + '"AES"' + ")"
    workflow_regex = (
        r"(?i)(api[_-]?key|secret|password|passwd|stream[_-]?key|access[_-]?token)"
        r"\s*[:=]\s*['\"][^'\"]{6,}['\"]"
    )
    samples = {
        "private key": sample_header,
        "aws access key id": "id " + aws_key,
        "quoted api key or token": quoted,
    }
    for kind, text in samples.items():
        hits = [name for name, pattern in SECRET_PATTERNS if pattern.search(text)]
        if kind not in hits:
            problems.append(f"self-test missed {kind}")
    if any(pattern.search(benign) for _, pattern in SECRET_PATTERNS):
        problems.append("self-test flagged a benign secret variable")
    if any(pattern.search(workflow_regex) for _, pattern in SECRET_PATTERNS):
        problems.append("self-test flagged the existing workflow regex")
    own_source = Path(__file__).read_text(encoding="utf-8")
    own_hits = findings_in_lines(own_source.splitlines(), "repo_hygiene.py")
    if own_hits:
        problems.extend(f"self-test flagged this script: {hit}" for hit in own_hits)
    extra = asset_findings(
        [
            "06 Prompts and Skills/Rex Voice/Rex 01.wav",
            "06 Prompts and Skills/Rex Voice/Rex 02.wav",
            "06 Prompts and Skills/Rex Voice/Rex 03.wav",
            "tmp/new.wav",
            "models/rex.onnx",
            "models/rex.gguf",
            "models/rex.pt",
            "models/rex.safetensors",
        ]
    )
    if len(extra) != 5:
        problems.append(f"self-test asset count was {len(extra)}: {extra}")
    clean = asset_findings(sorted(ALLOWED_WAV_PATHS))
    if clean:
        problems.append(f"self-test rejected the allowlist: {clean}")
    return problems


def workflow_lists_allowlist(root: Path) -> list[str]:
    workflow = root / ".github" / "workflows" / "build-debug-apk.yml"
    text = workflow.read_text(encoding="utf-8")
    return [path for path in sorted(ALLOWED_WAV_PATHS) if path not in text]


def main() -> int:
    problems = detector_self_test()
    root = repo_root()
    missing_from_workflow = workflow_lists_allowlist(root)
    for path in missing_from_workflow:
        problems.append(f"Workflow comment is missing allowlisted path: {path}")
    paths = tracked_files(root)
    problems.extend(asset_findings(paths))
    problems.extend(secret_findings(root, paths))
    if problems:
        print("Voice asset and secret check failed:")
        print("\n".join(problems))
        return 1
    print("Voice asset and secret check passed.")
    print("Allowlisted WAV files, by exact path:")
    for path in sorted(ALLOWED_WAV_PATHS):
        print(f"  {path}")
    print(
        "Those files move to the private OFFGRID_Voice_Assets repo "
        "after Royalty decides about history."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
