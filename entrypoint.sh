#!/bin/sh
set -e

if [ -n "$ELASTICSEARCH_CA_CERT" ]; then
  printf '%s\n' "$ELASTICSEARCH_CA_CERT" > /app/certs/ca.crt
fi

exec java -XX:MaxRAMPercentage=65.0 -XX:+ExitOnOutOfMemoryError -jar app.jar