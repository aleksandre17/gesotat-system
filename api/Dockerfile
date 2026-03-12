FROM eclipse-temurin:17-jre-focal
RUN groupadd -r app && useradd -r -g app -m app \
    && apt-get update && apt-get install -y --no-install-recommends wget gosu \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY app.jar app.jar
RUN mkdir -p /app/storage /app/logs /app/uploads \
    && chown -R app:app /app
RUN printf '#!/bin/sh\nchown -R app:app /app/logs\nexec gosu app java -jar /app/app.jar\n' > /entrypoint.sh \
    && chmod +x /entrypoint.sh
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=90s \
    CMD wget -qO- http://localhost:8081/health | grep -q UP || exit 1
ENTRYPOINT ["/entrypoint.sh"]
