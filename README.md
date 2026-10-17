# Private Media Converter

A privacy-first native Android app for offline media conversion.

## Current implementation

- HEIC/HEIF → JPEG using Android's on-device image decoder and JPEG encoder.
- Adjustable JPEG quality from 50% to 100% (default 95%).
- Batch file selection and output-folder selection through Android's system document pickers.
- MOV → MP4 by remuxing compatible encoded audio/video tracks without re-encoding.
- Output names are made unique; originals are not modified.
- Progress and per-file errors are shown in the app.

## Privacy

- No `INTERNET` permission, analytics, ads, accounts, tracking, or cloud uploads.
- Files are accessed through Android's Storage Access Framework.
- The app does not request broad storage access.

## Build APK

The GitHub Actions workflow builds the debug APK and runs unit tests. Open the repository's **Actions** tab, select a successful **Android CI** run, and download the `private-media-converter-debug-apk` artifact.

The workflow uses JDK 17 and Gradle 8.13 on a GitHub-hosted runner.

## Known limitations

- HEIC/HEIF decoding depends on the Android version and the particular image variant. Unsupported files produce an error instead of a fake conversion.
- The current MOV path **remuxes compatible existing audio/video streams**; it does not transcode incompatible codecs. MOV is a container, and not every codec can be copied into an MP4 container. Unsupported streams will fail with an error.
- The repository's automated tests are initial unit tests; real HEIC variants, different MOV codecs, and installation on physical devices still require testing.
- No successful build or physical-device test should be assumed until the corresponding GitHub Actions result and device checks are confirmed.
