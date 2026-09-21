# Eversolo Cover Scanner v0.1

Read-only scanner for Eversolo DMP-A6 music storage.

## Safety
- Does not modify audio files.
- Does not write or change tags.
- Does not rename, move, or delete music.
- Does not overwrite existing artwork.
- v0.1 does not download artwork.

## What v0.1 detects
- Audio files in the selected folder tree.
- Same-folder artwork named cover, folder, or front in JPG/JPEG/PNG.
- Embedded artwork when Android MediaMetadataRetriever can read it.
- Unsupported/unreadable embedded art is counted as Unknown instead of Missing.

## Build
GitHub Actions builds a debug APK automatically on pushes to main and via manual workflow dispatch.

After a successful run:
Actions -> Build APK -> latest successful run -> Artifacts -> EversoloCoverScanner-v0.1-debug
