#!/bin/sh
set -eu

port="${PORT:-8080}"
case "$port" in
  ''|*[!0-9]*)
    echo "PORT must be a numeric TCP port, got: $port" >&2
    exit 1
    ;;
esac

# Keep the application unchanged; only adjust Tomcat's connector at container startup.
sed -i "s/port=\"8080\" protocol=/port=\"${port}\" protocol=/" "$CATALINA_HOME/conf/server.xml"
exec "$@"
