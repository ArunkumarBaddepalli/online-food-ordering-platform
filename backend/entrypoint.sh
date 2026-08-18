#!/bin/sh
# Translates what the host provides into what Spring expects.
#
# Managed Postgres hands out a URL like postgres://user:pass@host:5432/name,
# which JDBC cannot read, and the host chooses the port at runtime. Rather than
# ask anyone to assemble those by hand in a dashboard, do it here.
set -e

if [ -n "$DATABASE_URL" ] && [ -z "$SPRING_DATASOURCE_URL" ]; then
    without_scheme=${DATABASE_URL#*://}
    credentials=${without_scheme%@*}
    host_and_db=${without_scheme#*@}

    SPRING_DATASOURCE_USERNAME=${credentials%%:*}
    SPRING_DATASOURCE_PASSWORD=${credentials#*:}
    SPRING_DATASOURCE_URL="jdbc:postgresql://${host_and_db}"

    export SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD SPRING_DATASOURCE_URL
    echo "Using the PostgreSQL database at ${host_and_db%%\?*}"
fi

# The host tells us which port to listen on; 8080 when running this by hand.
export SERVER_PORT="${PORT:-8080}"

exec java -XX:MaxRAMPercentage=75 -jar app.jar
