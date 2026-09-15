# Authorization Code + PKCE

## სრული მიმდევრობა

1. client ქმნის შემთხვევით `code_verifier`-ს;
2. ითვლის `BASE64URL(SHA256(verifier))`-ს — `code_challenge`;
3. აგზავნის authorization request-ს;
4. Keycloak აბრუნებს ერთჯერად code-ს callback-ზე;
5. client აგზავნის code + verifier-ს token endpoint-ზე;
6. Keycloak ამოწმებს challenge/verifier წყვილს და გასცემს token-ს.

## მოთხოვნის მაგალითი

```text
/auth?client_id=kids-runtime-test
&response_type=code
&redirect_uri=http://localhost:18766/callback
&code_challenge=<challenge>
&code_challenge_method=S256
```

`code_verifier` არასდროს გადის authorization URL-ში.
