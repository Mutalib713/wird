#!/usr/bin/env python3
"""
Wird — High-Converting Commercial Promo (WOW Edition)
Uses the ACTUAL high-resolution app screenshots inside a photorealistic 3D phone frame,
with floating studio lighting, kinetic typography, glowing pill badges, and a complete
feature narrative that wows and convinces users to download Wird.
"""

import math
import os
import subprocess
import sys
import time
from PIL import Image, ImageDraw, ImageFont, ImageFilter

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

WIDTH = 1080
HEIGHT = 1920
FPS = 30
DURATION_SEC = 52.96
TOTAL_FRAMES = int(FPS * DURATION_SEC)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
BRAIN_DIR = r"C:\Users\USER\.gemini\antigravity\brain\6df70f33-6dd8-4b46-be69-51f059f04de6"

AUDIO_PATH = os.path.join(BASE_DIR, "audio_wow.wav")
OUTPUT_VIDEO = os.path.join(BASE_DIR, "wird_promo_wow.mp4")

# Load and prepare ACTUAL high-res app screenshots
SCREEN_MUSHAF = Image.open(os.path.join(BRAIN_DIR, "live_device_mushaf.png")).convert("RGB")
SCREEN_HOME = Image.open(os.path.join(BRAIN_DIR, "live_home_screen.png")).convert("RGB")
SCREEN_SURAHS = Image.open(os.path.join(BRAIN_DIR, "phone_surahs_live.png")).convert("RGB")
SCREEN_CHAT = Image.open(os.path.join(BRAIN_DIR, "live_device_full_chat.png")).convert("RGB")

# Pre-cache resized versions for the phone screen (450 x 920)
PHONE_SCREEN_W = 432
PHONE_SCREEN_H = 880

PRE_SCREENS = {
    "mushaf": SCREEN_MUSHAF.resize((PHONE_SCREEN_W, PHONE_SCREEN_H), Image.Resampling.LANCZOS),
    "home": SCREEN_HOME.resize((PHONE_SCREEN_W, PHONE_SCREEN_H), Image.Resampling.LANCZOS),
    "surahs": SCREEN_SURAHS.resize((PHONE_SCREEN_W, PHONE_SCREEN_H), Image.Resampling.LANCZOS),
    "chat": SCREEN_CHAT.resize((PHONE_SCREEN_W, PHONE_SCREEN_H), Image.Resampling.LANCZOS),
}

# Fonts
FONT_REGULAR = "C:\\Windows\\Fonts\\segoeui.ttf"
FONT_BOLD = "C:\\Windows\\Fonts\\segoeuib.ttf"
FONT_BLACK = "C:\\Windows\\Fonts\\arialbd.ttf"

def get_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()

f_hero = get_font(FONT_BLACK, 72)
f_title = get_font(FONT_BLACK, 54)
f_h2 = get_font(FONT_BOLD, 36)
f_h3 = get_font(FONT_BOLD, 26)
f_body = get_font(FONT_REGULAR, 22)
f_pill = get_font(FONT_BOLD, 17)
f_caption = get_font(FONT_BOLD, 15)

def ease_out_cubic(x):
    return 1 - math.pow(1 - x, 3)

def draw_round_rect(draw, bbox, radius, fill=None, outline=None, width=1):
    x0, y0, x1, y1 = bbox
    draw.rounded_rectangle([x0, y0, x1, y1], radius=radius, fill=fill, outline=outline, width=width)

def draw_pill(draw, cx, cy, text, bg_color, border_color, text_color):
    bbox = draw.textbbox((0, 0), text, font=f_pill)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    pw = tw + 44
    ph = th + 24
    draw_round_rect(draw, [cx - pw//2, cy - ph//2, cx + pw//2, cy + ph//2], ph//2, fill=bg_color, outline=border_color, width=2)
    draw.text((cx, cy), text, fill=text_color, font=f_pill, anchor="mm")

def draw_rosette(draw, cx, cy, radius, stroke_color, rot=0.0):
    points = 16
    coords = []
    for i in range(points + 1):
        angle = (i * math.pi) / 8 + rot
        r = radius if (i % 2 == 0) else (radius * 0.72)
        x = cx + math.cos(angle) * r
        y = cy + math.sin(angle) * r
        coords.append((x, y))
    for i in range(len(coords) - 1):
        draw.line([coords[i], coords[i+1]], fill=stroke_color, width=3)

def paste_phone_with_screen(img, cx, cy, screen_key, rot_y=0.0):
    draw = ImageDraw.Draw(img)
    w, h = 460, 910
    radius = 50

    offset_x = int(math.sin(rot_y) * 20)
    x0 = cx - w // 2 + offset_x
    y0 = cy - h // 2
    x1 = cx + w // 2 + offset_x
    y1 = cy + h // 2

    # Grounded soft floor drop shadow
    shadow_y = y1 + 35
    draw.ellipse([cx - w//2 + 30, shadow_y - 22, cx + w//2 - 30, shadow_y + 22], fill=(0, 0, 0))

    # Outer titanium chassis (Deep Emerald Dark)
    draw_round_rect(draw, [x0, y0, x1, y1], radius, fill=(16, 36, 26), outline=(212, 175, 55), width=3)

    # Paste ACTUAL APP SCREEN inside display cutout
    screen_img = PRE_SCREENS.get(screen_key, PRE_SCREENS["home"])
    screen_x = x0 + 14
    screen_y = y0 + 15
    img.paste(screen_img, (screen_x, screen_y))

    # Re-draw the phone frame edges over the screen corners for crisp rounded bevels
    draw_round_rect(draw, [x0 + 12, y0 + 12, x1 - 12, y1 - 12], radius - 8, fill=None, outline=(6, 16, 11), width=6)
    draw_round_rect(draw, [x0 + 10, y0 + 10, x1 - 10, y1 - 10], radius - 6, fill=None, outline=(255, 255, 255, 30), width=1)

    # Dynamic camera notch pill at top
    notch_w = 88
    notch_h = 24
    draw_round_rect(draw, [cx - notch_w // 2, y0 + 20, cx + notch_w // 2, y0 + 20 + notch_h], 12, fill=(2, 6, 4))

def render_frame_wow(frame_idx):
    t = frame_idx / FPS
    img = Image.new("RGB", (WIDTH, HEIGHT), color=(4, 10, 7))
    draw = ImageDraw.Draw(img)

    cx, cy = WIDTH // 2, HEIGHT // 2

    # Floating ambient studio light orbs (Ref 4 style)
    orb1_y = int(cy - 220 + math.sin(t * 0.7) * 70)
    orb2_y = int(cy + 280 + math.cos(t * 0.6) * 60)
    draw.ellipse([cx - 420, orb1_y - 250, cx + 420, orb1_y + 250], fill=(12, 38, 25))
    draw.ellipse([cx - 360, orb2_y - 200, cx + 360, orb2_y + 200], fill=(28, 48, 32))

    # SCENE 1: The Relatable Friction Hook (0.0s - 7.5s)
    # "Be honest: How many Qur'an apps have you downloaded and abandoned? Rigid targets. You miss three days, your streak resets, and you quit."
    if t < 7.5:
        st = t

        # Red tension aura in background
        draw.ellipse([cx - 450, cy - 350, cx + 450, cy + 350], fill=(24, 10, 10))

        # Alert Banner
        notif_y = 150
        draw_round_rect(draw, [cx - 260, notif_y, cx + 260, notif_y + 88], 22, fill=(30, 12, 12), outline=(220, 50, 50), width=2)
        draw.text((cx - 230, notif_y + 20), "⚠️ STREAK BROKEN • DAY 0", fill=(248, 113, 113), font=f_caption)
        draw.text((cx - 230, notif_y + 46), "You missed your goal. Streak lost.", fill=(255, 255, 255), font=f_h3)

        # Phone floating in showing rigid streak shame
        paste_phone_with_screen(img, cx, cy + 220, "home", rot_y=0.1)

        # Big Hook Typography
        draw.text((cx, 280), "Be Honest:", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((cx, 340), "How many Qur'an apps have", fill=(255, 255, 255), font=f_title, anchor="mm")
        draw.text((cx, 400), "you abandoned?", fill=(245, 166, 35), font=f_title, anchor="mm")

        if st > 3.0:
            draw_pill(draw, cx, 475, "❌ Rigid 1 Juz Targets • Zero Forgiveness", (32, 14, 14), (239, 68, 68), (254, 202, 202))

    # SCENE 2: The Game-Changer — The Real Printed Mushaf (7.5s - 17.5s)
    # "Wird changes everything. It opens straight to your real printed Madani Mushaf: your exact portion is lit up, while the rest dims away."
    elif t < 17.5:
        st = t - 7.5
        ease = ease_out_cubic(min(st / 0.8, 1.0))

        # 3D Phone swoops in featuring the ACTUAL Madani Mushaf Page!
        paste_phone_with_screen(img, cx, cy + 180, "mushaf", rot_y=math.sin(t * 0.8) * 0.14)

        # Kinetic Text Top
        draw.text((cx, 140), "WIRD CHANGES EVERYTHING", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((cx, 200), "The Real Printed Mushaf", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((cx, 270), "Your exact portion is lit up.", fill=(245, 224, 140), font=f_h2, anchor="mm")
        draw.text((cx, 315), "The rest dims away so you never lose focus.", fill=(160, 190, 175), font=f_h3, anchor="mm")

        # Floating glowing badge
        draw_pill(draw, cx, 375, "✦ Authentic Madani QCF Font • No Generic Text", (16, 46, 32), (212, 175, 55), (245, 224, 140))

    # SCENE 3: Set Your Own Pace / Zero Guilt (17.5s - 25.5s)
    # "Set your own pace: half a page, one page, or three verses. Zero streak guilt."
    elif t < 25.5:
        st = t - 17.5
        # Phone showing Home Screen with Daily Card
        paste_phone_with_screen(img, cx, cy + 180, "home", rot_y=-0.12)

        draw.text((cx, 140), "PRESENCE OVER PRESSURE", fill=(110, 231, 183), font=f_caption, anchor="mm")
        draw.text((cx, 200), "Set Your Own Pace", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((cx, 270), "Half page • 1 page • Or 3 verses.", fill=(245, 224, 140), font=f_h2, anchor="mm")
        draw.text((cx, 315), "Whatever you can sustain with presence.", fill=(160, 190, 175), font=f_h3, anchor="mm")

        # Two floating badges
        draw_pill(draw, cx - 210, 380, "✓ Zero Streak Guilt", (14, 40, 28), (16, 185, 129), (110, 231, 183))
        draw_pill(draw, cx + 210, 380, "✓ Life-Adapted Habit", (14, 40, 28), (212, 175, 55), (245, 224, 140))

    # SCENE 4: 100% Offline Whisper AI (25.5s - 35.5s)
    # "And here is the real superpower: tap recite, and read aloud. Wird's on-device Whisper AI checks your recitation word-for-word. Zero internet. Zero data bundles. One hundred percent private."
    elif t < 35.5:
        st = t - 25.5
        # Phone showing chat / mic reciting
        paste_phone_with_screen(img, cx, cy + 190, "home", rot_y=0.12)

        draw.text((cx, 140), "THE REAL SUPERPOWER", fill=(52, 211, 153), font=f_caption, anchor="mm")
        draw.text((cx, 200), "100% Offline Whisper AI", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((cx, 270), "Tap recite, and read aloud.", fill=(245, 224, 140), font=f_h2, anchor="mm")
        draw.text((cx, 315), "Word-by-word voice auditor right on your phone.", fill=(160, 190, 175), font=f_h3, anchor="mm")

        # Live Oscillating Gold Waveform Bars
        wave_y = 385
        bars = 24
        bar_w = 12
        for b in range(bars):
            bh = int(32 * (math.sin(t * 14 + b * 0.45) * 0.5 + 0.5) + 10)
            bx = cx - 190 + b * 16
            draw.rectangle([bx, wave_y - bh, bx + bar_w, wave_y + bh], fill=(212, 175, 55))

        # Privacy badge
        draw_pill(draw, cx, 460, "🛡️ Zero Internet • Zero Data Bundles • 100% Private", (16, 44, 30), (52, 211, 153), (255, 255, 255))

    # SCENE 5: African & Ghanaian Madrasas + Free Sūrahs (35.5s - 44.5s)
    # "Built for African and Ghanaian madrasas: recite forward, or in reverse from Juz 'Amma. Plus, read Surah Al-Kahf on Friday freely without ever messing up your daily bookmark."
    elif t < 44.5:
        st = t - 35.5
        # Phone showing the real Sūrahs Tab!
        paste_phone_with_screen(img, cx, cy + 180, "surahs", rot_y=-0.14)

        draw.text((cx, 140), "ROOTED IN MADRASA HERITAGE", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((cx, 200), "Ghanaian & African Style", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((cx, 270), "Start forward, or reverse from Juz 'Amma.", fill=(245, 224, 140), font=f_h2, anchor="mm")

        # Sūrahs feature callout card
        draw_round_rect(draw, [cx - 260, 330, cx + 260, 420], 18, fill=(18, 48, 34), outline=(16, 185, 129), width=2)
        draw.text((cx, 360), "📑 Independent Sūrahs Reading", fill=(255, 255, 255), font=f_h3, anchor="mm")
        draw.text((cx, 390), "Read Al-Kahf on Friday freely without losing your bookmark", fill=(245, 224, 140), font=f_caption, anchor="mm")

    # SCENE 6: Climax & Call to Action (44.5s - 52.96s)
    # "Your Qur'an habit, rebuilt for real life. Download Wird free on Android."
    else:
        st = t - 44.5
        # Glowing rosette halo
        draw_rosette(draw, cx, cy - 180, 360, (212, 175, 55, 100), rot=st * 0.12)
        # Hero Phone
        paste_phone_with_screen(img, cx, cy + 180, "home", rot_y=0.0)

        draw.text((cx, 140), "YOUR QUR'AN HABIT", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((cx, 210), "Rebuilt for Real Life.", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((cx, 280), "Presence over pressure.", fill=(245, 224, 140), font=f_h2, anchor="mm")

        # Big Glowing CTA Button
        cta_y = HEIGHT - 220
        draw_round_rect(draw, [cx - 240, cta_y - 42, cx + 240, cta_y + 42], 24, fill=(212, 175, 55))
        draw.text((cx, cta_y), "Download Wird Free", fill=(7, 18, 13), font=f_h2, anchor="mm")
        draw.text((cx, cta_y + 65), "100% Offline • Zero Data Bundles • Android", fill=(160, 190, 175), font=f_body, anchor="mm")

    return img

def main():
    print(f"Starting WOW Commercial Render ({TOTAL_FRAMES} frames @ {FPS} FPS)...")
    start_time = time.time()

    ffmpeg_cmd = [
        "ffmpeg", "-y",
        "-f", "rawvideo",
        "-vcodec", "rawvideo",
        "-s", f"{WIDTH}x{HEIGHT}",
        "-pix_fmt", "rgb24",
        "-r", str(FPS),
        "-i", "-", # video frames from stdin
        "-i", AUDIO_PATH, # mixed voiceover + beat
        "-c:v", "libx264",
        "-preset", "fast",
        "-crf", "18",
        "-pix_fmt", "yuv420p",
        "-c:a", "aac",
        "-b:a", "192k",
        "-shortest",
        OUTPUT_VIDEO
    ]

    proc = subprocess.Popen(ffmpeg_cmd, stdin=subprocess.PIPE)

    for i in range(TOTAL_FRAMES):
        frame = render_frame_wow(i)
        proc.stdin.write(frame.tobytes())
        if i % 120 == 0:
            pct = (i / TOTAL_FRAMES) * 100
            print(f"  WOW Promo: {pct:.1f}% ({i}/{TOTAL_FRAMES} frames)")

    proc.stdin.close()
    proc.wait()

    elapsed = time.time() - start_time
    file_size_mb = os.path.getsize(OUTPUT_VIDEO) / (1024 * 1024)
    print(f"WOW Promo complete in {elapsed:.1f}s! File: {OUTPUT_VIDEO} ({file_size_mb:.2f} MB)")

if __name__ == "__main__":
    main()
