#!/usr/bin/env python3
"""Palette sanity and contrast; a UI/device test is still required."""
from pathlib import Path
root = Path(__file__).resolve().parents[2]
path = root / 'TMessagesProj/src/main/assets/miugram.attheme'
colors = {}
for line in path.read_text().splitlines():
    if '=' in line:
        key, value = line.split('=', 1)
        assert key not in colors, key
        colors[key] = int(value, 16) if value.startswith('#') else int(value)
assert len(colors) >= 500
source = (root / 'TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java').read_text()
assert 'themeInfo.assetName = "miugram.attheme"' in source
assert 'themesDict.put("Miugram", currentNightTheme = themeInfo)' in source

def luminance(color):
    c = [((color >> shift) & 255) / 255 for shift in (16, 8, 0)]
    c = [v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4 for v in c]
    return 0.2126*c[0] + 0.7152*c[1] + 0.0722*c[2]
for fg, bg in [('windowBackgroundWhiteBlackText', 'windowBackgroundWhite'),
               ('chat_messageTextIn', 'chat_inBubble'),
               ('chat_messageTextOut', 'chat_outBubble'),
               ('windowBackgroundWhiteBlueText', 'windowBackgroundWhite'),
               ('chats_message', 'windowBackgroundWhite')]:
    assert ((colors[fg] >> 24) & 255) == 255 and ((colors[bg] >> 24) & 255) == 255
    hi, lo = sorted((luminance(colors[fg]), luminance(colors[bg])), reverse=True)
    contrast = (hi + .05) / (lo + .05)
    assert contrast >= 4.5, (fg, bg, contrast)
    print(f'{fg}/{bg}: {contrast:.2f}:1')
print('Miugram theme palette checks passed; actual UI is unverified.')
