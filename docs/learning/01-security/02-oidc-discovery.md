# OIDC discovery

## ძირითადი endpoint

```http
GET /realms/geostat/.well-known/openid-configuration
```

Discovery აბრუნებს `issuer`, `authorization_endpoint`, `token_endpoint` და `jwks_uri` მნიშვნელობებს.

## რატომ არის ეს უკეთესი hardcode-ზე

კლიენტი issuer-იდან დინამიკურად იგებს endpoint-ებს. Key rotation-ისას ახალი public key JWKS-იდან იტვირთება; application code-ის შეცვლა არ არის საჭირო.

## შემოწმება

```powershell
curl.exe http://127.0.0.1:8080/realms/geostat/.well-known/openid-configuration
```

მიღებული `HTTP 200` მხოლოდ discovery-ის ხელმისაწვდომობას ამტკიცებს; ის ჯერ არ ამტკიცებს API authorization-ს.
