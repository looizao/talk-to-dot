#!/usr/bin/env python3
"""Regenerate committed PNG icons from the original SVG artwork. Requires CairoSVG."""
from pathlib import Path
import cairosvg
root=Path(__file__).resolve().parent.parent
for source in sorted((root/'art/icons').glob('*.svg')):
    target=root/'app/res/drawable-nodpi'/('icon_'+source.stem+'.png')
    target.parent.mkdir(parents=True,exist_ok=True)
    cairosvg.svg2png(url=str(source),write_to=str(target),output_width=512,output_height=512)
    print(target.relative_to(root))
