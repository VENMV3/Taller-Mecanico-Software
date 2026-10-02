#!/usr/bin/env bash
# Arranque local de TallerCore: MySQL + API Spring Boot + portal React.
set -euo pipefail
# Cada servicio en segundo plano obtiene su propio grupo; detener-taller.sh lo finaliza completo.
set -m

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE="/home/venmve/mis-contenedores/docker-compose.yml"
MAVEN="/home/venmve/.local/share/JetBrains/Toolbox/apps/intellij-idea/plugins/maven-plugin/lib/maven3/bin/mvn"
LOGS="$RAIZ/logs"
PID_BACKEND="$LOGS/backend.pid"
PID_FRONTEND="$LOGS/frontend.pid"

mkdir -p "$LOGS" /tmp/taller-m2

en_ejecucion() {
  local archivo="$1"
  [[ -f "$archivo" ]] && kill -0 -- -"$(<"$archivo")" 2>/dev/null
}

# Guarda el identificador de grupo del proceso recién iniciado para detener hijos también.
# Parámetros: PID del proceso lanzador y archivo destino. Retorno: escribe el PGID.
guardar_grupo() {
  local pid="$1" archivo="$2"
  ps -o pgid= -p "$pid" | tr -d ' ' > "$archivo"
}

if [[ ! -f "$COMPOSE" ]]; then
  echo "No encontré Docker Compose en: $COMPOSE"
  exit 1
fi

if ! command -v docker >/dev/null; then
  echo "Docker no está instalado o no está disponible."
  exit 1
fi

echo "[1/3] Iniciando MySQL..."
docker compose -f "$COMPOSE" up -d mysql

echo "[2/3] Iniciando API Spring Boot..."
if ! en_ejecucion "$PID_BACKEND"; then
  if [[ -x "$MAVEN" ]]; then
    (cd "$RAIZ/backend"; nohup "$MAVEN" -q -Dmaven.repo.local=/tmp/taller-m2 spring-boot:run >"$LOGS/backend.log" 2>&1 < /dev/null & guardar_grupo "$!" "$PID_BACKEND")
  elif command -v mvn >/dev/null; then
    (cd "$RAIZ/backend"; nohup mvn -q -Dmaven.repo.local=/tmp/taller-m2 spring-boot:run >"$LOGS/backend.log" 2>&1 < /dev/null & guardar_grupo "$!" "$PID_BACKEND")
  else
    echo "No encontré Maven. Instálalo o ajusta la variable MAVEN en este script."
    exit 1
  fi
fi

echo "[3/3] Iniciando portal web..."
if [[ ! -d "$RAIZ/frontend/node_modules" ]]; then
  (cd "$RAIZ/frontend" && npm install)
fi
if ! en_ejecucion "$PID_FRONTEND"; then
  (cd "$RAIZ/frontend"; nohup npm run dev >"$LOGS/frontend.log" 2>&1 < /dev/null & guardar_grupo "$!" "$PID_FRONTEND")
fi

echo
echo "TallerCore se está iniciando."
echo "Portal:   http://localhost:5173"
echo "API:      http://localhost:8080"
echo "Registros: $LOGS"
echo "Para detener los servidores: $RAIZ/detener-taller.sh"
