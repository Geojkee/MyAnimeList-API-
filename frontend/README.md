# MyAnimeList UI

Vite + React + TypeScript SPA for the Spring Boot `/api/v1` API.

## Prerequisites

- Node.js 20+ (npm)
- API running on `http://localhost:8080` (see root README / `make up && make run`)

## Develop

From the repo root:

```bash
make ui
# or:
cd frontend && npm install && npm run dev
```

UI: http://localhost:5173  

Vite proxies `/api` → `http://localhost:8080`, so auth cookies and Bearer tokens work without CORS headaches in local dev.

## Build

```bash
make ui-build
# or: cd frontend && npm run build
```

Output: `frontend/dist/`.

## Pages

| Route | Feature |
|-------|---------|
| `/` | Catalog browse + search/filters (`GET /anime`, genres) |
| `/anime/:id` | Detail, add to list, comments |
| `/auth/sign-in`, `/auth/sign-up` | Auth |
| `/my-list` | Own anime list (status / score / progress) |
| `/u/:username` | Public profile + list; edit when signed in as that user |
