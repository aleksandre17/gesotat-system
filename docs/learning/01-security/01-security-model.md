# Security model

## მიზანი

მონაცემზე წვდომა არ იწყება SQL-ით. ის იწყება identity-ით და მთავრდება evidence-ით:

```text
Untrusted request
 → authenticated principal
 → cryptographic token validation
 → role/tenant policy
 → contract/page authorization
 → bounded query
 → redacted/audited response
```

## Trust boundaries

- ბრაუზერი და frontend არის untrusted client;
- Keycloak არის identity authority;
- API არის policy enforcement point;
- DB/storage არის protected data boundary;
- logs/evidence არ უნდა შეიცავდეს token-ს, password-ს ან sensitive field-ს.

## cardinal წესები

- deny-by-default;
- least privilege;
- token-ის ხელმოწერა ყოველთვის მოწმდება;
- tenant boundary query-მდე უნდა დადგინდეს;
- გაურკვეველი policy-ისას ოპერაცია წყდება.
