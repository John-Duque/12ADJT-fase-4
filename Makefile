# Atalhos do dia a dia. "make" sem alvo lista os comandos.
.DEFAULT_GOAL := help
.PHONY: help verify install dev-ingestion dev-notifier dev-report docker-build docker-test up down ps logs smoke clean

FUNCTIONS := feedback-ingestion critical-notifier weekly-report

help: ## Lista os alvos disponíveis
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

verify: ## Build, testes e cobertura de todos os módulos
	mvn -B verify

install: ## Instala os módulos no Maven local, sem testes
	mvn -B install -DskipTests

dev-ingestion: install ## feedback-ingestion em modo dev (porta 8081)
	cd functions/feedback-ingestion && mvn quarkus:dev

dev-notifier: install ## critical-notifier em modo dev (porta 8082)
	cd functions/critical-notifier && mvn quarkus:dev

dev-report: install ## weekly-report em modo dev (porta 8083)
	cd functions/weekly-report && mvn quarkus:dev

docker-build: ## Gera as imagens das três funções
	docker compose build

docker-test: ## Roda os testes de cada função dentro do Docker
	@for module in $(FUNCTIONS); do \
	    echo "==> $$module"; \
	    docker build --target test --build-arg MODULE=$$module . || exit 1; \
	done

up: ## Sobe as três funções em segundo plano
	docker compose up --build -d

down: ## Para e remove os containers
	docker compose down

ps: ## Estado e health check dos containers
	docker compose ps

logs: ## Acompanha os logs das funções
	docker compose logs -f

smoke: ## Chama os 3 endpoints das funções em execução
	@echo "==> POST /avaliacao (8081)"
	@curl -fsS -X POST localhost:8081/avaliacao -H 'Content-Type: application/json' \
	    -d '{"descricao":"Aula sem áudio","nota":2}' && echo
	@echo "==> POST /notify-critical-feedback (8082)"
	@curl -fsS -X POST localhost:8082/notify-critical-feedback -H 'Content-Type: application/json' \
	    -d '{"id":"8f14e45f-ceea-467a-9b8e-6c2a9b8f1a11","description":"Aula sem áudio","rating":1,"urgency":"HIGH","createdAt":"2026-10-13T12:00:00Z"}' && echo
	@echo "==> POST /generate-weekly-report (8083)"
	@curl -fsS -X POST localhost:8083/generate-weekly-report && echo

clean: ## Apaga os artefatos do Maven e as imagens locais do compose
	mvn -B clean
	docker compose down --rmi local
