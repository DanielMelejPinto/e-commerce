#!/bin/bash
set -e

if [ ! -f ".env" ]; then
  echo "Error: Falta el archivo .env"
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "Error: curl no está instalado"
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "Error: python3 no está instalado"
  exit 1
fi

cd "$(dirname "$0")/.." && set -a && source .env && set +a
BASE=http://localhost:5173
J='Content-Type: application/json'
FAIL=0

req()   { RESP=$(curl --max-time 10 --connect-timeout 5 -s -w $'\n%{http_code}' "$@" || true); STATUS=${RESP##*$'\n'}; BODY=${RESP%$'\n'*}; }
field() { echo "$BODY" | python3 -c "import json,sys;print(json.load(sys.stdin).get('$1',''))" 2>/dev/null; }
check() { if [ "$2" = "$3" ]; then echo "OK    $1"; else echo "FALLA $1 (esperado $3, obtuvo '$2')"; echo "      status: $STATUS cuerpo: $(echo "$BODY" | head -c 300)"; FAIL=1; fi; }
jbody() { python3 -c "import json,os,sys;print(json.dumps(dict(a.split('=',1) for a in sys.argv[1:])))" "$@"; }

req $BASE/;                          check "front /" "$STATUS" 200
req $BASE/api/productos;             check "listar productos" "$STATUS" 200
req -X POST $BASE/api/usuarios/login -H "$J" -d '{}'; check "login vacio" "$STATUS" 400

req -X POST $BASE/api/usuarios/login -H "$J" -d "$(jbody email=$ADMIN_EMAIL password=$ADMIN_PASSWORD)"
check "login admin" "$STATUS" 200
AT=$(field token)

req -X POST $BASE/api/productos -H "Authorization: Bearer $AT" -H "$J" -d '{"nombre":"Producto E2E","descripcion":"Prueba automatica","precio":9990}'
check "crear producto" "$STATUS" 201
PID=$(field id)

START=$(date +%s)
for i in $(seq 1 60); do
  req $BASE/api/inventarios/producto/$PID -H "Authorization: Bearer $AT"
  [ "$STATUS" = "200" ] && break; sleep 2
done
check "inventario creado por Kafka" "$STATUS" 200
echo "      (tardo $(( $(date +%s) - START )) s)"

req -X PUT $BASE/api/inventarios/producto/$PID/agregar -H "Authorization: Bearer $AT" -H "$J" -d '{"cantidad":50}'
check "agregar stock" "$STATUS" 200

UEMAIL="e2e.$(date +%s)@test.com"
UPASS=$(python3 -c "import secrets;print(secrets.token_urlsafe(12))")
req -X POST $BASE/api/usuarios/registro -H "$J" -d "$(jbody nombre='Usuario E2E' email=$UEMAIL password=$UPASS)"
check "registro" "$STATUS" 201
req -X POST $BASE/api/usuarios/login -H "$J" -d "$(jbody email=$UEMAIL password=$UPASS)"
check "login usuario" "$STATUS" 200
UT=$(field token)

req -X POST $BASE/api/pedidos -H "$J" -d '{"items":[{"productoId":1,"cantidad":1}]}'
check "pedido sin token" "$STATUS" 401
req -X POST $BASE/api/pedidos -H "Authorization: Bearer $UT" -H "$J" -d "{\"items\":[{\"productoId\":$PID,\"cantidad\":0}]}"
check "pedido cantidad 0" "$STATUS" 400
req -X POST $BASE/api/pedidos -H "Authorization: Bearer $UT" -H "$J" -d '{"items":[]}'
check "pedido items vacio" "$STATUS" 400

req -X POST $BASE/api/pedidos -H "Authorization: Bearer $UT" -H "$J" -d "{\"items\":[{\"productoId\":$PID,\"cantidad\":2}]}"
check "crear pedido" "$STATUS" 201
OID=$(field id)
check "estado pedido" "$(field estado)" CONFIRMADO

req $BASE/api/pedidos/mis-pedidos -H "Authorization: Bearer $UT"
check "mis-pedidos" "$STATUS" 200

req $BASE/api/inventarios/producto/$PID -H "Authorization: Bearer $AT"
check "stock reservado" "$(field cantidadReservada)" 2
check "stock disponible tras reserva" "$(field cantidadDisponible)" 48

req -X POST $BASE/api/pedidos -H "Authorization: Bearer $UT" -H "$J" -d "{\"items\":[{\"productoId\":$PID,\"cantidad\":60}]}"
check "pedido sin stock" "$STATUS" 409

req -X POST $BASE/api/pedidos/$OID/cancelar -H "Authorization: Bearer $UT"
check "cancelar pedido" "$STATUS" 200
check "estado cancelado" "$(field estado)" CANCELADO

req $BASE/api/inventarios/producto/$PID -H "Authorization: Bearer $AT"
check "stock liberado" "$(field cantidadDisponible)" 50
check "reservado en 0" "$(field cantidadReservada)" 0

req -X PUT $BASE/api/inventarios/producto/$PID/reservar -H "Authorization: Bearer $UT" -H "$J" -d "{\"cantidad\":1,\"pedidoId\":123}"
check "usuario normal no puede reservar" "$STATUS" 403
req -X PUT $BASE/api/inventarios/producto/$PID/liberar -H "Authorization: Bearer $UT" -H "$J" -d "{\"cantidad\":1,\"pedidoId\":123}"
check "usuario normal no puede liberar" "$STATUS" 403

if [ $FAIL -eq 0 ]; then
  echo "TODO OK"
  exit 0
else
  echo "HAY FALLAS"
  exit 1
fi
