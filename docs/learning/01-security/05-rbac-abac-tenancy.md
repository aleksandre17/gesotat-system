# RBAC, ABAC და tenancy

## RBAC

Role პასუხობს კითხვას: **რა ტიპის მოქმედება შეუძლია principal-ს?**

მაგალითად: `contract.read`.

## ABAC

Attributes ამატებს კონტექსტს:

```json
{
  "tenant": "kids-staging",
  "site": "kids",
  "contract": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 8
}
```

## enforcement

```text
role allowed?
 → tenant matches?
 → site/product boundary matches?
 → contract revision approved?
 → requested fields allowed?
```

ერთი tenant-ის token-ით მეორე tenant-ის მონაცემი ყოველთვის უარყოფილი უნდა იყოს და ეს negative test-ით უნდა დასტურდებოდეს.
