-include .env
export

.PHONY: up down start stop db frontend build-frontend logs psql run-backend run-frontend dev

up:
	docker compose up --build

down:
	docker compose down

start: up

stop: down

db:
	docker compose up db -d

frontend:
	docker compose up --build frontend

build-frontend:
	docker compose build frontend

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

build-combined:
	docker build -t spotify-roast-combined .

run-combined:
	docker run --rm -p 8080:8080 --env-file .env -e PORT=8080 spotify-roast-combined
