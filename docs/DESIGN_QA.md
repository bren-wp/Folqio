# Folqio 1:1 UI acceptance criteria

Reference: the previously generated Folqio brand sheet and mobile concept images. These concept renders are **not** executable screens. Each actual screen needs a pixel-overlay comparison on supported device sizes before claiming 1:1.

## Visual tokens

- Background Midnight Navy `#0B1321`
- Surfaces Deep Slate `#1A2433`
- Accent Mint Teal `#22D3A7`
- Primary text Cool White `#F7FAFC`
- Secondary text Muted `#A9B9C9`
- 14–22 px card corner radii, refined spacing, minimal line icons, strong foreground contrast
- Folqio document/check symbol and wordmark. No demo usernames, fake notifications, store ratings or fictitious files.

## Screens to check on each platform

| Screen | Content to verify | Interaction to test |
| --- | --- | --- |
| Launch & onboarding | Brand icon, wordmark, offline messaging | New install and return visit |
| Home | Header, Quick actions, recent files, empty state | Open, camera, photo, file selection |
| Library | File rows, search, folders/categories | Filter, rename, delete, favorite |
| PDF reader | White page canvas, document controls, page counter | Swipe, zoom, search, bookmark |
| Annotation editor | Tool shelf, colors, notes, highlights | Add, undo/redo, save, reopen |
| Scanner | Capture, edge selection, crop, correction | Multi-page capture and export |
| Page manager | Grid thumbnails and page tools | Reorder, rotate, delete, extract, merge |
| OCR/search | Text results, highlights, extraction | Search real image-only PDFs |
| Security/settings | App lock, privacy, theme choices | Permissions, biometrics, relaunch |

## Required test matrix

- Android: compact 360 dp, standard 393 dp, large 480+ dp; light/dark system mode, font scale 100%/150%, landscape.
- iOS: compact iPhone, large iPhone and iPad; Dynamic Type, VoiceOver, portrait/landscape.
- Overlay each actual screenshot with its matching reference; inspect clipping, proportions, alignment, typography, safe-area insets and bottom navigation.
- Run click-by-click manual QA and re-open documents after termination.
- Mark an item **not implemented** rather than showing a nonfunctional control.

A 1:1 claim is **not yet verified**. The screenshots have not been compared on devices and several reference screens are still unimplemented.
