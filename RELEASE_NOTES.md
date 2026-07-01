# Release Notes - Monochrome v1.0

## 🚀 New Features & Enhancements

### 📂 Intelligent Local Library
- **Fully Automated Loading**: The app now silently remembers your last used music folder and reloads it instantly on start-up. No more re-selecting folders every time you open the app!
- **"Change Folder" Support**: Added full support for the "Change Folder" button in the web UI. You can now manually trigger the system file picker to switch music sources at any time.
- **Turbo Scanning**: Re-engineered the local file scanner to batch-detect file types. Your music library now loads near-instantly, even with thousands of tracks.

### 🌐 Immersive "We Go Local" Mode
- **Network Cut**: To ensure an uninterrupted offline experience, the app now automatically cuts the internet connection for 5 seconds whenever you switch to the "Local Files" tab or start playing a local song.
- **UI Feedback**: Provides clear "We Go Local" and "Enjoy" toast notifications during the network transition.

### ⚙️ Improved System Integration
- **Settings Import**: Fixed the "Import" button in the system settings. You can now correctly restore your settings backups using the Android system file picker.
- **Standardized Media Controls**: Refined the notification media controls (Play/Pause, Next, Previous) for better reliability and faster response times across all devices.

## 🐛 Bug Fixes & Optimizations

- **Smooth Navigation**: Resolved an issue where scrolling or clicking links on the home page would occasionally become unresponsive.
- **Android 14+ Ready**: Updated foreground service declarations to comply with strict Android 14+ requirements for media playback.
- **Zero Latency**: Restricted the internal network proxy to strictly essential tasks, allowing the main site to load natively with full browser caching.
- **Stability Pass**: Performed a comprehensive cleanup of debug logs and consolidated redundant logic for a cleaner, faster codebase.
