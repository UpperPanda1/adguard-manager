// Live probe of the AdGuard DNS website. Development-time only: nothing here is part of the app.
//
// The app shows AdGuard's dashboard inside WebViews, so several of its rules depend on facts about that website:
// which hosts the sign-in walks through (site/Site.java), which colours the page has (theme/Themes.java) and how
// AdGuard's "am I using AdGuard DNS?" check works (status/). Websites change, so these facts are re-checked here
// against the LIVE site instead of being trusted from an old note. The results and their dates are written down in
// docs/research/2026-11-19-adguard-live-probe.md.
//
//   node probe.mjs signin <Apple|Google|Facebook> [--disguised]
//       Opens the dashboard like a phone WebView, presses "Continue with <provider>" and prints every main-frame
//       navigation (secrets in the addresses are replaced by …), pop-ups, and all hosts the pages loaded from.
//       --disguised uses the desktop-Linux user agent that LLM Central's web/SignInUserAgent gives Google's page.
//   node probe.mjs colours
//       Finds the dashboard's current stylesheet and lets Chromium compute the page colours in light and dark.
//   node probe.mjs dnscheck
//       Asks AdGuard's resolver and Google's resolver (DNS over HTTPS) for a fresh random "dnscheck" name. Only
//       AdGuard DNS knows these names: that is how AdGuard's own check tells whether a phone uses AdGuard DNS.
//
// Note: headless Chromium is not Android WebView. It doesn't send WebView's "X-Requested-With" header, so Google
// may show its page here even where a real WebView gets "403 disallowed_useragent". Only a phone proves that part.
import { chromium } from 'playwright';
import { randomBytes } from 'crypto';

const DASHBOARD = 'https://adguard-dns.io/en/dashboard/';
// A typical Android WebView user agent (the "; wv" marks a WebView).
const WEBVIEW_UA = 'Mozilla/5.0 (Linux; Android 14; Pixel 8; wv) AppleWebKit/537.36 (KHTML, like Gecko) '
    + 'Version/4.0 Chrome/131.0.0.0 Mobile Safari/537.36';
// LLM Central web/SignInUserAgent.disguise: the first bracket becomes (X11; Linux <arch>).
const DISGUISED_UA = WEBVIEW_UA.replace(/\([^)]*\)/, '(X11; Linux aarch64)');

function launch() {
    return chromium.launch(process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {});
}

/** The address without query values (they hold one-time state, codes and ids); the names are kept. */
function redact(url) {
    const u = new URL(url);
    for (const key of [...u.searchParams.keys()]) {
        u.searchParams.set(key, '…');
    }
    return decodeURI(u.toString()).slice(0, 200);
}

async function signin(provider, disguised) {
    if (!['Apple', 'Google', 'Facebook'].includes(provider)) {
        console.error('provider must be Apple, Google or Facebook (as written on the login page)');
        process.exit(2);
    }
    const browser = await launch();
    const context = await browser.newContext({
        userAgent: disguised ? DISGUISED_UA : WEBVIEW_UA,
        viewport: { width: 412, height: 915 }, isMobile: true, hasTouch: true,
    });
    const page = await context.newPage();
    const steps = [];
    const hosts = new Set();
    const popups = [];
    page.on('request', r => {
        hosts.add(new URL(r.url()).host);
        if (r.isNavigationRequest() && r.frame() === page.mainFrame()) {
            steps.push(`${r.method()} ${redact(r.url())}`);
        }
    });
    context.on('page', p => { if (p !== page) popups.push(redact(p.url() || 'about:blank')); });
    await page.goto(DASHBOARD, { waitUntil: 'networkidle' });
    await page.getByText(`Continue with ${provider}`).click();
    await page.waitForTimeout(9000);
    const text = (await page.locator('body').innerText().catch(() => '')).replace(/\s+/g, ' ').slice(0, 160);
    console.log(`Sign-in with ${provider} (${disguised ? 'disguised' : 'WebView'} user agent), ${new Date().toISOString()}`);
    console.log('Main-frame navigations:\n  ' + steps.join('\n  '));
    console.log(`Ends on: ${new URL(page.url()).host}   Pop-ups: ${popups.length ? popups.join(', ') : 'none'}`);
    console.log(`All hosts: ${[...hosts].sort().join(' ')}`);
    console.log(`Page text: ${text}`);
    await browser.close();
}

async function colours() {
    // The dashboard's files live in a folder named after the website build; a public page names the folder.
    const html = await (await fetch('https://adguard-dns.io/en/welcome.html')).text();
    const folder = html.match(/https:\/\/st2\.adguardcdn\.com\/dns-website-[0-9a-f]+/);
    if (!folder) throw new Error('website build folder not found on welcome.html');
    // dashboard.min.css: the name was found by trying it next to landing.min.css (the signed-in page names it).
    const cssUrl = folder[0] + '/css/dashboard.min.css';
    const response = await fetch(cssUrl);
    if (!response.ok) throw new Error(`${cssUrl}: HTTP ${response.status}`);
    const css = await response.text();
    const browser = await launch();
    const page = await browser.newPage();
    console.log(`Stylesheet: ${cssUrl}`);
    for (const theme of ['light', 'dark']) {
        // The dashboard writes <html data-theme="light|dark"> (dashboard.min.js), so do the same here.
        await page.setContent(`<html data-theme="${theme}"><head><style>${css}</style></head><body></body></html>`);
        const c = await page.evaluate(() => {
            const body = getComputedStyle(document.body);
            return { background: body.backgroundColor, text: body.color,
                accent: body.getPropertyValue('--accent-main-default').trim() };
        });
        console.log(`${theme.padEnd(5)} page ${c.background}  text ${c.text}  accent ${c.accent}`);
    }
    await browser.close();
}

/** One A-record question in DNS wire format (RFC 1035), as DNS over HTTPS sends it (RFC 8484). */
function question(name) {
    const parts = [Buffer.from([0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0])]; // id 0, recursion desired, 1 question
    for (const label of name.split('.')) parts.push(Buffer.from([label.length]), Buffer.from(label));
    parts.push(Buffer.from([0, 0, 1, 0, 1])); // end of name, type A, class IN
    return Buffer.concat(parts);
}

async function dnscheck() {
    // The dashboard (dashboard.min.js checkDnsUsage) asks https://a<random>-dnscheck.<server domain>/dnscheck/test.
    const name = `a${randomBytes(6).toString('hex')}-dnscheck.adguard-dns.com`;
    console.log(`Name: ${name}`);
    for (const server of ['https://dns.adguard-dns.com/dns-query', 'https://dns.google/dns-query']) {
        const response = await fetch(server, { method: 'POST', body: question(name),
            headers: { 'Content-Type': 'application/dns-message', Accept: 'application/dns-message' } });
        const answer = Buffer.from(await response.arrayBuffer());
        const rcode = answer[3] & 0x0f; // 0 = the name exists, 3 = NXDOMAIN (no such name)
        const count = answer.readUInt16BE(6);
        console.log(`${server}: ${rcode === 0 ? 'NOERROR' : rcode === 3 ? 'NXDOMAIN' : 'rcode ' + rcode}, ${count} answer(s)`);
    }
}

const [mode, ...args] = process.argv.slice(2);
if (mode === 'signin') await signin(args[0], args.includes('--disguised'));
else if (mode === 'colours') await colours();
else if (mode === 'dnscheck') await dnscheck();
else {
    console.error('usage: node probe.mjs signin <Apple|Google|Facebook> [--disguised] | colours | dnscheck');
    process.exit(2);
}
