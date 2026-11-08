# MyAnimeList-API

[![Java CI with Maven](https://github.com/Geojkee/MyAnimeList-API-/actions/workflows/ci.yml/badge.svg)](https://github.com/Geojkee/MyAnimeList-API-/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Geojkee/MyAnimeList-API-/branch/master/graph/badge.svg)](https://codecov.io/gh/Geojkee/MyAnimeList-API-)

REST API для каталога аниме и пользовательских списков (учебный проект на **Java 21** / **Spring Boot 4**).

JWT-аутентификация с refresh-токеном в HTTP-only cookie, каталог аниме и жанров, профили, anime list, комментарии, друзья. Схема БД — Liquibase, документация — Swagger UI.

## Что нужно установить

| Инструмент | Зачем |
|------------|--------|
| **Java 21+** | запуск приложения |
| **Git** | клонирование репозитория |
| **Docker Desktop** (или Docker Engine + Compose) | PostgreSQL одной командой |
| **Make** (опционально) | короткие команды из `Makefile` (`make up`, `make run`, …) |
| Maven | **не обязателен** — есть `./mvnw` / `mvnw.cmd` |

Проверка версий:

```bash
java -version          # 21+
docker compose version
git --version
make -v                # опционально
```

---

## Запуск (рекомендуемый способ)

БД в Docker, API через Maven — удобно для разработки.

### Через Make (короче)

```bash
git clone https://github.com/Geojkee/MyAnimeList-API-.git
cd MyAnimeList-API-

make up      # .env (если нет) + PostgreSQL
make run     # Spring Boot на :8080
```

Список всех целей: `make` или `make help`.

### Без Make (те же шаги вручную)

```bash
# 1. Клонировать
git clone https://github.com/Geojkee/MyAnimeList-API-.git
cd MyAnimeList-API-

# 2. Конфиг окружения
cp .env.example .env

# 3. Поднять PostgreSQL
docker compose up -d postgres

# 4. Статус БД (опционально)
docker compose ps

# 5. Запустить API
./mvnw spring-boot:run
```

На **Windows** для API: `mvnw.cmd spring-boot:run`.  
`Makefile` рассчитан на Linux/macOS / Git Bash / WSL; в чистом cmd удобнее команды Compose/`mvnw.cmd` вручную.

Если на Linux/macOS `./mvnw` не запускается:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

В логах должно появиться примерно: `Started MyAnimeListApplication`.  
Миграции Liquibase применяются автоматически при старте.  
На пустой базе тот же старт один раз записывает администратора, жанры и 100 аниме — см. [Первый запуск](#первый-запуск-начальные-данные).

### Куда заходить

| Что | URL |
|-----|-----|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

### Остановка

```bash
# остановить API: Ctrl+C в терминале, где крутится make run / spring-boot:run

make down      # остановить контейнеры (данные сохранить)
make down-v    # остановить и удалить volume с данными БД

# то же без Make:
# docker compose down
# docker compose down -v
```

---

## Makefile — краткие команды

| Команда | Что делает |
|---------|------------|
| `make` / `make help` | список целей |
| `make env` | создать `.env` из `.env.example` (если ещё нет) |
| `make up` | поднять PostgreSQL |
| `make down` | остановить контейнеры |
| `make down-v` | остановить и удалить данные БД |
| `make ps` | статус контейнеров |
| `make logs` | логи Postgres |
| `make run` | запуск API (`./mvnw spring-boot:run`) |
| `make full` | Postgres + API целиком в Docker |
| `make full-down` | остановить полный стек |
| `make full-logs` | логи API + Postgres |
| `make test` | тесты |
| `make verify` | тесты + JaCoCo |
| `make package` | собрать jar |
| `make build` | собрать Docker-образ API |
| `make clean` | `mvn clean` |
| `make restart` | перезапуск Postgres |

---

## Другие способы запуска

### Всё в Docker (Postgres + API)

Нужен только Docker (Java на хосте не обязателен):

```bash
git clone https://github.com/Geojkee/MyAnimeList-API-.git
cd MyAnimeList-API-

make full
# или: cp .env.example .env && docker compose --profile full up --build -d
```

API: http://localhost:8080  
Остановка: `make full-down` (или `docker compose --profile full down`).

### Свой PostgreSQL без Docker

1. Создайте БД, например `myanimelist`, и пользователя с доступом к ней.
2. `cp .env.example .env`
3. В `.env` поправьте `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`.
4. `./mvnw spring-boot:run` (или `mvnw.cmd spring-boot:run` на Windows).

### Запуск из готового JAR

```bash
./mvnw -DskipTests package
java -jar target/MyAnimeList-0.0.1-SNAPSHOT.jar
```

Перед этим Postgres должен быть доступен (как в `.env` / defaults).

---

## Проверка, что всё работает

После старта API:

```bash
# каталог аниме (публичный)
curl http://localhost:8080/api/v1/anime

# регистрация
curl -X POST http://localhost:8080/api/v1/auth/sign-up \
  -H "Content-Type: application/json" \
  -d '{"username":"demo_user","email":"demo@example.com","password":"password1"}'

# вход (access token в поле token; refresh — в cookie)
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/sign-in \
  -H "Content-Type: application/json" \
  -d '{"username":"demo_user","password":"password1"}'

# свой профиль (подставьте token из ответа)
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer ВАШ_ACCESS_TOKEN"
```

Дальше удобнее кликать эндпоинты в Swagger: http://localhost:8080/swagger-ui/index.html  
В Swagger для защищённых методов: Authorize → `Bearer <token>`.

---

## Переменные окружения

Шаблон: [`.env.example`](.env.example). Скопируйте в `.env` (файл в `.gitignore`, в git не попадёт).

Spring читает `.env` как properties (`spring.config.import: optional:file:.env[.properties]`).  
В `application.yaml` есть **локальные defaults**: без `.env` тоже стартует, если Postgres на `localhost:5432`, БД `myanimelist`, user/password `postgres`.

| Переменная | По умолчанию (local) | Назначение |
|------------|----------------------|------------|
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/myanimelist` | JDBC URL |
| `POSTGRES_USER` | `postgres` | Пользователь БД / Liquibase |
| `POSTGRES_PASSWORD` | `postgres` | Пароль БД |
| `POSTGRES_DB` | `myanimelist` | Имя БД (Compose) |
| `POSTGRES_PORT` | `5432` | Порт Postgres (Compose) |
| `JWT_SECRET` | Base64-ключ в `.env.example` | Подпись JWT — **только Base64** |
| `JWT_EXPIRATION` | `86400000` | TTL access token, мс (~1 сутки) |
| `REFRESH_TOKEN_DURATION` | `2592000000` | TTL refresh token в БД, мс |
| `REFRESH_TOKEN_MAX_AGE_SECONDS` | `2592000` | Max-Age cookie |
| `COOKIE_PATH` | `/api/v1/auth` | Path cookie `refreshToken` |
| `COOKIE_SECURE` | `false` | `true` только за HTTPS |
| `SERVER_PORT` | `8080` | Порт приложения |
| `SEED_ADMIN_USERNAME` | `admin` | Логин администратора для первого запуска |
| `SEED_ADMIN_EMAIL` | `admin@myanimelist.local` | Почта этого администратора |
| `SEED_ADMIN_PASSWORD` | `local-admin` | Пароль администратора. В проде замените **до** первого запуска |

Сгенерировать новый `JWT_SECRET`:

```bash
python3 -c "import os,base64; print(base64.b64encode(os.urandom(32)).decode())"
```

---

## Первый запуск: начальные данные

`DatabaseSeedRunner` срабатывает один раз, когда база ещё пустая (профиль `test` его не загружает):

- один пользователь `ROLE_ADMIN`
- фиксированный набор жанров (Action, Adventure, Comedy, Drama, Fantasy, Horror, Mecha, Music, Mystery, Romance, Sci-Fi, Slice of Life, Sports, Supernatural, Thriller)
- 100 разных аниме: romaji- и english-название, тип (`TV`, `MOVIE`, `OVA`, `SPECIAL`), статус `FINISHED`, число эпизодов, жанры, дата выхода (`release_date`) и короткий синопсис. В каталоге известен год премьеры, поэтому в дату пишется 1 января этого года

Повторный старт строки не дублирует. Сид пропускается, если уже есть пользователь с именем или почтой администратора **или** в `genres` / `anime` уже есть хотя бы одна строка. Пароль записывается только в этот момент: новый `SEED_ADMIN_PASSWORD` подхватывается на пустой базе.

Локальный вход, пока переменные не переопределены:

| Поле | Значение |
|------|----------|
| username | `admin` |
| password | `local-admin` |
| email | `admin@myanimelist.local` |

```bash
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/sign-in \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"local-admin"}'
```

Пароль читается из `SEED_ADMIN_PASSWORD` (в коде его нет). Минимум 8 символов и без пробелов — иначе вход через API не пройдёт проверку. В проде задайте свой пароль в окружении до первого старта.

## Тесты

Тесты используют in-memory **H2** (профиль `test`), отдельный Postgres для них не нужен:

```bash
./mvnw test          # или: mvnw.cmd test
./mvnw verify        # тесты + отчёт JaCoCo (как в CI)
```

---

## Типичные проблемы

| Симптом | Что сделать |
|---------|-------------|
| `Connection to localhost:5432 refused` | Не запущен Postgres: `docker compose up -d postgres` |
| `database "myanimelist" does not exist` | Compose создаёт БД сам; при своём Postgres создайте БД вручную |
| `Illegal base64 character` / 500 на sign-up | `JWT_SECRET` не Base64 — возьмите значение из `.env.example` или сгенерируйте командой выше |
| Порт `8080` занят | Смените `SERVER_PORT` в `.env` или остановите другой процесс |
| `./mvnw: Permission denied` | `chmod +x mvnw` |
| Docker не найден | Установите Docker Desktop / Engine или используйте вариант со своим Postgres |

---

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
├── seed/         # DatabaseSeedRunner — первый старт
├── security/     # JWT filter, SecurityConfiguration
└── exception/    # GlobalExceptionHandler
src/main/resources/db/changelog/   # Liquibase V1–V8
.env.example                       # шаблон окружения
docker-compose.yaml                # postgres (+ app с --profile full)
Dockerfile                         # образ API
```

## Аутентификация

1. `POST /api/v1/auth/sign-up` — регистрация (`ROLE_USER`), access token в ответе.
2. `POST /api/v1/auth/sign-in` — access token в JSON (`token`), refresh в cookie `refreshToken`.
3. Защищённые запросы: заголовок `Authorization: Bearer <accessToken>`.
4. `POST /api/v1/auth/refresh` — ротация refresh (из cookie).
5. `POST /api/v1/auth/logout` — отзыв refresh (нужна аутентификация).

Публично без токена: sign-up / sign-in / refresh, GET аниме и жанров, публичный профиль и anime list, GET комментариев, Swagger.  
`ROLE_ADMIN` — запись в каталог аниме/жанров и удаление чужих комментариев (роль выставляется в БД; отдельного admin API нет).  
При первом старте создаётся локальный администратор `admin` / `local-admin` — см. [Первый запуск](#первый-запуск-начальные-данные).

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

Типы аниме: `TV`, `MOVIE`, `OVA`, `SPECIAL`.  
Статусы аниме: `ANNOUNCED`, `ONGOING`, `FINISHED`.  
Статусы списка: `PLAN_TO_WATCH`, `WATCHING`, `COMPLETED`, `ON_HOLD`, `DROPPED`.

## Полезные команды

```bash
make up && make run                    # типичный dev-цикл
make full                              # весь стек в Docker
make test                              # тесты
make package && java -jar target/MyAnimeList-0.0.1-SNAPSHOT.jar

# эквиваленты без Make:
./mvnw spring-boot:run
./mvnw test
./mvnw verify
./mvnw -DskipTests package
docker compose up -d postgres
docker compose --profile full up --build -d
docker compose down
docker compose down -v
```

## CI

[`.github/workflows/ci.yml`](.github/workflows/ci.yml): push / PR → `master`, JDK 21, `mvn -B verify`, Codecov.

## Заметки

- Учебный проект: дефолтные секреты только для local. В проде задайте свои env.
- `JWT_SECRET` читается как **Base64** (`Decoders.BASE64` в `JwtService`). Обычная строка с `-` даст 500 при выдаче токена.
- CORS: origin patterns `*`, credentials включены.
- Для «просто посмотреть» удобен Docker profile `full`; для разработки кода — Postgres в Compose + `./mvnw spring-boot:run`.

## Лицензия

В `pom.xml` не указана — уточняйте у автора репозитория.
