# გამოწვევები და გადაწყვეტილებები

| გამოწვევა | მიზეზი | სწორი გადაწყვეტა |
|---|---|---|
| hostname validation failure | `keycloak:8080` არასწორი KC hostname ფორმატი | `http://keycloak:8080` |
| connection reset | Keycloak restart-loop-ში იყო | logs → config correction → geostat-only recreate |
| invalid scope | client scopes არ იყო სწორად გამოცხადებული | approved default scopes |
| restart login cookie not found | სხვადასხვა hostname/ძველი session | ერთი issuer hostname და ახალი PKCE session |
| callback port conflict | Windows HTTP.sys reservation | ახალი callback port `18766`, client redirect update |
| RS256 vs HMAC 401 | API verifier-ის მოძველებული რეჟიმი | issuer/JWKS RS256 configuration |

## მთავარი საინჟინრო დასკვნა

ყოველი შეცდომა უნდა დაიყოს identity, transport, callback, token ან authorization კლასად. შემთხვევითი retry პრობლემას არ ხსნის და შესაძლოა audit trail-იც დააბინძუროს.
