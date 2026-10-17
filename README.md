# Private Media Converter

A privacy-first native Android app for offline media conversion.

## Current implementation

- **WhatsApp/share-sheet workflow:** use Share on a photo in WhatsApp and choose Private Media Converter. The app accepts Android image share intents, including batches when the source app supports them.
- Shared photos can be converted to standard JPEG and saved to `Pictures/Private Media Converter` without first downloading them or opening a file picker.
- The app also supports manual batch selection and a chosen output folder.
- HEIC/HEIF → JPEG using Android's on-device image decoder and JPEG encoder.
- Adjustable JPEG quality from 50% to 100% (default 95%).
- MOV → MP4 by remuxing compatible encoded audio/video tracks without re-encoding.
- Output names are made unique; originals are never modified.
- Refreshed Material 3 UI, progress and per-file errors, and a custom adaptive launcher icon.

## Privacy

- No `INTERNET` permission, analytics, ads, accounts, tracking, or cloud uploads.
- Shared files are read through Android-provided content URIs; converted images are written using MediaStore to the Pictures folder.
- The app does not request broad storage permission.

## Build APK

The GitHub Actions workflow builds the debug APK and runs unit tests. Open the repository's **Actions** tab, select a successful **Android CI** run, and download the `private-media-converter-debug-apk` artifact.

The workflow uses JDK 17 and Gradle 8.13 on a GitHub-hosted runner.

## Known limitations

- The source app must expose the photo through Android's share sheet. The exact options offered by WhatsApp can vary by media type/version.
- HEIC/HEIF decoding depends on Android's image decoder and the particular image variant. Unsupported files produce an error instead of a fake conversion.
- The current MOV path **remuxes compatible existing audio/video streams**; it does not transcode incompatible codecs. MOV is a container, and not every codec can be copied into an MP4 container.
- The repository's automated tests are initial unit tests; real HEIC variants, different MOV codecs, share flows and physical-device installation still require testing.
- Do not assume a successful build or device test until the corresponding GitHub Actions result and device checks are confirmed.
