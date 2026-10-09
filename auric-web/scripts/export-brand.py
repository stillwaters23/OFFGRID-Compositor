#!/usr/bin/env python3
"""Mechanical PNG exports from the locked master and approved icon derivative.

Requires Python 3 and Pillow. Does not call a model or redraw the master.
The tiny favicon/badge use the existing faceted-diamond visual vocabulary as
code-native geometry; a full wordmark is illegible at favicon/badge size.
"""
from pathlib import Path
import hashlib
import json
import math
import os
import tempfile
from PIL import Image, ImageDraw, ImageOps

ROOT = Path(__file__).resolve().parent.parent
PUBLIC = ROOT / 'public'
BRAND = PUBLIC / 'brand'
LANCZOS = Image.Resampling.LANCZOS

def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    # An interrupted export must leave the last complete asset in place.
    with tempfile.NamedTemporaryFile(dir=path.parent, suffix='.png', delete=False) as file:
        temporary = Path(file.name)
    try:
        image.save(temporary, format='PNG', optimize=True)
        with Image.open(temporary) as check:
            check.verify()
        os.replace(temporary, path)
    finally:
        temporary.unlink(missing_ok=True)

def contain(image, size, padding=0):
    canvas = Image.new('RGB', size, 'black')
    art = ImageOps.contain(image.convert('RGB'), (size[0]-padding*2, size[1]-padding*2), LANCZOS)
    canvas.paste(art, ((size[0]-art.width)//2, (size[1]-art.height)//2))
    return canvas

def small_mark(size, monochrome=False):
    # Vector-like source geometry, rendered large before antialiasing.
    n = 384
    image = Image.new('RGBA', (n, n), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    def points(values): return [(round(x*n/32), round(y*n/32)) for x,y in values]
    outline = [(8,5),(24,5),(29,12),(16,28),(3,12)]
    if monochrome:
        draw.line(points(outline+[outline[0]]), fill='white', width=24, joint='curve')
        draw.line(points([(3,12),(29,12)]), fill='white', width=16)
        draw.line(points([(8,5),(16,28),(24,5)]), fill='white', width=16)
    else:
        for poly, color in [
            ([(8,5),(24,5),(29,12),(3,12)], '#95dfff'),
            ([(8,5),(13,5),(9,12),(3,12)], '#237acc'),
            ([(19,5),(24,5),(29,12),(23,12)], '#0a438c'),
            ([(13,5),(19,5),(23,12),(9,12)], '#e3f8ff'),
            ([(3,12),(16,28),(9,12)], '#138edf'),
            ([(9,12),(16,28),(23,12)], '#66d0ff'),
            ([(23,12),(16,28),(29,12)], '#154e9c'),
        ]: draw.polygon(points(poly), fill=color)
        draw.line(points(outline+[outline[0]]), fill='#d9f5ff', width=9, joint='curve')
    return image.resize((size, size), LANCZOS)

def main():
    master_path = BRAND / 'auric_777_diamond_crown_emblem.png'
    icon_path = BRAND / 'auric-launcher-source.png'
    master = Image.open(master_path)
    icon = Image.open(icon_path).convert('RGB')
    # Both source images retain their supplied square proportions.
    assert master.width == master.height and icon.width == icon.height
    save(contain(master, (768,768), 20), BRAND / 'auric-masthead.png')
    save(contain(master, (1200,630), 30), PUBLIC / 'og.png')
    for size, name in [(180,'__grok/icon-180.png'),(192,'icon-192.png'),(512,'icon-512.png'),(192,'brand/notification-192.png')]:
        save(contain(icon, (size,size)), PUBLIC / name)
    # Fit every visible source pixel (>32 channel intensity) inside radius 0.38N,
    # stricter than the manifest maskable safe circle of radius 0.40N.
    center = (icon.width-1)/2
    radius = max(math.hypot(x-center,y-center) for y in range(icon.height)
                 for x in range(icon.width) if max(icon.getpixel((x,y))) > 32)
    extent = min(512, math.floor(icon.width * (512*0.38)/radius))
    canvas = Image.new('RGB', (512,512), 'black')
    art = icon.resize((extent,extent), LANCZOS)
    canvas.paste(art, ((512-extent)//2,(512-extent)//2))
    save(canvas, BRAND / 'icon-maskable-512.png')
    save(small_mark(16), BRAND / 'favicon-16.png')
    save(small_mark(32), BRAND / 'favicon-32.png')
    save(small_mark(96, monochrome=True), BRAND / 'notification-badge-96.png')
    provenance = {
        'identity': 'AURIC 777',
        'masterFile': str(master_path.relative_to(ROOT)),
        'masterSHA256': hashlib.sha256(master_path.read_bytes()).hexdigest(),
        'masterMode': master.mode,
        'originalFile': 'public/brand/auric_777_diamond_crown_emblem.original.jpg',
        'originalSHA256': hashlib.sha256((BRAND/'auric_777_diamond_crown_emblem.original.jpg').read_bytes()).hexdigest(),
        'originalFormat': 'JPEG bytes supplied with .png filename; canonical PNG preserves every decoded RGB pixel',
        'masterPreserved': True,
        'displayTreatment': 'Original opaque master, contained on black glass; no checkerboard cutout used.',
        'launcherSource': str(icon_path.relative_to(ROOT)),
        'launcherSourceSHA256': hashlib.sha256(icon_path.read_bytes()).hexdigest(),
        'launcherMethod': 'Built-in image generation; icon-only simplification of supplied 777 master. Full prompt in public/brand/README.md.',
        'exports': 'Pillow contain/Lanczos PNG size exports; no stretching or master redraw.',
        'smallMarks': 'Code-native faceted diamond for 16/32px favicon and 96px white-alpha notification badge.',
        'maskableVisibleRadiusLimit': 0.4,
    }
    (BRAND / 'provenance.json').write_text(json.dumps(provenance, indent=2)+'\n')
    print('PNG brand exports generated from preserved sources.')

if __name__ == '__main__': main()
