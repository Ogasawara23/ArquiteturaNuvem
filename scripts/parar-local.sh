#!/usr/bin/env bash
# Encerra os processos iniciados por rodar-local.sh, pelo dono real de cada porta.
# A infraestrutura (infra/sem-docker) e encerrada com Ctrl+C na janela dela.
PORTAS="8080 8081 8082 8083 8084 3001 3002 3003 7071 5100 5101 5200 5201 5202 5203"
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    powershell.exe -NoProfile -Command "foreach (\$p in '$PORTAS'.Split(' ')) { Get-NetTCPConnection -LocalPort ([int]\$p) -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id \$_ -Force; Write-Output \"parado :\$p\" } }" ;;
  *)
    for porta in $PORTAS; do
      pid=$(lsof -ti tcp:"$porta" 2> /dev/null | head -1)
      [[ -n "$pid" ]] && kill "$pid" && echo "parado :$porta"
    done ;;
esac
