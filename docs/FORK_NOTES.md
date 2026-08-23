# Secuveri Wallet (Android) - Fork Notes

Fork of `swiyu-admin-ch/eidch-android-wallet` repointed at the Secuveri trust
infrastructure (`issuer.secuveri.com`, `verifier.secuveri.com`,
`trust.secuveri.com`). This file is the upgrade map: every upstream file we
touch is listed at the bottom with the reason. Keep it current.

Companion fork: `secuveri/eidch-ios-wallet` (see its `docs/FORK_NOTES.md`);
both wallets talk to the same backend stack and the same trust model applies.

## Module map (upstream, unmodified)

Gradle modules:
- `app` - the wallet application; all feature and platform code lives in
  package trees under `ch.admin.foitt.wallet.{feature,platform}` (kept as-is,
  package names are technical identifiers)
- `openid4vc` - OID4VCI / OID4VP protocol library
- `theme` - Compose theme (colors, typography)
- `build-logic/convention` - convention plugins (`swiyu.android.application`),
  Java 21 toolchain, minSdk 31, compileSdk 37

Flavors (dimension `environment`): `dev`, `ref`, `abn`, `sandbox`, `prod`,
with `applicationIdSuffix` `.dev`/`.ref`/`.abn`/`.sandbox` and per-flavor
source sets under `app/src/<flavor>/`.

## Configuration model (the key to this fork)

All environment and trust configuration is one Hilt-injected interface:
`EnvironmentSetupRepository`
(`app/src/main/java/ch/admin/foitt/wallet/platform/environmentSetup/domain/repository/EnvironmentSetupRepository.kt`).

Selection is a Hilt multibinding `Map<Int, EnvironmentSetupRepository>`; the
highest `IntKey` wins (`EnvironmentSetupModule.provideEnvironmentSetup`):

- `MainEnvironmentSetupRepositoryImpl` at `IntKey(Int.MIN_VALUE)` carries the
  federal production values (`*.trust-infra.swiyu.admin.ch`).
- Upstream only ships a flavor override for `sandbox` (`IntKey(1)`); the
  dev/ref/abn flavors silently fall back to the federal production trust
  infrastructure in the OSS publish.
- Our fork adds `SecuveriEnvironmentSetupRepositoryImpl` (main source set) and
  provides it at `IntKey(10)` from the dev/ref/abn/sandbox flavor source sets.
  It wins over both Main and Sandbox. The `prod` flavor has no override and
  keeps the federal defaults.

## (a) DID resolution

DID resolution derives the DID-log URL from the `did:tdw`/`did:webvh` DID
itself and fetches it over HTTPS; there is no hostname allowlist, so
`did:tdw:...:trust.secuveri.com:dids:issuer` resolves as-is. No library fork
needed.

## (b) Trust decisions

- `GetActorEnvironment` classifies a DID against the injected
  `trustEnvironmentDidRegex` / `demoTrustEnvironmentDidRegex`; anything else
  is `ActorEnvironment.EXTERNAL`.
- Verifier hard gate: `ValidatePresentationRequestImpl` (lines 98-103)
  rejects presentation requests whose verifier DID classifies as EXTERNAL
  with `CredentialPresentationError.UnknownRegistry`. This is the repoint
  target and our negative test.
- Verifiers signing over the DID path get `verifierAttestationTrusted=null`;
  identity trust statements are then processed non-blocking. This is why
  empty `attestationServiceMapping` / `attestationsServiceTrustedDids` are
  safe: there is no Secuveri attestation service and none is required.
- Issuer trust badges: trust and status lookups go through the injected
  `trustRegistryMapping` / `statusListMapping`, both mapping
  `trust.secuveri.com` to itself. Failures degrade to untrusted/unknown
  badges, issuance is not blocked.

## (c) Feature flags in the Secuveri environment

Federal-only flows are switched off in `SecuveriEnvironmentSetupRepositoryImpl`:
e-ID request, beta-ID/OTP, non-compliance, version enforcement, AV/NFC,
proximity presentment. Their base URLs are `wallet.secuveri.com/unused/...`
placeholders kept only because the interface requires values.
`batchIssuanceEnabled` stays true. `prod` flavor is untouched.

## (d) Key storage

Android Keystore usage (StrongBox/TEE-backed keys) is untouched by this fork.

## (e) applicationId / branding

- `applicationId` is `ch.secuveri.wallet` (+ flavor suffixes),
  `testApplicationId` `ch.secuveri.wallet.test` (`app/build.gradle.kts`).
- `app/google-services.json` is the upstream placeholder Firebase project
  (project_id `placeholder`, dummy keys); only the `package_name` entries were
  renamed to match the new applicationIds, because the google-services plugin
  fails the build without a matching client entry. Push is effectively dead
  (`notificationBackendUrl` is an unused placeholder).
- Technical identifiers are NOT renamed: deeplink schemes
  (`openid-credential-offer`, `swiyu`, `openid4vp`, `swiyu-verify`, `mdoc`,
  https), Kotlin package names, DI keys, module names.
- License: MIT (Swiss Confederation); attribution retained.

## Modified upstream files

(Every change gets a line: path - why.)

- `docs/FORK_NOTES.md` - new; this file.
- `app/src/main/java/ch/admin/foitt/wallet/platform/environmentSetup/data/SecuveriEnvironmentSetupRepositoryImpl.kt`
  - new; the Secuveri environment (trust.secuveri.com repoint, federal
  features off). The entire trust repoint of this fork.
- `app/src/{dev,ref,abn,sandbox}/java/ch/admin/foitt/wallet/platform/environmentSetup/di/SecuveriEnvironmentSetupRepositoryModule.kt`
  - new (4 identical files); provide the Secuveri environment at `IntKey(10)`
  so it outranks Main (`Int.MIN_VALUE`) and Sandbox (`1`).
- `app/build.gradle.kts` - applicationId `ch.admin.foitt.swiyu` ->
  `ch.secuveri.wallet`, testApplicationId accordingly. Flavor suffixes,
  deeplink schemes and manifest placeholders unchanged.
- `app/google-services.json` - placeholder Firebase client entries renamed to
  the `ch.secuveri.wallet*` package names (build requirement, see above).
