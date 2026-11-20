# Reviews of AdGuard Manager

**No review of the AdGuard changes yet.** The plan's Phase 10 is an adversarial review of everything since the import
(`abb10bf`): sign-in hosts and the user-agent scope (Phase 5), the blob download bridge (Phase D), the status check
(Phase 6), the lock. It will be recorded here.

What this file holds until then: the **open findings inherited from NextDNS Manager+**, whose three reviews (2026-11-19)
covered the code both apps share. The full text, with evidence and the findings already fixed, is in NextDNS Manager+'s
`REVIEW.md` at commit `eaa1665` (`UpperPanda1/nextdnsmanager-plus`). IDs are kept so the two files can be compared.

## Contents

1. [Open findings in the shared code](#1-open-findings-in-the-shared-code)
2. [Open decisions inherited](#2-open-decisions-inherited)
3. [What the reviews checked and found correct](#3-what-the-reviews-checked-and-found-correct)
4. [Phase 10 checklist (to do)](#4-phase-10-checklist-to-do)

## 1. Open findings in the shared code

The owner deferred these for NextDNS Manager+'s first release (2026-11-19): not blockers. For AdGuard several are
absorbed by a phase, noted in the last column.

| ID | Finding | Severity | For AdGuard |
|---|---|---|---|
| 4.5 / F3 | `status/StatusRules.fromPrivateDns` counts a Private DNS host that merely *contains* the marker ("nextdns") as the service's; an explicit "not using" test answer never overrides a green Private DNS state | low–medium | **Phase 6 replaces it**: marker as a host suffix (`.adguard-dns.com`), and AdGuard's check answers only when the service is in use (research D3–D5) |
| F2 | `downloads/DataUrl` saves any well-formed MIME type the page names (e.g. an APK type) to Downloads; MediaStore may add the type's extension | low | **Phase D**: the blob path will save through the same code; restrict unknown types to `application/octet-stream` there |
| R4 | `Profiles.clearAllCaches` clears closed profiles in a tight loop; one per posted message would keep the screen responsive with many accounts | low | unchanged (shared; port to/from NextDNS, `UPSTREAM.md`) |
| R6 | `Downloads.start` hands web addresses to the browser itself instead of through `web/ExternalLinks.open` (the one guarded place) | very low | **Phase D** touches this method; do it there |
| 4.6 | Clear cache: the size in Settings is measured once 1.5 s later, before an asynchronous deletion may be done | cosmetic | unchanged |
| F4 | `zoom_viewport.js` defines an `off()` nothing calls | nit | unchanged |
| F5 | Android 15+ edge-to-edge: the Settings/About HTML pages don't pad themselves (LLM Central's identical dialogs looked right on the owner's Android 16 phone) | unverified | phone check |
| F6 | A 40-character account name in the account menu at 360 dp (ellipsized; should fit) | unverified | phone check |
| 4.7 | Back handling uses `onKeyDown(KEYCODE_BACK)`; at targetSdk 36 predictive back may stop calling it (unverified). Raise targetSdk only after a phone test | — | unchanged |
| 4.7 | `.gitignore`: a stray `.zip` line; `*.keystore` ignored while `app/debug.keystore` is tracked (force-added) | nit | unchanged |

Done in R5's spirit: `release.yml` stops a real release when `app.repoUrl` isn't the repository it runs in (it strips
the `\r` of `gradle.properties`' Windows line endings). For this repository the value is now right
(`https://github.com/UpperPanda1/adguard-manager`, 2026-11-20).

## 2. Open decisions inherited

| ID | Decision | For AdGuard |
|---|---|---|
| A | Final LLM Central address (`UpperPanda1/assist` or a renamed repository) | README credits and About "Built on" name it |
| B | Ping window shares account `main`'s cookie jar (default profile) | depends on Q2 (PROJECT.md §11 Phase 7): moot if it is removed or becomes "Connection details" |
| C | GPL credit chain beyond LLM Central (duckAssist/gptAssist) in README/About | same question |
| D | Working files (`CLAUDE.md`, `handoff.md`, `.claude/`, this file) in the public repository: keep or remove | **this repository is public** (checked 2026-11-20), so they are visible now |
| E | Name vs trademark | "AdGuard Manager" chosen 2026-11-20; the F-Droid question is in `fdroid.md` §3.2 |
| F | Status icon rule once a real "in use" answer is seen | Phase 6 + phone check P6 |

## 3. What the reviews checked and found correct

In the shared code, summarised from NextDNS Manager+ review 3 §10.2 (still the same code):

- **Lock rules:** lock on a fresh process and after > 60 s away; turning the lock on and off needs the phone's unlock;
  Back on the cover leaves the app; Settings/About/Ping/status/menus/find bar close before locking; content under the
  cover hidden from TalkBack and keyboard (fixed F1).
- **Navigation:** `Site.ownsHost` = exact host or real subdomain; a trailing dot or empty host fails closed. Injected
  scripts come only from assets and run only on the service's https origins (`Site.originRules`).
- **Zoom and desktop scripts:** no observer loop; order-independent; switching reloads the tab; desktop mode keeps
  `; wv`.
- **Theme and insets:** no double padding on Android 11–14; Android 15+ padded by the listener.
- **Local pages:** no `innerHTML`; user text as text nodes; the only `document.write` is the theme CSS built in Java
  from fixed colours; Settings bridge validates every value; links by name only.
- **Robustness:** every WebView client handles renderer death; the LRU never closes the tab on screen; a malformed
  saved account list falls back safely; `DataUrl` is strict (Base64, 10 MB cap, bad escapes refused).

## 4. Phase 10 checklist (to do)

From the plan (§6, §8) and the research, for the review of the AdGuard diff:

1. Sign-in hosts are used **only** for main-frame navigation decisions; `ownsHost` unchanged; lookalike hosts fail.
2. The Google user agent is set only while `accounts.google.com` is the main frame and is always restored; it never
   reaches AdGuard's pages; desktop mode doesn't undo it or leak it.
3. The blob download bridge: origin allow-list (AdGuard only), main frame only, size cap, name and type sanitised,
   no `addJavascriptInterface` on the website.
4. The status check: random name per request, timeout, no account data sent, answer parsed defensively.
5. `adguardcheckout.com` and every non-AdGuard link leave the app; Facebook's app-link step can't open anything but
   http(s)/mailto/tel.
6. "What if two things happen at once?": sign-out during a sign-in; account removed while a download is saved; network
   change during the status check.
7. Battery: no new timers or listeners while hidden.
