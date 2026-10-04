#!/usr/bin/env python3
"""Synchronize shared skill files from .agents/skills to .claude/skills."""

import argparse
import sys
from pathlib import Path


MIRRORED_SKILLS = (
    "run-appium-suite",
    "verify-sandbox-state",
    "gen-api-test",
    "gen-mobile-test",
    "test-repair",
    "automate-test-case",
)


def sync_skills(root: Path, check: bool) -> list[str]:
    """Return synchronization problems and optionally repair mirror files."""
    drift = []
    for name in MIRRORED_SKILLS:
        source = root / ".agents" / "skills" / name / "SKILL.md"
        target = root / ".claude" / "skills" / name / "SKILL.md"

        if not source.is_file():
            drift.append(f"missing canonical skill: {source.relative_to(root)}")
            continue

        source_bytes = source.read_bytes()
        if target.is_file() and target.read_bytes() == source_bytes:
            continue

        drift.append(f"out of sync: {target.relative_to(root)}")
        if not check:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(source_bytes)

    return drift


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="report drift without changing files",
    )
    args = parser.parse_args(argv)
    root = Path(__file__).resolve().parents[1]
    drift = sync_skills(root, args.check)

    if args.check:
        if drift:
            print("Agent skills are not synchronized:")
            for item in drift:
                print(f"- {item}")
            print("Run: python scripts/sync_agent_skills.py")
            return 1
        print(f"Agent skills are synchronized ({len(MIRRORED_SKILLS)} checked).")
        return 0

    for item in drift:
        print(item.replace("out of sync:", "synchronized:"))
    print(f"Agent skill sync complete ({len(MIRRORED_SKILLS)} checked).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
