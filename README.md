# OnlyOffice + Spring Boot (MinIO storage)

Spring Boot 3 / Java 21 app that integrates the OnlyOffice Document Server for
in-browser editing of Word/Excel/PowerPoint documents, storing files in MinIO
(S3-compatible), with JWT-secured editor config and save callbacks.

## Architecture

```
Browser ──loads api.js──▶ OnlyOffice Document Server (:8080)
   │                              │
   │ opens /editor                │ downloads original  ──▶ Spring /api/files/{name}/download
   ▼                              │ posts save callback ──▶ Spring /api/callback/{name}
Spring Boot (:8081) ◀─────────────┘
   │
   └── reads/writes documents ──▶ MinIO (:9000)
```

Because the Document Server runs in Docker, it reaches this app via
`host.docker.internal`. That host is configured in `app.public-url`.

## Run

1. Start Document Server + MinIO:

   ```bash
   docker compose up -d
   ```

   - OnlyOffice: http://localhost:8080 (healthcheck may take ~30s)
   - MinIO console: http://localhost:9001 (minioadmin / minioadmin)

2. Start the app (the `documents` bucket is auto-created on startup):

   ```bash
   ./mvnw spring-boot:run     # or: mvn spring-boot:run
   ```

3. Open http://localhost:8081 — upload a `.docx`/`.xlsx`/`.pptx`, then Edit.

## Key files

| File | Role |
|------|------|
| `DocumentManager` | Builds & signs the OnlyOffice editor config |
| `JwtService` | HS256 sign/verify with the shared secret |
| `CallbackController` | Verifies callbacks, saves edited docs back to MinIO |
| `StorageService` | MinIO (S3) upload/download/list |
| `DocumentKeyRegistry` | Version key that changes on every save |

## Configuration (`application.yml`)

- `onlyoffice.jwt-secret` **must equal** `JWT_SECRET` in `docker-compose.yml`.
- `app.public-url` must be reachable *from the Document Server container*
  (`host.docker.internal:8081` on Docker Desktop).
- `onlyoffice.docserver-url` is what the *browser* uses to load `api.js`.

## Callback status codes

| status | meaning | action |
|--------|---------|--------|
| 1 | document being edited | none |
| 2 | ready to save (MustSave) | download + persist |
| 3 | save error | logged |
| 4 | closed, no changes | none |
| 6 | force save | download + persist |
| 7 | force save error | logged |
