# API composition boundary

The API service is declared by the project-level `docker-compose.dev.yml` and
`docker-compose.prod.yml` entrypoints. This directory is the service-owned
extension point for future profiles, health and resource overrides. It must not
contain a second copy of the service definition: one service has one Compose
authority.
