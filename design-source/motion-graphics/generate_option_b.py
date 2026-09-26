#!/usr/bin/env python3
"""
Wird — Promo Option B: The Friction-to-Solution Hook
Inspired by Reference 2 (Limitless / Apptics) & Reference 3 (Uber).
Features:
- Opening relatable chat conversation and streak broken alert
- Circle wipe transition smashing the broken streak
- Cascading 3D physical cards explaining features
- 3D phone swooping in with authentic UI and live Whisper AI waveform
- Upbeat agency beat with Guy neural voiceover
"""

import math
import os
import subprocess
import sys
import time
from PIL import Image, ImageDraw, ImageFont

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

WIDTH = 1080
HEIGHT = 1920
FPS = 30
DURATION_SEC = 31.7
TOTAL_FRAMES = int(FPS * DURATION_SEC)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
AUDIO_PATH = os.path.join(BASE_DIR, "audio_option_b.wav")
OUTPUT_VIDEO = os.path.join(BASE_DIR, "wird_promo_option_b.mp4")

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
f_title = get_font(FONT_BLACK, 58)
f_h2 = get_font(FONT_BOLD, 38)
f_h3 = get_font(FONT_BOLD, 28)
f_body = get_font(FONT_REGULAR, 22)
f_chat = get_font(FONT_REGULAR, 20)
f_chat_bold = get_font(FONT_BOLD, 20)
f_pill = get_font(FONT_BOLD, 17)
f_caption = get_font(FONT_BOLD, 15)

def ease_out_cubic(x):
    return 1 - math.pow(1 - x, 3)

def draw_round_rect(draw, bbox, radius, fill=None, outline=None, width=1):
    x0, y0, x1, y1 = bbox
    draw.rounded_rectangle([x0, y0, x1, y1], radius=radius, fill=fill, outline=outline, width=width)

def draw_phone_chassis(draw, cx, cy, w, h, radius, rot_y=0.0):
    offset_x = math.sin(rot_y) * 24
    x0 = cx - w // 2 + offset_x
    y0 = cy - h // 2
    x1 = cx + w // 2 + offset_x
    y1 = cy + h // 2

    # Drop shadow
    shadow_y = y1 + 35
    draw.ellipse([cx - w//2 + 30, shadow_y - 20, cx + w//2 - 30, shadow_y + 20], fill=(0, 0, 0))

    # Outer chassis
    draw_round_rect(draw, [x0, y0, x1, y1], radius, fill=(16, 36, 26), outline=(212, 175, 55), width=3)
    # Inner display
    draw_round_rect(draw, [x0 + 10, y0 + 10, x1 - 10, y1 - 10], radius - 6, fill=(7, 18, 13), outline=(255, 255, 255, 25), width=1)
    # Dynamic notch
    notch_w = 90
    notch_h = 24
    draw_round_rect(draw, [cx - notch_w // 2, y0 + 18, cx + notch_w // 2, y0 + 18 + notch_h], 12, fill=(3, 7, 5))

    return (x0 + 14, y0 + 14, x1 - 14, y1 - 14)

def render_frame_b(frame_idx):
    t = frame_idx / FPS
    img = Image.new("RGB", (WIDTH, HEIGHT), color=(5, 12, 8))
    draw = ImageDraw.Draw(img)

    cx, cy = WIDTH // 2, HEIGHT // 2

    # SECTION 1: The Friction / Broken Streak & Chat Hook (0.0s - 4.5s)
    # "Streak broken. Day zero. Why do traditional Qur'an apps feel like homework?"
    if t < 4.5:
        st = t

        # Dark minimalist background with warning red aura
        draw.ellipse([cx - 450, cy - 350, cx + 450, cy + 350], fill=(22, 10, 10))

        # Alert Notification Banner at Top
        notif_y = 160
        draw_round_rect(draw, [cx - 260, notif_y, cx + 260, notif_y + 88], 22, fill=(28, 12, 12), outline=(220, 50, 50), width=2)
        draw.text((cx - 230, notif_y + 20), "⚠️ STREAK BROKEN • DAY 0", fill=(248, 113, 113), font=f_caption)
        draw.text((cx - 230, notif_y + 46), "You missed 14 days. Don't lose hope!", fill=(255, 255, 255), font=f_chat_bold)

        # Chat Bubble 1 (User friend)
        chat1_y = 380
        draw_round_rect(draw, [cx - 260, chat1_y, cx + 80, chat1_y + 60], 18, fill=(18, 42, 30))
        draw.text((cx - 240, chat1_y + 20), "Did you read your wird today?", fill=(255, 255, 255), font=f_chat)

        # Chat Bubble 2 (User reply)
        if st > 1.0:
            chat2_y = 460
            draw_round_rect(draw, [cx - 60, chat2_y, cx + 260, chat2_y + 70], 18, fill=(40, 70, 55))
            draw.text((cx - 40, chat2_y + 15), "School is crazy right now...", fill=(245, 224, 140), font=f_chat)
            draw.text((cx - 40, chat2_y + 40), "I missed two whole weeks 😔", fill=(245, 224, 140), font=f_chat)

        # Chat Bubble 3 (Friend reply)
        if st > 2.0:
            chat3_y = 550
            draw_round_rect(draw, [cx - 260, chat3_y, cx + 180, chat3_y + 70], 18, fill=(18, 42, 30))
            draw.text((cx - 240, chat3_y + 15), "Same! Apps force you to read", fill=(255, 255, 255), font=f_chat)
            draw.text((cx - 240, chat3_y + 40), "1 whole Juz or you fail.", fill=(255, 255, 255), font=f_chat)

        # Bottom big punchline
        if st > 3.0:
            draw.text((cx, cy + 220), "Why do Qur'an apps", fill=(255, 255, 255), font=f_title, anchor="mm")
            draw.text((cx, cy + 290), "feel like homework?", fill=(245, 166, 35), font=f_title, anchor="mm")

    # SECTION 2: The Solution / Meet Wird (4.5s - 9.0s)
    # "Meet Wird. No more streak shame. No more rigid rules."
    elif t < 9.0:
        st = t - 4.5
        ease = ease_out_cubic(min(st / 0.8, 1.0))

        # Dynamic Circle Wipe Transition from center (Ref 3 style)
        if st < 0.6:
            wipe_r = int((st / 0.6) * WIDTH * 1.2)
            draw.ellipse([cx - wipe_r, cy - wipe_r, cx + wipe_r, cy + wipe_r], fill=(12, 36, 24))

        # Emerald background
        draw.ellipse([cx - 400, cy - 200, cx + 400, cy + 200], fill=(16, 48, 32))

        # Title
        draw.text((cx, 160), "THE SOLUTION", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((cx, 230), "Meet Wird (ورْد)", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((cx, 300), "No more streak shame.", fill=(110, 231, 183), font=f_h2, anchor="mm")

        # Cascading 3D Cards (Ref 2 style)
        card1_y = int(cy - 20 + (1 - ease) * 200)
        draw_round_rect(draw, [cx - 250, card1_y - 65, cx + 250, card1_y + 65], 22, fill=(18, 46, 32), outline=(212, 175, 55), width=2)
        draw.text((cx - 220, card1_y - 35), "NO RIGID 1 JUZ TARGETS", fill=(245, 224, 140), font=f_caption)
        draw.text((cx - 220, card1_y - 5), "Choose Your Real Pace", fill=(255, 255, 255), font=f_h3)
        draw.text((cx - 220, card1_y + 30), "Half page • 1 page • Or 3 verses.", fill=(160, 190, 175), font=f_body)

        if st > 1.2:
            card2_y = card1_y + 160
            draw_round_rect(draw, [cx - 250, card2_y - 65, cx + 250, card2_y + 65], 22, fill=(14, 38, 26), outline=(16, 185, 129), width=2)
            draw.text((cx - 220, card2_y - 35), "ZERO STREAK GUILT", fill=(110, 231, 183), font=f_caption)
            draw.text((cx - 220, card2_y - 5), "Presence Over Pressure", fill=(255, 255, 255), font=f_h3)
            draw.text((cx - 220, card2_y + 30), "Missed a day? Pick right back up.", fill=(245, 224, 140), font=f_body)

    # SECTION 3: Your Pace & Target (9.0s - 14.5s)
    # "Choose your pace: half a page, one page, or three verses."
    elif t < 14.5:
        st = t - 9.0
        draw_phone_chassis(draw, cx, cy + 200, 450, 900, 52, rot_y=math.sin(t) * 0.15)

        draw.text((cx, 150), "SET YOUR OWN DAILY TARGET", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((cx, 210), "Consistency Over Volume", fill=(255, 255, 255), font=f_title, anchor="mm")
        draw.text((cx, 270), "Half page • 1 page • Or 3 verses", fill=(245, 224, 140), font=f_h2, anchor="mm")

        # 3 Pace Choice Pills
        draw_round_rect(draw, [cx - 240, 330, cx - 90, 380], 14, fill=(18, 44, 32), outline=(212, 175, 55), width=2)
        draw.text((cx - 165, 355), "½ Page", fill=(245, 224, 140), font=f_chat_bold, anchor="mm")

        draw_round_rect(draw, [cx - 75, 330, cx + 75, 380], 14, fill=(212, 175, 55))
        draw.text((cx, 355), "1 Page ★", fill=(7, 18, 13), font=f_chat_bold, anchor="mm")

        draw_round_rect(draw, [cx + 90, 330, cx + 240, 380], 14, fill=(18, 44, 32), outline=(212, 175, 55), width=2)
        draw.text((cx + 165, 355), "2 Pages", fill=(245, 224, 140), font=f_chat_bold, anchor="mm")

    # SECTION 4: Separate Sūrahs (14.5s - 20.0s)
    # "Read your favorite Surahs freely without messing up your daily wird."
    elif t < 20.0:
        st = t - 14.5
        draw_phone_chassis(draw, cx, cy + 220, 440, 880, 52, rot_y=-0.14)

        draw.text((cx, 150), "ISOLATED BOOKMARKS", fill=(110, 231, 183), font=f_caption, anchor="mm")
        draw.text((cx, 210), "Independent Sūrahs Tab", fill=(255, 255, 255), font=f_title, anchor="mm")
        draw.text((cx, 270), "Read Al-Kahf or Al-Mulk freely.", fill=(245, 224, 140), font=f_h2, anchor="mm")

        draw_round_rect(draw, [cx - 260, 330, cx + 260, 430], 20, fill=(16, 42, 30), outline=(16, 185, 129), width=2)
        draw.text((cx, 365), "Your daily wird bookmark never changes", fill=(255, 255, 255), font=f_body, anchor="mm")
        draw.text((cx, 400), "Zero conflict between routine and Friday recitation", fill=(245, 224, 140), font=f_caption, anchor="mm")

    # SECTION 5: Whisper AI & Madrasas (20.0s - 26.0s)
    # "And check your recitation with 100% offline Whisper AI. Start forward, or reverse from Juz 'Amma."
    elif t < 26.0:
        st = t - 20.0
        draw_phone_chassis(draw, cx, cy + 200, 450, 900, 52, rot_y=0.12)

        draw.text((cx, 150), "100% OFFLINE SPEECH AUDITOR", fill=(52, 211, 153), font=f_caption, anchor="mm")
        draw.text((cx, 210), "Whisper AI Voice Check", fill=(255, 255, 255), font=f_title, anchor="mm")
        draw.text((cx, 270), "Ghanaian & African Madrasa Direction", fill=(245, 224, 140), font=f_h3, anchor="mm")

        # Waveform animation
        wave_y = 330
        bars = 22
        bar_w = 12
        for b in range(bars):
            bh = int(35 * (math.sin(t * 14 + b * 0.45) * 0.5 + 0.5) + 10)
            bx = cx - 180 + b * 16
            draw.rectangle([bx, wave_y - bh, bx + bar_w, wave_y + bh], fill=(212, 175, 55))

        draw.text((cx, 395), "Start forward or reverse from Juz 'Amma", fill=(255, 255, 255), font=f_body, anchor="mm")
        draw.text((cx, 425), "Zero data bundles • 100% On-Device", fill=(110, 231, 183), font=f_caption, anchor="mm")

    # SECTION 6: Climax & CTA (26.0s - 31.7s)
    # "Your Qur'an habit, rebuilt for real life. Download Wird today."
    else:
        st = t - 26.0
        draw_phone_chassis(draw, cx, cy + 180, 440, 880, 52)

        draw.text((cx, 150), "YOUR QUR'AN HABIT", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((cx, 220), "Rebuilt for Real Life.", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((cx, 290), "Presence over pressure.", fill=(245, 224, 140), font=f_h2, anchor="mm")

        # CTA Button
        cta_y = HEIGHT - 240
        draw_round_rect(draw, [cx - 220, cta_y - 38, cx + 220, cta_y + 38], 22, fill=(212, 175, 55))
        draw.text((cx, cta_y), "Download Wird Today", fill=(7, 18, 13), font=f_h2, anchor="mm")
        draw.text((cx, cta_y + 60), "100% Offline • Zero Data Bundles • Android", fill=(160, 190, 175), font=f_body, anchor="mm")

    return img

def main():
    print(f"Starting Option B Render ({TOTAL_FRAMES} frames @ {FPS} FPS)...")
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
        frame = render_frame_b(i)
        proc.stdin.write(frame.tobytes())
        if i % 90 == 0:
            pct = (i / TOTAL_FRAMES) * 100
            print(f"  Option B: {pct:.1f}% ({i}/{TOTAL_FRAMES} frames)")

    proc.stdin.close()
    proc.wait()

    elapsed = time.time() - start_time
    file_size_mb = os.path.getsize(OUTPUT_VIDEO) / (1024 * 1024)
    print(f"Option B complete in {elapsed:.1f}s! File: {OUTPUT_VIDEO} ({file_size_mb:.2f} MB)")

if __name__ == "__main__":
    main()
