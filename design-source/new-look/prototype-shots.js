// Walk the prototype through its screens and screenshot the phone in each state.
// Usage: node prototype-shots.js <outdir>
const path = require('path');
const fs = require('fs');
const { chromium } = require(path.join('C:/Users/USER/MyClaudeProjects/_tools/node_modules/playwright'));

const STEPS = [
  ['01-home-day', async p => { await set(p, 'sky', 'day'); }],
  ['02-home-sunset', async p => { await set(p, 'sky', 'sunset'); }],
  ['03-sheet', async p => { await set(p, 'sky', 'day'); await p.click('#trackChip'); await p.waitForTimeout(350); }],
  ['04-home-done', async p => { await p.click('#sheetClose'); await p.waitForTimeout(300); await p.click('#markDone'); await p.waitForTimeout(500); }],
  ['05-quran', async p => { await p.click('.nav [data-tab="quran"]'); }],
  ['06-surahs', async p => { await p.click('#quran [data-go="surahs"]'); }],
  ['07-surahs-madani', async p => { await p.click('[data-f="Madani"]'); }],
  ['08-juz', async p => { await p.click('#surahs [data-back]'); await p.click('#quran [data-go="juz"]'); }],
  ['09-companion', async p => { await p.click('.nav [data-tab="companion"]'); }],
  ['10-chat-kursi', async p => { await p.click('#sugg [data-ask="1"]'); await p.waitForTimeout(1300); await p.$eval('#chat', el => el.scrollTop = 0); }],
  ['11-chat-open-question', async p => { await p.fill('#chatInput', 'What does the Quran say about patience?'); await p.press('#chatInput', 'Enter'); await p.waitForTimeout(1300); }],
  ['12-wird', async p => { await p.click('.nav [data-tab="wird"]'); }],
  ['13-wird-progress', async p => { await p.click('[data-seg-btn="progress"]'); }],
  ['14-settings', async p => { await p.click('.nav [data-tab="home"]'); await p.click('.gear'); }],
  ['15-about', async p => { await p.click('#settings [data-go="about"]'); }],
  ['16-home-dark', async p => { await p.click('#about [data-back]'); await p.click('#settings [data-back]'); await set(p, 'theme', 'dark'); }],
  ['17-sheet-dark', async p => { await p.click('#trackChip'); await p.waitForTimeout(350); }],
  ['18-chat-dark', async p => { await p.click('#sheetClose'); await p.click('.nav [data-tab="companion"]'); await p.click('#sugg [data-ask="0"]'); await p.waitForTimeout(1300); await p.$eval('#chat', el => el.scrollTop = 0); }],
  ['19-wird-dark', async p => { await p.click('.nav [data-tab="wird"]'); await p.click('[data-seg-btn="tracks"]'); }],
  ['20-surahs-dark', async p => { await p.click('.nav [data-tab="quran"]'); await p.click('#quran [data-go="surahs"]'); await p.click('[data-f="all"]'); }],
];
const set = (p, ctl, v) => p.click(`.seg[data-ctl="${ctl}"] [data-v="${v}"]`);

(async () => {
  const out = process.argv[2] || 'proto-shots';
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch({ channel: 'chrome' });
  const ctx = await browser.newContext({ viewport: { width: 1400, height: 1000 }, deviceScaleFactor: 2, colorScheme: 'light' });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  p.on('console', m => { if (m.type() === 'error') errors.push(m.text()); });
  await p.goto('file:///' + path.resolve(__dirname, 'prototype/index.html').replace(/\\/g, '/'));
  await p.evaluate(() => { localStorage.clear(); });
  await p.reload();
  await p.evaluate(() => document.fonts.ready);
  await p.waitForTimeout(400);
  for (const [name, act] of STEPS) {
    await act(p);
    await p.waitForTimeout(350);
    await (await p.$('#phone')).screenshot({ path: path.join(out, name + '.png') });
  }
  const fonts = await p.evaluate(() => [...new Set([...document.fonts].filter(f => f.status === 'loaded').map(f => f.family + ' ' + f.weight))]);
  console.log('fonts:', fonts.join(', '));
  console.log('errors:', errors.length ? errors.join(' | ') : 'none');
  await browser.close();
})();
