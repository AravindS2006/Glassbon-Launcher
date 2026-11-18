#!/usr/bin/env python3
"""Generate non-empty GitHub Release notes and fill empty CHANGELOG.md sections.

Priority order when resolving release notes for a tag (e.g. v1.2.0):
1. Explicit non-empty --manual-notes (e.g. from workflow_dispatch input).
2. Non-empty version section in CHANGELOG.md matching the tag (e.g. ## [1.2.0]).
3. Non-empty ## [Unreleased] section in CHANGELOG.md.
4. Auto-generated notes grouped from git commits since the previous tag.
5. Guaranteed default fallback summary so release notes are never blank.
"""

from __future__ import annotations

import argparse
from datetime import date
from pathlib import Path
import re
import subprocess
import sys
from typing import Iterable


HEADER_PATTERN = re.compile(
  r"^##\s+\[?(?P<label>[^\]]+?)\]?(?:\s+-\s+(?P<date>.*))?\s*$"
)
CONVENTIONAL_PATTERN = re.compile(
  r"^(?P<kind>[a-zA-Z]+)(?:\((?P<scope>[^)]+)\))?(?P<breaking>!)?:?\s+(?P<subject>.+)$"
)
HTML_COMMENT_PATTERN = re.compile(r"<!--.*?-->", re.DOTALL)


def normalize_version(tag: str) -> str:
  cleaned = tag.strip()
  if cleaned.lower().startswith("v") and len(cleaned) > 1 and cleaned[1].isdigit():
    return cleaned[1:]
  return cleaned


def strip_empty_markup(text: str) -> str:
  without_comments = HTML_COMMENT_PATTERN.sub("", text)
  lines = [line.rstrip() for line in without_comments.splitlines()]
  while lines and not lines[0].strip():
    lines.pop(0)
  while lines and not lines[-1].strip():
    lines.pop()
  return "\n".join(lines).strip()


def split_changelog_sections(changelog_text: str) -> tuple[str, list[dict[str, str]]]:
  lines = changelog_text.splitlines()
  preamble: list[str] = []
  sections: list[dict[str, str]] = []
  current: dict[str, str] | None = None
  body_lines: list[str] = []

  for line in lines:
    match = HEADER_PATTERN.match(line)
    if match:
      if current is not None:
        current["body"] = "\n".join(body_lines)
        sections.append(current)
      current = {
        "raw_header": line,
        "label": match.group("label").strip(),
        "date": (match.group("date") or "").strip(),
      }
      body_lines = []
    else:
      if current is None:
        preamble.append(line)
      else:
        body_lines.append(line)

  if current is not None:
    current["body"] = "\n".join(body_lines)
    sections.append(current)

  return "\n".join(preamble).rstrip(), sections


def rebuild_changelog(preamble: str, sections: list[dict[str, str]]) -> str:
  chunks: list[str] = []
  if preamble.strip():
    chunks.append(preamble.strip())
  for section in sections:
    body = section["body"].strip()
    if body:
      chunks.append(f"{section['raw_header']}\n\n{body}")
    else:
      chunks.append(section["raw_header"])
  return "\n\n".join(chunks).rstrip() + "\n"


def _matches_version(label: str, version: str) -> bool:
  return normalize_version(label).lower() == normalize_version(version).lower()


def _is_unreleased(label: str) -> bool:
  return label.strip().lower() == "unreleased"


def run_git(args: list[str], cwd: Path) -> tuple[int, str]:
  try:
    proc = subprocess.run(
      ["git", *args],
      cwd=str(cwd),
      capture_output=True,
      text=True,
      check=False,
    )
    return proc.returncode, proc.stdout.strip()
  except OSError:
    return 1, ""


def find_previous_tag(tag: str, repo_root: Path) -> str | None:
  code, desc = run_git(["describe", "--tags", "--abbrev=0", "HEAD^"], repo_root)
  if code == 0 and desc and normalize_version(desc) != normalize_version(tag):
    return desc

  code, tags_out = run_git(["tag", "--sort=-version:refname"], repo_root)
  if code == 0 and tags_out:
    for candidate in tags_out.splitlines():
      candidate = candidate.strip()
      if candidate and candidate != tag and normalize_version(candidate) != normalize_version(tag):
        return candidate
  return None


def collect_commits(tag: str, repo_root: Path) -> tuple[str | None, list[str]]:
  prev_tag = find_previous_tag(tag, repo_root)
  rev_range = f"{prev_tag}..HEAD" if prev_tag else "HEAD"
  code, log_output = run_git(
    ["log", "--no-merges", "--max-count=40", "--pretty=format:%s (%h)", rev_range],
    repo_root,
  )
  if code != 0 or not log_output:
    code, log_output = run_git(
      ["log", "--no-merges", "--max-count=15", "--pretty=format:%s (%h)"],
      repo_root,
    )
  if code != 0 or not log_output:
    return prev_tag, []

  commits: list[str] = []
  for raw_line in log_output.splitlines():
    line = raw_line.strip()
    if not line or "[skip ci]" in line.lower():
      continue
    commits.append(line)
  return prev_tag, commits


def format_commit_sections(commits: Iterable[str], tag: str, prev_tag: str | None) -> str:
  added: list[str] = []
  fixed: list[str] = []
  improved: list[str] = []

  for commit in commits:
    match = CONVENTIONAL_PATTERN.match(commit)
    if match:
      kind = match.group("kind").lower()
      scope = match.group("scope")
      subject = match.group("subject").strip()
      if scope:
        formatted = f"- **{scope}**: {subject}"
      else:
        formatted = f"- {subject}"

      if kind in {"feat", "add", "feature"}:
        added.append(formatted)
      elif kind in {"fix", "bugfix", "hotfix"}:
        fixed.append(formatted)
      else:
        improved.append(formatted)
    else:
      improved.append(f"- {commit}")

  parts: list[str] = []
  if added:
    parts.append("### Added\n" + "\n".join(added))
  if fixed:
    parts.append("### Fixed\n" + "\n".join(fixed))
  if improved:
    parts.append("### Changed & Improved\n" + "\n".join(improved))

  if not parts:
    return build_fallback_body(tag)

  if prev_tag:
    parts.append(f"_Changes included since `{prev_tag}`._")
  return "\n\n".join(parts)


def build_fallback_body(tag: str) -> str:
  version = normalize_version(tag)
  return "\n".join([
    "### Release Highlights",
    f"- Packaged **Glassbon {tag}** (version `{version}`) as an R8-minified, Play-Protect clean release APK.",
    "- Ships with zero manifest `<uses-permission>` declarations and auto-detects installed Android Lock Screen apps with adaptive Preferences fallback.",
    "- Includes butter-smooth Gaussian alphabet wave focus blur, Ultra-Smooth & Low Power profiles, and sculpted `MistHaptics` tactile feedback.",
  ])


def generate_release_notes(
  tag: str,
  changelog_path: Path,
  manual_notes: str = "",
  repo_root: Path | None = None,
  fill_changelog: bool = False,
  release_date: str | None = None,
) -> str:
  root = repo_root or changelog_path.parent
  version = normalize_version(tag)
  date_str = release_date or date.today().isoformat()

  existing_text = changelog_path.read_text(encoding="utf-8") if changelog_path.exists() else ""
  preamble, sections = split_changelog_sections(existing_text)

  version_section = next((s for s in sections if _matches_version(s["label"], version)), None)
  unreleased_section = next((s for s in sections if _is_unreleased(s["label"])), None)

  manual_clean = strip_empty_markup(manual_notes)
  version_body = strip_empty_markup(version_section["body"]) if version_section else ""
  unreleased_body = strip_empty_markup(unreleased_section["body"]) if unreleased_section else ""

  if manual_clean:
    resolved_body = manual_clean
    source = "manual"
  elif version_body:
    resolved_body = version_body
    source = "version_section"
  elif unreleased_body:
    resolved_body = unreleased_body
    source = "unreleased_section"
  else:
    prev_tag, commits = collect_commits(tag, root)
    resolved_body = format_commit_sections(commits, tag, prev_tag)
    source = "git_commits"

  if fill_changelog and changelog_path.exists():
    updated = False
    if version_section is not None and not version_body:
      version_section["body"] = resolved_body
      updated = True
    elif version_section is None:
      new_section = {
        "raw_header": f"## [{version}] - {date_str}",
        "label": version,
        "date": date_str,
        "body": resolved_body,
      }
      insert_at = 0
      if unreleased_section is not None:
        if source == "unreleased_section":
          unreleased_section["body"] = ""
        insert_at = sections.index(unreleased_section) + 1
      sections.insert(insert_at, new_section)
      updated = True
    if updated:
      changelog_path.write_text(rebuild_changelog(preamble, sections), encoding="utf-8")

  header = f"## Glassbon {tag}"
  apk_name = f"Glassbon-{tag}-release.apk"
  footer = (
    "---\n"
    f"**Artifact:** `{apk_name}` (R8-minified, non-debuggable, zero sensitive permissions)"
  )
  return f"{header}\n\n{resolved_body}\n\n{footer}\n"


def main(argv: list[str] | None = None) -> int:
  parser = argparse.ArgumentParser(description="Generate non-empty changelog / GitHub release notes.")
  parser.add_argument("--tag", required=True, help="Release tag, e.g. v1.1.0")
  parser.add_argument("--changelog", default="CHANGELOG.md", help="Path to CHANGELOG.md")
  parser.add_argument("--manual-notes", default="", help="Optional manual release description override")
  parser.add_argument("--output", required=True, help="Destination markdown file path")
  parser.add_argument(
    "--fill-changelog",
    action="store_true",
    help="Fill an empty version entry or promote [Unreleased] inside CHANGELOG.md",
  )
  args = parser.parse_args(argv)

  changelog_path = Path(args.changelog).resolve()
  output_path = Path(args.output)
  output_path.parent.mkdir(parents=True, exist_ok=True)

  notes = generate_release_notes(
    tag=args.tag,
    changelog_path=changelog_path,
    manual_notes=args.manual_notes,
    repo_root=changelog_path.parent,
    fill_changelog=args.fill_changelog,
  )
  output_path.write_text(notes, encoding="utf-8")
  print(f"Generated release notes for {args.tag} -> {output_path} ({len(notes.splitlines())} lines)")
  return 0


if __name__ == "__main__":
  sys.exit(main())
