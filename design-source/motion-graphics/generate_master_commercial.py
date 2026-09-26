#!/usr/bin/env python3
"""
Wird — Broadcast-Grade Promotional Motion Graphics Commercial
Key Superpowers Highlighted:
1. Authentic App Screens: 100% genuine Android app screenshots (1440x3120).
2. True Home Screen: Mosque header, Ramadan 2026 track, Daily Wird card, action buttons, check-in card.
3. Live Vertical Scrolling: Screen scrolls down in real-time to reveal Recite / Mark Done / Listen / Mushaf buttons.
4. Exact Screen Sync:
   - 0.00s – 8.17s: Hook on Home Screen (Streak broken alert)
   - 8.17s – 13.65s: Daily Portion card (Focus on today's verses)
   - 13.65s – 19.25s: Live Vertical Scroll to Recite / Done / Listen buttons & check-in
   - 19.25s – 29.02s: Real Printed Madani Mushaf with glowing illuminated ayahs
   - 29.02s – 37.60s: Reading Modes Manager (Home / School / Ramadan)
   - 37.60s – 42.57s: Reading Track Setup (Hifz memorization / Tilawah / Mon-Thu)
   - 42.57s – 51.25s: Offline AI Companion Chat with quick action chips
   - 51.25s – 59.10s: Whisper AI speech check with 24 live pulsating gold waveforms
   - 59.10s – 64.37s: African / Ghanaian madrasa reverse recitation (Towards Al-Fatihah)
   - 64.37s – 69.23s: 114 Surahs directory tab
   - 69.23s – 74.56s: Grand Hero Showcase, rotating Islamic rosette halo, and CTA
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
DURATION_SEC = 74.56
TOTAL_FRAMES = int(FPS * DURATION_SEC)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
BRAIN_DIR = r"C:\Users\USER\.gemini\antigravity\brain\6df70f33-6dd8-4b46-be69-51f059f04de6"

AUDIO_PATH = os.path.join(BASE_DIR, "audio_master.wav")
OUTPUT_VIDEO = os.path.join(BASE_DIR, "wird_promo_master.mp4")

# Load all 8 real Android screenshots (1440 x 3120)
RAW_SCREENS = {
    "home": Image.open(os.path.join(BRAIN_DIR, "live_home_new_mode.png")).convert("RGB"),
    "mushaf": Image.open(os.path.join(BRAIN_DIR, "live_device_mushaf.png")).convert("RGB"),
    "modes": Image.open(os.path.join(BRAIN_DIR, "live_mode_switched.png")).convert("RGB"),
    "tracks": Image.open(os.path.join(BRAIN_DIR, "live_track_setup_with_pos.png")).convert("RGB"),
    "chat": Image.open(os.path.join(BRAIN_DIR, "live_device_chatscreen.png")).convert("RGB"),
    "speech": Image.open(os.path.join(BRAIN_DIR, "screen_recite.png")).convert("RGB"),
    "direction": Image.open(os.path.join(BRAIN_DIR, "live_add_track_dialog.png")).convert("RGB"),
    "surahs": Image.open(os.path.join(BRAIN_DIR, "phone_surahs_live.png")).convert("RGB"),
}

SCREEN_W = 420
SCREEN_H = 750
VIEWPORT_H = int(1440 * (SCREEN_H / SCREEN_W)) # 2571 px

# Windows System Fonts
FONT_REGULAR = "C:\\Windows\\Fonts\\segoeui.ttf"
FONT_BOLD = "C:\\Windows\\Fonts\\segoeuib.ttf"
FONT_BLACK = "C:\\Windows\\Fonts\\arialbd.ttf"

def get_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()

f_hero = get_font(FONT_BLACK, 68)
f_title = get_font(FONT_BLACK, 50)
f_h2 = get_font(FONT_BOLD, 35)
f_h3 = get_font(FONT_BOLD, 26)
f_body = get_font(FONT_REGULAR, 22)
f_pill = get_font(FONT_BOLD, 18)
f_caption = get_font(FONT_BOLD, 15)

def ease_out_cubic(x):
    return 1 - math.pow(1 - x, 3)

def ease_in_out_cubic(x):
    return 4 * x * x * x if x < 0.5 else 1 - math.pow(-2 * x + 2, 3) / 2

def draw_round_rect(draw, bbox, radius, fill=None, outline=None, width=1):
    x0, y0, x1, y1 = bbox
    draw.rounded_rectangle([x0, y0, x1, y1], radius=radius, fill=fill, outline=outline, width=width)

def draw_pill(draw, cx, cy, text, bg_color, border_color, text_color):
    bbox = draw.textbbox((0, 0), text, font=f_pill)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    pw = tw + 46
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

def get_cropped_screen(screen_key, scroll_y=0):
    raw = RAW_SCREENS.get(screen_key, RAW_SCREENS["home"])
    max_scroll = max(0, raw.height - VIEWPORT_H)
    actual_scroll = min(max_scroll, max(0, int(scroll_y)))
    cropped = raw.crop((0, actual_scroll, 1440, actual_scroll + VIEWPORT_H))
    return cropped.resize((SCREEN_W, SCREEN_H), Image.Resampling.BILINEAR)

def paste_phone(img, cx, cy, screen_key, scroll_y=0, rot_y=0.0, scale=1.0):
    w = int((SCREEN_W + 28) * scale)
    h = int((SCREEN_H + 30) * scale)
    radius = int(48 * scale)

    # 3D horizontal tilt offset
    offset_x = int(math.sin(rot_y) * 26)
    x0 = cx - w // 2 + offset_x
    y0 = cy - h // 2
    x1 = cx + w // 2 + offset_x
    y1 = cy + h // 2

    draw = ImageDraw.Draw(img)

    # Soft grounded floor drop shadow responding to elevation
    shadow_y = y1 + 32
    sw = int(w * 0.82)
    sh = 24
    draw.ellipse([cx - sw//2, shadow_y - sh, cx + sw//2, shadow_y + sh], fill=(0, 0, 0))

    # Titanium outer shell with refined metallic highlight
    draw_round_rect(draw, [x0, y0, x1, y1], radius, fill=(18, 24, 20), outline=(52, 72, 62), width=2)

    # Scaled screen crop with anti-aliased rounded mask (no sharp corners peeking)
    screen_img = get_cropped_screen(screen_key, scroll_y)
    sw_target = int(SCREEN_W * scale)
    sh_target = int(SCREEN_H * scale)
    if scale != 1.0:
        screen_img = screen_img.resize((sw_target, sh_target), Image.Resampling.BILINEAR)

    # Rounded mask for screen
    mask = Image.new("L", (sw_target, sh_target), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle([0, 0, sw_target, sh_target], int(36 * scale), fill=255)

    screen_x = x0 + int(14 * scale)
    screen_y = y0 + int(15 * scale)
    img.paste(screen_img, (screen_x, screen_y), mask)

    # Inner bezel chamfer
    draw_round_rect(draw, [x0 + int(13*scale), y0 + int(14*scale), x1 - int(13*scale), y1 - int(14*scale)], int(36*scale), fill=None, outline=(8, 14, 11), width=3)

    # Camera punch hole / pill notch
    notch_w = int(80 * scale)
    notch_h = int(22 * scale)
    notch_x0 = cx - notch_w // 2 + offset_x
    notch_y0 = y0 + int(18 * scale)
    draw_round_rect(draw, [notch_x0, notch_y0, notch_x0 + notch_w, notch_y0 + notch_h], int(11 * scale), fill=(2, 4, 3))

def render_frame_master(frame_idx):
    t = frame_idx / FPS
    img = Image.new("RGB", (WIDTH, HEIGHT), color=(4, 10, 7))
    draw = ImageDraw.Draw(img)

    cx, cy = WIDTH // 2, HEIGHT // 2 + 130

    # Ambient studio light atmosphere
    orb1_x = int(WIDTH // 2 + math.sin(t * 0.5) * 110)
    orb1_y = int(cy - 220 + math.cos(t * 0.6) * 70)
    orb2_x = int(WIDTH // 2 - math.sin(t * 0.4) * 90)
    orb2_y = int(cy + 260 + math.sin(t * 0.5) * 60)
    draw.ellipse([orb1_x - 400, orb1_y - 240, orb1_x + 400, orb1_y + 240], fill=(12, 38, 25))
    draw.ellipse([orb2_x - 350, orb2_y - 190, orb2_x + 350, orb2_y + 190], fill=(24, 46, 30))

    # Natural floating breathing oscillation
    float_bob = int(math.sin(t * 1.5) * 8)
    phone_cy = cy + float_bob

    # =========================================================================
    # SCENE 1: The Friction Hook (0.00s – 8.17s)
    # Voice: "Why do traditional Qur'an apps feel like homework? Rigid targets. One busy week at school, your streak breaks, and you quit."
    # =========================================================================
    if t < 8.17:
        st = t
        # Red tension ambient glow
        draw.ellipse([WIDTH//2 - 420, cy - 320, WIDTH//2 + 420, cy + 320], fill=(22, 10, 10))

        # Floating streak broken alert card
        notif_y = 150
        draw_round_rect(draw, [WIDTH//2 - 270, notif_y, WIDTH//2 + 270, notif_y + 88], 22, fill=(30, 12, 12), outline=(220, 50, 50), width=2)
        draw.text((WIDTH//2 - 240, notif_y + 18), "STREAK BROKEN • DAY 0", fill=(248, 113, 113), font=f_caption)
        draw.text((WIDTH//2 - 240, notif_y + 45), "You missed 14 days. Don't quit!", fill=(255, 255, 255), font=f_h3)

        # Phone floating and tilting in 3D perspective
        phone_rot = math.sin(st * 1.2) * 0.14
        phone_enter_y = phone_cy + int((1.0 - min(st / 0.8, 1.0)) * 250)
        paste_phone(img, WIDTH//2, phone_enter_y, "home", scroll_y=0, rot_y=phone_rot)

        draw.text((WIDTH//2, 280), "Be Honest:", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((WIDTH//2, 340), "Why do Qur'an apps", fill=(255, 255, 255), font=f_title, anchor="mm")
        draw.text((WIDTH//2, 400), "feel like homework?", fill=(245, 166, 35), font=f_title, anchor="mm")

        if st > 3.0:
            draw_pill(draw, WIDTH//2, 475, "Rigid 1-Juz Targets • Zero Forgiveness", (32, 14, 14), (239, 68, 68), (254, 202, 202))

    # =========================================================================
    # SCENE 2: The Daily Portion (8.17s – 13.65s)
    # Voice: "Meet Wird. It starts with your Daily Portion: your exact page and verses for today."
    # =========================================================================
    elif t < 13.65:
        st = t - 8.17
        phone_rot = -0.10 + math.sin(st * 1.1) * 0.04
        # Camera pushes in smoothly
        scale = 1.0 + min(st / 2.0, 1.0) * 0.08
        paste_phone(img, WIDTH//2, phone_cy, "home", scroll_y=0, rot_y=phone_rot, scale=scale)

        draw.text((WIDTH//2, 140), "MEET WIRD • QUR'AN COMPANION", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((WIDTH//2, 205), "Your Daily Portion", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((WIDTH//2, 275), "Tells you your exact page and verses.", fill=(245, 224, 140), font=f_h2, anchor="mm")
        draw.text((WIDTH//2, 320), "Surah Ya-Sin • Page 442 • 1 Page Today", fill=(160, 190, 175), font=f_h3, anchor="mm")

        draw_pill(draw, WIDTH//2, 380, "Focus On One Small Portion Every Day", (16, 44, 30), (212, 175, 55), (245, 224, 140))

    # =========================================================================
    # SCENE 3: Live Vertical Scroll to Action Buttons (13.65s – 19.25s)
    # Voice: "Scroll down, and choose how you complete it: recite aloud, listen, or tap done when finished."
    # =========================================================================
    elif t < 19.25:
        st = t - 13.65
        # Dynamic real-time vertical scroll down from 0 to 580 px!
        scroll_progress = ease_in_out_cubic(min(st / 3.0, 1.0))
        current_scroll = int(scroll_progress * 580)

        phone_rot = 0.08 - math.sin(st * 0.9) * 0.03
        paste_phone(img, WIDTH//2, phone_cy, "home", scroll_y=current_scroll, rot_y=phone_rot)

        draw.text((WIDTH//2, 140), "CHOOSE HOW YOU COMPLETE IT", fill=(110, 231, 183), font=f_caption, anchor="mm")
        draw.text((WIDTH//2, 205), "Recite, Listen, or Tap", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((WIDTH//2, 275), "Recite aloud, listen to reciter audio,", fill=(245, 224, 140), font=f_h2, anchor="mm")
        draw.text((WIDTH//2, 320), "or tap done when finished.", fill=(160, 190, 175), font=f_h3, anchor="mm")

        draw_pill(draw, WIDTH//2, 380, "Log Recited vs Tapped Honestly", (14, 42, 30), (16, 185, 129), (110, 231, 183))

    # =========================================================================
    # SCENE 4: The Printed Madani Mushaf (19.25s – 29.02s)
    # Voice: "Tap into your portion, and it opens straight to the real printed Madani Mushaf. Your exact verses are highlighted with illuminated borders, while the rest dims away so you never lose focus."
    # =========================================================================
    elif t < 29.02:
        st = t - 19.25
        phone_rot = -0.14 + math.sin(st * 0.8) * 0.05
        paste_phone(img, WIDTH//2, phone_cy, "mushaf", scroll_y=0, rot_y=phone_rot)

        draw.text((WIDTH//2, 140), "THE REAL PRINTED MUSHAF", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((WIDTH//2, 205), "Zero Generic Text", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((WIDTH//2, 275), "Your portion is illuminated.", fill=(245, 224, 140), font=f_h2, anchor="mm")
        draw.text((WIDTH//2, 320), "The rest dims away so you never lose focus.", fill=(160, 190, 175), font=f_h3, anchor="mm")

        draw_pill(draw, WIDTH//2, 380, "Authentic Madani QCF Script & Page Layout", (16, 46, 32), (212, 175, 55), (245, 224, 140))

    # =========================================================================
    # SCENE 5: Reading Modes Manager (29.02s – 37.60s)
    # Voice: "Here is what no other app has: Reading Modes and Tracks. Switch between Home Mode, School Mode, or Work Mode, each with its own reminder time."
    # =========================================================================
    elif t < 37.60:
        st = t - 29.02
        phone_rot = 0.14 - math.sin(st * 0.8) * 0.05
        paste_phone(img, WIDTH//2, phone_cy, "modes", scroll_y=0, rot_y=phone_rot)

        draw.text((WIDTH//2, 140), "EXCLUSIVE FEATURE", fill=(52, 211, 153), font=f_caption, anchor="mm")
        draw.text((WIDTH//2, 205), "Reading Modes", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((WIDTH//2, 275), "Home Mode • School Mode • Work Mode", fill=(245, 224, 140), font=f_h2, anchor="mm")
        draw.text((WIDTH//2, 320), "Each mode holds its own reminder times.", fill=(160, 190, 175), font=f_h3, anchor="mm")

        draw_pill(draw, WIDTH//2, 380, "Auto-Adapts to Where You Are", (14, 40, 28), (52, 211, 153), (255, 255, 255))

    # =========================================================================
    # SCENE 6: Multiple Life Tracks (37.60s – 42.57s)
    # Voice: "And run separate tracks for your daily wird, your Ramadan goal, or your Hifz memorization."
    # =========================================================================
    elif t < 42.57:
        st = t - 37.60
        phone_rot = -0.11 + math.sin(st * 0.9) * 0.04
        paste_phone(img, WIDTH//2, phone_cy, "tracks", scroll_y=0, rot_y=phone_rot)

        draw.text((WIDTH//2, 140), "MULTIPLE LIFE TRACKS", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((WIDTH//2, 205), "Separate Your Goals", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((WIDTH//2, 275), "Daily Wird • Ramadan • Hifz Track", fill=(245, 224, 140), font=f_h2, anchor="mm")
        draw.text((WIDTH//2, 320), "Each track keeps its own independent bookmark.", fill=(160, 190, 175), font=f_h3, anchor="mm")

        draw_pill(draw, WIDTH//2, 380, "Separate Your Reading Disciplines", (16, 44, 30), (212, 175, 55), (245, 224, 140))

    # =========================================================================
    # SCENE 7: Offline AI Companion Chat (42.57s – 51.25s)
    # Voice: "Need help understanding an Ayah? Open the offline AI Companion. Ask about word meanings, Tafsir, or pause your streak without guilt."
    # =========================================================================
    elif t < 51.25:
        st = t - 42.57
        phone_rot = 0.09 - math.sin(st * 0.8) * 0.04
        paste_phone(img, WIDTH//2, phone_cy, "chat", scroll_y=0, rot_y=phone_rot)

        draw.text((WIDTH//2, 140), "OFFLINE AI COMPANION", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((WIDTH//2, 205), "Ayah Tafsir & Meanings", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((WIDTH//2, 275), "Ask anything about word meanings.", fill=(245, 224, 140), font=f_h2, anchor="mm")
        draw.text((WIDTH//2, 320), "Pause streak when you travel with zero guilt.", fill=(160, 190, 175), font=f_h3, anchor="mm")

        draw_pill(draw, WIDTH//2, 380, "100% Offline Chatbot • Zero Data Needed", (16, 44, 30), (212, 175, 55), (245, 224, 140))

    # =========================================================================
    # SCENE 8: Whisper AI Speech Auditor (51.25s – 59.10s)
    # Voice: "And tap recite to check your recitation word-for-word with on-device Whisper AI. Zero internet. Zero data bundles."
    # =========================================================================
    elif t < 59.10:
        st = t - 51.25
        phone_rot = -0.08 + math.sin(st * 0.8) * 0.03
        paste_phone(img, WIDTH//2, phone_cy, "speech", scroll_y=0, rot_y=phone_rot)

        draw.text((WIDTH//2, 140), "WORD-BY-WORD SPEECH AUDITOR", fill=(52, 211, 153), font=f_caption, anchor="mm")
        draw.text((WIDTH//2, 205), "100% Offline Whisper AI", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((WIDTH//2, 275), "Tap recite, and read aloud.", fill=(245, 224, 140), font=f_h2, anchor="mm")
        draw.text((WIDTH//2, 320), "Checks your recitation word-for-word.", fill=(160, 190, 175), font=f_h3, anchor="mm")

        # 24 Live Oscillating Gold Waveform Bars
        wave_y = 390
        bars = 24
        bar_w = 12
        for b in range(bars):
            bh = int(32 * (math.sin(t * 14 + b * 0.45) * 0.5 + 0.5) + 8)
            bx = WIDTH//2 - 190 + b * 16
            draw.rectangle([bx, wave_y - bh, bx + bar_w, wave_y + bh], fill=(212, 175, 55))

        draw_pill(draw, WIDTH//2, 465, "Zero Internet • Zero Data Bundles • Private", (16, 44, 30), (52, 211, 153), (255, 255, 255))

    # =========================================================================
    # SCENE 9: African / Ghanaian Madrasa Heritage (59.10s – 64.37s)
    # Voice: "Adapted for African and Ghanaian madrasas: recite forward, or in reverse from Juz 'Amma."
    # =========================================================================
    elif t < 64.37:
        st = t - 59.10
        phone_rot = 0.12 - math.sin(st * 0.8) * 0.04
        paste_phone(img, WIDTH//2, phone_cy, "direction", scroll_y=0, rot_y=phone_rot)

        draw.text((WIDTH//2, 140), "ROOTED IN MADRASA HERITAGE", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((WIDTH//2, 205), "Ghanaian & African Style", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((WIDTH//2, 275), "Start forward, or reverse from Juz 'Amma.", fill=(245, 224, 140), font=f_h2, anchor="mm")
        draw.text((WIDTH//2, 320), "Honors how millions memorized in West Africa.", fill=(160, 190, 175), font=f_h3, anchor="mm")

        draw_pill(draw, WIDTH//2, 380, "Ghanaian Madrasa Style • Towards Al-Fatihah", (16, 46, 32), (212, 175, 55), (245, 224, 140))

    # =========================================================================
    # SCENE 10: Independent Surahs Reading (64.37s – 69.23s)
    # Voice: "Plus, read Surah Al-Kahf on Friday freely without ever messing up your daily bookmark."
    # =========================================================================
    elif t < 69.23:
        st = t - 64.37
        phone_rot = -0.10 + math.sin(st * 0.8) * 0.04
        paste_phone(img, WIDTH//2, phone_cy, "surahs", scroll_y=0, rot_y=phone_rot)

        draw.text((WIDTH//2, 140), "INDEPENDENT SURAHS READING", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((WIDTH//2, 205), "Read Freely on Friday", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((WIDTH//2, 275), "Read Al-Kahf without messing up daily wird.", fill=(245, 224, 140), font=f_h2, anchor="mm")
        draw.text((WIDTH//2, 320), "Every Surah keeps its own independent bookmark.", fill=(160, 190, 175), font=f_h3, anchor="mm")

        draw_pill(draw, WIDTH//2, 380, "114 Surahs Directory • Never Resets Bookmark", (14, 42, 30), (16, 185, 129), (110, 231, 183))

    # =========================================================================
    # SCENE 11: Grand Finale & Call to Action (69.23s – 74.56s)
    # Voice: "Your Qur'an habit, rebuilt for real life. Download Wird free on Android."
    # =========================================================================
    else:
        st = t - 69.23
        # Rotating 16-point golden Islamic rosette halo
        draw_rosette(draw, WIDTH//2, cy - 60, 360, (212, 175, 55), rot=st * 0.12)
        # Hero Phone upright with true Home Screen
        paste_phone(img, WIDTH//2, phone_cy, "home", scroll_y=0, rot_y=0.0)

        draw.text((WIDTH//2, 130), "YOUR QUR'AN HABIT", fill=(212, 175, 55), font=f_caption, anchor="mm")
        draw.text((WIDTH//2, 195), "Rebuilt for Real Life.", fill=(255, 255, 255), font=f_hero, anchor="mm")
        draw.text((WIDTH//2, 265), "Presence over pressure.", fill=(245, 224, 140), font=f_h2, anchor="mm")

        # Glowing CTA Button at the bottom
        cta_y = HEIGHT - 200
        draw_round_rect(draw, [WIDTH//2 - 250, cta_y - 44, WIDTH//2 + 250, cta_y + 44], 24, fill=(212, 175, 55))
        draw.text((WIDTH//2, cta_y), "Download Wird Free", fill=(7, 18, 13), font=f_h2, anchor="mm")
        draw.text((WIDTH//2, cta_y + 65), "100% Offline • Zero Data Bundles • Android", fill=(160, 190, 175), font=f_body, anchor="mm")

    return img

def main():
    print(f"Starting Master Commercial Render ({TOTAL_FRAMES} frames @ {FPS} FPS)...")
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
        frame = render_frame_master(i)
        proc.stdin.write(frame.tobytes())
        if i % 150 == 0:
            pct = (i / TOTAL_FRAMES) * 100
            print(f"  Master Commercial: {pct:.1f}% ({i}/{TOTAL_FRAMES} frames)")

    proc.stdin.close()
    proc.wait()

    elapsed = time.time() - start_time
    file_size_mb = os.path.getsize(OUTPUT_VIDEO) / (1024 * 1024)
    print(f"Master Commercial complete in {elapsed:.1f}s! File: {OUTPUT_VIDEO} ({file_size_mb:.2f} MB)")

if __name__ == "__main__":
    main()
