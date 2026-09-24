#!/usr/bin/env bash
# Detiene los servidores locales iniciados por iniciar-taller.sh.
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOGS="$RAIZ/logs"

detener() {
  local nombre="$1"
  local archivo="$LOGS/$nombre.pid"

  if [[ ! -f "$archivo" ]]; then
    echo "$nombre: no hay PID registrado."
    return
  fi

  local pid
  pid="$(<"$archivo")"
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid"
    echo "$nombre detenido (PID $pid)."
  else
    echo "$nombre ya no estaba en ejecución."
  fi
  rm -f "$archivo"
}

detener backend
detener frontend

echo "MySQL permanece activo. Para detenerlo también:"
echo "docker compose -f /home/venmve/mis-contenedores/docker-compose.yml stop mysql"
