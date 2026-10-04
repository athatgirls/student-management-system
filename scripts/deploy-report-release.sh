#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
release=${1:?Usage: deploy.sh RELEASE REVISION PREVIOUS_RELEASE}
revision=${2:?Expected full Git revision}
previous=${3:?Expected previous application tag}
[[ "$release" =~ ^[a-f0-9]{7,40}$ && "$revision" =~ ^[a-f0-9]{40}$ && "$previous" =~ ^[a-f0-9]{7,40}$ ]] || exit 1
[[ "$revision" == "$release"* ]] || exit 1
deployment=/home/hbutjsj/mis-deploy-fdd0a6d/mis-offline-fdd0a6d
bundle=$(cd -- "$(dirname -- "$0")" && pwd -P)
[[ $(id -u) == 0 && -f "$deployment/.env" ]] || { echo 'Root and existing deployment required'; exit 1; }
cd "$bundle"
sha256sum -c SHA256SUMS
expected_page_hash=$(tr -d '\r\n' < frontend-index.sha256)
[[ "$expected_page_hash" =~ ^[a-f0-9]{64}$ ]] || exit 1
mapfile -t backends < <(docker ps -q --filter label=com.docker.compose.project=mis --filter label=com.docker.compose.service=backend)
mapfile -t frontends < <(docker ps -q --filter label=com.docker.compose.project=mis --filter label=com.docker.compose.service=frontend)
[[ ${#backends[@]} == 1 && ${#frontends[@]} == 1 ]] || { echo 'Expected one running backend and frontend'; exit 1; }
backend=${backends[0]}
frontend=${frontends[0]}
[[ $(docker inspect -f '{{.Config.Image}}' "$backend") == mis-backend:$previous ]]
[[ $(docker inspect -f '{{.Config.Image}}' "$frontend") == mis-frontend:$previous ]]
# The frontend has the latest overlay, which preserves the current backend too.
config_list=$(docker inspect -f '{{index .Config.Labels "com.docker.compose.project.config_files"}}' "$frontend")
IFS=, read -r -a configs <<< "$config_list"
old=(docker compose --project-directory "$deployment" --env-file "$deployment/.env" -p mis)
for config in "${configs[@]}"; do
  [[ "$config" == "$deployment/"* && -f "$config" ]] || { echo 'Unexpected active Compose path'; exit 1; }
  old+=(-f "$config")
done
[[ ${#configs[@]} -gt 0 ]] || exit 1
"${old[@]}" config --quiet
docker load -i images.tar
for service in backend frontend; do
  [[ $(docker image inspect -f '{{index .Config.Labels "org.opencontainers.image.revision"}}' mis-$service:$release) == "$revision" ]]
done
backup="$deployment/backups/report-${release}-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$backup/config" "$backup/uploads" "$deployment/releases/$release"
cp -p "$deployment/.env" "$backup/config/.env"
for i in "${!configs[@]}"; do cp -p "${configs[$i]}" "$backup/config/compose-$i.yaml"; done
docker inspect -f '{{.Config.Image}} {{.Image}}' "$backend" "$frontend" > "$backup/previous-images.txt"
printf '%q ' "${old[@]}" > "$backup/compose-command.txt"
printf '\n' >> "$backup/compose-command.txt"
cp compose.release.yaml "$deployment/releases/$release/compose.release.yaml"
new=("${old[@]}" -f "$deployment/releases/$release/compose.release.yaml")
"${new[@]}" config --quiet
rollback() {
  code=$?
  trap - ERR
  echo "Upgrade failed; restoring previous application images. Backup: $backup"
  "${old[@]}" up -d --no-deps --pull never --wait --wait-timeout 240 backend frontend || true
  exit "$code"
}
trap rollback ERR
"${old[@]}" stop backend
# No database migrations; pause the app for a consistent backup before replacement.
"${old[@]}" exec -T mongo sh -c 'exec mongodump --username "$MONGO_INITDB_ROOT_USERNAME" --password "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin --db disciplinary_construction --archive --gzip' > "$backup/mongo.archive.gz" 2> "$backup/mongodump.log"
test -s "$backup/mongo.archive.gz"
gzip -t "$backup/mongo.archive.gz"
docker cp "$backend:/app/uploads/." "$backup/uploads/"
echo "BACKUP_OK $backup"
"${new[@]}" up -d --no-deps --pull never --wait --wait-timeout 240 backend frontend
"${new[@]}" exec -T frontend nginx -t
"${new[@]}" exec -T backend curl --fail --silent http://127.0.0.1:1010/SCSE@hbut/actuator/health
page_hash=$(curl --fail --silent --max-time 15 http://127.0.0.1:8080/ | sha256sum | cut -d' ' -f1)
[[ "$page_hash" == "$expected_page_hash" ]]
[[ $(curl --silent --max-time 15 -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/SCSE@hbut/uploads/nonexistent.pdf) == 401 ]]
[[ $(curl --silent --max-time 15 -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{}' http://127.0.0.1:8080/SCSE@hbut/msi/student/batch-update-major) == 401 ]]
"${new[@]}" ps
printf '%q ' "${new[@]}" > "$deployment/releases/$release/compose-command.txt"
printf '\n' >> "$deployment/releases/$release/compose-command.txt"
trap - ERR
echo "DEPLOYMENT_OK $release"
echo "Backup: $backup"
echo 'Existing database and uploads preserved; no production student records changed by verification.'
