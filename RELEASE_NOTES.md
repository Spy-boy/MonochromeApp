# Release Notes - Monochrome v1.1.0

## 🚀 Improvements & New Features

### 📁 Enhanced Local Music Support
- **Automated Folder Loading**: The app now silently remembers your music folder and reloads it automatically on start-up.
- **Manual "Change Folder" Override**: Added full support for the "Change Folder" button to easily switch music sources.
- **High-Speed Scanning**: Optimized the folder scanner to load large music libraries near-instantly by batching file type detection.
- **We Go Local Mode**: Switching to the "Local Files" section now triggers a brief 5-second network blackout to ensure an immersive offline-focused experience.

### 🧹 Codebase Cleanup & Optimization
- **Refactored MainActivity**: Reorganized lifecycle management and UI setup logic for better maintainability and performance.
- **Standardized Notifications**: Centralized toast message handling via `ToastHelper` for consistent UI feedback across the app.
- **Enhanced Logging**: Improved network error reporting in `NetworkHelper` to assist with future troubleshooting.

## 🐛 Bug Fixes

### 🛡️ Post-Download Crash Fix
- Fixed a critical `NullPointerException` that occurred after downloading songs. This was caused by UI updates (toasts) being triggered from background threads. All UI notifications are now safely dispatched to the main thread.

### 📺 Blank Screen Resolution
- Resolved an issue where the app would occasionally display a blank screen on the first launch. This was fixed by optimizing how the app handles initial page loads and JavaScript hook injections.

---
*For more details on the implementation, please refer to the internal documentation.*
