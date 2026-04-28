# Docker Team Workflow

This project now has two Docker Compose entry points.

## Development

Use this when several teammates are editing the project together:

```powershell
Copy-Item .env.example .env
docker compose -f docker-compose.dev.yml up --build
```

Open:

- Frontend dev server: `http://localhost:3000`
- Backend API: `http://localhost:1010/SCSE@hbut/msi`
- Backend docs: `http://localhost:1010/SCSE@hbut/doc.html`
- MongoDB: `localhost:27017`
- Redis: `localhost:6379`

In development mode, source folders are mounted into containers:

- `./index` -> frontend container
- `./disciplinary_construction` -> backend container

So teammates can edit files locally and let Docker provide the same Node, Java, Maven, MongoDB, and Redis environment.

## Deployment Preview

Use this when you want a closer-to-production preview:

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

Open:

- Frontend: `http://localhost:8080`
- Backend through Nginx: `http://localhost:8080/SCSE@hbut/msi`

## Common Commands

```powershell
docker compose -f docker-compose.dev.yml logs -f
docker compose -f docker-compose.dev.yml down
docker compose -f docker-compose.dev.yml down -v
docker compose up -d --build
docker compose down
```

`down -v` removes database/upload/cache volumes, so only use it when you are sure local test data can be deleted.

## Production Notes

Before real deployment, change these values in `.env`:

- `JWT_SECRET`
- `APP_CORS_ALLOWED_ORIGINS`
- `REDIS_PASSWORD` if Redis is exposed or shared

The default secret is only a development placeholder.
