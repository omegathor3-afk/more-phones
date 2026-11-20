# MORE PHONES service

This Vercel Node.js API is the private control plane for the Android app. MoreLogin credentials are server-side only and must never be placed in the APK.

## Required server environment variables
- `MORELOGIN_CLIENT_ID`
- `MORELOGIN_CLIENT_SECRET`

Set both as encrypted Vercel Environment Variables before deploying. Without them, endpoints return 503 and no cloud phone can be created.

## Routes
- `GET /api/phones?installId=<UUID>`
- `POST /api/phones/create` JSON: `{"installId":"<UUID>"}`
- `POST /api/phones/:id/power` JSON: `{"installId":"<UUID>","on":true}`
- `POST /api/phones/:id/link` JSON: `{"installId":"<UUID>"}`

Provider device remarks are namespaced with the app installation UUID so one installation does not list/control another installation's phones. Each installation is capped at 10 phones.

## Security / production gate
This first service slice keeps provider credentials off-device and validates ownership before control operations. Before opening public registration at scale, add durable per-installation rate limiting and abuse protection (for example Vercel Firewall/BotID plus a durable store). Do not expose the provider API secret in Android resources, BuildConfig, or source control.
