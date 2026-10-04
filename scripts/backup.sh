#!/usr/bin/env sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_dir"

if [ ! -f .env ]; then
  echo "未找到 .env，请在项目根目录配置生产环境变量。" >&2
  exit 1
fi

set -a
. ./.env
set +a

backup_dir="$project_dir/backups"
timestamp=$(date +%Y%m%d_%H%M%S)
compose_project=${COMPOSE_PROJECT_NAME:-mis}
rustfs_volume="${compose_project}_rustfs_data"
mkdir -p "$backup_dir"

docker compose exec -T mongo mongodump \
  --username "$MONGO_ROOT_USERNAME" \
  --password "$MONGO_ROOT_PASSWORD" \
  --authenticationDatabase admin \
  --db disciplinary_construction \
  --archive --gzip > "$backup_dir/mongo_$timestamp.archive.gz"

docker compose exec -T backend tar -czf - -C /app uploads \
  > "$backup_dir/uploads_$timestamp.tar.gz"

docker run --rm -v "$rustfs_volume":/data:ro -v "$backup_dir":/backup alpine:3.20 \
  tar -czf "/backup/rustfs_$timestamp.tar.gz" -C /data .

echo "备份完成：$backup_dir（RustFS 卷：$rustfs_volume）"
