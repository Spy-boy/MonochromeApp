# Release Notes - Monochrome v1.1.0

## 🚀 Improvements & New Features

### 🔄 Refined Pull-to-Reload
- **Restricted Trigger Zone**: The pull-to-reload gesture is now restricted to the top **20% of the screen**. This significantly reduces accidental reloads while scrolling or interacting with the main content area.
- **Improved Stability**: The reload mechanism is now more robust against rapid gestures.

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
