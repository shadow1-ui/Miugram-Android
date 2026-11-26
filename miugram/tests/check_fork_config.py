#!/usr/bin/env python3
"""Static configuration guardrails, not a build or runtime test."""
from pathlib import Path

root = Path(__file__).resolve().parents[2]
def read(path):
    return (root / path).read_text(encoding="utf-8")

gradle = read("TMessagesProj/build.gradle")
vars = read("TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java")
launch = read("TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java")
profile = read("TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java")
assert "System.getenv('MIUGRAM_TELEGRAM_API_ID')" in gradle
assert "System.getenv('MIUGRAM_TELEGRAM_API_HASH')" in gradle
assert "Integer.MAX_VALUE" in gradle and "miugramApiHash.isEmpty()" in gradle
assert 'APP_ID = BuildConfig.MIUGRAM_API_ID' in vars
assert 'APP_HASH = BuildConfig.MIUGRAM_API_HASH' in vars
assert 'CHECK_UPDATES = false' in vars and 'SUPPORTS_PASSKEYS = false' in vars
assert 'if (!BuildVars.CHECK_UPDATES)' in launch
assert 'return showMiugramUsernameMenu(view);' in profile
assert '"tg://user?id=" + user.id' in profile
assert 'getString(R.string.MiugramIdLinkCopied)' in profile
print("Fork API, update and profile-link static checks passed (runtime unverified).")
