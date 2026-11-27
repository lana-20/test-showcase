# The Tidepool demo web app, served by a dependency-free Node static server.
FROM node:24.22.3-bookworm-slim
WORKDIR /app
COPY --chown=node:node demo-app/ ./
USER node
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s CMD node -e "fetch('http://127.0.0.1:8080/index.html').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
CMD ["node", "serve.mjs", "8080"]
