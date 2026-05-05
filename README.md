
# Monochrome Android App

A native Android wrapper for [monochrome.tf](https://monochrome.tf) — a HiRes music streaming platform.

![Platform](https://img.shields.io/badge/Platform-Android-brightgreen) ![Language](https://img.shields.io/badge/Language-Kotlin-orange) 

---

## Features

- 🎵 **Background Audio** — Music keeps playing when the screen is off or app is minimised
- 🔔 **Media Notification** — Shows the current track name with a persistent, non-dismissible notification and an Exit button
- ⬇️ **HiRes Downloads** — Downloads FLAC, WAV, MP3 and other formats with the correct track filename
- 📁 **Local Files** — Select a local music folder (including subfolders) and play tracks directly in the app
- 🖥️ **Full-Screen WebView** — Clean, immersive experience with no browser chrome
- 🔄 **Pull to Reload** — Pull down and hold for 3 seconds to reload the page
- 🔒 **Session Persistence** — Stay logged in across app restarts
- 🎧 **Headphone Unplug** — Pauses playback automatically when headphones are unplugged
- 📱 **Portrait & Landscape** — Supports all screen orientations without reloading

---



## Project Structure

```
MonochromeApp
└── app/src/main/
    ├── kotlin/com/monochrome/app/
    │   ├── MainActivity.kt       # WebView host, CORS proxy, JS injection
    │   ├── MusicService.kt       # Foreground media service & notification
    │   ├── JsBlobReceiver.kt     # JS↔Kotlin bridge (downloads, tracks, folders)
    │   ├── AppExitReceiver.kt    # Handles Exit button in notification
    │   └── NoisyReceiver.kt      # Pauses on headphone unplug
    ├── res/
    │   ├── layout/activity_main.xml
    │   └── drawable/             # App icon (Monochrome logo)
    └── AndroidManifest.xml
```

---

## Permissions

| Permission | Reason |
|-----------|--------|
| `INTERNET` | Load the website |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Background audio |
| `WAKE_LOCK` | Keep audio alive when screen is off |
| `POST_NOTIFICATIONS` | Media playback notification (Android 13+) |
| `READ_MEDIA_AUDIO` | Access local music files (Android 13+) |
| `WRITE_EXTERNAL_STORAGE` | Save downloads (Android 9 and below) |

---

## License

This project is for personal use. [monochrome.tf](https://monochrome.tf) and its content are property of their respective owners.
