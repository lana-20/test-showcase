# Cloud device and browser programs for open source (recorded, not applied for)

Nothing here has been submitted. This page records what each program asks for and gives, so a decision can be
made later. Sources are the vendors' own pages, read on 2026-11-26. "Not stated" means the vendor's page doesn't
say, not that the answer is no.

The repo meets the usual bar: it's public on GitHub, has an OSI-approved license ([MIT](../LICENSE)), is actively
developed and has a README.

## BrowserStack Open Source Program

Source: https://www.browserstack.com/open-source

| | |
|---|---|
| Eligibility | Not stated on the page. |
| The form asks for | Full name, email (labeled business email), password and project URL, plus agreeing to the Terms of Service and Privacy Policy. Google sign-in is offered. The form creates the account and applies at the same time. |
| Approval time | Not stated. The "within 12 hours" elsewhere on the site belongs to the sales contact form. |
| What you get | "Unlimited testing on desktop and mobile: Live, Automate and Percy", "access to 5 users and 5 parallels", "3500+ browsers and devices". Membership "lasts for the duration of your projects". |
| Obligations | Not stated. |
| Unclear | Whether App Automate (real-device Appium) is included today. The current page lists Live, Automate and Percy. A 2018 press release listed App Automate and real devices, but that may be out of date. Ask before relying on it. |

## TestMu AI (formerly LambdaTest) Open Source Program

Source: https://www.testmuai.com/open-source/ (lambdatest.com/open-source redirects here)

| | |
|---|---|
| Eligibility | The project "must have a valid open-source license (e.g., MIT, Apache 2.0, or GPL), be publicly accessible on platforms like GitHub or GitLab, and show active development through recent updates. It should also have clear documentation, such as a README". |
| The form asks for | First name, last name, business email, project URL, and "About the Project". |
| Approval time | Not stated. Questions go to support@testmuai.com. |
| What you get | "Free licenses to test your open-source projects across 5,000+ browsers and devices". Users, parallel sessions, minutes and real versus virtual devices are not stated. |
| Obligations | Not stated. |
| Also on the page | A separate grant for QA and testing open-source projects. Not relevant here. |

## Sauce Labs: Open Sauce

| | |
|---|---|
| Status | **Unclear, possibly discontinued.** No public program page or form could be found. saucelabs.com/open-source, /open-sauce and similar URLs return 404, and the pricing page lists no free or open-source plan. |
| Only current mention | The Support Policy dated 2025-09-30 (https://saucelabs.com/doc/support-policy) still says Sauce Labs gives "free unlimited use Sauce Labs accounts for individuals or organizations working on bonafide Open Source projects". |
| Eligibility, form, approval time, limits | Could not be verified. The only details found were search-engine snippets of a dead page: 5 parallel VMs, a reply within 48 hours, and test results made public. Don't treat them as current. To pursue it, contact Sauce Labs. |

## What we would wire up once approved

- **Appium Java on real devices.** The same `suites/appium-java` tests run against the vendor's Appium endpoint
  instead of a local server. `APPIUM_URL` and the capabilities already come from the environment
  (`support/Config.java` and `DriverFactory.java`), so this needs a vendor options block, the app uploaded to the
  vendor's storage, and secrets for the user name and key. The goals are a real Android phone and a real iPhone.
  The iPhone needs an iOS build of a sample app, such as the iOS build of Appium's test app.
- **Playwright across browsers.** One more Playwright project that connects to the vendor's Playwright or CDP
  endpoint, with Chrome, Edge, Firefox and WebKit/Safari legs, against the public demo app at
  https://lana-20.github.io/test-showcase/demo/.
- Each run gets its own `status/<id>.json` and Allure report through the same `ci/lib/status.py` path, and is
  labeled with the vendor it really ran on.
