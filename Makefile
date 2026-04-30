include .env
export

.PHONY: up down db logs psql run-backend run-frontend dev

up:
	docker compose up --build

down:
	docker compose down

db:
	docker compose up db -d

logs:
	docker compose logs -f

psql:
	docker compose exec db psql -U roast -d spotifyroast

run-backend:
	cd backend && ./gradlew bootRun

run-frontend:
	cd frontend && npm run dev

dev: db
	$(MAKE) -j2 run-backend run-frontend
