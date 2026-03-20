#!/bin/sh
set -eu

MASTER_HOST="${MASTER_HOST:-mysql-master}"
SLAVE_HOST="${SLAVE_HOST:-mysql-slave}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
MYSQL_REPL_USER="${MYSQL_REPL_USER:-repl}"
MYSQL_REPL_PASSWORD="${MYSQL_REPL_PASSWORD:-repl123}"

wait_for_mysql() {
  host="$1"
  echo "waiting for ${host}..."
  until mysql -h"${host}" -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "SELECT 1" >/dev/null 2>&1; do
    sleep 2
  done
}

wait_for_mysql "${MASTER_HOST}"
wait_for_mysql "${SLAVE_HOST}"

mysql -h"${SLAVE_HOST}" -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "STOP REPLICA;" >/dev/null 2>&1 || true
mysql -h"${SLAVE_HOST}" -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "RESET REPLICA ALL;" >/dev/null 2>&1 || true

mysql -h"${SLAVE_HOST}" -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='${MASTER_HOST}',
  SOURCE_PORT=3306,
  SOURCE_USER='${MYSQL_REPL_USER}',
  SOURCE_PASSWORD='${MYSQL_REPL_PASSWORD}',
  SOURCE_AUTO_POSITION=1,
  GET_SOURCE_PUBLIC_KEY=1;
START REPLICA;
SQL

attempt=0
while [ "${attempt}" -lt 30 ]; do
  status="$(mysql -h"${SLAVE_HOST}" -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "SHOW REPLICA STATUS\\G" 2>/dev/null || true)"
  echo "${status}" | grep -q "Replica_IO_Running: Yes" && echo "${status}" | grep -q "Replica_SQL_Running: Yes" && exit 0
  attempt=$((attempt + 1))
  sleep 2
done

echo "replica startup failed"
mysql -h"${SLAVE_HOST}" -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "SHOW REPLICA STATUS\\G" || true
exit 1
