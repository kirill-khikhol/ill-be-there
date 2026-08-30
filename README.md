# I'll Be There

Пользователь обещает прийти на спортплощадку к выбранному времени. Остальные видят это на карте в виде календаря дня (слоты по 30 минут). При обещании событие добавляется в Google Calendar.

Один репозиторий: `frontend/` (React + TypeScript) и `backend/` (Spring Boot).

Города первой версии: **Хайфа**, **Тель-Авив**, **Рамат-Ган**. Точки подтягиваются из OpenStreetMap; свою площадку можно добавить вручную.

## Локальный запуск

Нужны Java 21+, Node 18+ (на Node 16 тоже должно собраться: Vite 4), Docker (для Postgres).

```bash
docker compose up db -d
cd backend
./mvnw spring-boot:run
```

В другом терминале:

```bash
cd frontend
npm install
npm run dev
```

Откройте http://localhost:5173

Полный стек в Docker: `docker compose up --build`. Фронт тогда на порту 5173 через nginx, API на 8080.

Карта и список площадок работают без Google. Логин и запись в календарь — после настройки OAuth.

Первый запрос `GET /api/locations` при пустой базе сам импортирует OSM (до минуты). Повторить импорт:

```bash
curl -X POST http://localhost:8080/api/admin/osm/import -H "X-Import-Token: dev-import"
```

## Google OAuth и Calendar

1. Создайте проект в [Google Cloud Console](https://console.cloud.google.com/).
2. Включите **Google Calendar API**.
3. OAuth consent screen, тип External, добавьте тестовых пользователей.
4. OAuth Client ID типа Web:
   - Authorized JavaScript origins: `http://localhost:5173`, URL фронта на Render
   - Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google` и `https://<api>.onrender.com/login/oauth2/code/google`
5. Скопируйте Client ID и Secret в `backend/.env` (файл не коммитится):

```
GOOGLE_CLIENT_ID=....apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=....
```

Перезапустите бэкенд из папки `backend` (чтобы подтянуть свежий код). В самом начале лога должно быть:

`I'll Be There: loaded ...\backend\.env (GOOGLE_CLIENT_ID=true, GOOGLE_CLIENT_SECRET=true)`

затем `Google OAuth enabled, clientId ends with …xxxxxxxx`.

Если видите `no .env file found` — процесс запущен из другой папки; положите `.env` в `backend/` или задайте переменные в Run Configuration IDE.

Проверка, что ключи дошли до процесса (секреты не печатаются):

```bash
curl http://localhost:8080/api/auth/config
```

Ожидается `"googleEnabled": true` и `clientIdHint` с хвостом вашего Client ID. Если `false` — приложение не видит переменные: ключи должны быть в `backend/.env` или в env процесса Java, не только в Google Console.

При входе запрашивается scope `calendar.events`. Если Google не отдал refresh token, обещание в приложении всё равно создаётся.

## Бесплатный деплой с GitHub (Render)

1. Залейте репозиторий на GitHub.
2. На [Render](https://render.com) → **New** → **Blueprint** → выберите репозиторий (`render.yaml`).
3. В dashboard заполните:
   - `FRONTEND_ORIGIN` — URL статического сайта, например `https://ill-be-there-web.onrender.com`
   - `VITE_API_URL` — URL API, например `https://ill-be-there-api.onrender.com`
   - `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`
4. После первого деплоя добавьте redirect URI API в Google Console.
5. Если фронт собрался до того, как вы задали `VITE_API_URL`, сделайте **Manual Deploy** фронта ещё раз.

**Как проводить демо.** Free API засыпает через 15 минут без трафика. Фронт не спит. Откройте сайт за 1–2 минуты до показа: появится экран «сервер просыпается». Когда health ответит, приложение тёплое — пока идёт трафик и ещё 15 минут после последнего запроса. Пять минут после пробуждения хватает с запасом.

Не ставьте внешний cron, который пингует API каждые 5 минут: на Free это может съесть квоту.

Free Postgres на Render живёт 30 дней. Для следующей презентации создайте новую базу или переключите `DATABASE_URL` на [Neon](https://neon.tech) Free.

## API (кратко)

- `GET /actuator/health` — пробуждение / проверка
- `GET /api/locations?city=HAIFA|TEL_AVIV|RAMAT_GAN`
- `POST /api/locations` — своя точка (нужен JWT)
- `GET /api/locations/{id}/promises?date=YYYY-MM-DD` — слоты и количества
- `GET /api/locations/{id}/promises/details?date=&slot=19:00` — имена и аватары
- `POST /api/promises` `{ locationId, date, slot }`
- `DELETE /api/promises/{id}`
- `GET /oauth2/authorization/google` — вход
