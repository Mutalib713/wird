#!/usr/bin/env python3
"""
Fetch the Qur'an's Arabic text and write it into the app's assets.

PLAN task 14's second half. The recitation checker can transcribe what was recited, but the
app had nothing to compare it against: the mushaf is stored as GLYPH CODES for a per-page
font, not as words. This is the missing side of that comparison.

⚠ SACRED RULE 2 GOVERNS THIS FILE ABSOLUTELY. Qur'anic text comes from a verified source and
is never generated, guessed, corrected or "cleaned up". This script writes exactly what
api.quran.com returns and does nothing else to it.

WHICH TEXT, AND WHY IT IS NOT THE UTHMANI ONE
---------------------------------------------
Quran.com serves several spellings. The obvious choice is `text_uthmani`, which is what the
mushaf prints. This uses `text_imlaei` instead, and the reason is what the text is FOR:

  * It is never displayed. The page on screen is drawn from the QCF font, exactly as before.
  * Its only job is to be compared against a speech model's output, and a speech model
    produces ordinary written Arabic - not the Uthmani orthography with its superscript
    alifs, small seen and pause marks.

Comparing ASR output against Uthmani would flag half of every page as a "mistake" because the
SPELLING differs, which is precisely the false accusation this feature must never make.

One file per page - assets/arabic/{page}.json - matching how the translations are stored, so a
lookup is a filename rather than an index and nothing holds the whole Qur'an in memory.

Run:  python tools/fetch-arabic.py
      python tools/fetch-arabic.py --force
"""

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request

API = "https://api.quran.com/api/v4/verses/by_page/{page}?fields=text_imlaei&per_page=300"
PAGES = 604
OUT = os.path.join("app", "src", "main", "assets", "arabic")

# The Qur'an has exactly this many verses. A run that does not add up to it did not finish,
# whatever the exit code says - the same integrity check the translations use, and the same
# reason: two fetch runs once died mid-way and looked successful.
TOTAL_VERSES = 6236

# ⚠ The API answers urllib's default agent with 403. Same header the translations fetcher uses.
HEADERS = {"User-Agent": "wird/0.1 (+https://github.com/Mutalib713/wird)"}


def fetch(page: int, attempts: int = 4):
    """One page, with retries. Returns a list of {v, t} or raises."""
    url = API.format(page=page)
    last = None
    for attempt in range(attempts):
        try:
            request = urllib.request.Request(url, headers=HEADERS)
            with urllib.request.urlopen(request, timeout=30) as response:
                data = json.loads(response.read().decode("utf-8"))
            verses = data.get("verses", [])
            out = []
            for verse in verses:
                key = verse.get("verse_key")
                text = verse.get("text_imlaei")
                # ⚠ A verse with no text is a hole, not a blank line. Refuse the page rather
                # than write a gap that would later read as "he skipped this ayah".
                if not key or not text:
                    raise ValueError(f"page {page}: verse {key} came back empty")
                out.append({"v": key, "t": text.strip()})
            if not out:
                raise ValueError(f"page {page}: no verses")
            return out
        except (urllib.error.HTTPError, urllib.error.URLError, OSError, ValueError,
                json.JSONDecodeError) as e:
            # ⚠ urllib.error.URLError does NOT cover http.client.RemoteDisconnected, which is
            # how two earlier fetch runs in this project died with exit code 0. OSError does.
            last = e
            time.sleep(1.5 * (attempt + 1))
    raise RuntimeError(f"page {page} failed after {attempts} attempts: {last}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--force", action="store_true", help="refetch pages already written")
    args = parser.parse_args()

    os.makedirs(OUT, exist_ok=True)
    written = 0
    verses = 0

    for page in range(1, PAGES + 1):
        path = os.path.join(OUT, f"{page}.json")
        if os.path.exists(path) and not args.force:
            with open(path, encoding="utf-8") as f:
                verses += len(json.load(f))
            continue

        rows = fetch(page)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(rows, f, ensure_ascii=False, separators=(",", ":"))
        written += 1
        verses += len(rows)
        if page % 50 == 0:
            print(f"  page {page}/{PAGES}, {verses} verses so far")

    size = sum(
        os.path.getsize(os.path.join(OUT, n)) for n in os.listdir(OUT) if n.endswith(".json")
    )
    print(f"\nwrote {written} pages, {verses} verses, {size / 1024 / 1024:.2f} MB")

    # The integrity check that means more than an exit code.
    if verses != TOTAL_VERSES:
        print(f"FAIL: expected {TOTAL_VERSES} verses, counted {verses}", file=sys.stderr)
        return 1
    print(f"OK: {TOTAL_VERSES} verses, which is the Qur'an's own count")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
