// Walk the first-time experience as a new user and screenshot the phone at each step.
// Usage: node firstrun-shots.js <outdir> [dark]
const path = require('path');
const fs = require('fs');
const { chromium } = require(path.join('C:/Users/USER/MyClaudeProjects/_tools/node_modules/playwright'));

(async () => {
  const out = process.argv[2] || 'fr-shots', dark = process.argv[3] === 'dark';
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch({ channel: 'chrome' });
  const ctx = await browser.newContext({ viewport: { width: 1400, height: 1000 }, deviceScaleFactor: 2, colorScheme: dark ? 'dark' : 'light' });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  p.on('console', m => { if (m.type() === 'error') errors.push(m.text()); });
  await p.goto('file:///' + path.resolve(__dirname, 'prototype/index.html').split(path.sep).join('/'));
  await p.evaluate(() => localStorage.clear());
  await p.reload();
  await p.evaluate(() => document.fonts.ready);
  await p.click(`.seg[data-ctl="theme"] [data-v="${dark ? 'dark' : 'light'}"]`);
  await p.click('[data-fr-start="welcome"]');
  let n = 0;
  const shot = async name => { await p.waitForTimeout(320); n++; await (await p.$('#phone')).screenshot({ path: path.join(out, `${String(n).padStart(2, '0')}-${name}.png`) }); };
  const next = () => p.click('#fr [data-act="next"]');

  await shot('welcome');
  for (const s of ['tracks', 'multi', 'quran', 'consistent', 'companion']) { await next(); await shot(s); }
  await next(); await p.fill('#readerName', 'Mutalib'); await shot('ready');
  await next(); await p.fill('#fr [data-in="name"]', 'Juz \'Amma'); await shot('setup-name');
  await next(); await p.click('#fr [data-set="goal"][data-val="memorize"]'); await shot('setup-goal');
  await next(); await p.click('#fr [data-set="every"][data-val="no"]'); await shot('setup-days-none');
  for (const d of [0, 1, 2, 3]) await p.click(`#fr [data-day="${d}"]`);
  await shot('setup-days');
  await next(); await shot('setup-order');
  await next(); await shot('setup-start');
  await p.click('#fr [data-mode="ayah"]'); await p.selectOption('#fr select[data-in="surah"]', '2');
  await p.fill('#fr [data-in="ayah"]', '300'); await shot('setup-start-error');
  await p.fill('#fr [data-in="ayah"]', '255'); await shot('setup-start-ayah');
  await p.click('#fr [data-pick="114"]');
  await next(); await p.click('#fr [data-set="amount"][data-val="half"]'); await shot('setup-amount');
  await next(); await p.click('#fr [data-set="intention"][data-val="Memorize new verses"]'); await shot('setup-intention');
  await next(); await p.click('#fr [data-prayer="Isha"]'); await shot('setup-reminder');
  await p.click('#fr [data-set="remind"][data-val="time"]'); await p.click('#fr [data-repeat="custom"]'); await shot('setup-reminder-time');
  await p.click('#fr [data-set="remind"][data-val="prayer"]');
  await next(); await shot('review');
  await next(); await shot('mic');
  await p.click('#fr [data-act="mic-ask"]'); await shot('mic-android');
  await p.click('#fr [data-act="mic-yes"]'); await shot('created');
  await p.click('#fr [data-act="finish"]'); await p.waitForTimeout(400);
  for (let i = 1; i <= 6; i++) { await shot(`tour-${i}`); await p.click('#tourTip [data-t="next"]'); }
  await shot('home-after');
  await p.click('.nav [data-tab="wird"]'); await shot('wird-tracks');
  await p.click('[data-seg-btn="progress"]'); await shot('wird-progress');
  await p.click('.nav [data-tab="quran"]'); await shot('quran');

  console.log('shots:', n, '| errors:', errors.length ? errors.join(' | ') : 'none');
  await browser.close();
})();
