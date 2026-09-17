FROM eclipse-temurin:17-jre-focal
ARG IMAGE_REVISION=unknown
LABEL org.opencontainers.image.revision="$IMAGE_REVISION" org.opencontainers.image.source="geostat-system"
ENV BUILD_REVISION="$IMAGE_REVISION"
RUN groupadd -r app && useradd -r -g app -m app \
    && apt-get update && apt-get install -y --no-install-recommends wget gosu \
    && rm -rf /var/lib/apt/lists/*
# Shared JVM security policy (same file as the remote dev image).
COPY docker/legacy-sqlserver-tls.security /tmp/legacy-sqlserver-tls.security
RUN cat /tmp/legacy-sqlserver-tls.security >> "$JAVA_HOME/conf/security/java.security" \
    && rm /tmp/legacy-sqlserver-tls.security
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
