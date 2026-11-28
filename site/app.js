// Renders the suite cards from suites.json and each suite's status JSON
// (data/status/<id>.json, written by ci/lib/status.py on whichever CI ran it).
(() => {
  "use strict";
  const REPO = "https://github.com/lana-20/test-showcase";
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  const getJSON = (url) =>
    fetch(url, { cache: "no-store" }).then((r) => (r.ok ? r.json() : null)).catch(() => null);

  function ago(iso) {
    const then = new Date(iso);
    if (Number.isNaN(then.getTime())) return "";
    const s = Math.round((Date.now() - then.getTime()) / 1000);
    const units = [["day", 86400], ["hour", 3600], ["minute", 60]];
    for (const [name, size] of units) {
      if (s >= size) {
        const n = Math.floor(s / size);
        return `${n} ${name}${n === 1 ? "" : "s"} ago`;
      }
    }
    return "just now";
  }

  function when(iso) {
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return "unknown";
    const abs = d.toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
    return `${abs} (${ago(iso)})`;
  }

  function el(tag, props = {}, ...children) {
    const node = document.createElement(tag);
    for (const [k, v] of Object.entries(props)) {
      if (k === "class") node.className = v;
      else if (k === "text") node.textContent = v;
      else node.setAttribute(k, v);
    }
    node.append(...children.filter((c) => c !== null && c !== undefined));
    return node;
  }

  function link(href, text) {
    return href ? el("a", { href, text }) : document.createTextNode(text);
  }

  // Where the run happened, said plainly. A target CI is only named as
  // "ready" until a run there actually produced the report.
  function ranOn(status, suite, ci) {
    const target = ci[suite.targetCi]?.name ?? suite.targetCi;
    if (!status) return { text: "No run yet", note: `${target} pipeline ready`, short: null };
    const actual = status.ran_on || { id: "unknown", name: "Unknown" };
    const onTarget = actual.id === suite.targetCi;
    return {
      text: actual.name,
      url: actual.url,
      note: onTarget ? null : `${target} pipeline ready`,
      short: (ci[actual.id]?.name ?? actual.name),
      onTarget,
    };
  }

  function counts(status) {
    const box = el("div", { class: "counts" });
    if (!status) {
      box.append(el("span", { class: "badge unknown", text: "No run yet" }));
      return box;
    }
    const errorLabel = status.total ? "Run failed" : "No results";
    const label = { passed: "Passing", failed: "Failing", error: errorLabel }[status.outcome] ?? status.outcome;
    box.append(
      el("span", { class: `badge ${status.outcome}`, text: label, title: status.reason || "" }),
      el("span", { class: "count pass", text: `${status.passed} passed` }),
      el("span", { class: "count fail", text: `${status.failed + (status.broken || 0)} failed` }),
      el("span", { class: "count skip", text: `${status.skipped} skipped` }),
    );
    return box;
  }

  function metaRows(dl, rows) {
    for (const [k, v] of rows) {
      if (v === null || v === undefined) continue;
      dl.append(el("dt", { text: k }), el("dd", {}, v));
    }
  }

  function terminalRun(card, suite, status, reportUrl) {
    const button = card.querySelector(".run");
    const term = card.querySelector(".term");
    button.addEventListener("click", async () => {
      button.disabled = true;
      term.hidden = false;
      term.textContent = "";
      const fresh = (await getJSON(`data/status/${suite.id}.json`)) || status;
      const lines = [`$ ci/run-${suite.id}.sh   # latest result`, `> reading data/status/${suite.id}.json`];
      if (fresh) {
        lines.push(
          `> last run ${ago(fresh.finished_at)} on ${fresh.ran_on?.name ?? "unknown"}`,
          `> ${fresh.passed} passed · ${fresh.failed + (fresh.broken || 0)} failed · ${fresh.skipped} skipped`,
        );
      } else {
        lines.push("> no status yet");
      }
      lines.push("> opening the Allure report");
      if (reduceMotion) {
        term.textContent = lines.join("\n");
      } else {
        // Type each line out, terminal style, with a blinking cursor.
        let done = "";
        for (const line of lines) {
          for (let i = 0; i <= line.length; i += 3) {
            term.replaceChildren(document.createTextNode(done + line.slice(0, i)),
              el("span", { class: "cursor", text: " " }));
            await new Promise((r) => setTimeout(r, 14));
          }
          done += line + "\n";
          await new Promise((r) => setTimeout(r, 160));
        }
      }
      await new Promise((r) => setTimeout(r, reduceMotion ? 300 : 450));
      window.location.href = reportUrl;
      button.disabled = false;
    });
  }

  function extraRuns(card, suite, extraStatuses) {
    const box = card.querySelector(".extra");
    (suite.extra || []).forEach((x, i) => {
      const st = extraStatuses[i];
      const sub = el("div", { class: "sub" });
      sub.append(el("strong", { text: x.label }));
      if (st) {
        const failed = st.failed + (st.broken || 0);
        sub.append(document.createTextNode(
          `: ${st.passed} passed · ${failed} failed. Ran on ${st.ran_on?.name ?? "unknown"}, ${ago(st.finished_at)}. `),
          el("a", { href: `reports/${x.id}/`, text: "Open report" }));
        if (st.ran_on?.url) sub.append(document.createTextNode(" · "), el("a", { href: st.ran_on.url, text: "run" }));
      } else {
        // Never claim a run that did not happen.
        sub.append(document.createTextNode(`: ${x.pending ?? "no run yet"}.`));
      }
      if (x.about) sub.append(el("div", { class: "muted", text: x.about }));
      box.append(sub);
    });
  }

  function render(config, statuses, extras) {
    const list = document.getElementById("suite-list");
    const tpl = document.getElementById("suite-template");
    const ciList = document.getElementById("ci-list");
    config.suites.forEach((suite, i) => {
      const status = statuses[i];
      const where = ranOn(status, suite, config.ci);
      const card = tpl.content.firstElementChild.cloneNode(true);
      card.id = suite.id;
      card.style.setProperty("--accent", `var(--a-${suite.id})`);
      card.setAttribute("aria-labelledby", `${suite.id}-title`);
      card.querySelector(".suite-index").textContent = String(i + 1);
      const title = card.querySelector(".suite-title");
      title.id = `${suite.id}-title`;
      title.textContent = suite.title;
      card.querySelector(".proves").textContent = suite.proves;
      const tags = card.querySelector(".tags");
      for (const t of suite.tags) tags.append(el("li", { text: t }));
      if (where.short) tags.append(el("li", { class: "ci-tag", text: `CI: ${where.short}` }));
      card.querySelector(".desc").textContent = suite.description;
      card.querySelector(".counts").replaceWith(counts(status));

      const ranOnNode = el("span", {}, link(where.url, where.text),
        where.note ? el("span", { class: "muted", text: ` · ${where.note}` }) : null);
      const imageNode = status?.image
        ? el("code", { text: status.image })
        : suite.image ? el("code", { text: suite.image }) : el("span", { class: "muted", text: "None: runs on the emulator host" });
      metaRows(card.querySelector(".meta"), [
        ["Ran on", ranOnNode],
        ["Last run", status ? document.createTextNode(when(status.finished_at)) : document.createTextNode("not yet")],
        ["Image", imageNode],
        ["Run log", status?.ran_on?.url ? link(status.ran_on.url, "CI run") : null],
      ]);

      const reportUrl = `reports/${suite.id}/`;
      const reportLink = card.querySelector(".report-link");
      reportLink.href = reportUrl;
      reportLink.textContent = `${suite.title} report`;
      const rerun = card.querySelector(".rerun");
      rerun.href = `${REPO}/actions/workflows/${suite.workflow}`;
      rerun.title = "Opens the workflow on GitHub. Only the repository owner can start a run.";
      terminalRun(card, suite, status, reportUrl);
      extraRuns(card, suite, extras[i]);
      list.append(card);

      const target = config.ci[suite.targetCi]?.name ?? suite.targetCi;
      const li = el("li", {}, el("b", { text: target }),
        el("span", { text: where.onTarget ? `${suite.title}` : `${suite.title} (ready; runs on ${where.short ?? "GitHub Actions"} for now)` }));
      li.style.setProperty("--accent", `var(--a-${suite.id})`);
      ciList.append(li);
    });
    if (location.hash) document.getElementById(location.hash.slice(1))?.scrollIntoView();
  }

  async function main() {
    const config = await getJSON("suites.json");
    if (!config) return;
    const available = new Set((await getJSON("data/status/index.json")) || []);
    const status = (id) => (available.has(id) ? getJSON(`data/status/${id}.json`) : Promise.resolve(null));
    const statuses = await Promise.all(config.suites.map((s) => status(s.id)));
    const extras = await Promise.all(config.suites.map((s) => Promise.all((s.extra || []).map((x) => status(x.id)))));
    render(config, statuses, extras);
  }
  main();
})();
