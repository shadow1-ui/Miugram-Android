#!/usr/bin/env python3
"""Static profile-background guardrails; not an Android runtime test."""
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[2]
src=root/'TMessagesProj/src/main/java/org/telegram'
helper=(src/'messenger/MiugramProfileBackground.java').read_text()
settings=(src/'ui/SettingsActivity.java').read_text()
profile=(src/'ui/ProfileActivity.java').read_text()
assert 'getNoBackupFilesDir()' in helper and 'accountId <= 0' in helper
assert 'AtomicFile' in helper and '.delete()' in helper
assert 'ProfileActivity.of(account)' in settings
assert 'photoUri' in settings and 'freeform' in settings and 'ACTION_OPEN_DOCUMENT' in settings
assert 'showMiugramBackgroundConfirmation(account, copy, 0)' in settings
confirm=settings[settings.index('private void showMiugramBackgroundConfirmation'):settings.index('public void onActivityResultFragment', settings.index('private void showMiugramBackgroundConfirmation'))]
assert confirm.index('builder.setPositiveButton') < confirm.index('MiugramProfileBackground.save(account, image)')
assert 'if (!saving[0] && !image.isRecycled()) image.recycle()' in confirm
assert 'myProfile || userId == account' in profile and 'MiugramProfileBackground.load(account)' in profile
res=root/'TMessagesProj/src/main/res'
for language in ('values','values-ru'):
    names=[e.get('name') for e in ET.parse(res/language/'strings.xml').getroot()]
    for n in ('MiugramProfileBackground','MiugramProfileBackgroundChoose','MiugramProfileBackgroundRemove','MiugramProfileBackgroundPreview','MiugramProfileBackgroundConfirm','MiugramProfileBackgroundSaved','MiugramProfileBackgroundFailed'):
        assert names.count(n)==1, (language,n)
print('Profile background static checks passed; actual device behavior is unverified.')
