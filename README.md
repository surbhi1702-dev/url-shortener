# URL Shortener

A URL shortening service built with **Java 17, Spring Boot 3, PostgreSQL, REST APIs and an HTML/CSS/JavaScript frontend**.
It turns long URLs into short codes and redirects visitors to the original URL.

## Features

- `POST /api/shorten` creates a unique 7-character Base62 short code for a URL
- `GET /{code}` redirects (HTTP 302) to the original URL
- **URL expiration:** optional `expiresInDays` per link; expired links return **410 Gone**, and a
  scheduled job deletes them after a retention period (default 30 days)
- **Click analytics:** every redirect is recorded (time, referrer, user agent); the click counter
  is incremented atomically in the database so concurrent clicks are never lost
- `GET /api/urls/{code}/stats` returns total clicks, last click, clicks per day (last 7 days) and top referrers
- Input validation (only absolute `http`/`https` URLs) with JSON error responses; 404 for unknown codes
- Data stored in PostgreSQL via Spring Data JPA (`urls` and `clicks` tables, indexed for lookups)

## Run it locally

Prerequisites: Java 17+ and PostgreSQL. Maven is not needed: the included wrapper (`mvnw` / `mvnw.cmd`) downloads it.

```bash
# 1. Create the database
psql -U postgres -c "CREATE DATABASE urlshortener"

# 2. Start the app (defaults: localhost:5432, user postgres / password postgres)
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run

# Or point it at your own database
DB_URL=jdbc:postgresql://localhost:5432/urlshortener DB_USERNAME=me DB_PASSWORD=secret ./mvnw spring-boot:run
```

The `urls` table is created automatically on startup.

## Try it

Open **http://localhost:8080** in your browser to shorten links and view click stats.

Or use the API directly:

```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://github.com/spring-projects/spring-boot"}'
```

```json
{
  "shortCode": "0d9A5mh",
  "shortUrl": "http://localhost:8080/0d9A5mh",
  "originalUrl": "https://github.com/spring-projects/spring-boot",
  "createdAt": "2026-10-05T07:25:15.068Z"
}
```

Open `http://localhost:8080/0d9A5mh` in a browser and you are redirected.

A link that expires in 7 days:

```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com", "expiresInDays": 7}'
```

Click analytics for a link:

```bash
curl http://localhost:8080/api/urls/0d9A5mh/stats
```

```json
{
  "shortCode": "0d9A5mh",
  "originalUrl": "https://example.com",
  "createdAt": "2026-10-06T19:35:57.337Z",
  "expiresAt": "2026-10-13T19:35:57.330Z",
  "expired": false,
  "totalClicks": 2,
  "lastClickedAt": "2026-10-06T19:35:57.405Z",
  "clicksPerDay": { "2026-09-30": 0, "...": 0, "2026-10-06": 2 },
  "topReferrers": [{ "referrer": "https://linkedin.com", "clicks": 1 }]
}
```

## API

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/shorten` | Body `{"url": "...", "expiresInDays": 7}` (`expiresInDays` optional). 201 with the short link |
| GET | `/{code}` | 302 redirect; 404 if unknown; 410 if expired |
| GET | `/api/urls/{code}/stats` | Click analytics for a link |

## Tests

```bash
./mvnw test                   # Windows: .\mvnw.cmd test
```

Tests use an in-memory H2 database, so PostgreSQL is not needed to run them.

## Project structure

```
src/main/java/com/surbhi/urlshortener
├── controller/   REST endpoints
├── service/      Shortening, click tracking, stats, expired-link cleanup job
├── repository/   Spring Data JPA repositories
├── model/        Url and Click entities
├── dto/          Request/response bodies
├── config/       Clock bean and scheduling
└── exception/    Error types and global handler

src/main/resources/static/   Frontend (index.html, style.css, app.js)
```

## Roadmap

- [x] **Milestone 1:** shorten + redirect with PostgreSQL
- [x] **Milestone 2:** URL expiration (`expiresAt` per link, 410 Gone once expired, scheduled cleanup)
- [x] **Milestone 3:** click analytics (count + per-click log, `GET /api/urls/{code}/stats`)
- [x] **Milestone 4:** HTML/CSS/JavaScript frontend to shorten links and view stats
