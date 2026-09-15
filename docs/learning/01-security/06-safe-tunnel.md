# Safe SSH tunnel

ტესტისთვის გამოიყენება:

```text
localhost:8080 → SSH → remote geostat-keycloak:8080
```

ეს არ ხსნის Keycloak-ს საჯარო ინტერნეტში. Windows hosts mapping:

```text
127.0.0.1 keycloak
```

Tunnel გამოიყენება მხოლოდ staging verification-ისას და არ ცვლის production ingress-ს, TLS-ს ან OIDC authority-ს.
