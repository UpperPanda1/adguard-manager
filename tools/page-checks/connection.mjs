// Connection details (assets/connection.html): the page opens in the "checking" state and asks the app for a fresh
// check; the app's answers (onStatus, onLatency: status/ConnectionReport's JSON) fill the rows; latency rows arrive one
// by one, fastest first, with a "Fastest" chip at the end; "not in use" collapses the details into one row; values are
// shown as text, never as HTML; Back and "Check again" call the app; light, dark (app colours) and 360 px all work.
import { run, phonePage, pageUrl, expect, shot, schemeCss } from './common.mjs';

const SERVERS = { default: 'dns.adguard-dns.com', unfiltered: 'unfiltered.adguard-dns.com',
    family: 'family.adguard-dns.com', personal: 'd.adguard-dns.com' };
const IN_USE = { state: 'SECURE', sentence: 'This phone uses AdGuard DNS over an encrypted connection.', color: '#198754',
    checking: false, privateDns: { active: true, host: 'abcd1234.d.adguard-dns.com' }, servers: SERVERS,
    check: { outcome: 'IN_USE', protocol: 'dot', encrypted: true, serverType: 'private', deviceId: 'abcd1234',
        location: 'ams', node: 'ams-01' } };
const LATENCY_DONE = { done: true, ipv6: false, rows: [
    { server: 'family', ip: 4, ms: 8.4 }, { server: 'default', ip: 4, ms: 12 },
    { server: 'unfiltered', ip: 4, ms: 13 }, { server: 'personal', ip: 4, ms: null }] };
const BRIDGE = `window.calls = [];
    window.AndroidConnection = { refresh: () => calls.push('refresh'), close: () => calls.push('close') };`;

const text = (page, id) => page.$eval('#' + id, e => e.textContent);
// What is really on screen (a layout box), not the hidden attribute: CSS can override that attribute.
const visible = (page, id) => page.$eval('#' + id, e => e.getClientRects().length > 0);

await run(async (browser) => {
    // ---- light: the whole flow
    const page = await phonePage(browser, 'light');
    await page.addInitScript(BRIDGE);
    await page.goto(pageUrl('connection.html'));
    expect((await page.evaluate(() => calls)).filter(c => c === 'refresh').length === 1, 'opening asks for a fresh check');
    expect(await text(page, 'statusText') === 'Checking…', 'checking at first');
    expect(await page.$eval('#refresh', b => b.disabled), 'Check again is off while checking');
    expect((await page.$$eval('#latency .row', r => r.length)) === 1
        && (await page.textContent('#latency')).includes('Measuring…'), 'latency: measuring');

    await page.evaluate(s => onStatus(s), IN_USE);
    expect(await text(page, 'statusText') === IN_USE.sentence, 'status sentence');
    expect(await text(page, 'statusSub') === 'Seen by AdGuard’s DNS check', 'status source');
    expect(await page.$eval('#status svg', s => getComputedStyle(s).color) === 'rgb(25, 135, 84)', 'icon colour from the app');
    expect((await page.$eval('#statusShape', p => p.getAttribute('d'))).includes('L10,14.17'), 'tick symbol when secure');
    expect(await text(page, 'protocol') === 'DNS-over-TLS · encrypted', 'protocol');
    expect((await text(page, 'server')).startsWith('Your own server'), 'own server');
    expect(await visible(page, 'deviceRow') && await text(page, 'device') === 'abcd1234', 'device id');
    expect(await text(page, 'location') === 'AMS · server ams-01', 'location');
    expect(await text(page, 'privateDns') === 'abcd1234.d.adguard-dns.com', 'Private DNS host');
    expect(!(await visible(page, 'notSeen')), 'no "not seen" row');

    // Latency arrives row by row, then done.
    await page.evaluate(() => onLatency({ done: false, ipv6: false, rows: [{ server: 'default', ip: 4, ms: 12 }] }));
    expect((await page.$$eval('#latency .row', r => r.length)) === 2, 'one result + measuring');
    expect(!(await page.$$eval('#latency .chip', c => c.length)), 'no "Fastest" before the end');
    await page.evaluate(l => onLatency(l), LATENCY_DONE);
    const rows = await page.$$eval('#latency .row', r => r.map(x => x.innerText.replace(/\s+/g, ' ').trim()));
    expect(rows.length === 4, 'four rows: ' + rows.length);
    expect(rows[0].startsWith('Family protection') && rows[0].includes('Fastest') && rows[0].endsWith('8.4 ms'), 'fastest first: ' + rows[0]);
    expect(rows[0].includes('family.adguard-dns.com · IPv4'), 'host and IP version: ' + rows[0]);
    expect(rows[1].endsWith('12 ms'), 'whole ms from 10 ms: ' + rows[1]);
    expect(rows[3].startsWith('Personal servers') && rows[3].endsWith('No answer'), 'no answer last: ' + rows[3]);
    expect(await visible(page, 'noIpv6'), 'IPv6 note when no IPv6 row');
    expect(!(await page.$eval('#refresh', b => b.disabled)), 'Check again on when done');
    await shot(page, 'connection-light');

    // Check again, Back.
    await page.click('#refresh');
    expect((await page.evaluate(() => calls)).filter(c => c === 'refresh').length === 2, 'Check again asks the app');
    await page.click('.appbar .icon-btn');
    expect((await page.evaluate(() => calls)).includes('close'), 'Back closes');

    // Not in use: one row instead of empty details; no device; warning symbol.
    await page.evaluate(s => onStatus(s), { ...IN_USE, state: 'NOT_PROTECTED', color: '#dc3545',
        sentence: 'This phone is not using AdGuard DNS.', privateDns: { active: false, host: null },
        check: { outcome: 'NOT_IN_USE', protocol: '', encrypted: false, serverType: '', deviceId: '', location: '', node: '' } });
    expect(await visible(page, 'notSeen') && (await text(page, 'notSeenTitle')).startsWith('AdGuard didn'), 'not seen row');
    for (const id of ['protocolRow', 'serverRow', 'deviceRow', 'locationRow']) {
        expect(!(await visible(page, id)), id + ' hidden when not in use');
    }
    expect(await text(page, 'privateDns') === 'Not in use', 'Private DNS off');
    expect(!(await page.$eval('#statusShape', p => p.getAttribute('d'))).includes('L10,14.17'), 'warning symbol');
    await shot(page, 'connection-not-in-use');

    // Values are text, never HTML (the app validates them too: status/DnsCheck).
    await page.evaluate(s => onStatus(s), { ...IN_USE, sentence: '<img src=x id=evil>',
        check: { ...IN_USE.check, deviceId: '<b id=evil2>x</b>', location: '<i>' } });
    expect(!(await page.$('#evil')) && !(await page.$('#evil2')), 'no HTML from values');
    expect(page.errors.length === 0, 'script errors: ' + page.errors.join('; '));
    await page.close();

    // ---- dark: the phone is light, the app says dark (theme/ThemeBridge), then a 360 px phone
    const dark = await phonePage(browser, 'light');
    await dark.addInitScript(BRIDGE + `window.AndroidTheme = { css: () => ${JSON.stringify(schemeCss('DARK'))} };`);
    await dark.goto(pageUrl('connection.html'));
    await dark.evaluate(s => onStatus(s), { ...IN_USE, color: '#75b798' });
    await dark.evaluate(l => onLatency(l), { ...LATENCY_DONE, ipv6: true,
        rows: [...LATENCY_DONE.rows.slice(0, 3), { server: 'family', ip: 6, ms: 9.1 }, LATENCY_DONE.rows[3]] });
    expect(await dark.evaluate(() => getComputedStyle(document.body).backgroundColor) === 'rgb(33, 37, 41)', 'app colours win');
    expect(!(await visible(dark, 'noIpv6')), 'no IPv6 note when IPv6 rows exist');
    await shot(dark, 'connection-dark');
    await dark.setViewportSize({ width: 360, height: 780 });
    expect(await dark.evaluate(() => document.documentElement.scrollWidth) <= 360, 'no sideways scrolling at 360 px');
    await shot(dark, 'connection-360');
    expect(dark.errors.length === 0, 'script errors: ' + dark.errors.join('; '));
});
