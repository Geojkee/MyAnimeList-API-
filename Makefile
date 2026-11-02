# Короткие команды для локальной разработки и Docker.
# Использование: make <цель>   |   make help

.DEFAULT_GOAL := help

COMPOSE ?= docker compose
MVNW    ?= ./mvnw

.PHONY: help env up down down-v ps logs full full-down full-logs restart \
	run test verify package clean build

help: ## Показать список команд
	@echo "MyAnimeList-API — make-цели:"
	@grep -E '^[a-zA-Z0-9_-]+:.*?##' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

env: ## Создать .env из .env.example (если ещё нет)
	@if [ -f .env ]; then \
		echo ".env уже есть — пропускаю"; \
	else \
		cp .env.example .env; \
		echo "Создан .env из .env.example"; \
	fi

up: env ## Поднять PostgreSQL (Docker)
	$(COMPOSE) up -d postgres
	@echo "Postgres: localhost:$${POSTGRES_PORT:-5432}, БД myanimelist"

down: ## Остановить контейнеры (данные сохранить)
	$(COMPOSE) --profile full down

down-v: ## Остановить контейнеры и удалить volume с данными БД
	$(COMPOSE) --profile full down -v

ps: ## Статус контейнеров
	$(COMPOSE) ps

logs: ## Логи PostgreSQL (follow)
	$(COMPOSE) logs -f postgres

full: env ## Поднять весь стек: Postgres + API (Docker)
	$(COMPOSE) --profile full up --build -d
	@echo "API: http://localhost:$${SERVER_PORT:-8080}"
	@echo "Swagger: http://localhost:$${SERVER_PORT:-8080}/swagger-ui/index.html"

full-down: ## Остановить полный стек
	$(COMPOSE) --profile full down

full-logs: ## Логи API + Postgres (follow)
	$(COMPOSE) --profile full logs -f

restart: down up ## Перезапустить Postgres

run: env ## Запустить API локально (нужен Postgres: make up)
	$(MVNW) spring-boot:run

test: ## Прогнать тесты (H2, без Docker)
	$(MVNW) test

verify: ## Тесты + JaCoCo (как в CI)
	$(MVNW) verify

package: ## Собрать jar (без тестов)
	$(MVNW) -DskipTests package

clean: ## Очистить target/
	$(MVNW) clean

build: ## Собрать Docker-образ API
	$(COMPOSE) --profile full build app
