# URL Shortener

A URL shortening service built with **Java 17, Spring Boot 3, PostgreSQL and REST APIs**.
It turns long URLs into short codes and redirects visitors to the original URL.

## Features (milestone 1)

- `POST /api/shorten` creates a unique 7-character Base62 short code for a URL
- `GET /{code}` redirects (HTTP 302) to the original URL
- Input validation (only absolute `http`/`https` URLs) with JSON error responses
- 404 for unknown short codes
- URLs stored in PostgreSQL via Spring Data JPA

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

## Tests

```bash
./mvnw test                   # Windows: .\mvnw.cmd test
```

Tests use an in-memory H2 database, so PostgreSQL is not needed to run them.

## Project structure

```
src/main/java/com/surbhi/urlshortener
├── controller/   REST endpoints
├── service/      Shortening logic and short-code generation
├── repository/   Spring Data JPA repository
├── model/        Url entity
├── dto/          Request/response bodies
└── exception/    Error types and global handler
```

## Roadmap

- [x] **Milestone 1:** shorten + redirect with PostgreSQL
- [ ] **Milestone 2:** URL expiration (`expiresAt` per link, 410 Gone once expired, scheduled cleanup)
- [ ] **Milestone 3:** click analytics (count + per-click log, `GET /api/urls/{code}/stats`)
- [ ] **Milestone 4:** HTML/CSS/JavaScript frontend to shorten links and view stats
