#!/usr/bin/env python3
"""Print bounded CI evidence from technical test artifacts; never reads personal data or keys."""
import argparse
import base64
import io
import json
from pathlib import Path
import xml.etree.ElementTree as ET

parser=argparse.ArgumentParser();parser.add_argument('--preview',action='store_true');args=parser.parse_args()
roots=['domain/build/test-results','data/build/outputs/androidTest-results','app/build/outputs/androidTest-results']
for root in roots:
    for path in sorted(Path(root).rglob('TEST-*.xml')):
        suite=ET.parse(path).getroot()
        print('SOPHIA_TEST_REPORT '+json.dumps(dict(path=str(path),tests=suite.get('tests'),failures=suite.get('failures'),errors=suite.get('errors'),skipped=suite.get('skipped'))))
for path in Path('.').glob('*/build/reports/lint-results-debug.xml'):
    root=ET.parse(path).getroot();severity=[issue.get('severity') for issue in root.findall('issue')]
    print('SOPHIA_LINT_REPORT '+json.dumps(dict(module=path.parts[0],errors=severity.count('Error')+severity.count('Fatal'),warnings=severity.count('Warning'))))
if args.preview:
    from PIL import Image, ImageOps, ImageDraw
    names=['01-inicio','14-categorias','16-referencias-hinduismo','15-historia-hermetismo']
    sheet=Image.new('RGB',(320*len(names),720),'#e9ecf5');draw=ImageDraw.Draw(sheet)
    for index,name in enumerate(names):
        path=Path('screenshots')/(name+'.png')
        draw.text((index*320+8,8),name,fill='black')
        if path.exists():
            image=Image.open(path).convert('RGB');image.thumbnail((312,680))
            sheet.paste(image,(index*320+(320-image.width)//2,30))
    output=io.BytesIO();sheet.save(output,format='PNG',optimize=True)
    Path('screenshots/revisao-visual.png').write_bytes(output.getvalue())
    print('SOPHIA_PREVIEW_PNG '+base64.b64encode(output.getvalue()).decode())
