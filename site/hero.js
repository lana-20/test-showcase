// Hero banner: rows of keywords drift sideways while bobbing along slow sine
// waves, over a faint field of ASCII wave glyphs. Plain canvas, no libraries.
// With prefers-reduced-motion the canvas is hidden and a static list is shown.
(() => {
  "use strict";
  const KEYWORDS = [
    "SDET", "Mobile Test Automation", "iOS", "Android", "Appium", "XCUITest", "Espresso",
    "UiAutomator2", "Playwright", "Selenium WebDriver BiDi", "pytest", "LLM Evals",
    "LLM-as-judge", "RAG quality gates", "Prompt A/B tests", "Red-teaming", "MCP",
    "AI agents", "Mobium", "Vibium", "GitHub Actions", "Real devices", "Flaky-test triage",
    "Root cause", "Quality signal",
  ];
  const GLYPHS = ["~", "≈", "∿", "~", "-", "≈"];

  const hero = document.querySelector(".hero");
  const canvas = document.getElementById("waves");
  const staticList = document.querySelector(".hero-static");
  if (!hero || !canvas) return;

  staticList.replaceChildren(...KEYWORDS.map((k) => {
    const li = document.createElement("li");
    li.textContent = k;
    return li;
  }));

  const reduce = window.matchMedia("(prefers-reduced-motion: reduce)");
  const ctx = canvas.getContext("2d");
  let W = 0, H = 0, dpr = 1, rows = [], ink = "16, 50, 79", running = false, raf = 0, last = 0, t = 0;
  const mono = getComputedStyle(document.body).getPropertyValue("--mono") || "monospace";

  // A small seeded random, so the layout is the same on every visit.
  let seed = 7;
  const rand = () => ((seed = (seed * 16807) % 2147483647) - 1) / 2147483646;

  function readInk() {
    ink = getComputedStyle(document.documentElement).getPropertyValue("--wave-ink").trim() || ink;
  }

  function layout() {
    const r = hero.getBoundingClientRect();
    dpr = Math.min(window.devicePixelRatio || 1, 2);
    W = Math.max(1, Math.round(r.width));
    H = Math.max(1, Math.round(r.height));
    canvas.width = W * dpr;
    canvas.height = H * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

    seed = 7;
    const count = Math.max(4, Math.min(7, Math.round(H / 62)));
    rows = [];
    for (let i = 0; i < count; i++) {
      const size = 13 + Math.round(rand() * 9);              // 13–22px
      ctx.font = `${size}px ${mono}`;
      // Each row gets its own shuffled run of phrases, joined by wave glyphs.
      const words = KEYWORDS.slice().sort(() => rand() - 0.5);
      const items = [];
      let x = 0;
      const gap = ctx.measureText("  ∿  ").width;
      for (const w of words) {
        const width = ctx.measureText(w).width;
        items.push({ text: w, x, width });
        x += width + gap;
      }
      rows.push({
        y: ((i + 0.75) / count) * H,
        size,
        items,
        span: x,                                               // one full lap
        speed: (10 + rand() * 16) * (i % 2 ? -1 : 1),          // px/s, alternating direction
        amp: 5 + rand() * 9,                                   // px
        freq: 0.006 + rand() * 0.006,                          // radians per px
        omega: 0.35 + rand() * 0.45,                           // radians per s
        phase: rand() * Math.PI * 2,
        alpha: 0.16 + rand() * 0.26,
        offset: rand() * x,
      });
    }
  }

  // Words fade near the middle, where the name sits.
  function centerFade(x, y) {
    const dx = (x - W / 2) / Math.max(W * 0.34, 210);
    const dy = (y - H / 2) / Math.max(H * 0.36, 150);
    const d = Math.sqrt(dx * dx + dy * dy);
    return Math.min(1, 0.12 + Math.max(0, d - 0.35) * 1.1);
  }

  function drawGlyphField() {
    ctx.font = `14px ${mono}`;
    const stepX = 26, stepY = 24;
    for (let gy = 10; gy < H; gy += stepY) {
      for (let gx = 0; gx < W + stepX; gx += stepX) {
        const k = (Math.floor(gx / stepX) + Math.floor(gy / stepY)) % GLYPHS.length;
        const wobble = Math.sin(gx * 0.02 + t * 0.6 + gy * 0.05) * 3;
        const a = 0.05 + 0.04 * Math.sin(gx * 0.013 - t * 0.5 + gy * 0.02);
        ctx.fillStyle = `rgba(${ink}, ${a.toFixed(3)})`;
        ctx.fillText(GLYPHS[k], gx - ((t * 8) % stepX), gy + wobble);
      }
    }
  }

  function draw() {
    ctx.clearRect(0, 0, W, H);
    drawGlyphField();
    for (const row of rows) {
      ctx.font = `${row.size}px ${mono}`;
      const shift = ((row.offset + row.speed * t) % row.span + row.span) % row.span;
      for (let lap = -1; lap <= Math.ceil(W / row.span); lap++) {
        for (const item of row.items) {
          const x = item.x - shift + lap * row.span;
          if (x > W || x + item.width < 0) continue;
          const cx = x + item.width / 2;
          const y = row.y + row.amp * Math.sin(cx * row.freq + row.omega * t + row.phase);
          const a = row.alpha * centerFade(cx, y);
          ctx.fillStyle = `rgba(${ink}, ${a.toFixed(3)})`;
          ctx.fillText(item.text, x, y);
          ctx.fillStyle = `rgba(${ink}, ${(a * 0.6).toFixed(3)})`;
          ctx.fillText("∿", x + item.width + 10, y);
        }
      }
    }
  }

  function frame(now) {
    if (!running) return;
    const dt = Math.min(0.05, (now - (last || now)) / 1000);
    last = now;
    t += dt;
    draw();
    raf = requestAnimationFrame(frame);
  }

  function start() {
    if (running || reduce.matches || document.hidden) return;
    running = true;
    last = 0;
    raf = requestAnimationFrame(frame);
  }
  function stop() {
    running = false;
    cancelAnimationFrame(raf);
  }

  function init() {
    readInk();
    layout();
    draw();
    if (!reduce.matches) start();
  }

  let visible = true;
  new IntersectionObserver(([e]) => {
    visible = e.isIntersecting;
    visible ? start() : stop();
  }).observe(hero);
  document.addEventListener("visibilitychange", () => (document.hidden || !visible ? stop() : start()));
  let resizeTimer;
  window.addEventListener("resize", () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => { layout(); draw(); }, 120);
  });
  window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => { readInk(); draw(); });
  reduce.addEventListener("change", () => (reduce.matches ? stop() : start()));

  if (document.fonts && document.fonts.ready) document.fonts.ready.then(init); else init();
})();
