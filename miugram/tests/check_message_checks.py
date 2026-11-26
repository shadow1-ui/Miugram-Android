#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[2]
res=root/'TMessagesProj/src/main/res'
ns='{http://schemas.android.com/apk/res/android}'
for name in ('miugram_check_rounded','miugram_halfcheck_rounded'):
    vector=ET.parse(res/'drawable'/f'{name}.xml').getroot()
    assert vector.tag=='vector' and vector.attrib[ns+'width']=='12dp' and vector.attrib[ns+'height']=='12dp'
    path=vector.find('path');assert path is not None and path.attrib[ns+'strokeLineCap']=='round'
for lang in ('values','values-ru'):
    tree=ET.parse(res/lang/'strings.xml')
    names=[x.attrib.get('name') for x in tree.getroot()]
    for n in ('MiugramChecks','MiugramChecksClassic','MiugramChecksRounded','MiugramChecksRestart'):
        assert names.count(n)==1, (lang,n)
theme=(root/'TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java').read_text()
settings=(root/'TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java').read_text()
assert 'miugram_rounded_checks' in theme and 'miugram_rounded_checks' in settings
print('Checkmark resources and settings passed static checks; runtime unverified.')
