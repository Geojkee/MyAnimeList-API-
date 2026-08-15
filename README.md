# MyAnimeList-API

[![Java CI with Maven](https://github.com/Geojkee/MyAnimeList-API-/actions/workflows/ci.yml/badge.svg)](https://github.com/Geojkee/MyAnimeList-API-/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Geojkee/MyAnimeList-API-/branch/master/graph/badge.svg)](https://codecov.io/gh/Geojkee/MyAnimeList-API-)

## 📖 Описание

**MyAnimeList-API** — это REST API для управления аниме-коллекцией, написанное на **Java 21** с использованием **Spring Boot 4**. Проект реализует полноценную JWT-аутентификацию с refresh-токенами, CRUD-операции для каталога аниме, управление жанрами и пользовательскими списками.

Проект создан в учебных целях, но следует лучшим практикам промышленной разработки: модульная архитектура, тестирование, CI/CD, документация Swagger и безопасное хранение секретов через переменные окружения.

## 🚀 Технологический стек

- **Java 21** 
- **Spring Boot 4.0.6**
- **Spring Security** + **JWT**
- **Spring Data JPA** + **Hibernate**
- **PostgreSQL** (основная БД) + **H2** (для тестов)
- **Liquibase** (миграции схемы)
- **Lombok**
- **SpringDoc OpenAPI** (Swagger)
- **Maven**
- **GitHub Actions** (CI)
- **JaCoCo** (покрытие кода)
- **Codecov** (анализ покрытия)

## 🛠️ Требования для локального запуска

- Java 21 (или выше)
- Maven 3.9+
- PostgreSQL 15+ (созданная БД)
- Git

## ⚙️ Конфигурация и запуск

### 1. Клонирование репозитория

```bash
git clone https://github.com/Geojkee/MyAnimeList-API-.git
cd MyAnimeList-API-