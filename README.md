# MORE PHONES — Local + Cloud Device Lab

MORE PHONES is an Android-first organizer for local APK workspaces and externally hosted Android sessions.

## Build 0.3.0
- Create and delete multiple local file workspaces, each with its own app-private APK vault.
- Import APK files into a selected workspace using Android's document picker.
- Save HTTPS links to externally hosted Android sessions and open them through Android.
- Workspace metadata is stored locally in SharedPreferences; APK copies are stored under app-private files.
- No account, analytics, or backend credentials are collected.

## Important capability boundaries
A local workspace is an isolated file vault, NOT a virtual Android operating system. APKs copied into it are not installed or executed inside a separate Android guest. Android's normal app sandbox prevents an ordinary app from silently creating independent Android users/profiles or installing apps into them.

The Android Virtualization Framework APIs were introduced in API 34 and are restricted system APIs requiring MANAGE_VIRTUAL_MACHINE; they are not a general-purpose option for third-party apps on Android 12. See https://android.googlesource.com/platform/packages/modules/Virtualization/+/HEAD/libs/framework-virtualization/README.md

Cloud links are connection bookmarks only. They do not provision cloud devices. A real cloud Android service, account/project, authorization and possibly paid usage are required. Google's Device Streaming is a developer device-testing service with remote physical devices, not an always-on personal fleet; sessions are wiped when returned. See https://developer.android.com/studio/run/android-device-streaming

## Privacy
- No INTERNET permission is declared.
- APK selection is user-initiated through Android's Storage Access Framework.
- APKs are copied into the app's private storage.
- Cloud URLs are opened by the Android browser; no passwords or tokens are collected by this app.

## Build
GitHub Actions builds a debug APK on every push to main.
