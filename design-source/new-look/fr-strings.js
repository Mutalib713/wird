// Collect every string the first-time screens show, by rendering each step, for the humanizer gate.
const path = require('path');
const fs = require('fs');
const { chromium } = require(path.join('C:/Users/USER/MyClaudeProjects/_tools/node_modules/playwright'));
(async () => {
  const browser = await chromium.launch({ channel: 'chrome' });
  const p = await (await browser.newContext()).newPage();
  await p.goto('file:///' + path.resolve(__dirname, 'prototype/index.html').split(path.sep).join('/'));
  const text = await p.evaluate(() => {
    const out = [];
    const steps = [...INTRO, ...SETUP, 'mic', 'created'];
    for (const st of steps) { frShow(st); out.push(document.querySelector('#fr').innerText); }
    FR.t.every = false; frShow('days'); out.push(document.querySelector('#fr').innerText);
    FR.t.amount = 'verses'; frShow('amount'); out.push(document.querySelector('#fr').innerText);
    FR.t.remind = 'time'; FR.t.repeat = 'custom'; frShow('reminder'); out.push(document.querySelector('#fr').innerText);
    FR.sys = true; document.querySelector('#fr').innerHTML = VIEW.mic(); out.push(document.querySelector('#fr').innerText);
    for (const m of ['yes', 'once', 'no']) { FR.mic = m; frShow('created'); out.push(document.querySelector('#fr .foot-note').innerText); }
    TOUR.forEach(t => out.push(t[1] + '\n' + t[2]));
    return out.join('\n\n');
  });
  const lines = [...new Set(text.split('\n').map(s => s.trim()).filter(s => s.length > 2))];
  fs.writeFileSync(process.argv[2], lines.join('\n\n') + '\n');
  console.log(lines.length, 'strings');
  await browser.close();
})();
