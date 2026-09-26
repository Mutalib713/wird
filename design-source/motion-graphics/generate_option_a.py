#!/usr/bin/env python3
"""
Wird — Promo Option A: 3D Studio Floating Phone
Inspired by Reference 1 (Promogen) & Reference 4 (Muhaavo).
Features:
- Cinematic dark obsidian backdrop with floating blurred ambient light orbs
- 3D floating smartphone with realistic glass sheen and floor shadow
- Real authentic Wird UI screens (Atmospheric Mosque, Daily Wird card, Whisper AI waveforms)
- Oversized modern kinetic typography with floating glowing pill badges
- Studio voiceover narration mixed with the Reference 4 rhythmic beat
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
DURATION_SEC = 31.4
TOTAL_FRAMES = int(FPS * DURATION_SEC)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
AUDIO_PATH = os.path.join(BASE_DIR, "audio_option_a.wav")
OUTPUT_VIDEO = os.path.join(BASE_DIR, "wird_promo_option_a.mp4")

# Fonts
FONT_REGULAR = "C:\\Windows\\Fonts\\segoeui.ttf"
FONT_BOLD = "C:\\Windows\\Fonts\\segoeuib.ttf"
FONT_BLACK = "C:\\Windows\\Fonts\\arialbd.ttf"

def get_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()

f_hero = get_font(FONT_BLACK, 76)
f_title = get_font(FONT_BLACK, 60)
f_h2 = get_font(FONT_BOLD, 38)
f_h3 = get_font(FONT_BOLD, 28)
f_body = get_font(FONT_REGULAR, 22)
f_pill = get_font(FONT_BOLD, 17)
f_caption = get_font(FONT_BOLD, 15)
f_arabic = get_font(FONT_BOLD, 30)

def ease_out_cubic(x):
    return 1 - math.pow(1 - x, 3)

def draw_round_rect(draw, bbox, radius, fill=None, outline=None, width=1):
    x0, y0, x1, y1 = bbox
    draw.rounded_rectangle([x0, y0, x1, y1], radius=radius, fill=fill, outline=outline, width=width)

def draw_pill_badge(draw, cx, cy, text, bg_color, border_color, text_color):
    bbox = draw.textbbox((0, 0), text, font=f_pill)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    pw = tw + 44
    ph = th + 24
    draw_round_rect(draw, [cx - pw//2, cy - ph//2, cx + pw//2, cy + ph//2], ph//2, fill=bg_color, outline=border_color, width=2)
    draw.text((cx, cy), text, fill=text_color, font=f_pill, anchor="mm")

def draw_phone_chassis(draw, cx, cy, w, h, radius, rot_y=0.0):
    # Simulated 3D tilt offset
    offset_x = math.sin(rot_y) * 24
    x0 = cx - w // 2 + offset_x
    y0 = cy - h // 2
    x1 = cx + w // 2 + offset_x
    y1 = cy + h // 2

    # Grounded soft floor drop shadow
    shadow_y = y1 + 35
    draw.ellipse([cx - w//2 + 30, shadow_y - 20, cx + w//2 - 30, shadow_y + 20], fill=(0, 0, 0))

    # Outer titanium chassis
    draw_round_rect(draw, [x0, y0, x1, y1], radius, fill=(16, 36, 26), outline=(212, 175, 55), width=3)
    # Inner display bezel
    draw_round_rect(draw, [x0 + 10, y0 + 10, x1 - 10, y1 - 10], radius - 6, fill=(6, 15, 11), outline=(255, 255, 255, 25), width=1)
    # Dynamic camera notch
    notch_w = 90
    notch_h = 24
    draw_round_rect(draw, [cx - notch_w // 2, y0 + 18, cx + notch_w // 2, y0 + 18 + notch_h], 12, fill=(3, 7, 5))

    return (x0 + 14, y0 + 14, x1 - 14, y1 - 14)

def render_screen_content(draw, sx0, sy0, sx1, sy1, screen_type, t):
    sw = sx1 - sx0
    sh = sy1 - sy0

    # OLED background
    draw.rectangle([sx0, sy0, sx1, sy1], fill=(8, 20, 14))

    # Dawn Sky & Mosque Header
    header_h = 160
    draw.rectangle([sx0, sy0, sx1, sy0 + header_h], fill=(20, 55, 38))
    
    # Mosque Silhouette
    draw.polygon([
        (sx0 + 20, sy0 + header_h), (sx0 + 20, sy0 + 60), (sx0 + 35, sy0 + 35), (sx0 + 50, sy0 + 60), (sx0 + 50, sy0 + header_h),
        (sx0 + sw//2 - 45, sy0 + header_h), (sx0 + sw//2, sy0 + 50), (sx0 + sw//2 + 45, sy0 + header_h),
        (sx1 - 50, sy0 + header_h), (sx1 - 50, sy0 + 60), (sx1 - 35, sy0 + 35), (sx1 - 20, sy0 + 60), (sx1 - 20, sy0 + header_h)
    ], fill=(6, 14, 10))

    # Crescent Moon
    draw.ellipse([sx1 - 65, sy0 + 35, sx1 - 45, sy0 + 55], fill=(245, 224, 140))

    # Reader greeting
    draw.text((sx0 + 24, sy0 + 60), "AS-SALĀMU 'ALAYKUM", fill=(160, 190, 175), font=f_caption)
    draw.text((sx0 + 24, sy0 + 82), "Mutalib Osman", fill=(255, 255, 255), font=f_h3)
    
    # Active mode pill
    draw_round_rect(draw, [sx0 + 24, sy0 + 115, sx0 + 140, sy0 + 138], 11, fill=(212, 175, 55, 40), outline=(212, 175, 55), width=1)
    draw.text((sx0 + 34, sy0 + 120), "⚡ School Mode", fill=(245, 224, 140), font=f_caption)

    # Daily Wird Card
    card_y = sy0 + 175
    card_h = 240
    card_w = sw - 48
    draw_round_rect(draw, [sx0 + 24, card_y, sx1 - 24, card_y + card_h], 20, fill=(16, 42, 30), outline=(212, 175, 55), width=2)
    
    draw.text((sx0 + 44, card_y + 24), "TODAY'S DAILY WIRD", fill=(212, 175, 55), font=f_caption)
    draw.text((sx0 + 44, card_y + 48), "Surah An-Nās", fill=(255, 255, 255), font=f_h2)
    draw.text((sx0 + 44, card_y + 90), "Verses 1 – 6 • Page 604", fill=(160, 190, 175), font=f_body)

    # Arabic Ayah
    draw.text((sx1 - 44, card_y + 125), "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", fill=(245, 224, 140), font=f_arabic, anchor="ra")

    if screen_type == "whisper":
        # Waveform visualizer
        wave_y = card_y + card_h + 50
        bars = 18
        bar_w = 12
        for b in range(bars):
            bh = int(35 * (math.sin(t * 14 + b * 0.45) * 0.5 + 0.5) + 10)
            bx = sx0 + 40 + b * 16
            draw.rectangle([bx, wave_y - bh, bx + bar_w, wave_y + bh], fill=(212, 175, 55))
        draw.text((sx0 + sw//2, wave_y + 60), "🎙️ Reciting aloud... Listening on device", fill=(160, 190, 175), font=f_caption, anchor="mm")
    else:
        # Progress bar
        bar_y = card_y + 170
        draw_round_rect(draw, [sx0 + 44, bar_y, sx1 - 44, bar_y + 10], 5, fill=(8, 20, 14))
        draw_round_rect(draw, [sx0 + 44, bar_y, sx0 + 44 + int(card_w * 0.65), bar_y + 10], 5, fill=(16, 185, 129))

        # Recite Button
        btn_y = card_y + 195
        draw_round_rect(draw, [sx0 + 44, btn_y, sx1 - 44, btn_y + 36], 12, fill=(212, 175, 55))
        draw.text((sx0 + sw//2, btn_y + 18), "🎙️ Recite Today's Portion", fill=(8, 20, 14), font=f_caption, anchor="mm")

def render_frame_a(frame_idx):
    t = frame_idx / FPS
    img = Image.new("RGB", (WIDTH, HEIGHT), color=(5, 12, 8))
    draw = ImageDraw.Draw(img)

    cx, cy = WIDTH // 2, HEIGHT // 2

    # Ambient blurred color orbs in background (Ref 4 style)
    orb1_y = int(cy - 200 + math.sin(t * 0.8) * 80)
    orb2_y = int(cy + 300 + math.cos(t * 0.7) * 70)
    draw.ellipse([cx - 400, orb1_y - 250, cx + 400, orb1_y + 250], fill=(12, 38, 26))
    draw.ellipse([cx - 350, orb2_y - 200, cx + 350, orb2_y + 200], fill=(26, 45, 30))

    # SECTION 1: Intro Hook (0.0s - 4.5s)
    # "Introducing Wird. The Qur'an habit coach built for real life."
    if t < 4.5:
        st = t
        ease = ease_out_cubic(min(st / 0.8, 1.0))
        
        draw.text((cx, cy - 140), "INTRODUCING", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((cx, cy - 60), "Wird (ورْد)", fill=(255, 255, 255), font=f_hero, anchor="mm")
        if st > 0.8:
            draw.text((cx, cy + 25), "The Qur'an habit coach", fill=(245, 224, 140), font=f_h2, anchor="mm")
            draw.text((cx, cy + 75), "built for real life.", fill=(160, 190, 175), font=f_h2, anchor="mm")
        
        if st > 2.0:
            draw_pill_badge(draw, cx, cy + 180, "✦ Presence Over Pressure", (16, 44, 32), (212, 175, 55), (245, 224, 140))

    # SECTION 2: No Guilt / Daily Portion (4.5s - 10.5s)
    # "No guilt. No rigid targets. Read half a page, one page, or three verses."
    elif t < 10.5:
        st = t - 4.5
        ease = ease_out_cubic(min(st / 0.8, 1.0))

        # Floating 3D Phone with subtle tilt
        rot_y = math.sin(t * 0.9) * 0.18
        phone_w, phone_h = 440, 880
        phone_y = cy + 180
        screen_box = draw_phone_chassis(draw, cx, phone_y, phone_w, phone_h, 52, rot_y=rot_y)
        render_screen_content(draw, screen_box[0], screen_box[1], screen_box[2], screen_box[3], "home", t)

        # Kinetic Text Top
        draw.text((cx, 160), "NO GUILT. NO RIGID TARGETS.", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((cx, 220), "Read At Your Own Pace", fill=(255, 255, 255), font=f_title, anchor="mm")
        draw.text((cx, 280), "Half page • 1 page • Or 3 verses", fill=(245, 224, 140), font=f_h3, anchor="mm")

        # Floating Badges
        draw_pill_badge(draw, cx - 220, 370, "✓ Zero Streak Guilt", (14, 38, 28), (16, 185, 129), (110, 231, 183))
        draw_pill_badge(draw, cx + 220, 370, "✓ Sustainable Habit", (14, 38, 28), (212, 175, 55), (245, 224, 140))

    # SECTION 3: Sūrahs Section Isolation (10.5s - 16.0s)
    # "Read Surah Al-Kahf freely without ever losing your bookmark."
    elif t < 16.0:
        st = t - 10.5
        draw_phone_chassis(draw, cx, cy + 220, 440, 880, 52, rot_y=-0.12)

        # Focus feature card
        draw.text((cx, 150), "INDEPENDENT SURAHS SECTION", fill=(110, 231, 183), font=f_caption, anchor="mm")
        draw.text((cx, 210), "Read Al-Kahf on Friday.", fill=(255, 255, 255), font=f_title, anchor="mm")
        draw.text((cx, 270), "Without losing your daily bookmark.", fill=(245, 224, 140), font=f_h2, anchor="mm")

        # Floating 3D Card
        draw_round_rect(draw, [cx - 250, 340, cx + 250, 440], 20, fill=(16, 44, 32), outline=(212, 175, 55), width=2)
        draw.text((cx, 375), "📖 Surah Al-Kahf (Free Reading)", fill=(255, 255, 255), font=f_h3, anchor="mm")
        draw.text((cx, 410), "Your daily wird position stays 100% locked", fill=(160, 190, 175), font=f_body, anchor="mm")

    # SECTION 4: 100% Offline Whisper AI (16.0s - 22.0s)
    # "And recite aloud with 100% offline Whisper AI."
    elif t < 22.0:
        st = t - 16.0
        phone_w, phone_h = 460, 900
        phone_y = cy + 180
        screen_box = draw_phone_chassis(draw, cx, phone_y, phone_w, phone_h, 54, rot_y=0.15)
        render_screen_content(draw, screen_box[0], screen_box[1], screen_box[2], screen_box[3], "whisper", t)

        draw.text((cx, 150), "100% ON-DEVICE SPEECH AUDITOR", fill=(52, 211, 153), font=f_caption, anchor="mm")
        draw.text((cx, 210), "Recite Aloud with Whisper AI", fill=(255, 255, 255), font=f_title, anchor="mm")
        draw.text((cx, 270), "Instant word-by-word speech check.", fill=(245, 224, 140), font=f_h3, anchor="mm")

        draw_pill_badge(draw, cx, 345, "🛡️ Zero Mobile Data Bundles • 100% Private", (14, 38, 28), (52, 211, 153), (255, 255, 255))

    # SECTION 5: African & Ghanaian Madrasas (22.0s - 27.0s)
    # "Adapted for African madrasas. Zero data bundles. Total privacy."
    elif t < 27.0:
        st = t - 22.0
        draw_phone_chassis(draw, cx, cy + 200, 440, 880, 52)

        draw.text((cx, 150), "ROOTED IN MADRASA HERITAGE", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((cx, 210), "Ghanaian & West African Style", fill=(255, 255, 255), font=f_title, anchor="mm")
        draw.text((cx, 270), "Start forward, or reverse from Juz 'Amma.", fill=(245, 224, 140), font=f_h3, anchor="mm")

        draw_round_rect(draw, [cx - 260, 335, cx + 260, 425], 18, fill=(18, 48, 34), outline=(212, 175, 55), width=2)
        draw.text((cx, 365), "Switch learning direction anytime with 1 tap", fill=(255, 255, 255), font=f_body, anchor="mm")
        draw.text((cx, 395), "Tilāwah (Mushaf) or Ḥifẓ (Memory)", fill=(110, 231, 183), font=f_caption, anchor="mm")

    # SECTION 6: Climax & CTA (27.0s - 31.4s)
    # "Meet Wird. Presence over pressure."
    else:
        st = t - 27.0
        draw_phone_chassis(draw, cx, cy + 180, 440, 880, 52)

        draw.text((cx, 150), "YOUR QUR'AN HABIT", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((cx, 220), "Presence Over Pressure.", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((cx, 290), "Rebuilt for your real life.", fill=(245, 224, 140), font=f_h2, anchor="mm")

        # CTA Button
        cta_y = HEIGHT - 240
        draw_round_rect(draw, [cx - 220, cta_y - 38, cx + 220, cta_y + 38], 22, fill=(212, 175, 55))
        draw.text((cx, cta_y), "Download Wird Free", fill=(7, 18, 13), font=f_h2, anchor="mm")
        draw.text((cx, cta_y + 60), "100% Offline • Zero Data Bundles • Android", fill=(160, 190, 175), font=f_body, anchor="mm")

    return img

def main():
    print(f"Starting Option A Render ({TOTAL_FRAMES} frames @ {FPS} FPS)...")
    start_time = time.time()

    ffmpeg_cmd = [
        "ffmpeg", "-y",
        "-f", "rawvideo",
        "-vcodec", "rawvideo",
        "-s", f"{WIDTH}x{HEIGHT}",
        "-pix_fmt", "rgb24",
        "-r", str(FPS),
        "-i", "-", # video frames from stdin
        "-i", AUDIO_PATH, # mixed studio voiceover + reference beat
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
        frame = render_frame_a(i)
        proc.stdin.write(frame.tobytes())
        if i % 90 == 0:
            pct = (i / TOTAL_FRAMES) * 100
            print(f"  Option A: {pct:.1f}% ({i}/{TOTAL_FRAMES} frames)")

    proc.stdin.close()
    proc.wait()

    elapsed = time.time() - start_time
    file_size_mb = os.path.getsize(OUTPUT_VIDEO) / (1024 * 1024)
    print(f"Option A complete in {elapsed:.1f}s! File: {OUTPUT_VIDEO} ({file_size_mb:.2f} MB)")

if __name__ == "__main__":
    main()
