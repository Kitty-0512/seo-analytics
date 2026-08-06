#!/bin/sh
set -e

# Render DATABASE_URL is postgresql://... ; Spring needs jdbc:postgresql://...
if [ -n "${DATABASE_URL:-}" ]; then
  JDBC_URL=$(printf '%s' "$DATABASE_URL" \
    | sed -e 's|^postgres://|jdbc:postgresql://|' \
          -e 's|^postgresql://|jdbc:postgresql://|')
  # Render Postgres requires SSL for JDBC
  case "$JDBC_URL" in
    *sslmode=*) ;;
    *\?*) JDBC_URL="${JDBC_URL}&sslmode=require" ;;
    *) JDBC_URL="${JDBC_URL}?sslmode=require" ;;
  esac
  export SPRING_DATASOURCE_URL="$JDBC_URL"
fi

# Prefer Redis URL from Render Key Value when present
if [ -n "${REDIS_URL:-}" ]; then
  export SPRING_DATA_REDIS_URL="$REDIS_URL"
fi

# Private-network AI service: hostport -> full URL
if [ -n "${AI_HOSTPORT:-}" ] && [ -z "${AI_SERVICE_URL:-}" ]; then
  export AI_SERVICE_URL="http://${AI_HOSTPORT}"
fi

# Free-tier friendly heap
JAVA_OPTS="${JAVA_OPTS:--Xms128m -Xmx384m}"

exec java $JAVA_OPTS -jar /app/app.jar
