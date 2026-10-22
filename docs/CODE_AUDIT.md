# Folqio Android-only source audit (0.1.13)

Only the Android Kotlin and Gradle project is maintained. Historical repository commits may still refer to former platforms; active source, CI, and release packaging are Android-only.

Run python3 scripts/audit_source.py. The script enforces SDK 36 target, release minification/shrinking, matching manifest and application ID, restricted manifest permissions, backup policy, live FileProvider ownership, version formatting, real Android CI and no tracked iOS source. It also flags duplicate imports and lexical dead-code candidates for manual review.

Confirmed change in this cycle: Android PDF deletion recovery no longer destroys a conflicting staged copy when a live PDF with the same filename exists. JVM regressions cover referenced and unreferenced conflict cases.

The audit is static; it cannot verify runtime safety, complete dead-code reachability, safe PDF parsing of arbitrary inputs, signature preservation, accessibility or Play Store acceptance. Do not delete classes merely because a lexical search finds no calls.

## v0.1.14 — comprehensive Android-only static audit

- Reviewed **all** current Kotlin source paths under `android/src`, including main and JVM tests, and all Android resource XML paths with `scripts/audit_source.py`.
- The script now enforces duplicate top-level type/import checks, verifies Android-only CI, API 36/Play build configuration, manifest permissions, non-exported FileProvider, opt-out from backup, release version, packaging and store disclosures.
- It reports potential unreferenced private helpers, imports and resource files as **review-only** candidates, never auto-deletes symbols that might be registered through Compose callbacks, Android XML, reflection or compiler-generated references.
- Focused manual cleanup: shared Folqio palette token replaced a duplicated thumbnail accent color. An otherwise unused `zipReady` translation key is now used by the ZIP completion message, preserving the existing 25 language options.
- Functional fixes: reader/document navigation persists across rotation, repeated external document intents are distinguished, and processing actions are disabled while busy.
- New Python tests cover audit detection and conservative false-positive handling.
- **Not proven by a static audit:** full call-graph reachability, absence of crashes across PDF formats, memory safety on all devices or reference-image pixel parity. Continue Kotlin lint, R8, Android tests and physical/emulator QA.
