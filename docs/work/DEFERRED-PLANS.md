# გადადებული გეგმები

გადაწყვეტილებით გადადებული შესაძლებლობები. თითოეულს აქვს მიზეზი, ADR, მოქმედი compensating controls
და ხელახლა გახსნის trigger. აქ მოხვედრა ნიშნავს: **ამჟამად არ ვაკეთებთ და არ ვბლოკავთ მასზე სხვა სამუშაოს**.

## DP-001 — Artifact malware scanning

- **სტატუსი:** `DEFERRED` (2026-09-18) · **ADR:** ADR-009 (`docs/platform-decisions.md`)
- **უარყოფილი ვარიანტი:** ClamAV / `clamd` — არ გამოიყენება.
- **რა ამოვიღეთ:** scanner port და ClamAV adapter, admission gate (ZIP, inventory import, Access upload),
  quarantine registry და write-path, scanner-ის 422/503 mapping, metrics, კონფიგურაცია, clamd acceptance probe,
  quarantine SQL test.
- **რა დარჩა:** migrations 090/095 (immutable ledger) და უმოქმედო `ingest.artifact_quarantine` ცხრილი;
  ისტორიული evidence ფაილები (`docs/evidence/*2026-09-18*`) უცვლელია, როგორც იმ მომენტის ფაქტი.
- **Compensating controls:** ADR-009-ში.
- **ხელახლა გახსნის trigger:** გარე/anonymous upload, public production release, ან ორგანიზაციული scanner სერვისი.
  გახსნისას: provider-neutral port + adapter, fail-closed admission ყველა untrusted upload-ზე, quarantine write,
  runtime evidence (clean/infected/unavailable).
