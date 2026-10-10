#!/usr/bin/env python3
"""Verify source PNG exports, live manifest renderer and static asset references.

This is not a browser-rendering test. Requires Python 3, Pillow and Node.
"""
from pathlib import Path
import hashlib
import json
import math
import re
import subprocess
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
PUBLIC = ROOT / 'public'

def main():
    expected = {
        'brand/auric_777_diamond_crown_emblem.png':(1536,1536),
        'brand/auric-launcher-source.png':(1254,1254),
        'brand/auric-masthead.png':(768,768),
        '__grok/icon-180.png':(180,180), 'icon-192.png':(192,192), 'icon-512.png':(512,512),
        'brand/icon-maskable-512.png':(512,512), 'brand/favicon-16.png':(16,16),
        'brand/favicon-32.png':(32,32), 'brand/notification-192.png':(192,192),
        'brand/notification-badge-96.png':(96,96), 'og.png':(1200,630),
    }
    rows=[]
    for name,size in expected.items():
        path=PUBLIC/name
        with Image.open(path) as check:
            check.verify()
        with Image.open(path) as image:
            image.load()
            assert image.format=='PNG' and image.size==size, (name,image.size,size)
            row={'path':'/'+name,'size':list(size),'mode':image.mode,'bytes':path.stat().st_size,'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}
            if 'A' in image.getbands():
                alpha=image.getchannel('A')
                assert alpha.getextrema()==(0,255),name
                bbox=alpha.point(lambda x:255 if x>16 else 0).getbbox()
                assert bbox and bbox[0]>0 and bbox[1]>0 and bbox[2]<size[0] and bbox[3]<size[1],(name,bbox)
                row['visibleAlphaBounds']=list(bbox)
                if 'badge' in name:
                    assert all(image.getpixel((x,y))[:3]==(255,255,255) for y in range(image.height) for x in range(image.width) if image.getpixel((x,y))[3]>0),name
            elif name!='brand/auric_777_diamond_crown_emblem.png':
                # Art is padded on intentional black; no bright artwork meets an export edge.
                rgb=image.convert('RGB');w,h=rgb.size
                assert max(max(rgb.getpixel((x,y))) for x,y in [(x,0) for x in range(w)]+[(x,h-1) for x in range(w)]+[(0,y) for y in range(h)]+[(w-1,y) for y in range(h)]) < 32,name
            rows.append(row)
    original=PUBLIC/'brand/auric_777_diamond_crown_emblem.original.jpg'
    assert hashlib.sha256(original.read_bytes()).hexdigest()=='0ab8d022504af2fff919b6cd019c5e5a1d81bcdff64b97248e86a5e447b52619','Supplied master bytes were altered'
    assert hashlib.sha256((PUBLIC/'brand/auric-launcher-source.png').read_bytes()).hexdigest()=='65c55114bf6474bf0d5bab39f5f1763b8174a949a9466d4c37101005983b099a','Launcher source bytes were altered'
    assert Image.open(original).convert('RGB').tobytes()==Image.open(PUBLIC/'brand/auric_777_diamond_crown_emblem.png').convert('RGB').tobytes(),'Canonical PNG changed source pixels'
    assert (PUBLIC/'og.png').stat().st_size <= 600*1024,'Share card exceeds platform gate'
    image=Image.open(PUBLIC/'brand/icon-maskable-512.png').convert('RGB')
    outside=sum(1 for y in range(512) for x in range(512)
                if math.hypot(x-255.5,y-255.5)>204.8 and max(image.getpixel((x,y)))>32)
    assert outside==0,'Maskable identity escapes safe circle'
    manifest=json.loads(subprocess.check_output(['node','--input-type=module','-e',
        'import {renderWebManifest} from "./scripts/grok-pwa-shared.mjs"; process.stdout.write(renderWebManifest("auric.example"));'],cwd=ROOT,text=True))
    assert manifest['short_name']=='Auric' and len(manifest['icons'])==3
    for icon in manifest['icons']:
        path=PUBLIC/icon['src'].lstrip('/')
        with Image.open(path) as image: assert icon['sizes']==f'{image.width}x{image.height}' and icon['type']=='image/png'
    identity=json.loads((PUBLIC/'brand/brand.json').read_text())
    for key in ['master','masthead','splash','favicon','notificationIcon','notificationBadge','appleTouchIcon','ogImage']:
        assert (PUBLIC/identity[key].lstrip('/')).is_file(),(key,identity[key])
    sw=(PUBLIC/'sw.js').read_text()
    for key in ['notificationIcon','notificationBadge']: assert identity[key] in sw,key
    references=[]
    for directory in ['src','public']:
        for path in (ROOT/directory).rglob('*'):
            if path.suffix not in ['.tsx','.ts','.js','.css','.html'] or '.test.' in path.name: continue
            for match in re.finditer(r'''["'`\(](/[^\s"'`\)<>]+\.(?:png|jpg|jpeg|webp|svg|ico))(?:[?][^"'`]*)?["'`\)]''',path.read_text()):
                asset=match.group(1)
                assert (PUBLIC/asset.lstrip('/')).is_file(),(str(path.relative_to(ROOT)),asset)
                references.append({'source':str(path.relative_to(ROOT)),'asset':asset})
    result={'ok':True,'scope':'Source image dimensions, alpha, safe margins, manifest generation and local runtime asset references; no rendered browser claim',
            'masterPreserved':True,'images':rows,'manifest':manifest,'maskablePixelsOutsideSafeCircle':outside,'staticAssetReferences':references,
            'mastheadTransparency':'Opaque supplied master intentionally preserved on black glass; no checkerboard cutout used'}
    out=ROOT/'artifacts/qa/brand-verification.json'
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({'ok':True,'images':len(rows),'manifestIcons':len(manifest['icons']),'localAssetReferences':len(references)}))

if __name__=='__main__': main()
