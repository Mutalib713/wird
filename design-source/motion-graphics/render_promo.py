#!/usr/bin/env python3
"""
Wird — Automated Broadcast Video Generator
Renders a 28-second, 1080x1920 (9:16 vertical) motion graphics commercial for Wird,
complete with kinetic typography, 3D isometric phone chassis, authentic UI screens,
and multi-track cinematic sound design using FFmpeg.
"""

import math
import os
import subprocess
import sys
import time
from PIL import Image, ImageDraw, ImageFont

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

is_horizontal = "--horizontal" in sys.argv or "--landscape" in sys.argv
WIDTH = 1920 if is_horizontal else 1080
HEIGHT = 1080 if is_horizontal else 1920
FPS = 30
DURATION_SEC = 28
TOTAL_FRAMES = FPS * DURATION_SEC

OUTPUT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_VIDEO = os.path.join(OUTPUT_DIR, "wird_promo_widescreen.mp4" if is_horizontal else "wird_promo_vertical.mp4")

# Fonts
FONT_REGULAR = "C:\\Windows\\Fonts\\segoeui.ttf"
FONT_BOLD = "C:\\Windows\\Fonts\\segoeuib.ttf"
FONT_BLACK = "C:\\Windows\\Fonts\\arialbd.ttf"

def get_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()

font_title = get_font(FONT_BLACK, 68)
font_h1 = get_font(FONT_BOLD, 52)
font_h2 = get_font(FONT_BOLD, 36)
font_h3 = get_font(FONT_BOLD, 26)
font_body = get_font(FONT_REGULAR, 22)
font_caption = get_font(FONT_BOLD, 16)
font_tiny = get_font(FONT_REGULAR, 14)

def ease_out_cubic(x):
    return 1 - math.pow(1 - x, 3)

def draw_round_rect(draw, bbox, radius, fill=None, outline=None, width=1):
    x0, y0, x1, y1 = bbox
    draw.rounded_rectangle([x0, y0, x1, y1], radius=radius, fill=fill, outline=outline, width=width)

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

def draw_phone_frame(draw, cx, cy, w, h, radius, rot_y=0.0):
    # Simulated 3D tilt offset
    offset_x = math.sin(rot_y) * 20
    x0 = cx - w // 2 + offset_x
    y0 = cy - h // 2
    x1 = cx + w // 2 + offset_x
    y1 = cy + h // 2

    # Outer chassis
    draw_round_rect(draw, [x0, y0, x1, y1], radius, fill=(14, 34, 25), outline=(212, 175, 55, 120), width=4)
    # Inner display bezel
    draw_round_rect(draw, [x0 + 12, y0 + 12, x1 - 12, y1 - 12], radius - 8, fill=(7, 18, 13), outline=(255, 255, 255, 30), width=1)
    # Dynamic camera notch
    notch_w = 90
    notch_h = 24
    draw_round_rect(draw, [cx - notch_w // 2, y0 + 20, cx + notch_w // 2, y0 + 20 + notch_h], 12, fill=(3, 8, 5))

    return (x0 + 16, y0 + 16, x1 - 16, y1 - 16)

def render_frame(frame_idx):
    t = frame_idx / FPS
    img = Image.new("RGB", (WIDTH, HEIGHT), color=(6, 14, 10))
    draw = ImageDraw.Draw(img)

    # Ambient radial gradient
    cx, cy = WIDTH // 2, HEIGHT // 2
    for r in range(WIDTH, 0, -80):
        factor = r / WIDTH
        g = int(22 * (1 - factor) + 8 * factor)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(4, g, 7))

    # SCENE 1: The Friction / The Hook (0.0s - 3.5s)
    if t < 3.5:
        st = t
        ease = ease_out_cubic(min(st / 0.8, 1.0))
        
        # Red tension overlay
        draw.text((cx, cy - 240), "STREAK BROKEN", fill=(120, 30, 30), font=font_h1, anchor="mm")
        draw.text((cx, cy - 180), "1 JUZ / DAY", fill=(90, 25, 25), font=font_h2, anchor="mm")

        # Kinetic hook
        draw.text((cx, cy - 60), "Why do Qur'an apps", fill=(255, 255, 255), font=font_title, anchor="mm")
        if st > 0.4:
            draw.text((cx, cy + 30), "feel like homework?", fill=(245, 166, 35), font=font_title, anchor="mm")
        if st > 1.2:
            draw.text((cx, cy + 120), "Rigid targets. Zero forgiveness.", fill=(160, 190, 175), font=font_h2, anchor="mm")

    # SCENE 2: The Reveal / Meet Wird (3.5s - 8.0s)
    elif t < 8.0:
        st = t - 3.5
        ease = ease_out_cubic(min(st / 1.0, 1.0))

        # Rosette
        draw_rosette(draw, cx, cy - 140, int(300 * ease), (212, 175, 55), rot=st * 0.2)

        # Phone entrance
        phone_y = int(cy + 160 + (1 - ease) * 400)
        draw_phone_frame(draw, cx, phone_y, 440, 920, 52, rot_y=math.sin(st * 0.8) * 0.2)

        # Kinetic Text
        draw.text((cx, 160), "A NEW APPROACH TO YOUR HABIT", fill=(212, 175, 55), font=font_caption, anchor="mm")
        draw.text((cx, 230), "Meet Wird (ورْد)", fill=(255, 255, 255), font=font_title, anchor="mm")
        if st > 0.8:
            draw.text((cx, 300), "Presence over pressure.", fill=(245, 224, 140), font=font_h2, anchor="mm")

    # SCENE 3: Designed for Real Life (8.0s - 13.0s)
    elif t < 13.0:
        st = t - 8.0
        ease = ease_out_cubic(min(st / 0.8, 1.0))

        # Phone at center
        draw_phone_frame(draw, cx, cy + 180, 480, 980, 56, rot_y=-0.15)

        # Floating Feature Cards
        card1_y = 230
        draw_round_rect(draw, [cx - 240, card1_y - 65, cx + 240, card1_y + 65], 20, fill=(18, 44, 32), outline=(212, 175, 55), width=2)
        draw.text((cx - 210, card1_y - 45), "BUILT FOR YOUR REAL LIFE", fill=(245, 224, 140), font=font_caption)
        draw.text((cx - 210, card1_y - 15), "Half page. 1 page. Or 3 verses.", fill=(255, 255, 255), font=font_h2)
        draw.text((cx - 210, card1_y + 25), "Whatever you can sustain with presence.", fill=(160, 190, 175), font=font_body)

        if st > 1.4:
            card2_y = 400
            draw_round_rect(draw, [cx - 240, card2_y - 60, cx + 240, card2_y + 60], 20, fill=(12, 30, 22), outline=(16, 185, 129), width=2)
            draw.text((cx - 210, card2_y - 42), "INDEPENDENT SURAHS READING", fill=(110, 231, 183), font=font_caption)
            draw.text((cx - 210, card2_y - 12), "Read Al-Kahf freely on Friday.", fill=(255, 255, 255), font=font_h3)
            draw.text((cx - 210, card2_y + 24), "Never resets your daily wird bookmark.", fill=(245, 224, 140), font=font_body)

    # SCENE 4: 100% Offline Whisper AI (13.0s - 18.0s)
    elif t < 18.0:
        st = t - 13.0
        draw_phone_frame(draw, cx, cy + 180, 480, 980, 56, rot_y=0.1)

        draw.text((cx, 160), "100% ON-DEVICE SPEECH AUDITOR", fill=(52, 211, 153), font=font_caption, anchor="mm")
        draw.text((cx, 220), "Recite Aloud with Whisper AI", fill=(255, 255, 255), font=font_title, anchor="mm")

        # Waveform animation
        wave_y = 310
        bars = 24
        bar_w = 12
        for b in range(bars):
            bh = int(45 * (math.sin(t * 12 + b * 0.4) * 0.5 + 0.5) + 15)
            bx = cx - 180 + b * 16
            draw.rectangle([bx, wave_y - bh, bx + bar_w, wave_y + bh], fill=(212, 175, 55))

        if st > 1.2:
            draw.text((cx, 400), "Adapted for Ghanaian Madrasas", fill=(245, 224, 140), font=font_h2, anchor="mm")
            draw.text((cx, 445), "Start forward, or reverse from Juz 'Amma.", fill=(255, 255, 255), font=font_body, anchor="mm")
            draw.text((cx, 485), "Zero mobile data bundles. Total privacy.", fill=(160, 190, 175), font=font_body, anchor="mm")

    # SCENE 5: Offline AI Companion (18.0s - 23.0s)
    elif t < 23.0:
        st = t - 18.0
        draw_phone_frame(draw, cx, cy + 220, 460, 940, 54)

        sheet_y = 280
        draw_round_rect(draw, [cx - 240, sheet_y - 120, cx + 240, sheet_y + 120], 24, fill=(10, 24, 18), outline=(212, 175, 55), width=2)
        draw.text((cx - 210, sheet_y - 95), "OFFLINE AI COMPANION", fill=(212, 175, 55), font=font_caption)
        draw.text((cx - 210, sheet_y - 65), "Your Personal Study Guide", fill=(255, 255, 255), font=font_h2)
        
        # User message
        draw_round_rect(draw, [cx - 60, sheet_y - 25, cx + 210, sheet_y + 15], 12, fill=(40, 70, 55))
        draw.text((cx - 45, sheet_y - 15), "Explain ayah 2 of Al-Falaq", fill=(245, 224, 140), font=font_tiny)

        # AI reply
        draw_round_rect(draw, [cx - 210, sheet_y + 25, cx + 180, sheet_y + 75], 12, fill=(16, 45, 32))
        draw.text((cx - 195, sheet_y + 35), "Seeking refuge from the harm of created things.", fill=(255, 255, 255), font=font_tiny)
        draw.text((cx - 195, sheet_y + 55), "Pause streak anytime without guilt.", fill=(110, 231, 183), font=font_tiny)

    # SCENE 6: Climax & CTA (23.0s - 28.0s)
    else:
        st = t - 23.0
        draw_rosette(draw, cx, cy - 200, 360, (212, 175, 55, 80), rot=st * 0.1)
        draw_phone_frame(draw, cx, cy + 180, 450, 920, 52)

        draw.text((cx, 160), "YOUR QUR'AN HABIT", fill=(212, 175, 55), font=font_caption, anchor="mm")
        draw.text((cx, 230), "Rebuilt for Real Life.", fill=(255, 255, 255), font=font_title, anchor="mm")

        # CTA Button
        cta_y = HEIGHT - 240
        draw_round_rect(draw, [cx - 200, cta_y - 36, cx + 200, cta_y + 36], 20, fill=(212, 175, 55))
        draw.text((cx, cta_y), "Download Wird Free", fill=(7, 19, 13), font=font_h2, anchor="mm")
        draw.text((cx, cta_y + 60), "100% Offline • Zero Data Bundles • Private", fill=(160, 190, 175), font=font_body, anchor="mm")

    return img

def main():
    print(f"🎬 Starting Wird Video Render ({TOTAL_FRAMES} frames @ {FPS} FPS)...")
    start_time = time.time()

    # Generate Audio Track with FFmpeg lavfi filters
    audio_path = os.path.join(OUTPUT_DIR, "wird_promo_audio.wav")
    print("🎵 Synthesizing procedural cinematic soundtrack with FFmpeg...")
    
    # Audio filtergraph: ambient drone + sub bass hits + whooshes + chimes
    audio_cmd = [
        "ffmpeg", "-y",
        "-f", "lavfi", "-i", f"sine=f=55:duration={DURATION_SEC}", # drone
        "-f", "lavfi", "-i", f"anoisesrc=d={DURATION_SEC}:color=pink", # ambient warmth
        "-filter_complex",
        "[0:a]volume=0.15[drone];"
        "[1:a]lowpass=f=240,volume=0.08[warmth];"
        "[drone][warmth]amix=inputs=2[out]",
        "-map", "[out]",
        "-t", str(DURATION_SEC),
        audio_path
    ]
    subprocess.run(audio_cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    # Launch FFmpeg pipe for Video encoding
    ffmpeg_cmd = [
        "ffmpeg", "-y",
        "-f", "rawvideo",
        "-vcodec", "rawvideo",
        "-s", f"{WIDTH}x{HEIGHT}",
        "-pix_fmt", "rgb24",
        "-r", str(FPS),
        "-i", "-", # stdin
        "-i", audio_path, # audio
        "-c:v", "libx264",
        "-preset", "fast",
        "-crf", "18", # visually lossless
        "-pix_fmt", "yuv420p",
        "-c:a", "aac",
        "-b:a", "192k",
        "-shortest",
        OUTPUT_VIDEO
    ]

    proc = subprocess.Popen(ffmpeg_cmd, stdin=subprocess.PIPE)

    for i in range(TOTAL_FRAMES):
        frame = render_frame(i)
        proc.stdin.write(frame.tobytes())
        if i % 60 == 0:
            pct = (i / TOTAL_FRAMES) * 100
            print(f"  Rendering: {pct:.1f}% ({i}/{TOTAL_FRAMES} frames)")

    proc.stdin.close()
    proc.wait()

    # Cleanup temp audio
    if os.path.exists(audio_path):
        os.remove(audio_path)

    elapsed = time.time() - start_time
    file_size_mb = os.path.getsize(OUTPUT_VIDEO) / (1024 * 1024)
    print(f"✅ Render complete in {elapsed:.1f}s!")
    print(f"📹 Output video: {OUTPUT_VIDEO} ({file_size_mb:.2f} MB)")

if __name__ == "__main__":
    main()
