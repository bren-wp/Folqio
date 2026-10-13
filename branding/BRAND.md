# Folqio — brand system

**Name:** FOLQIO  
**Tagline:** Your documents. Your device.  
**Voice:** calm, precise, private, confident.  
**Signature:** a white folded paper + mint verified/check detail on midnight navy.

## Design tokens

| Token | Color |
| --- | --- |
| Midnight | `#0B1321` |
| Slate surface | `#1A2433` |
| Mint accent | `#22D3A7` |
| Pearl text | `#F7FAFC` |
| Muted text | `#A9B9C9` |

## Interface principles

- High contrast document typography; dark chrome, light PDF canvas.
- 14–22 px equivalent corner radius on interactive cards.
- No notification or account UI: no authentication, no analytics, no cloud.
- Empty states must explain how to import a real PDF. No mock files are bundled.
- Reuse system icons and bundled vector resources rather than a CDN.
- Ensure actual UI is audited on Android and iOS devices; concept renders are a visual target, **not** an assertion of pixel-level parity.

## Asset note

The brand mark is implemented as a vector drawable on Android and a SwiftUI composition on iOS. A production app-store icon set requires dedicated raster icon exports and separate design QA.
