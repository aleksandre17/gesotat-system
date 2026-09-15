# Security operational runbook

## Preparation

1. შეამოწმე მხოლოდ `geostat` compose stack;
2. შეამოწმე Keycloak health;
3. შეამოწმე discovery `HTTP 200`;
4. გახსენი SSH tunnel;
5. გამოიყენე ახალი PKCE session და უნიკალური callback port.

## Execution

```powershell
powershell.exe -ExecutionPolicy Bypass -File \
  "C:\Users\Test-User\Desktop\gesotat-system\ops\scripts\complete-kids-oidc-pkce.ps1" \
  -IssuerBase "http://keycloak:8080" -Port 18766
```

შემდეგ გამოიყენე იმავე პროცესის მიერ გენერირებული URL. ძველი URL-ის ხელახლა გამოყენება დაუშვებელია.

## Verification

- `TOKEN_READY`;
- token-ის issuer/audience/alg შემოწმება redacted tooling-ით;
- API introspection;
- page query;
- tenant-negative test;
- evidence file.
