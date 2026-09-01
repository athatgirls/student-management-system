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
mkdir -p "$backup_dir"

docker compose exec -T mongo mongodump \
  --username "$MONGO_ROOT_USERNAME" \
  --password "$MONGO_ROOT_PASSWORD" \
  --authenticationDatabase admin \
  --db disciplinary_construction \
  --archive --gzip > "$backup_dir/mongo_$timestamp.archive.gz"

docker compose exec -T backend tar -czf - -C /app uploads \
  > "$backup_dir/uploads_$timestamp.tar.gz"

echo "备份完成：$backup_dir"
