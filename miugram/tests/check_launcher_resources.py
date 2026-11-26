#!/usr/bin/env python3
"""Static consistency checks; launcher/device behavior still requires testing."""
from pathlib import Path
import struct
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'TMessagesProj/src/main/res'
ANDROID = '{http://schemas.android.com/apk/res/android}'
main = ET.parse(ROOT / 'TMessagesProj/src/main/AndroidManifest.xml').getroot()
standalone = ET.parse(ROOT / 'TMessagesProj/config/release/AndroidManifest_standalone.xml').getroot()
for name, number in [('DefaultIcon', 1), ('VintageIcon', 2), ('AquaIcon', 3), ('PremiumIcon', 4)]:
    for manifest in [main] + ([standalone] if number > 1 else []):
        aliases = [a for a in manifest.findall('./application/activity-alias')
                   if a.get(ANDROID + 'name') == 'org.telegram.messenger.' + name]
        assert len(aliases) == 1, name
        for attr in ('icon', 'roundIcon'):
            assert aliases[0].get(ANDROID + attr) == f'@mipmap/miugram_launcher_{number}'
    for path, dimensions in [
        (RES / f'mipmap-nodpi/miugram_launcher_{number}.png', (384, 384)),
        (RES / f'drawable-nodpi/miugram_icon_fg_{number}.png', (432, 432)),
    ]:
        data = path.read_bytes()
        assert data[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II', data[16:24]) == dimensions, path
    adaptive = ET.parse(RES / f'mipmap-anydpi-v26/miugram_launcher_{number}.xml').getroot()
    assert adaptive.find('foreground').get(ANDROID + 'drawable') == f'@drawable/miugram_icon_fg_{number}'
    assert adaptive.find('background').get(ANDROID + 'drawable') == f'@drawable/miugram_icon_bg_{number}'
    ET.parse(RES / f'drawable/miugram_icon_bg_{number}.xml')
print('Four launcher aliases and resources are consistent.')
