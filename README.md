# MyAnimeList-API

[![Java CI with Maven](https://github.com/Geojkee/MyAnimeList-API-/actions/workflows/ci.yml/badge.svg)](https://github.com/Geojkee/MyAnimeList-API-/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Geojkee/MyAnimeList-API-/branch/master/graph/badge.svg)](https://codecov.io/gh/Geojkee/MyAnimeList-API-)

REST API для каталога аниме и пользовательских списков в стиле MyAnimeList. Учебный backend на **Java 21** и **Spring Boot 4** с JWT-аутентификацией, refresh-токенами в HTTP-only cookie, ролями `ROLE_USER` / `ROLE_ADMIN`, Liquibase-миграциями и Swagger UI.

## Возможности

- Регистрация, вход, обновление access token и logout
- Каталог аниме: пагинация, поиск и фильтры (`search`, `type`, `status`, `genreId`)
- CRUD жанров (запись — только `ROLE_ADMIN`)
- Личный anime list: статус просмотра, оценка, число эпизодов
- Публичные и приватные профили пользователей
- Комментарии к аниме
- Список друзей и заявки в друзья
- Единый формат ошибок через `GlobalExceptionHandler`
- OpenAPI / Swagger UI
- Интеграционные тесты на H2, CI через GitHub Actions + JaCoCo / Codecov

## Стек

| Компонент | Технология |
|-----------|------------|
| Язык / runtime | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Security | Spring Security, JWT (jjwt 0.12.6), BCrypt |
| Persistence | Spring Data JPA, Hibernate, Liquibase |
| БД | PostgreSQL 15 (runtime), H2 (tests) |
| API docs | springdoc-openapi 2.7.0 |
| Build | Maven Wrapper |
| CI | GitHub Actions, JaCoCo, Codecov |
| Local DB | Docker Compose (`postgres:15`) |

## Структура проекта

```
src/main/java/com/dwtd/myanimelist/
├── MyAnimeListApplication.java
├── exception/          # CustomException, ErrorResponse, GlobalExceptionHandler
├── features/
│   ├── anime/          # каталог аниме
│   ├── auth/           # регистрация, login, refresh, logout
│   ├── comment/        # комментарии к аниме
│   ├── friendlist/     # друзья и заявки
│   ├── genre/          # жанры
│   ├── tracking/       # пользовательский anime list
│   └── user/           # профили
└── security/           # SecurityConfiguration, JwtService, JwtAuthenticationFilter

src/main/resources/
├── application.yaml
├── application-test.yaml
└── db/changelog/       # Liquibase (V1–V7)
```

## Требования

- Java 21+
- Maven 3.9+ (или `./mvnw`)
- PostgreSQL 15+ **или** Docker / Docker Compose
- Git

## Быстрый старт

### 1. Клонирование

```bash
git clone https://github.com/Geojkee/MyAnimeList-API-.git
cd MyAnimeList-API-
```

### 2. PostgreSQL

Через Docker Compose (из корня репозитория):

```bash
docker compose up -d
```

Сервис поднимает контейнер `mal_postgres` на порту `5432` (переопределяется через `POSTGRES_PORT`). По умолчанию: БД / пользователь / пароль — `postgres`.

Либо используйте уже установленный PostgreSQL и укажите его параметры в `.env`.

### 3. Переменные окружения

Приложение читает `.env` как properties-файл (`spring.config.import: optional:file:.env[.properties]`). Файл `.env` в `.gitignore` — создайте его локально.

Пример `.env`:

```properties
POSTGRES_URL=jdbc:postgresql://localhost:5432/postgres
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

JWT_SECRET=change-me-to-a-long-random-secret-at-least-32-chars
JWT_EXPIRATION=86400000

REFRESH_TOKEN_DURATION=2592000000
REFRESH_TOKEN_MAX_AGE_SECONDS=2592000
COOKIE_PATH=/api/v1/auth
COOKIE_SECURE=false
```

| Переменная | Назначение |
|------------|------------|
| `POSTGRES_URL` | JDBC URL PostgreSQL |
| `POSTGRES_USER` | Пользователь БД (также для Liquibase) |
| `POSTGRES_PASSWORD` | Пароль БД |
| `JWT_SECRET` | Секрет подписи JWT (достаточно длинная строка) |
| `JWT_EXPIRATION` | TTL access token, мс |
| `REFRESH_TOKEN_DURATION` | TTL refresh token в БД, мс |
| `REFRESH_TOKEN_MAX_AGE_SECONDS` | `Max-Age` cookie `refreshToken` |
| `COOKIE_PATH` | `Path` cookie (обычно `/api/v1/auth`) |
| `COOKIE_SECURE` | Флаг `Secure` у cookie (`true` для HTTPS) |

Для Docker Compose дополнительно: `POSTGRES_DB`, `POSTGRES_PORT` (см. `docker-compose.yaml`).

Схема БД применяется Liquibase при старте (`ddl-auto: none`).

### 4. Запуск приложения

```bash
./mvnw spring-boot:run
```

По умолчанию сервер слушает порт **8080**. Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html), OpenAPI JSON: `/v3/api-docs`.

## Аутентификация

1. `POST /api/v1/auth/sign-up` — регистрация (`ROLE_USER`), в ответе access token.
2. `POST /api/v1/auth/sign-in` — вход: access token в JSON (`token`), refresh token в HTTP-only cookie `refreshToken` (`SameSite=Strict`).
3. Защищённые запросы: заголовок `Authorization: Bearer <accessToken>`.
4. `POST /api/v1/auth/refresh` — ротация refresh token (из cookie) и новый access token.
5. `POST /api/v1/auth/logout` — отзыв refresh token и очистка cookie (нужна аутентификация).

Публичные без токена: `sign-up`, `sign-in`, `refresh`, GET каталога аниме/жанров, публичный профиль, публичный anime list, GET комментариев, Swagger.

Роли:

- `ROLE_USER` — обычный пользователь
- `ROLE_ADMIN` — создание/изменение/удаление аниме и жанров; удаление чужих комментариев

Новые пользователи получают `ROLE_USER`. Назначение `ROLE_ADMIN` в API не предусмотрено — роль выставляется в БД при необходимости.

## Обзор API

Базовый префикс: `/api/v1`.

### Auth — `/api/v1/auth`

| Метод | Путь | Доступ | Описание |
|-------|------|--------|----------|
| POST | `/sign-up` | public | Регистрация |
| POST | `/sign-in` | public | Вход |
| POST | `/refresh` | public (cookie) | Обновление токенов |
| POST | `/logout` | authenticated | Выход |

### Anime — `/api/v1/anime`

| Метод | Путь | Доступ | Описание |
|-------|------|--------|----------|
| GET | `/` | public | Список (pageable; query: `search`, `type`, `status`, `genreId`) |
| GET | `/{id}` | public | Аниме по id |
| POST | `/` | `ROLE_ADMIN` | Создать |
| PUT | `/{id}` | `ROLE_ADMIN` | Обновить |
| DELETE | `/{id}` | `ROLE_ADMIN` | Удалить |

Типы аниме: `TV`, `MOVIE`, `OVA`, `SPECIAL`. Статусы: `ANNOUNCED`, `ONGOING`, `FINISHED`.

### Genre — `/api/v1/genre`

| Метод | Путь | Доступ | Описание |
|-------|------|--------|----------|
| GET | `/` | public | Все жанры |
| GET | `/{id}` | public | Жанр по id |
| POST | `/` | `ROLE_ADMIN` | Создать |
| PUT | `/{id}` | `ROLE_ADMIN` | Обновить |
| DELETE | `/{id}` | `ROLE_ADMIN` | Удалить |

### User profile — `/api/v1/users`

| Метод | Путь | Доступ | Описание |
|-------|------|--------|----------|
| GET | `/me` | authenticated | Свой профиль (включая email) |
| PATCH | `/me` | authenticated | Обновить username/email (новый access token в ответе) |
| GET | `/{username}` | public | Публичный профиль |

### User anime list — `/api/v1/users`

| Метод | Путь | Доступ | Описание |
|-------|------|--------|----------|
| POST | `/me/animelist` | authenticated | Добавить аниме (`animeId`) |
| GET | `/{username}/animelist` | public | Список пользователя (опционально `?status=`) |
| PATCH | `/me/animelist/{animeId}` | authenticated | Обновить `status` / `score` / `watchedEpisodes` |
| DELETE | `/me/animelist/{animeId}` | authenticated | Удалить из списка |

Статусы списка: `PLAN_TO_WATCH`, `WATCHING`, `COMPLETED`, `ON_HOLD`, `DROPPED`. Оценка: 1–10.

### Comments — `/api/v1/anime/{animeId}/comments`

| Метод | Путь | Доступ | Описание |
|-------|------|--------|----------|
| GET | `/` | public | Комментарии (pageable) |
| POST | `/` | authenticated | Создать (`text`, до 500 символов) |
| DELETE | `/{commentId}` | authenticated | Удалить (автор или `ROLE_ADMIN`) |

### Friends — `/api/v1/users/me/friends`

Все эндпоинты требуют аутентификации.

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/{friendId}` | Отправить заявку |
| GET | `/` | Список друзей |
| GET | `/requests/incoming` | Входящие заявки |
| GET | `/requests/outgoing` | Исходящие заявки |
| PATCH | `/requests/{requestId}/accept` | Принять |
| PATCH | `/requests/{requestId}/reject` | Отклонить |
| DELETE | `/requests/{requestId}` | Отменить исходящую |
| DELETE | `/{friendId}` | Удалить из друзей |

Статусы заявок в модели: `PENDING`, `ACCEPTED`, `REJECTED`, `BLOCKED`.

## Скрипты и команды

```bash
./mvnw spring-boot:run          # запуск приложения
./mvnw test                     # тесты
./mvnw verify                   # тесты + отчёт JaCoCo (как в CI)
./mvnw package                  # сборка jar
docker compose up -d            # PostgreSQL
docker compose down             # остановить PostgreSQL
```

Профиль `test` (`application-test.yaml`): in-memory H2, Liquibase выключен, `ddl-auto: create-drop`. Интеграционные тесты наследуют `BaseIntegrationTest` (`@SpringBootTest` + MockMvc).

## CI

Workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml):

- триггеры: push на любую ветку, pull request в `master`
- JDK 21 (Temurin), `mvn -B verify`
- артефакт JaCoCo, загрузка покрытия в Codecov (`CODECOV_TOKEN`)

## Деплой

В репозитории нет Dockerfile приложения и готовой конфигурации деплоя. Для локальной разработки используется только `docker-compose.yaml` с PostgreSQL. Для окружения нужно самостоятельно собрать jar (`./mvnw package`) и задать переменные из раздела выше.

## Заметки

- Учебный проект; секреты только через env / `.env`, не коммитьте их в git.
- CORS разрешает origin patterns `*`, credentials включены.
- Подробные схемы запросов/ответов — в Swagger UI после запуска.
- Вкладки и детали эндпоинтов удобнее смотреть в `/swagger-ui.html`.

## Лицензия

Лицензия в `pom.xml` не указана. Уточняйте у автора репозитория при необходимости.
