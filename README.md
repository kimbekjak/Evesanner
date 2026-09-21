# Eversolo Cover Scanner v0.1

Read-only first-stage scanner for Eversolo DMP-A6 music storage.

## Safety rules
- Does NOT modify audio files.
- Does NOT write tags.
- Does NOT rename, move, or delete music.
- Does NOT overwrite existing artwork.
- v0.1 does NOT download artwork.

## What v0.1 detects
- Audio files in the selected tree.
- Same-folder artwork named `cover`, `folder`, or `front` in JPG/JPEG/PNG.
- Embedded artwork where Android's `MediaMetadataRetriever` can read it.
- Files/folders that Android cannot inspect are counted as `unknown`, not falsely marked missing.

## GitHub build
Push this repository to GitHub. The included GitHub Actions workflow builds a debug APK automatically.
Open **Actions → Build APK → latest run → Artifacts** and download `EversoloCoverScanner-v0.1-debug`.

## A6 use
Install the APK, open it, select the music storage/folder, and run the scan.

Note: DSF/DFF metadata extraction support can vary by Android/Eversolo firmware. v0.1 therefore separates `unknown` from `missing`.
