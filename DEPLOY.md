# Production deployment

Copy `.env.example` to `.env` and provide production values. Never commit `.env`.

Required values include `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (at least 32 random bytes), `APP_BASE_URL`, `UPLOAD_DIR`, `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_FROM_NAME`, `MAIL_SMTP_AUTH`, and `MAIL_SMTP_STARTTLS`.

Build and start:

```powershell
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d
```

Flyway validates and applies migrations at backend startup. Back up PostgreSQL before upgrades:

```powershell
docker compose -f docker-compose.prod.yml exec db pg_dump -U $env:DB_USERNAME -d $env:DB_NAME > backup.sql
```

Restore after stopping writes:

```powershell
Get-Content backup.sql | docker compose -f docker-compose.prod.yml exec -T db psql -U $env:DB_USERNAME -d $env:DB_NAME
```

Keep the database volume and uploaded avatar directory in separate backups. Do not expose PostgreSQL publicly.
