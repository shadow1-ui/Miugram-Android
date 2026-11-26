# Miugram for Android

Miugram is an unofficial Telegram client based on the official upstream source. The upstream README, license and third-party notices remain applicable.

## Baseline

- Upstream: https://github.com/TelegramOrg/Telegram-Android
- Initial commit: `21f7d109da6ab3506c9e9577f98c6a38771ab335`
- Project assets in `miugram/assets/logos`: the four supplied concepts and original composite. Four launcher icon resources have been generated from these concepts; the cat-and-plane variant is the default. They have not yet been tested on an Android device.

## Current status

An initial Miugram settings entry exposes account ID and current DC when available and links to the app icon selector. Four user-supplied logo concepts have adaptive and legacy launcher resources; the cat-and-plane variant is the default. A profile username tap/long-press menu now offers copying a public link and an ID deep link, with an explicit access limitation; the same actions are available in the own-profile username menu. ID links do not bypass Telegram privacy and may not resolve for recipients who do not know the user. This work is unbuilt and untested on a device. No tested Miugram builds or downloadable releases are available. The official API credentials are no longer used: builds read `MIUGRAM_TELEGRAM_API_ID` and `MIUGRAM_TELEGRAM_API_HASH` from the environment; a build without them cannot sign in. The common upstream update-check entry point and official-app passkeys are disabled. Other update paths still require an audit. The Telegram package/signing identity, Firebase configuration and remaining branding still need to be replaced before distribution. An API hash is embedded in an APK and should not be treated as a secret from recipients. Never commit credentials or signing keys. Follow the upstream build instructions and supply your own Telegram API ID and hash.

## Planned features

A built-in graphite and muted-pink Android theme is included for first launch, but has not been visually checked in the app; further cohesive design and fast, consistent animations remain; a dedicated Miugram settings area; account ID and real current DC; user links with privacy limitations; configurable message status indicators; local profile background with cropping; and selectable app icons where supported. Custom interface icon design is deferred pending approval.

The requested option to retain locally received messages deleted by others must be clearly marked, safely stored and deletable. It cannot recover messages never received and must not bypass self-destructing or protected content.

## Experimental ghost mode (Android, not yet tested)

The per-account switch defaults off. While on, it attempts to suppress ordinary cloud-chat history and discussion read requests, normal media/mention read requests, mark-all mentions/reactions and live-location content reads. It rechecks queued history reads at send time. Unread badges can remain visible because the local read state is not advanced. It intentionally leaves secret chats and expiring media read/delete flows unchanged. Stories, typing/online status, outgoing messages, other sessions, already sent requests and actions queued before or across toggles can still reveal activity or change read state. Not a guarantee of invisibility. Real two-account network tests, including TTL, toggles and race cases, are mandatory before release; this code has not yet been compiled or device-tested.

Telegram's API terms explicitly prohibit manipulating read statuses, including ghost mode; violation can result in loss of API access. See https://core.telegram.org/api/terms.

## Build verification

The `Miugram Android debug build` workflow attempts an upstream-derived debug compilation. It does not publish its output and is not a release pipeline. A successful compile does not verify login, notifications, calls, user data isolation, or runtime behavior. Own API credentials, app identity, signing, and device testing remain required.

## Telegram API credentials

Obtain a separate API ID and hash at https://core.telegram.org/api/obtaining_api_id. Set `MIUGRAM_TELEGRAM_API_ID` and `MIUGRAM_TELEGRAM_API_HASH` in the build environment (use protected CI secrets for private builds). Do not commit them. An empty configuration is only suitable for compilation checks and cannot authenticate. Builds and third-party Firebase settings are not yet production-ready.

## Experimental message checkmarks (Android)

Miugram settings offers classic and rounded checkmark styles. The selection is stored locally and applies after restarting the app. The underlying sent/read status logic is unchanged. Resources and translations are checked statically; actual appearance and state transitions have not yet been verified on a device.

## Experimental local profile background (Android)

Miugram settings can choose an image through the system picker, crop it manually or accept the initial frame, and remove it. The cropped image is stored in the app-private, non-backed-up directory per account, never uploaded by this feature. The own-profile header uses a center crop and dark overlay. The settings also offer a profile preview after applying the crop; the automatic framing is geometric center crop, not subject-aware. This code has not been compiled or checked on a device; layout, readability, permissions, cancellation and low-memory behavior still require testing.

The debug CI workflow reads the Telegram API ID and hash from GitHub Actions secrets. It still does not publish a release or prove that login works. The hash is embedded in the resulting application and is extractable from it.

## Standalone signing gate

A separate signing key was generated for Miugram and kept outside the repositories, with a copy in GitHub Actions secrets. The standalone Gradle variant fails closed unless `MIUGRAM_ANDROID_KEYSTORE_PATH`, `MIUGRAM_ANDROID_KEYSTORE_PASSWORD`, `MIUGRAM_ANDROID_KEY_ALIAS`, a non-Telegram `MIUGRAM_ANDROID_APPLICATION_ID`, and the Telegram API pair are supplied. `MIUGRAM_ANDROID_KEY_PASSWORD` can override the store password. The application ID requires a matching, independently configured Firebase project; the checked-in upstream configuration must not be used for distribution. A signing key is not a tested release, and the identity, push notifications, signing, device behavior and update path still require validation. Keep a secure backup of the private key and its password; losing them prevents signing compatible updates.

The standalone build also rejects the checked-in upstream Firebase project and requires an independently configured `TMessagesProj_AppStandalone/google-services.json` containing a client for the final installed package (`MIUGRAM_ANDROID_APPLICATION_ID` plus `.web`). This is a configuration gate, not a verification of Firebase push delivery or an Android release.
