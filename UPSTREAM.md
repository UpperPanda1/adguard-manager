# Upstream: keeping AdGuard DNS Manager+ and NextDNS Manager+ in step

This app started as an **unchanged copy** of NextDNS Manager+ (one import commit, no history). From then on the two
apps live in separate repositories, so a fix made in one of them reaches the other only if someone ports it by hand.
This file is that "port list" (plan `ADGUARD_PLAN.md` §7.5).

**When to add a row:** a commit changes **shared code**, i.e. code that is the same in both apps: `account/`, `web/`
(except the sign-in files), `ui/`, `lock/`, `downloads/`, `storage/`, `theme/` (except `Themes.java`), the build
files, `tools/`, the workflows. Service-specific code (`site/Site.java`, `theme/Themes.java`, `status/StatusRules`,
sign-in, texts, icon) is not ported.

**How to port:** the Java package is the same in both apps (`org.dnsmanager.app`), so a shared fix usually applies as
`git format-patch` from one repository + `git am -3` in the other. Run `test lint assembleDebug assembleRelease` and
the page checks, then add the row in **both** repositories' `UPSTREAM.md` (NextDNS Manager+ gets its own file the
first time a fix goes that way).

## Import

| Date | NextDNS Manager+ commit | AdGuard commit | Notes |
|---|---|---|---|
| 2026-11-19 | `eaa1665` ("Rename applicationId to org.rafee_m.nextdnsmanagerplus"), branch `claude/nextdns-manager-plus-review-el0q2u` of `UpperPanda1/nextdnsmanager-plus` | `abb10bf` | Tree identical (git tree `11ec3ad6`); 92 tests, 0 lint issues, 6 page checks, CI green |

## NextDNS → AdGuard (ported here)

| Date | NextDNS commit | AdGuard commit | What |
|---|---|---|---|
| — | — | — | nothing yet |

## AdGuard → NextDNS (to port there)

| Date | AdGuard commit | Ported in NextDNS? | What |
|---|---|---|---|
| — | — | — | nothing yet |

Check before each AdGuard release: NextDNS commits after the last row above that touch shared code
(`git log <last ported>..origin/<branch> -- app/src/main/java/org/dnsmanager/app/account …` in the NextDNS repository).
