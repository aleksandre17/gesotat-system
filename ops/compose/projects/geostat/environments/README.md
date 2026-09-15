# Compose environment boundary

Environment selection and non-secret overlays belong here. Secrets are owned
by `ops/config/projects/geostat` and are injected at deploy time; they are never
embedded in Compose source.
