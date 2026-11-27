// Tidepool: a deliberately small app for the test suites in this repo.
// State lives in localStorage so each browser context starts clean.
(() => {
  "use strict";
  const USER = "demo";
  const PASS = "tide-pass";
  const store = {
    get: (k, d) => { try { return JSON.parse(localStorage.getItem(k)) ?? d; } catch { return d; } },
    set: (k, v) => localStorage.setItem(k, JSON.stringify(v)),
    del: (k) => localStorage.removeItem(k),
  };
  const $ = (sel) => document.querySelector(sel);
  const page = location.pathname.split("/").pop() || "index.html";

  function requireUser() {
    const user = store.get("tp-user", null);
    if (!user) { location.replace("index.html?next=" + encodeURIComponent(page)); return null; }
    $("#greeting").textContent = "Signed in as " + user;
    $("#sign-out").addEventListener("click", () => { store.del("tp-user"); location.href = "index.html"; });
    return user;
  }

  // --- Sign in ------------------------------------------------------------
  if (page === "index.html") {
    const form = $("#login-form");
    const error = $("#login-error");
    form.addEventListener("submit", (e) => {
      e.preventDefault();
      const u = $("#username").value.trim();
      const p = $("#password").value;
      error.hidden = true;
      if (!u || !p) { error.textContent = "Enter a username and a password."; error.hidden = false; return; }
      const btn = $("[data-testid=sign-in]");
      btn.disabled = true; btn.textContent = "Signing in…";
      // A short, variable delay: tests must wait for the outcome, not sleep.
      setTimeout(() => {
        btn.disabled = false; btn.textContent = "Sign in";
        if (u === USER && p === PASS) {
          store.set("tp-user", u);
          const next = new URLSearchParams(location.search).get("next");
          location.href = next && /^[a-z]+\.html$/.test(next) ? next : "board.html";
        } else {
          error.textContent = "Wrong username or password.";
          error.hidden = false;
        }
      }, 250 + Math.random() * 400);
    });
  }

  // --- Tasks --------------------------------------------------------------
  if (page === "board.html" && requireUser()) {
    let tasks = store.get("tp-tasks", []);
    let filter = "all";
    const list = $("#task-list");
    const save = () => store.set("tp-tasks", tasks);
    function render() {
      list.replaceChildren();
      tasks.filter((t) => filter === "all" || (filter === "done") === t.done).forEach((t) => {
        const li = document.createElement("li");
        li.dataset.testid = "task";
        li.className = t.done ? "done" : "";
        const box = document.createElement("input");
        box.type = "checkbox"; box.checked = t.done; box.id = "t-" + t.id;
        box.setAttribute("aria-label", "Complete " + t.title);
        box.dataset.testid = "toggle";
        box.addEventListener("change", () => { t.done = box.checked; save(); render(); });
        const label = document.createElement("label");
        label.htmlFor = box.id; label.textContent = t.title; label.dataset.testid = "task-title";
        const del = document.createElement("button");
        del.className = "link"; del.textContent = "Delete"; del.dataset.testid = "delete";
        del.setAttribute("aria-label", "Delete " + t.title);
        del.addEventListener("click", () => { tasks = tasks.filter((x) => x !== t); save(); render(); });
        li.append(box, label, del);
        list.append(li);
      });
      const left = tasks.filter((t) => !t.done).length;
      $("#remaining").textContent = left + (left === 1 ? " task left" : " tasks left");
    }
    $("#task-form").addEventListener("submit", (e) => {
      e.preventDefault();
      const input = $("#new-task");
      const title = input.value.trim();
      const err = $("#task-error");
      err.hidden = true;
      if (!title) { err.textContent = "A task needs a title."; err.hidden = false; return; }
      if (tasks.some((t) => t.title.toLowerCase() === title.toLowerCase())) {
        err.textContent = "That task is already on the list."; err.hidden = false; return;
      }
      tasks.push({ id: Date.now().toString(36) + tasks.length, title, done: false });
      input.value = ""; save(); render();
    });
    document.querySelectorAll("[data-filter]").forEach((b) => b.addEventListener("click", () => {
      filter = b.dataset.filter;
      document.querySelectorAll("[data-filter]").forEach((x) => x.setAttribute("aria-pressed", String(x === b)));
      render();
    }));
    render();
  }

  // --- Catalog ------------------------------------------------------------
  if (page === "catalog.html" && requireUser()) {
    let devices = [];
    let cart = store.get("tp-cart", []);
    const status = $("#status");
    const results = $("#results");
    function renderCart() {
      $("#cart-count").textContent = String(cart.length);
      $("#checkout").disabled = cart.length === 0;
    }
    function render() {
      const q = $("#search").value.trim().toLowerCase();
      const p = $("#platform").value;
      const shown = devices.filter((d) => (!p || d.platform === p) &&
        (!q || d.name.toLowerCase().includes(q) || d.os.toLowerCase().includes(q)));
      results.replaceChildren();
      shown.forEach((d) => {
        const li = document.createElement("li");
        li.dataset.testid = "result";
        li.dataset.id = d.id;
        const name = document.createElement("span");
        name.textContent = d.name + " · " + d.os;
        const add = document.createElement("button");
        const inCart = cart.includes(d.id);
        add.textContent = inCart ? "Added" : "Add to cart";
        add.disabled = inCart;
        add.dataset.testid = "add-" + d.id;
        add.setAttribute("aria-label", "Add " + d.name + " to cart");
        add.addEventListener("click", () => { cart.push(d.id); store.set("tp-cart", cart); renderCart(); render(); });
        li.append(name, add);
        results.append(li);
      });
      status.textContent = shown.length === 0 ? "No devices match." :
        shown.length + (shown.length === 1 ? " device" : " devices");
    }
    let timer;
    $("#search").addEventListener("input", () => { clearTimeout(timer); timer = setTimeout(render, 150); });
    $("#platform").addEventListener("change", render);
    $("#checkout").addEventListener("click", () => {
      const n = cart.length;
      cart = []; store.set("tp-cart", cart); renderCart(); render();
      const c = $("#confirmation");
      c.textContent = "Reserved " + n + (n === 1 ? " device." : " devices.");
      c.hidden = false;
    });
    renderCart();
    // Simulate a slow API so tests exercise waiting.
    setTimeout(() => {
      fetch("devices.json").then((r) => r.json()).then((d) => { devices = d; render(); })
        .catch(() => { status.textContent = "Could not load devices."; });
    }, 300 + Math.random() * 500);
  }
})();
