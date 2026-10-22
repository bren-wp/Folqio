# Folqio Android UI acceptance criteria

Folqio uses existing brand colors: Midnight Navy #0B1321, Slate #1A2433, Mint #22D3A7, Cool White #F7FAFC and Muted #A9B9C9. Keep the existing icon and design.

Run real Android screenshots and interaction checks (not mockups) on 360 dp compact phones, 393 dp standard phones, 480+ dp large phones and tablets; test portrait/landscape, font scale 100%/150%, TalkBack and gesture navigation.

Check only real screens and controls: launch, home, recent files, document picker, system camera, photos, favorites, search, reader, zoom, bookmarks, rasterized merge/extract/rotate, ZIP export, settings and error/empty/loading states. Validate window insets, clipping, text contrast, touch-target size and accidental taps.

Advanced annotation/OCR/scanner/editor views described in old concept artwork are not currently implemented; never show dead buttons or describe them as running features.

Screenshot automation lives in .github/workflows/visual-screenshot.yml and captures authentic emulator screenshots. Device-by-device visual parity has not been verified.

## v0.1.14 Android interaction polish

- Saveable tab, search query, opened document URI and page position preserve user context during activity recreation.
- Quick action cards and ZIP export are explicitly disabled while asynchronous file operations run, with Material 3 disabled-state semantics.
- PDF thumbnail placeholders use the same Folqio mint token as other UI controls instead of an independent literal.
- Existing midnight/navy, slate and mint colors and brand mark remain unchanged.
- Pixel-perfect acceptance remains **unverified** pending real device/emulator screenshots aligned with provided references on all target densities and font sizes.
