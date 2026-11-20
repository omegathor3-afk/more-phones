# MORE PHONES — Cloud Android fleet

MORE PHONES 0.4.0 integrates with MoreLogin Cloud Phone Open API.

## This build
- Connects to the user's MoreLogin account using API ID and API Key.
- Encrypts API credentials at rest with Android Keystore (AES-GCM).
- Lists real cloud Android devices from the provider.
- Creates one independent Android 12 cloud phone per new space.
- Optionally binds a distinct SOCKS5 proxy to each phone.
- Starts and stops each phone independently.
- Requests a short-lived (5-minute) remote-control browser link for a running phone.
- Shows the provider-reported egress IP when available.

## Requirements and limitations
- A MoreLogin account and Open API credentials are required. In MoreLogin, open Settings → API & MCP to obtain API ID and API Key.
- Cloud phone creation/start is subject to account quota, concurrency, balance, provisioning and provider policies. Starting a pay-per-minute phone consumes provider balance/minutes.
- MoreLogin support guidance says cloud phones require a usable SOCKS5 proxy to start. Configure a different proxy endpoint per device for distinct egress IPs. A VPN app can also be installed inside each cloud Android; use a separate VPN exit endpoint per device if unique public IPs are required.
- The app does not provide VPN subscriptions, proxy servers, cloud compute, or free device capacity.
- New devices are provisioned asynchronously. Refresh the list and wait for the provider to report them ready before starting.
- Remote-control URLs are temporary and confidential; the provider documents a five-minute validity period.
- The app does not bypass app anti-fraud systems or falsify device/network identity.

## Installing apps
Each cloud phone is a normal remote Android environment. Open its remote-control session and install apps inside Android through its browser/app store or the provider's supported app workflow. This build does not silently install arbitrary APKs into a remote device.

## Credential security
API ID and API Key are encrypted on-device using Android Keystore. This avoids plaintext preferences, but credentials are still used by the app on the user's device. Use a dedicated MoreLogin API key and revoke it from MoreLogin if the device is lost.

## Build
GitHub Actions builds a debug APK on pushes to main.
