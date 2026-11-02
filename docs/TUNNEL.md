# Temporary public tunnel

This setup exposes the Vite development server only for short-lived testing. It is not a production deployment.

## Configure the project

1. Start from a clean local Mailpit configuration in the root `.env`.
2. Set the tunnel URL after the tunnel starts:

```env
APP_BASE_URL=https://your-tunnel.example
APP_ALLOWED_HOSTS=.trycloudflare.com
VITE_API_PROXY_TARGET=http://localhost:8080
COOKIE_SECURE=true
```

`APP_BASE_URL` controls email links and the backend CORS origin. `APP_ALLOWED_HOSTS` is optional; use a comma-separated list or a Vite wildcard such as `.trycloudflare.com`. The Vite proxy keeps `/api` requests on the public frontend origin and forwards them to the local backend.

## Start the services

From the repository root, start PostgreSQL and Mailpit as usual:

```powershell
docker compose up -d
```

Start the backend from its directory so Spring imports the root `.env`:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Start the frontend in a second terminal:

```powershell
cd frontend
npm run dev -- --host 0.0.0.0
```

## Start one tunnel

Cloudflare quick tunnel:

```powershell
cloudflared tunnel --url http://localhost:5173
```

Equivalent ngrok command:

```powershell
ngrok http 5173
```

Copy the HTTPS URL printed by the tunnel into `APP_BASE_URL` (without a trailing slash), restart the backend, and restart Vite if the host was not already allowed. Send a new invite, open it from a phone using mobile data, set the password, verify the email, and test password reset.

When the tunnel URL changes, update `APP_BASE_URL`, restart the backend, restart Vite if needed, and resend the invite. Old links point to the old URL.

## URL and cookie dependencies

- Email links are built from `APP_BASE_URL` in `EmailSenderJob`.
- Backend CORS allows `APP_BASE_URL` through `CorsConfig`.
- Vite derives `server.allowedHosts` from `APP_BASE_URL` and `APP_ALLOWED_HOSTS`; its `/api` proxy target is `VITE_API_PROXY_TARGET`.
- Refresh cookies are httpOnly, `SameSite=Strict`, path `/api/auth`, and have no domain so they belong to the tunnel host. `COOKIE_SECURE=true` works when the phone uses the HTTPS tunnel. For plain HTTP on a local network, set `COOKIE_SECURE=false`.
- The remaining `localhost` values are development defaults: backend port/database/Mailpit defaults, the default `APP_BASE_URL`, the default Vite proxy target, and test-profile values. They do not override configured tunnel values.

Because the browser talks to the frontend tunnel and the frontend proxy talks to the backend, the API calls remain same-origin from the browser. Strict SameSite cookies are therefore suitable for this tunnel arrangement. A real deployment should use a stable HTTPS domain, a reverse proxy or deployed frontend/backend, production secrets and JWT settings, a managed database, a real SMTP provider, restricted CORS, secure cookie policy, monitoring, backups, and rate limiting.

While the tunnel runs, the app is publicly reachable. Invite links stop working when the tunnel stops or its URL changes. Use a strong Manager password and test only with accounts and addresses you control.
