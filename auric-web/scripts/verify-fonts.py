#!/usr/bin/env python3
"""Verify the catalog's exact CSS requests and Latin font binaries; no rendering claim."""
import concurrent.futures
import hashlib
import io
import json
from pathlib import Path
import re
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parent.parent
catalog = json.loads((ROOT / 'artifacts/qa/font-catalog.json').read_text())

def verify(face):
    url = 'https://fonts.googleapis.com/css2?family=' + urllib.parse.quote(face['family'], safe='') + ':wght@' + str(face['weight']) + '&display=swap'
    result = {'family': face['family'], 'weight': face['weight'], 'stylesheet': url}
    try:
        with urllib.request.urlopen(url, timeout=20) as response:
            css = response.read().decode()
            result['cssStatus'] = response.status
        urls = re.findall(r'url\((https://fonts\.gstatic\.com/[^)]+)\)', css)
        if not urls or not re.search(r'font-weight:\s*' + str(face['weight']) + r'\b', css):
            raise ValueError('Expected family weight or binary URL missing')
        # Google places the base Latin subset last when returning multiple blocks.
        with urllib.request.urlopen(urls[-1], timeout=20) as response:
            data = response.read()
            result['binaryStatus'] = response.status
        if data[:4] not in [b'wOF2', b'wOFF', b'\x00\x01\x00\x00', b'OTTO']:
            raise ValueError('Unexpected font file signature')
        result.update(bytes=len(data), sha256=hashlib.sha256(data).hexdigest())
        try:
            from fontTools.ttLib import TTFont
            font = TTFont(io.BytesIO(data))
            cmap = font.getBestCmap()
            missing = [c for c in '0123456789:.' if ord(c) not in cmap]
            if missing: raise ValueError('Missing required clock glyphs: ' + ''.join(missing))
            result['clockGlyphs'] = 'verified'
        except ImportError:
            result['clockGlyphs'] = 'fontTools unavailable; not checked'
        result['ok'] = True
    except Exception as error:
        result.update(ok=False, error=str(error))
    return result

if __name__ == '__main__':
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        results = list(pool.map(verify, catalog['families']))
    out = ROOT / 'artifacts/qa/font-network-results.json'
    out.write_text(json.dumps({'scope': 'CSS and binary retrieval, not browser rendering', 'results': results}, indent=2) + '\n')
    print(json.dumps({'checked': len(results), 'passed': sum(r['ok'] for r in results), 'failed': [r for r in results if not r['ok']]}))
    raise SystemExit(0 if all(r['ok'] for r in results) else 1)
