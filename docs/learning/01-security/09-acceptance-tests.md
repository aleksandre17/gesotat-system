# Security acceptance tests

## Positive

- valid RS256 token → 200;
- approved role + tenant → permitted page query;
- valid contract revision → introspection and projection.

## Negative

- missing token → 401;
- expired token → 401;
- wrong issuer → 401;
- wrong audience → 401;
- invalid signature → 401;
- wrong tenant → 403/404 policy;
- unapproved revision → deny;
- forbidden field → deny or redacted response.

## Evidence

ყოველი test ინახავს status-ს, request fingerprint-ს, policy decision-ს და timestamp-ს; secret/token სრულად redacted არის.
