// Extra checks: dark mode screens, the "use the defaults" path with no name, and the day picker's fit.
const path = require('path');
const fs = require('fs');
const { chromium } = require(path.join('C:/Users/USER/MyClaudeProjects/_tools/node_modules/playwright'));
(async () => {
  const out = 'fr-extra'; fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch({ channel: 'chrome' });
  const errors = [];
  const open = async (dark) => {
    const ctx = await browser.newContext({ viewport: { width: 1400, height: 1000 }, deviceScaleFactor: 2, colorScheme: dark ? 'dark' : 'light' });
    const p = await ctx.newPage();
    p.on('pageerror', e => errors.push(e.message));
    await p.goto('file:///' + path.resolve(__dirname, 'prototype/index.html').split(path.sep).join('/'));
    await p.evaluate(() => localStorage.clear()); await p.reload(); await p.evaluate(() => document.fonts.ready);
    await p.click(`.seg[data-ctl="theme"] [data-v="${dark ? 'dark' : 'light'}"]`);
    return p;
  };
  let n = 0;
  const shot = async (p, name) => { await p.waitForTimeout(320); n++; await (await p.$('#phone')).screenshot({ path: path.join(out, `${String(n).padStart(2, '0')}-${name}.png`) }); };

  // light: welcome again, then the defaults path with no name
  let p = await open(false);
  await p.click('[data-fr-start="welcome"]'); await shot(p, 'welcome-light');
  await p.click('#fr [data-to="ready"]'); await p.click('#fr [data-act="defaults"]');
  await p.click('#fr [data-act="mic-later"]'); await shot(p, 'created-defaults');
  await p.click('#fr [data-act="finish"]'); await p.waitForTimeout(300); await p.click('#tourTip [data-t="skip"]'); await shot(p, 'home-no-name');
  // the day picker: every chip inside the phone, and 44px tall
  await p.click('[data-fr-start="name"]'); await p.click('#fr [data-act="next"]'); await p.click('#fr [data-act="next"]');
  await p.click('#fr [data-set="every"][data-val="no"]');
  const fit = await p.evaluate(() => {
    const ph = document.querySelector('#phone').getBoundingClientRect();
    return [...document.querySelectorAll('#fr .days button')].map(b => { const r = b.getBoundingClientRect(); return { right: Math.round(ph.right - r.right), h: Math.round(r.height) }; });
  });
  console.log('day chips (px from phone right edge, height):', JSON.stringify(fit));
  await shot(p, 'days-fit');

  // dark: welcome, ready, a setup step, review, created, tour
  p = await open(true);
  await p.click('[data-fr-start="welcome"]'); await shot(p, 'dark-welcome');
  await p.click('#fr [data-act="next"]'); await shot(p, 'dark-tracks');
  await p.click('#fr [data-to="ready"]'); await shot(p, 'dark-ready');
  await p.click('#fr [data-act="next"]'); await p.click('#fr [data-act="next"]'); await shot(p, 'dark-goal');
  for (let i = 0; i < 4; i++) await p.click('#fr [data-act="next"]');
  await shot(p, 'dark-amount');
  for (let i = 0; i < 3; i++) await p.click('#fr [data-act="next"]');
  await shot(p, 'dark-review');
  await p.click('#fr [data-act="next"]'); await p.click('#fr [data-act="mic-ask"]'); await shot(p, 'dark-android');
  await p.click('#fr [data-act="mic-no"]'); await shot(p, 'dark-created');
  await p.click('#fr [data-act="finish"]'); await p.waitForTimeout(400); await shot(p, 'dark-tour-1');
  await p.click('#tourTip [data-t="next"]'); await p.click('#tourTip [data-t="next"]'); await shot(p, 'dark-tour-3');
  console.log('shots', n, '| errors:', errors.length ? errors.join(' | ') : 'none');
  await browser.close();
})();
