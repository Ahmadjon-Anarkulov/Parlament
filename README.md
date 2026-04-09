# Parlament Bot (Spring Boot)

Production-ready Telegram bot template for the **Parlament** store, upgraded to **Spring Boot 3.3** + **Java 21** and prepared for **Railway.app** deployment.

## Features

- Spring Boot 3.3 (Java 21) executable JAR
- Telegram **long polling** (default) + **optional webhook** mode (env-configurable)
- Env-only configuration via `@ConfigurationProperties`
- `/actuator/health` for Railway health checks
- JSON console logging (Logback + Logstash encoder)
- Basic input validation / update sanitization (ignores malformed or oversized updates)
- Graceful shutdown enabled
- GitHub Actions CI (build + tests)
- Docker multi-stage build with non-root runtime user

## Tech stack

- Java 21
- Spring Boot 3.3.x
- Maven
- TelegramBots Java SDK (`org.telegram:telegrambots`)
- JUnit 5 (via `spring-boot-starter-test`)

## Configuration (Environment Variables)

All configuration must be provided **only** through environment variables (Railway Variables or local `.env`).

| Variable | Required | Example | Description |
|---|---:|---|---|
| `BOT_TOKEN` | ✅ | `123:abc...` | Telegram bot token |
| `BOT_USERNAME` | ✅ | `Parlamentclothes_bot` | Bot username without `@` |
| `BOT_MODE` | ⛔ | `long_polling` | `long_polling` \| `webhook` |
| `BOT_WEBHOOK_ENABLED` | ⛔ | `false` | Enable webhook mode |
| `BOT_WEBHOOK_PUBLIC_URL` | webhook only | `https://<app>.up.railway.app` | Public base URL |
| `BOT_WEBHOOK_PATH` | ⛔ | `/telegram/webhook` | Webhook endpoint path |
| `BOT_WEBHOOK_SECRET_TOKEN` | ⛔ | `some-secret` | Optional request validation header |
| `PORT` | ⛔ | `8080` | HTTP port (Railway sets this automatically) |
| `LOG_LEVEL_ROOT` | ⛔ | `INFO` | Root log level |

See `.env.example`.

## Local development

1) Create `.env` from example:

```bash
cp .env.example .env
```

2) Export variables (PowerShell example):

```powershell
$env:BOT_TOKEN="..."
$env:BOT_USERNAME="..."
$env:BOT_MODE="long_polling"
```

3) Run:

```bash
mvn spring-boot:run
```

Health check:

- `GET http://localhost:8080/actuator/health`

## Webhook mode (optional)

Webhook mode is useful when you want Telegram to push updates to your HTTP endpoint.

Set:

- `BOT_MODE=webhook`
- `BOT_WEBHOOK_ENABLED=true`
- `BOT_WEBHOOK_PUBLIC_URL=https://<your-public-domain>`

Optional:

- `BOT_WEBHOOK_SECRET_TOKEN` (Telegram will send header `X-Telegram-Bot-Api-Secret-Token`)

The app will call Telegram `setWebhook` on startup.

## Deploy to Railway

### Option A: Deploy from GitHub (recommended)

1) Push this repo to GitHub.
2) In Railway: **New Project** → **Deploy from GitHub repo**
3) Add variables:
   - `BOT_TOKEN`
   - `BOT_USERNAME`
   - (optional) webhook variables
4) Railway will build the Dockerfile and run the app.
5) Verify health: open `/actuator/health` on your Railway domain.

### Important security note

The original project had a bot token committed in `src/main/resources/bot.properties`.
That file has been removed, but **you must rotate the token in BotFather** because it existed in git history.

