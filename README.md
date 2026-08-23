![secuveri GitHub banner](./resources/secuveriBanner.jpg)

# secuveri for Android

**secuveri** is an Android identity wallet that holds verifiable KYC
credentials issued by the Secuveri trust infrastructure. Verify once, reuse
everywhere: after a single full KYC verification, users can present selected
claims (e.g. "over 18", "KYC level: full", "nationality") to any relying
party in the Secuveri trust ecosystem: cryptographically signed, selectively
disclosed, and instantly revocable, without re-running KYC or over-sharing
personal data.

Built on the OpenID for Verifiable Credentials stack:

- **Issuance:** OID4VCI (pre-authorized code flow, SD-JWT VC)
- **Presentation:** OID4VP with DCQL and selective disclosure, encrypted
  responses (`direct_post.jwt`)
- **Trust:** `did:tdw` DIDs anchored on [trust.secuveri.com](https://trust.secuveri.com);
  requests from verifiers outside the Secuveri trust environment are refused
- **Status:** IETF Token Status Lists; credentials show Valid / Suspended /
  Revoked live from the registry
- **Keys:** holder keys live in the Android Keystore (StrongBox/TEE);
  credentials are device-bound

## About this fork

This app is a fork of the official Swiss Government wallet
[swiyu-admin-ch/eidch-android-wallet](https://github.com/swiyu-admin-ch/eidch-android-wallet)
by the Federal Office of Information Technology, Systems and
Telecommunication FOITT, published under the MIT license. The fork is
deliberately **configuration-first**: the cryptography, key management and
protocol implementations are unchanged upstream code; the fork repoints the
trust infrastructure at Secuveri's self-hosted services and rebrands the app.

| | Upstream (swiyu) | This fork (secuveri) |
|---|---|---|
| Issuer | swiyu issuance service | issuer.secuveri.com (swiyu-issuer) |
| Verifier | swiyu verification service | verifier.secuveri.com (swiyu-verifier) |
| Trust registry / DIDs / status lists | `*.trust-infra.swiyu.admin.ch` | trust.secuveri.com |
| Application id | `ch.admin.foitt.swiyu` | `ch.secuveri.wallet` |

Every modified upstream file and the reasoning behind it is tracked in
[docs/FORK_NOTES.md](docs/FORK_NOTES.md).

## Installation and building

Requires at least Android 12 (S) and JDK 21.

```sh
./gradlew app:assembleDevDebug
```

The `dev`, `ref`, `abn` and `sandbox` flavors all point at the Secuveri
environment; the `prod` flavor keeps the federal defaults. The generated APK
lands under `app/build/outputs/apk/<flavor>/<buildType>/`.

For release builds you must set up your own keystore
(`RELEASE_STORE_FILE` etc. in your Gradle properties).

### URI schemes

The wallet keeps the upstream schemes for compatibility with the
swiyu-issuer/-verifier deeplink formats:
`openid-credential-offer`, `openid4vp`, `swiyu`, `swiyu-verify`, `mdoc`.

### Features intentionally disabled

Federal-infrastructure features that have no Secuveri counterpart (yet) are
switched off by configuration, not removed: e-ID request flow, beta-ID/OTP,
version enforcement, non-compliance reporting, AV/NFC, proximity
presentment. See
`app/src/main/java/ch/admin/foitt/wallet/platform/environmentSetup/data/SecuveriEnvironmentSetupRepositoryImpl.kt`.

## License

MIT, see [LICENSE](LICENSE). Original work © Swiss Confederation (FOITT);
fork modifications © Secuveri. Upstream third-party license attributions are
retained in the app's About section.
