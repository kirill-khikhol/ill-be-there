# I'll Be There

Users promise to show up at a sports ground at a chosen time. Others see those promises on a map as a day calendar (30-minute slots). Creating a promise also adds an event to Google Calendar.

This is a single repository: `frontend/` (React + TypeScript) and `backend/` (Spring Boot).

Cities in the first version: **Haifa**, **Tel Aviv**, **Ramat Gan**. Places are imported from OpenStreetMap; users can also add their own pin.

## Local run

You need Java 21+, Node 18+ (Node 16 can still build with Vite 4), and Docker (for Postgres).

```bash
docker compose up db -d
cd backend
./mvnw spring-boot:run
```

In another terminal:

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173

Full stack in Docker: `docker compose up --build`. The frontend is then on port 5173 via nginx, the API on 8080.

The map and place list work without Google. Login and calendar writes require OAuth.

The first `GET /api/locations` on an empty database imports OSM (up to about a minute). To re-run the import:

```bash
curl -X POST http://localhost:8080/api/admin/osm/import -H "X-Import-Token: dev-import"
```

## Google OAuth and Calendar

1. Create a project in [Google Cloud Console](https://console.cloud.google.com/).
2. Enable **Google Calendar API**.
3. OAuth consent screen, type External; add test users.
4. Create a Web application OAuth Client ID:
   - Authorized JavaScript origins: `http://localhost:5173`, plus the Render frontend URL
   - Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google` and `https://<api>.onrender.com/login/oauth2/code/google`
   - Also add `http://localhost:8080` as a JavaScript origin (login starts on the API host)
5. Put the Client ID and Secret in `backend/.env` (this file is gitignored):

```
GOOGLE_CLIENT_ID=....apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=....
```

Restart the backend from the `backend` directory so it picks up new code. The first log lines should include:

`I'll Be There: loaded ...\backend\.env (GOOGLE_CLIENT_ID=true, GOOGLE_CLIENT_SECRET=true)`

then `Google OAuth enabled, clientId ends with …xxxxxxxx`.

If you see `no .env file found`, the process was started from another working directory. Place `.env` in `backend/` or set the variables in the IDE run configuration.

Check that the keys reached the process (secrets are not printed):

```bash
curl http://localhost:8080/api/auth/config
```

Expect `"googleEnabled": true` and `clientIdHint` matching the tail of your Client ID. If it is `false`, the app does not see the variables: they must be in `backend/.env` or in the Java process environment, not only in Google Console.

Login requests the `calendar.events` scope. If Google does not return a refresh token, the promise is still created in the app. After changing calendar scopes, users must log out and log in again.

## Frontend on GitHub Pages

The frontend deploys from GitHub Actions on every push to `master` that touches `frontend/`.

URL: https://kirill-khikhol.github.io/ill-be-there/

First-time setup (once): repo **Settings → Pages → Source: GitHub Actions**. After that, **Actions → Deploy frontend → Run workflow** if you need a manual deploy.

Until the API is live, the map still opens (tiles work; location data will fail until `VITE_API_URL` is set). After the backend is on Render, add a repository variable `VITE_API_URL` (the API origin) and re-run the workflow.

## Free GitHub deploy (Render)

1. Push the repository to GitHub.
2. On [Render](https://render.com) → **New** → **Blueprint** → select the repository (`render.yaml`).
3. In the dashboard set:
   - `FRONTEND_ORIGIN` — static site URL, e.g. `https://ill-be-there-web.onrender.com`
   - `VITE_API_URL` — API URL, e.g. `https://ill-be-there-api.onrender.com`
   - `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`
4. After the first deploy, add the API redirect URI in Google Console.
5. If the frontend was built before you set `VITE_API_URL`, run **Manual Deploy** on the frontend again.

**Demo flow.** The free API spins down after 15 minutes without traffic. The frontend does not sleep. Open the site 1–2 minutes before the demo: the “server is waking up” screen appears. Once health returns, the app stays warm while there is traffic, and for 15 minutes after the last request. Five minutes after wake-up is plenty.

Do not add an external cron that pings the API every 5 minutes: on the Free plan that can exhaust the quota.

Free Render Postgres expires after 30 days. For the next presentation, create a new database or point `DATABASE_URL` at [Neon](https://neon.tech) Free.

## API (short)

- `GET /actuator/health` — wake-up / health check
- `GET /api/locations?city=HAIFA|TEL_AVIV|RAMAT_GAN`
- `POST /api/locations` — user-created place (JWT required)
- `GET /api/locations/{id}/promises?date=YYYY-MM-DD` — slots and counts (only slots after now)
- `GET /api/locations/{id}/promises/details?date=&slot=19:00` — names and avatars
- `POST /api/promises` `{ locationId, date, slot }`
- `DELETE /api/promises/{id}`
- `GET /api/favorites` — favorites sorted by last activity
- `POST /api/favorites` `{ locationId }`
- `DELETE /api/favorites/{locationId}`
- `GET /oauth2/authorization/google` — login
