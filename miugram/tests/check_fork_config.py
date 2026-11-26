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
standalone = read("TMessagesProj_AppStandalone/build.gradle")
assert "MIUGRAM_ANDROID_KEYSTORE_PATH" in standalone and "MIUGRAM_ANDROID_APPLICATION_ID" in standalone
assert "new groovy.json.JsonSlurper().parse(firebaseFile)" in standalone
assert "firebaseProject == 'tmessages2'" in standalone
assert "miugramApplicationId + '.web'" in standalone and 'mobilesdk_app_id' in standalone
assert "require a private signing keystore" in standalone and "require an independent application ID" in standalone
assert 'storeFile file("../TMessagesProj/config/release.keystore")' not in standalone.split("        release {", 1)[1].split("        }", 1)[0]
candidate = read('.github/workflows/miugram-android-candidate.yml')
assert 'workflow_dispatch:' in candidate and 'MIUGRAM_ANDROID_FIREBASE_CONFIG_B64' in candidate
assert 'apksigner" verify' in candidate and 'manifest application-id' in candidate
assert 'upload-artifact@v4' in candidate and 'gh release' not in candidate
print("Fork API, signing gates, update and profile-link static checks passed (runtime unverified).")
