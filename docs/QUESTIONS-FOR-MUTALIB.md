# Open questions

Things a session hit that need Mutalib's decision rather than a guess. Added as they come up
during autonomous work; cleared when we talk.

## Needs a decision before it can be built

- **Task 11 — share a recording to WhatsApp. Probably delete it.**
  PROFILE.md § 5a already says this task loses its reason to exist now that he will not be
  sending recordings to anyone, and that it should be **cut rather than built** — but it also
  says *"confirm before deleting it."* So it sits here. **Question: delete task 11?**

- **Task 22 — what powers the companion's understanding.**
  Rules, an on-device small model, or a cloud LLM. He said *"we will decide when we get
  there"*, and the chat interface is now built, so we are there. § 5m records the risk in
  full: the chat box invites anything and the parser handles about seven phrasings. Typing
  "what does this surah mean" gets a shrug, and that is § 5h's approved feature.

- **Task 16 — the app icon.**
  The palette is pinned so the colours are settled, but an icon is a creative choice and not
  a session's to make. `MissingApplicationIcon` stays disabled in lint until it exists.

- **Tasks 17 and 18 — the Vercel page and tester feedback.**
  Need his accounts and his phone number.

- **Task 23 — WhatsApp for himself only.**
  Needs Meta console setup that only he can do. ⚠ And CLAUDE.md's standing rule: never link
  his personal number to an unofficial gateway.

## Unblocked, queued for autonomous work

In the order they will be attempted, after translations:

1. **Task 19 — export everything.** No decisions needed: recordings, the day log, the
   conversation and the bookmarks into one folder that opens on a computer.
2. **Task 21 — the commitment loop.** Reply to the nudge from the notification itself. Fully
   specified in PLAN.md and § 5b; task 8's scheduler already turns "after Isha" into a real
   alarm, so it is mostly wiring.
3. **Task 12 — deep links out to Quran for Android and Tarteel.** A ⚠ task where failure is a
   legitimate result: if the target apps accept no deep link, the buttons get deleted and
   PROFILE.md § 11 records why. That outcome needs no permission.
4. **Task 15, the buildable half.** Detect the manufacturer and show the right steps for that
   phone. ⚠ The *verification* needs a real Tecno or Infinix and cannot happen here.

## Verification that cannot be done here

- **Task 10's second half.** *"Still shows the right thing the next morning"* needs a night to
  pass **and a real Transsion phone** — surviving the OEMs that kill background work is the
  whole point, so the emulator proves the easy case only.
- **Task 15.** Same reason.
- **Task 14's accuracy numbers.** The recitation checker has to be measured on his voice, on
  his phone, in his room. Nothing here can stand in for that.
