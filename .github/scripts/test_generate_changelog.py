from pathlib import Path
import tempfile
import unittest

import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
import generate_changelog as gc


class GenerateChangelogTest(unittest.TestCase):

  def test_empty_version_section_gets_auto_filled_from_unreleased_or_commits(self):
    with tempfile.TemporaryDirectory() as tmp:
      root = Path(tmp)
      changelog = root / "CHANGELOG.md"
      changelog.write_text(
        "# Changelog\n\n"
        "## [Unreleased]\n\n"
        "### Added\n"
        "- Auto-detect installed Lock Screen apps and adaptive Preferences fallback.\n\n"
        "## [1.1.0] - 2026-11-18\n\n"
        "## [1.0.0] - 2026-11-17\n\n"
        "### Added\n"
        "- Initial release.\n",
        encoding="utf-8",
      )

      notes = gc.generate_release_notes(
        tag="v1.1.0",
        changelog_path=changelog,
        manual_notes="",
        repo_root=root,
        fill_changelog=True,
        release_date="2026-11-18",
      )

      self.assertIn("Auto-detect installed Lock Screen apps", notes)
      updated = changelog.read_text(encoding="utf-8")
      self.assertIn("## [1.1.0] - 2026-11-18\n\n### Added\n- Auto-detect installed Lock Screen apps", updated)

  def test_empty_changelog_and_empty_manual_notes_falls_back_cleanly(self):
    with tempfile.TemporaryDirectory() as tmp:
      root = Path(tmp)
      changelog = root / "CHANGELOG.md"
      changelog.write_text("# Changelog\n\n## [2.0.0] - 2026-12-01\n\n<!-- empty -->\n", encoding="utf-8")

      notes = gc.generate_release_notes(
        tag="v2.0.0",
        changelog_path=changelog,
        manual_notes="   ",
        repo_root=root,
        fill_changelog=True,
        release_date="2026-12-01",
      )

      self.assertIn("Glassbon v2.0.0", notes)
      self.assertIn("Glassbon-v2.0.0-release.apk", notes)
      updated = changelog.read_text(encoding="utf-8")
      self.assertIn("### Release Highlights", updated)

  def test_manual_notes_take_priority_over_everything(self):
    with tempfile.TemporaryDirectory() as tmp:
      root = Path(tmp)
      changelog = root / "CHANGELOG.md"
      changelog.write_text("# Changelog\n\n## [1.0.1]\n\n### Fixed\n- Old line.\n", encoding="utf-8")

      notes = gc.generate_release_notes(
        tag="v1.0.1",
        changelog_path=changelog,
        manual_notes="### Fixed\n- Hotfix shipped cleanly.",
        repo_root=root,
        fill_changelog=False,
      )

      self.assertIn("Hotfix shipped cleanly.", notes)
      self.assertNotIn("Old line.", notes)


if __name__ == "__main__":
  unittest.main()
