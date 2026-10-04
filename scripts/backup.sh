#!/usr/bin/env bash
# Quiesced offline backup using the active release overlay chain.
set -Eeuo pipefail
umask 077
deployment=$(cd -- "${1:-$(dirname -- "$0")/..}" && pwd -P)
export RUSTFS_ENV_FILE="$deployment/rustfs.env"
[[ $(id -u) == 0 && -f "$deployment/.env" ]] || { echo 'Root and deployment directory required'; exit 1; }
mapfile -t frontend_ids < <(docker ps -q --filter label=com.docker.compose.project=mis --filter label=com.docker.compose.service=frontend)
[[ ${#frontend_ids[@]} == 1 ]]
config_list=$(docker inspect -f '{{index .Config.Labels "com.docker.compose.project.config_files"}}' "${frontend_ids[0]}")
IFS=, read -r -a configs <<< "$config_list"
compose=(docker compose --project-directory "$deployment" --env-file "$deployment/.env" -p mis)
for config in "${configs[@]}"; do
  [[ "$config" == "$deployment/"* && -f "$config" ]]
  compose+=(-f "$config")
done
"${compose[@]}" config --quiet
backup="$deployment/backups/manual-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$backup/config" "$backup/uploads"
cp -p "$deployment/.env" "$backup/config/.env"
[[ ! -f "$RUSTFS_ENV_FILE" ]] || cp -p "$RUSTFS_ENV_FILE" "$backup/config/rustfs.env"
for i in "${!configs[@]}"; do cp -p "${configs[$i]}" "$backup/config/compose-$i.yaml"; done
backend=$("${compose[@]}" ps -q backend)
[[ -n "$backend" ]]
storage=$("${compose[@]}" ps -q rustfs)
resume() {
  [[ -z "$storage" ]] || docker start "$storage" >/dev/null
  docker start "$backend" >/dev/null
}
trap resume EXIT
"${compose[@]}" stop backend
"${compose[@]}" exec -T mongo sh -c 'exec mongodump --username "$MONGO_INITDB_ROOT_USERNAME" --password "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin --db disciplinary_construction --archive --gzip' > "$backup/mongo.archive.gz" 2> "$backup/mongodump.log"
gzip -t "$backup/mongo.archive.gz"
docker cp "$backend:/app/uploads/." "$backup/uploads/"
if [[ -n "$storage" ]]; then
  storage_volume=$(docker inspect -f '{{range .Mounts}}{{if eq .Destination "/data"}}{{.Name}}{{end}}{{end}}' "$storage")
  storage_image=$(docker inspect -f '{{.Config.Image}}' "$storage")
  [[ -n "$storage_volume" ]]
  docker stop "$storage" >/dev/null
  docker run --rm --pull never --entrypoint sh -v "$storage_volume:/data:ro" "$storage_image" -c 'tar -czf - -C /data .' > "$backup/rustfs.tar.gz"
  gzip -t "$backup/rustfs.tar.gz"
fi
echo "BACKUP_OK $backup"
