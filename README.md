# MyAnimeList-API

[![Java CI with Maven](https://github.com/Geojkee/MyAnimeList-API-/actions/workflows/ci.yml/badge.svg)](https://github.com/Geojkee/MyAnimeList-API-/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Geojkee/MyAnimeList-API-/branch/master/graph/badge.svg)](https://codecov.io/gh/Geojkee/MyAnimeList-API-)

REST API для каталога аниме и пользовательских списков (учебный проект на **Java 21** / **Spring Boot 4**).

JWT-аутентификация с refresh-токеном в HTTP-only cookie, каталог аниме и жанров, профили, anime list, комментарии, друзья. Схема БД — Liquibase, документация — Swagger UI.

## Быстрый старт (локально)

Нужны: **Java 21**, **Docker** (для PostgreSQL) или свой PostgreSQL 15+, **Git**. Maven ставится не обязательно — в репозитории есть `./mvnw`.

### Вариант A — БД в Docker, приложение через Maven (рекомендуется для разработки)

```bash
git clone https://github.com/Geojkee/MyAnimeList-API-.git
cd MyAnimeList-API-

cp .env.example .env

docker compose up -d postgres

./mvnw spring-boot:run
```

Откройте:

- API: http://localhost:8080  
- Swagger UI: http://localhost:8080/swagger-ui/index.html  
- OpenAPI JSON: http://localhost:8080/v3/api-docs  

Остановка БД: `docker compose down` (данные сохраняются в volume `postgres_data`).

### Вариант B — всё в Docker (Postgres + API)

```bash
git clone https://github.com/Geojkee/MyAnimeList-API-.git
cd MyAnimeList-API-

cp .env.example .env

docker compose --profile full up --build
```

API будет на http://localhost:8080. Остановка: `docker compose --profile full down`.

### Вариант C — свой PostgreSQL без Docker

1. Создайте БД, например `myanimelist`.
2. `cp .env.example .env` и поправьте `POSTGRES_URL` / `POSTGRES_USER` / `POSTGRES_PASSWORD`.
3. `./mvnw spring-boot:run`

Liquibase применит миграции при старте.

> На Windows используйте `mvnw.cmd` вместо `./mvnw`. Если `./mvnw` не исполняется: `chmod +x mvnw`.

## Переменные окружения

Файл `.env` (шаблон — `.env.example`) подхватывается Spring Boot (`optional:file:.env[.properties]`).  
В `application.yaml` заданы **локальные значения по умолчанию**: без `.env` приложение тоже стартует, если PostgreSQL доступен на `localhost:5432` с БД `myanimelist` и пользователем/паролем `postgres`.

| Переменная | По умолчанию (local) | Назначение |
|------------|----------------------|------------|
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/myanimelist` | JDBC URL |
| `POSTGRES_USER` | `postgres` | Пользователь БД / Liquibase |
| `POSTGRES_PASSWORD` | `postgres` | Пароль БД |
| `POSTGRES_DB` | `myanimelist` | Имя БД (Compose) |
| `POSTGRES_PORT` | `5432` | Порт Postgres (Compose) |
| `JWT_SECRET` | Base64 local key (≥32 байт после decode) | Подпись JWT — **только Base64**, смените в проде |
| `JWT_EXPIRATION` | `86400000` | TTL access token, мс |
| `REFRESH_TOKEN_DURATION` | `2592000000` | TTL refresh token в БД, мс |
| `REFRESH_TOKEN_MAX_AGE_SECONDS` | `2592000` | Max-Age cookie |
| `COOKIE_PATH` | `/api/v1/auth` | Path cookie `refreshToken` |
| `COOKIE_SECURE` | `false` | `true` только за HTTPS |
| `SERVER_PORT` | `8080` | Порт приложения |

`.env` в git не коммитится.

## Стек

- Java 21, Spring Boot 4.0.6, Spring Security, JWT (jjwt)
- Spring Data JPA + Liquibase + PostgreSQL 15 (H2 в тестах)
- springdoc-openapi (Swagger), Lombok, Maven Wrapper
- GitHub Actions, JaCoCo, Codecov
- Docker Compose (Postgres; опционально API через profile `full`)

## Структура

```
src/main/java/com/dwtd/myanimelist/
├── features/     # anime, auth, comment, friendlist, genre, tracking, user
├── security/     # JWT filter, SecurityConfiguration
└── exception/    # GlobalExceptionHandler
src/main/resources/db/changelog/   # Liquibase V1–V7
```

## Аутентификация (кратко)

1. `POST /api/v1/auth/sign-up` — регистрация (`ROLE_USER`), access token в ответе.  
2. `POST /api/v1/auth/sign-in` — access token в JSON (`token`), refresh в cookie `refreshToken`.  
3. Защищённые запросы: `Authorization: Bearer <accessToken>`.  
4. `POST /api/v1/auth/refresh` — ротация refresh (из cookie).  
5. `POST /api/v1/auth/logout` — отзыв refresh (нужна аутентификация).

Публично: sign-up / sign-in / refresh, GET аниме и жанров, публичный профиль и anime list, GET комментариев, Swagger.  
`ROLE_ADMIN` — запись в каталог аниме/жанров и удаление чужих комментариев (роль выставляется в БД; отдельного admin API нет).

## Обзор API (`/api/v1`)

| Группа | Базовый путь | Заметки |
|--------|--------------|---------|
| Auth | `/auth` | sign-up, sign-in, refresh, logout |
| Anime | `/anime` | фильтры `search`, `type`, `status`, `genreId`; pageable |
| Genre | `/genre` | CRUD; запись — admin |
| Profile | `/users/me`, `/users/{username}` | PATCH `/me` отдаёт новый token |
| Anime list | `/users/me/animelist`, `/users/{username}/animelist` | статусы `PLAN_TO_WATCH` … `DROPPED` |
| Comments | `/anime/{animeId}/comments` | pageable |
| Friends | `/users/me/friends` | заявки accept/reject/cancel |

Типы аниме: `TV`, `MOVIE`, `OVA`, `SPECIAL`. Статусы: `ANNOUNCED`, `ONGOING`, `FINISHED`.  
Подробные схемы — в Swagger: http://localhost:8080/swagger-ui/index.html

## Полезные команды

```bash
./mvnw spring-boot:run    # запуск API
./mvnw test               # тесты (H2, профиль test)
./mvnw verify             # тесты + JaCoCo (как в CI)
./mvnw package            # jar в target/
docker compose up -d postgres
docker compose --profile full up --build
docker compose down
```

## CI

[`.github/workflows/ci.yml`](.github/workflows/ci.yml): push / PR → `master`, JDK 21, `mvn -B verify`, Codecov.

## Заметки

- Учебный проект: дефолтные секреты только для local. В проде задайте свои env.
- `JWT_SECRET` читается как **Base64** (`Decoders.BASE64` в `JwtService`). Обычная «человеческая» строка с `-` приведёт к 500 при выдаче токена.
- CORS: origin patterns `*`, credentials включены.
- Полный Docker-стек (`profile: full`) удобен для «поднять и посмотреть»; для разработки кода удобнее вариант A.

## Лицензия

В `pom.xml` не указана — уточняйте у автора репозитория.
