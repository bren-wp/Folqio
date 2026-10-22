# Folqio Android-only source audit (0.1.13)

Only the Android Kotlin and Gradle project is maintained. Historical repository commits may still refer to former platforms; active source, CI, and release packaging are Android-only.

Run python3 scripts/audit_source.py. The script enforces SDK 36 target, release minification/shrinking, matching manifest and application ID, restricted manifest permissions, backup policy, live FileProvider ownership, version formatting, real Android CI and no tracked iOS source. It also flags duplicate imports and lexical dead-code candidates for manual review.

Confirmed change in this cycle: Android PDF deletion recovery no longer destroys a conflicting staged copy when a live PDF with the same filename exists. JVM regressions cover referenced and unreferenced conflict cases.

The audit is static; it cannot verify runtime safety, complete dead-code reachability, safe PDF parsing of arbitrary inputs, signature preservation, accessibility or Play Store acceptance. Do not delete classes merely because a lexical search finds no calls.
