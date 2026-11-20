// assets/js/zoom_viewport.js ("Pinch to zoom") on a page that forbids zooming the way the AdGuard DNS website does
// (its <meta name="viewport">, read live 2026-11-20: "maximum-scale=1, user-scalable=0"; research F8): the ban is
// lifted, other parts of the tag stay, and a tag the page adds later is fixed too.
import { readFileSync } from 'fs';
import { run, phonePage, assetPath, expect } from './common.mjs';

await run(async (browser) => {
    const script = readFileSync(assetPath('js/zoom_viewport.js'), 'utf8');
    const page = await phonePage(browser);
    await page.route('https://adguard-dns.io/en/dashboard/', r => r.fulfill({ contentType: 'text/html', body:
        '<!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=0"></head><body>x</body></html>' }));
    await page.addInitScript(script); // the app adds it at document start (web/PageZoom)
    await page.goto('https://adguard-dns.io/en/dashboard/');
    const content = await page.$eval('meta[name=viewport]', m => m.content);
    expect(!/maximum-scale=1(\D|$)/.test(content) && !content.includes('user-scalable=0')
        && content.includes('user-scalable=yes'), 'ban lifted: ' + content);
    expect(content.includes('width=device-width') && content.includes('initial-scale=1'), 'rest kept: ' + content);
    await page.evaluate(() => {
        const m = document.createElement('meta'); m.name = 'viewport'; m.content = 'width=device-width, user-scalable=no';
        document.head.append(m);
    });
    await page.waitForTimeout(50);
    const later = await page.$$eval('meta[name=viewport]', ms => ms.map(m => m.content));
    expect(later.every(c => c.includes('user-scalable=yes') && !c.includes('user-scalable=no')), 'later tag fixed: ' + later);
    expect(page.errors.length === 0, 'script errors: ' + page.errors.join('; '));
});
