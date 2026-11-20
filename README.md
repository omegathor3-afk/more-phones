# MORE PHONES — Local Device Lab

MORE PHONES is an Android app project for organizing local app-testing workflows on one physical Android device.

## Current MVP (0.1.0)
- Dark, Arabic-first device-lab dashboard.
- Pick an APK file using Android's system document picker.
- Hand the selected APK to Android's package installer; the user confirms installation.
- No account, backend, analytics, or network permission is requested.

## Important platform boundary
This MVP does NOT create full virtual Android phones, independent Android user profiles, independent hardware identities, or per-profile IP addresses. Ordinary third-party Android apps cannot silently create managed profiles, install arbitrary packages, or provide multiple fully isolated Android operating systems on every device.

A true local multi-VM implementation would require device/OS support for hardware virtualization and a compatible hypervisor/runtime. Availability cannot be assumed on a normal Android 12 handset. VPN routing also needs a supported per-profile/per-app network architecture; a UI profile alone does not create a separate network identity.

## Privacy
- No INTERNET permission is declared.
- APK selection is user-initiated through the Android Storage Access Framework.
- Installation is handed to the system package installer and requires user approval.
- The app does not inspect app-private data, passwords, or authentication tokens.

## Build
GitHub Actions builds a debug APK on every push to main.
