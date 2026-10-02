#!/usr/bin/env bash
# Detiene los servidores locales iniciados por iniciar-taller.sh.
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOGS="$RAIZ/logs"

# Detiene el grupo de procesos creado por iniciar-taller.sh.
# Parámetro: nombre del servicio cuyo PID se registró.
# Retorno: 0 aunque el proceso ya no exista.
detener() {
  local nombre="$1"
  local archivo="$LOGS/$nombre.pid"

  if [[ ! -f "$archivo" ]]; then
    echo "$nombre: no hay PID registrado."
    return
  fi

  local grupo
  grupo="$(<"$archivo")"
  if kill -0 -- -"$grupo" 2>/dev/null; then
    # setsid hace que el PID registrado sea también el identificador del grupo.
    # Así se detienen Maven/Vite y los procesos hijo que lanzan.
    if kill -- -"$grupo" 2>/dev/null; then
      echo "$nombre detenido (grupo $grupo)."
    else
      echo "$nombre no pudo detenerse por grupo; se intentará localizarlo por puerto."
    fi
  else
    echo "$nombre ya no estaba en ejecución."
  fi
  rm -f "$archivo"
}

detener backend
detener frontend

# Recupera instancias iniciadas con la versión anterior, que podía perder sus PID.
# Parámetros: nombre, puerto y fragmento inequívoco de la ruta del proyecto.
# Retorno: 0; solo señala procesos cuyo comando contiene la ruta esperada.
detener_huerfano_por_puerto() {
  local nombre="$1" puerto="$2" marcador="$3" pid comando
  for pid in $(fuser -n tcp "$puerto" 2>/dev/null || true); do
    comando="$(ps -p "$pid" -o args= 2>/dev/null || true)"
    if [[ "$comando" == *"$marcador"* ]]; then
      kill "$pid" 2>/dev/null || true
      echo "$nombre detenido por puerto $puerto (PID $pid)."
    fi
  done
}

detener_huerfano_por_puerto backend 8080 "$RAIZ/backend"
detener_huerfano_por_puerto frontend 5173 "$RAIZ/frontend"

echo "MySQL permanece activo. Para detenerlo también:"
echo "docker compose -f /home/venmve/mis-contenedores/docker-compose.yml stop mysql"
