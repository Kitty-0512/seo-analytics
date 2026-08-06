#!/bin/sh
set -e

# Render DATABASE_URL is postgresql://user:pass@host/db
# Spring needs jdbc:postgresql://host/db + separate username/password (Render sets both).
if [ -n "${DATABASE_URL:-}" ]; then
  REST=$(printf '%s' "$DATABASE_URL" | sed -E 's|^postgres(ql)?://||' | sed -E 's|^[^@]+@||')
  JDBC_URL="jdbc:postgresql://${REST}"
  case "$JDBC_URL" in
    *sslmode=*) ;;
    *\?*) JDBC_URL="${JDBC_URL}&sslmode=require" ;;
    *) JDBC_URL="${JDBC_URL}?sslmode=require" ;;
  esac
  export SPRING_DATASOURCE_URL="$JDBC_URL"
fi

# Render Key Value / Redis
if [ -n "${REDIS_URL:-}" ]; then
  export SPRING_DATA_REDIS_URL="$REDIS_URL"
fi

# Private-network AI service: hostport -> full URL
if [ -n "${AI_HOSTPORT:-}" ] && [ -z "${AI_SERVICE_URL:-}" ]; then
  export AI_SERVICE_URL="http://${AI_HOSTPORT}"
fi

JAVA_OPTS="${JAVA_OPTS:--Xms128m -Xmx384m}"

exec java $JAVA_OPTS -jar /app/app.jar
