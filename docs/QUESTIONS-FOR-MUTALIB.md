# Open questions

Things a session hit that need Mutalib's decision rather than a guess. Rewritten 2026-10-04; the
August list was mostly settled (task 11 deleted, the companion's engine decided, the icon made).

## Needs a decision

- **The new onboarding, four items.** Quick start from An-Nās instead of An-Naba'; "which way"
  and "where to start" on one screen; the splash button "See how Wird works"; the phone's own
  fonts instead of Literata and Amiri. Details at the end of
  [design-source/onboarding/IMPLEMENT.md](../design-source/onboarding/IMPLEMENT.md).
- **Word search: keep the 911 KB?** Built (PLAN task 26). The search text ships ready-made, which
  makes the first search take about 160 ms on a release build instead of many seconds, and adds
  911 KB to a 10.8 MB APK. The other way costs no space and makes the first search slow on a
  freshly installed phone (51 s on the emulator before any speed work). PROFILE § 5bg.
- **Broken letters in Gumi's Hausa.** Three characters stand in for Hausa letters throughout: `¡`
  for Ƙ (91 times, "Rãnar ¡iyãma" for Rãnar Ƙiyãma), `¦` for Ɗ (43, "¦an Maryama"), `¥` for Ɓ
  (2, "¥arnã" in 30:41 and 41:42), plus one Tamil letter inside a word in 59:23 ("Mai t஛astãwa").
  Readers see them on the reading page, and a search for "kiyama" misses those 91 verses. Quran.com
  and the fawazahmed0 copy carry the same characters, so the fault is upstream, and fixing it means
  correcting the translation's text ourselves. The first three mappings are clear from context;
  59:23 isn't. Fix them, ideally with a Hausa reader checking a few, or leave the text as published?
- **Dark mode for the new onboarding.** Not designed. The brief says to build the light version
  only for now.
- **Release signing.** The app is still signed with the debug key, and the showcase site offers a
  debug build. A real key is a one-way door: lose it and the app can never be updated in place.
- **Task 17, tester feedback.** The showcase is live, with the feedback form removed. How testers
  should reach him is his call.
- **Task 23, WhatsApp for himself.** Needs his Meta developer console setup. ⚠ Never link his
  personal number to an unofficial gateway.

## Only a real phone can answer

- **Task 14:** how accurate Recite & Review is, recited in his voice, in his room.
- **Tasks 10 and 15:** whether reminders and the widget survive overnight on a Tecno, Infinix or
  itel phone. The emulator proves only the easy case.
- **Reminders for several tracks at once,** firing on the Pixel.
- **Madrasa order (§ 5bf)** over real days: unit tests replay the whole mushaf, and one day was
  checked on the emulator.
