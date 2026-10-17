# Private Media Converter

A privacy-first native Android app for offline media conversion.

## Status

Initial project setup is in progress. The app is intended to support HEIC/HEIF to JPEG conversion and MOV to MP4 conversion using on-device Android media APIs. Device-dependent codec support will be reported rather than hidden.

## Privacy

- No internet permission, analytics, ads, accounts, or cloud uploads.
- Files are selected with Android's Storage Access Framework.
- Original files are preserved.

## Build

The GitHub Actions workflow builds the debug APK using a hosted Android SDK and Gradle. Download the `private-media-converter-debug-apk` artifact from a successful workflow run.

## Important limitations

HEIC/HEIF decoding depends on Android/device codec support. MOV files can contain codecs that are not compatible with MP4; those require on-device transcoding and will be reported as unsupported if the device cannot decode/encode the necessary formats.
