# სისტემის იდენტობა

ჩვენ ვქმნით **contract-driven, metadata-driven, schema/provider/site-agnostic data-product platform-ს**.

ეს ნიშნავს:

- contract არის ქცევის source of truth;
- metadata განსაზღვრავს fields, filters, relations, projections და response shape-ს;
- core არ უნდა შეიცავდეს KIDS-specific branching-ს;
- ახალი provider/site/data family ემატება contract/configuration-ით;
- implementation-ის დასრულება მოითხოვს tests + evidence + approval-ს.
