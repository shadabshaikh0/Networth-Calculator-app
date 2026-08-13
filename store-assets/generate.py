#!/usr/bin/env python3
"""Generate the Play Store graphics (macOS fonts).

    python3 -m venv venv && venv/bin/pip install Pillow
    venv/bin/python store-assets/generate.py

Outputs:
    store-assets/play-icon-512.png        (512x512 app icon)
    store-assets/play-feature-1024x500.png (1024x500 feature graphic)
"""
from PIL import Image, ImageDraw, ImageFont

GOLD = (0xD5, 0xB4, 0x75)
DARK = (0x0B, 0x0B, 0x0B)
HELV = "/System/Library/Fonts/Helvetica.ttc"                 # index 1 = Bold, has the ₹ glyph
BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"   # Latin title


def center(dr, cx, cy, text, font, fill):
    l, t, r, b = dr.textbbox((0, 0), text, font=font)
    dr.text((cx - (r - l) / 2 - l, cy - (b - t) / 2 - t), text, font=font, fill=fill)


# ---- 512x512 app icon: gold square + dark ₹ ----
icon = Image.new("RGB", (512, 512), GOLD)
center(ImageDraw.Draw(icon), 256, 256, "₹", ImageFont.truetype(HELV, 360, index=1), DARK)
icon.save("store-assets/play-icon-512.png")

# ---- 1024x500 feature graphic ----
W, H = 1024, 500
feat = Image.new("RGB", (W, H), GOLD)
d = ImageDraw.Draw(feat)
for y in range(H):                                            # subtle vertical gradient
    k = 1 - 0.12 * (y / H)
    d.line([(0, y), (W, y)], fill=(int(0xD5 * k), int(0xB4 * k), int(0x75 * k)))
cx, cy, rad = 248, 250, 150
d.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=DARK)
center(d, cx, cy, "₹", ImageFont.truetype(HELV, 200, index=1), GOLD)
tx = 468
d.text((tx, 150), "Net Worth", font=ImageFont.truetype(BOLD, 92), fill=DARK)
d.text((tx, 252), "Calculator", font=ImageFont.truetype(BOLD, 92), fill=DARK)
tag, size = "Private, offline net-worth tracker", 36          # auto-fit within safe margin
while tx + d.textlength(tag, font=ImageFont.truetype(BOLD, size)) > W - 48 and size > 24:
    size -= 1
d.text((tx + 2, 378), tag, font=ImageFont.truetype(BOLD, size), fill=(0x43, 0x35, 0x18))
feat.save("store-assets/play-feature-1024x500.png")
print("Generated store-assets/play-icon-512.png and play-feature-1024x500.png")
