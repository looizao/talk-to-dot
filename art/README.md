# Dot-style icon artwork

These original SVG illustrations were created for Talk to Dot and are covered by the repository’s MIT license. The frog was visually guided by Zip’s rounded green character and bow tie; these are not official OpenAI assets.

`art/icons/*.svg` are the editable sources. `scripts/render-icons.py` renders 512×512 PNG files into `app/res/drawable-nodpi/`, using CairoSVG. The generated PNGs are committed, so Android builds and CI do not require a rendering dependency. See the main README for regeneration commands.

The Classic D option retains the vector drawable from v1.2.1 in `app/res/drawable/icon.xml`.
