#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js no está instalado. Instala Node >= 24.15 y vuelve a intentar."
  exit 1
fi

exec node scripts/start-local.mjs "$@"
