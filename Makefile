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
