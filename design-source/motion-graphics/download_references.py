#!/usr/bin/env python3
"""
Downloads the 4 reference TikTok videos without watermarks using direct API streams,
saving them to design-source/motion-graphics/references/
"""

import json
import os
import sys
import urllib.request

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

VIDEOS = [
    ("ref1_promogen.mp4", "https://www.tiktok.com/@promogen.app/video/7549541179306659094"),
    ("ref2_limitless.mp4", "https://www.tiktok.com/@limitless.media_/video/7616364510693035282"),
    ("ref3_iwkii.mp4", "https://www.tiktok.com/@iwkii_tt/video/7562645617407724822"),
    ("ref4_muhaavo.mp4", "https://www.tiktok.com/@muhaavo/video/7545898951279512854"),
]

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "references")
os.makedirs(OUT_DIR, exist_ok=True)

for filename, tiktok_url in VIDEOS:
    dest_path = os.path.join(OUT_DIR, filename)
    if os.path.exists(dest_path) and os.path.getsize(dest_path) > 100000:
        print(f"Already downloaded: {filename} ({os.path.getsize(dest_path)} bytes)")
        continue

    api_url = f"https://www.tikwm.com/api/?url={tiktok_url}"
    print(f"Fetching stream metadata for {filename}...")
    req = urllib.request.Request(api_url, headers={"User-Agent": "Mozilla/5.0"})
    try:
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            if data.get("code") == 0:
                video_url = data["data"]["play"]
                title = data["data"].get("title", "")
                music = data["data"].get("music", "")
                print(f"  Title: {title[:80]}...")
                print(f"  Music: {music[:60] if music else 'None'}")
                print(f"  Downloading video to {filename}...")

                v_req = urllib.request.Request(video_url, headers={"User-Agent": "Mozilla/5.0"})
                with urllib.request.urlopen(v_req) as v_resp, open(dest_path, "wb") as f_out:
                    f_out.write(v_resp.read())

                print(f"  Saved {filename}: {os.path.getsize(dest_path)} bytes")
            else:
                print(f"  API Error for {filename}: {data.get('msg')}")
    except Exception as e:
        print(f"  Download error for {filename}: {e}")

print("All reference downloads finished!")
