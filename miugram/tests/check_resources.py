#!/usr/bin/env python3
"""Check the initial Miugram resource additions; not a substitute for a build."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
NAMES = ('MiugramSettings', 'MiugramAccountInfo', 'MiugramAccountId', 'MiugramAccountDc',
         'MiugramSettingsInfo', 'MiugramAppIcons', 'MiugramIconPlane',
         'MiugramIconSleep', 'MiugramIconM', 'MiugramIconEnvelope')
for locale in ('values', 'values-ru'):
    path = ROOT / 'TMessagesProj/src/main/res' / locale / 'strings.xml'
    root = ET.parse(path).getroot()
    for name in NAMES:
        matches = [node for node in root if node.get('name') == name]
        assert len(matches) == 1 and (matches[0].text or '').strip(), f'{path}: invalid {name}'
    print(f'{locale}: Miugram strings OK')
