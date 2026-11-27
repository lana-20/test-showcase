// A dependency-free static server for the demo app: node serve.mjs [port]
import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, join, normalize } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL(".", import.meta.url));
const port = Number(process.argv[2] ?? process.env.PORT ?? 8080);
const types = { ".html": "text/html; charset=utf-8", ".js": "text/javascript", ".css": "text/css", ".json": "application/json" };

// One route behind HTTP Basic auth, for tests of browser-level authentication.
// Demo credentials, published in the open like demo / tide-pass on the sign-in page.
const LAB_PATH = "/lab";
const LAB_AUTH = "Basic " + Buffer.from("lab:reef-pass").toString("base64");
const LAB_PAGE = `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><title>Tidepool · Lab</title><link rel="stylesheet" href="app.css"></head>
<body><main class="card" aria-labelledby="title"><h1 id="title">Lab console</h1>
<p data-testid="lab-status">Signed in to the device lab.</p></main></body></html>
`;

function lab(req, res) {
  if (req.headers.authorization !== LAB_AUTH) {
    res.writeHead(401, { "www-authenticate": 'Basic realm="Tidepool lab", charset="UTF-8"', "content-type": "text/plain" })
      .end("authentication required");
    return;
  }
  res.writeHead(200, { "content-type": types[".html"], "cache-control": "no-store" }).end(LAB_PAGE);
}

createServer(async (req, res) => {
  const path = normalize(decodeURIComponent(new URL(req.url, "http://x").pathname)).replace(/^(\.\.[/\\])+/, "");
  if (path === LAB_PATH) { lab(req, res); return; }
  const file = join(root, path === "/" ? "index.html" : path);
  if (!file.startsWith(root)) { res.writeHead(403).end(); return; }
  try {
    const body = await readFile(file);
    res.writeHead(200, { "content-type": types[extname(file)] ?? "application/octet-stream", "cache-control": "no-store" });
    res.end(body);
  } catch {
    res.writeHead(404, { "content-type": "text/plain" }).end("not found");
  }
}).listen(port, "0.0.0.0", () => console.log(`demo app on http://0.0.0.0:${port}/`));
