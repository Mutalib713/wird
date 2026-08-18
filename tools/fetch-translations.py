#!/usr/bin/env python3
"""
Fetch the three translations Mutalib chose and write them into the app's assets.

PROFILE.md section 5z: Saheeh International (20), Gumi Hausa (32), Transliteration (57),
bundled rather than fetched at runtime so the app works offline from the moment it installs.

One file per page per translation: assets/translations/{id}/{page}.json. Per-page because the
app renders a page at a time, so a lookup is a filename rather than an index, and nothing has
to hold a megabyte in memory on a Tecno.

Sacred Rule 2 governs this script. Text is written as the API returns it, with removals only,
never edits:

  - Footnote markers go whole, tag AND the digit inside. See the FOOTNOTE note below; this is
    the single biggest trap in this file and it has already caught me once.
  - Any other HTML tag is stripped, because the app cannot render it.
  - Whitespace is collapsed, and a gap left where a marker used to sit is closed.
  - Nothing else. No rewrapping, no rewording, no cleaning up the transliteration's
    AA-for-ayn scheme. He was offered that and declined.

Run:  python tools/fetch-translations.py
      python tools/fetch-translations.py --force    (refetch pages already present)
"""

import http.client
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request

API = "https://api.quran.com/api/v4"
PAGES = 604

# id -> human name, for the summary only. The folder is the id, so the app never maps a name
# to a number.
SOURCES = {
    20: "Saheeh International",
    32: "Gumi (Hausa)",
    57: "Transliteration",
}

OUT = os.path.join("app", "src", "main", "assets", "translations")

# The API answers curl but returns 403 to urllib's default "Python-urllib/3.13".
# Measured 2026-08-18; not a rate limit, the very first request fails.
HEADERS = {"User-Agent": "wird/0.1 (+https://github.com/Mutalib713/wird)"}

# Everything worth retrying. RemoteDisconnected lives under http.client and is NOT a URLError,
# which is how two runs died silently — see the note in fetch_page.
RETRYABLE = (
    urllib.error.URLError,
    http.client.HTTPException,
    ConnectionError,
    TimeoutError,
    json.JSONDecodeError,
)

# quran.com drops the connection under a fast unpaced loop. A short pause between pages costs
# about 90 seconds over the whole mushaf and has held where hammering did not.
PAUSE_SECONDS = 0.15

# A footnote marker looks like:  <sup foot_note=195932>1</sup>
#
# The DIGIT INSIDE has to be removed along with the tag. The first version of this script
# stripped tags only, which left the number glued to the preceding word: "In the name of
# Allah,1 the Entirely Merciful". Measured before it shipped: 1,400 of Saheeh's 6,236 verses
# were affected, so 22% of the Qur'an would have rendered with stray digits. That is exactly
# the flaw Maududi was rejected for in PROFILE.md section 5z, and it was about to ship in the
# translation chosen instead of it.
FOOTNOTE = re.compile(r"<sup[^>]*>.*?</sup>", re.DOTALL)
TAG = re.compile(r"<[^>]+>")
SPACE_BEFORE_PUNCT = re.compile(r"\s+([,.;:!?])")
WHITESPACE = re.compile(r"\s+")


def clean(text):
    """Remove markers and tags, then tidy whitespace. Never changes a word."""
    text = FOOTNOTE.sub("", text)
    text = TAG.sub("", text)
    # Removing a marker that sat against punctuation can leave " ," behind. The backreference
    # KEEPS the punctuation and drops only the space. An earlier version of this line replaced
    # with an empty string and would have deleted the punctuation itself.
    text = SPACE_BEFORE_PUNCT.sub(r"\1", text)
    return WHITESPACE.sub(" ", text).strip()


def fetch_page(page, attempt=1):
    ids = ",".join(str(i) for i in SOURCES)
    url = f"{API}/verses/by_page/{page}?translations={ids}&per_page=50"
    try:
        req = urllib.request.Request(url, headers=HEADERS)
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.loads(r.read().decode("utf-8"))
    except RETRYABLE:
        if attempt >= 6:
            raise
        # A single page failing must not lose the whole run.
        #
        # ⚠ The first version caught only urllib.error.URLError, which does NOT cover
        # http.client.RemoteDisconnected — and that is exactly what quran.com throws when it
        # has had enough. Two runs died partway (at page 398 and 403) with a traceback and an
        # exit code of 0, which looked like success until the page count was checked.
        time.sleep(attempt * 3)
        return fetch_page(page, attempt + 1)


def main():
    force = "--force" in sys.argv
    for sid in SOURCES:
        os.makedirs(os.path.join(OUT, str(sid)), exist_ok=True)

    written = skipped = 0
    for page in range(1, PAGES + 1):
        paths = {sid: os.path.join(OUT, str(sid), str(page) + ".json") for sid in SOURCES}
        if not force and all(os.path.exists(p) for p in paths.values()):
            skipped += 1
            continue

        data = fetch_page(page)
        rows = {sid: [] for sid in SOURCES}
        for v in data.get("verses", []):
            key = v["verse_key"]
            for t in v.get("translations", []):
                sid = t.get("resource_id")
                if sid in rows:
                    rows[sid].append({"v": key, "t": clean(t["text"])})

        for sid, path in paths.items():
            if not rows[sid]:
                print("  !! page " + str(page) + ": nothing for source " + str(sid),
                      file=sys.stderr)
            with open(path, "w", encoding="utf-8") as f:
                json.dump(rows[sid], f, ensure_ascii=False, separators=(",", ":"))
        written += 1
        time.sleep(PAUSE_SECONDS)

        if page % 100 == 0:
            print("  " + str(page) + "/" + str(PAGES) + " ...", flush=True)

    print("done: " + str(written) + " pages written, " + str(skipped) + " already present")

    total = 0
    for sid in SOURCES:
        d = os.path.join(OUT, str(sid))
        files = os.listdir(d)
        size = sum(os.path.getsize(os.path.join(d, f)) for f in files)
        total += size
        print("  {:<4} {:<24} {:>4} files  {:.2f} MB".format(
            sid, SOURCES[sid], len(files), size / 1024 / 1024))
    print("  actual bytes: {:.2f} MB".format(total / 1024 / 1024))
    print("  (du reports about 3x this: 1812 small files each take a whole 4 KB disk block.")
    print("   The APK stores real bytes and compresses them, so du is not the number to use.)")


if __name__ == "__main__":
    main()
