# Folqio Android functional QA

1. Launch a fresh install in airplane mode and verify empty library, no account and no networking requests.
2. Import a normal multi-page PDF; navigate, zoom, bookmark and restore the previous page.
3. Test malformed, encrypted, oversized and revoked-provider PDF imports, then reopen the application.
4. Search document names, rename display labels, favorite/unfavorite and change tabs.
5. Take a photo using the system camera and create a new PDF; cancel capture and verify temporary cleanup.
6. Create PDFs from 1, 2 and 10 picked photos; test 25 MB size limits, corrupt images, EXIF rotations and memory pressure.
7. Merge 2–5 PDFs and test a total of 50 and 51 pages; confirm rasterized output is clearly distinguished from lossless editing.
8. Extract and rotate an individual page into a new copy; verify original PDF bytes stay identical.
9. ZIP export; verify names, file count, contents and CRC. Test insufficient space and cancelled destination.
10. Remove a locally created PDF; verify metadata rollback, interrupted deletion recovery, and conflict preservation when pending and live bytes differ.
11. Test small phones, tablet, landscape, 150% font scaling, TalkBack and gesture navigation. No clipped controls or unreachable actions.
12. Review final merged manifest, Data safety disclosures, signing identity, 16 KB compatibility if native libraries exist, and authentic Play listing screenshots.

CI gate: python3 scripts/audit_source.py and Gradle testDebugUnitTest, lintDebug, assembleDebug, bundleRelease. CI success is not proof of physical-device QA or Play approval.
