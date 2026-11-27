# WebDriver BiDi, CDP or Playwright: which to choose

This repo uses all three, on purpose. The Selenium BiDi suite speaks WebDriver BiDi to Chrome, Firefox and Edge.
The Playwright suite drives Chromium, and its Kernel leg connects over CDP. Vibium speaks only BiDi. This note says
when each one is the right call, and what the suites here actually showed.

## The short version

| If you need… | Choose | Why |
|---|---|---|
| The same event-driven tests (logs, network, preload scripts) on Chrome, Firefox and Edge | **WebDriver BiDi** | It is a W3C protocol that every major browser implements, and Selenium Grid routes it. |
| A Selenium or Grid estate you already run, plus network mocking and console capture | **WebDriver BiDi** (Selenium 4) | No new runner and no new infrastructure. The BiDi WebSocket goes through the hub. |
| Something only Chromium exposes, such as performance tracing, coverage, or a target that speaks nothing else | **CDP** | CDP is Chromium's own protocol, and it covers far more than BiDi does today. It is Chromium only. |
| An Android WebView | **CDP** | A WebView publishes CDP and no BiDi endpoint. See below. |
| A new web UI suite, with fast feedback and the strongest tooling | **Playwright** | Auto-waiting, web-first assertions, traces, a test runner and fixtures in one package. |

## WebDriver BiDi

BiDi is the bidirectional successor to classic WebDriver. Commands go over a WebSocket, and the browser pushes
events back: `log.entryAdded`, `network.beforeRequestSent`, `browsingContext.load` and others. That is what lets a
test wait for the browser to say something happened, instead of polling the page for side effects.

Choose it when you need cross-browser coverage that includes Firefox, when the tests already live in Selenium,
or when the browsers run on a Grid or a vendor cloud that speaks WebDriver. Chrome and Edge implement BiDi
through chromedriver's BiDi-to-CDP mapper, and Firefox implements it natively.

What the Selenium BiDi suite found (Selenium 4.54, Chrome 158, Firefox 161, Edge on the matching Grid image):

- All six areas it uses work on all three browsers: log events, request interception with `provideResponse` and
  `continueRequest`, `authRequired` with `continueWithAuth` and `cancelAuth`, preload scripts, browsing-context
  events, and element and document screenshots. Nothing needed a skip.
- The browsers do not report everything the same way. In `network.authRequired`, Chrome reports the response
  status as `-1` and Firefox reports `401`. The suite reads the 401 from `responseCompleted`, which both browsers
  fill in.
- A classic `driver.get()` to a page that asks for Basic auth can block inside chromedriver before the BiDi
  handler answers. A BiDi `browsingContext.navigate` does not block, so the auth tests navigate that way.
- Selenium's Java `ResponseData.getAuthChallenge()` came back empty on both browsers, so the test checks the
  `WWW-Authenticate` header instead.

## CDP (Chrome DevTools Protocol)

CDP is Chromium's internal debugging protocol, and it is what DevTools itself uses. It covers far more than BiDi:
tracing, coverage, heap snapshots, emulation details, and every Chromium-specific feature. The price is that it
is Chromium only, and it is versioned with the browser, so tooling has to keep up with each Chrome release
(Selenium ships a `selenium-devtools-v1NN` module per version for that reason).

Choose CDP when you need a Chromium-only capability, or when the target speaks nothing else. The second case
is real. Mobium's [decision record 0001](https://github.com/lana-20/mobium/blob/main/docs/decisions/0001-cdp-not-webdriver-bidi.md)
measured it: an Android WebView answers `/json/version` with `Protocol-Version: 1.3` and returns 404 for
`/session`. It speaks CDP and has no BiDi endpoint. Chrome the browser supports both, but "Chromium supports
BiDi" does not carry over to everything that embeds Chromium. That record's rule applies here too: check what
the target speaks before you build on it.

## Playwright

Playwright is a framework, not a protocol. It drives Chromium over CDP, and Firefox and WebKit through its own
protocol and patched builds. It can also attach to any Chromium over CDP, which is how this repo's Kernel leg
runs (`connectOverCDP`). Its strengths are the runner, auto-waiting, web-first assertions, traces and fixtures,
and its network mocking (`page.route`) is mature.

Choose it for a new web suite where the team owns the test stack and wants the fastest, least flaky path. Its
Firefox and WebKit builds are Playwright's own, not the stock browsers your users run.

## How this repo splits them

- **Selenium WebDriver BiDi** (`suites/selenium-bidi`): standard protocol, stock Chrome, Firefox and Edge on a
  Selenium Grid in Kubernetes, with event-driven checks of logs, network, scripts and navigation.
- **Playwright** (`suites/playwright`): the product-style UI suite, plus CDP to a Kernel cloud browser.
- **Vibium** (`suites/vibium-python`, `suites/vibium-mcp`): BiDi only, so it reaches the same Kernel browsers
  over BiDi, not CDP.
- **Mobium**: CDP for Android WebViews, for the reason in its decision record.
