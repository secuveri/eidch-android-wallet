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

## Phase 2 runtime verification (2026-08-23, Samsung device, dev flavor)

- Issuance E2E: pre-auth OID4VCI offer from issuer.secuveri.com accepted;
  credential shows with status badge "Valid" (token status list on
  trust.secuveri.com, same list as the iOS fork).
- Presentation E2E: DCQL request from verifier.secuveri.com
  (`response_mode: direct_post.jwt`), selective disclosure of 3 claims,
  verifier state SUCCESS with decrypted claims. The verifier itself must be
  allowed to fetch the status list: swiyu-verifier's
  `application.accepted-registry-hosts` defaults to federal hosts only, which
  produced `unresolvable_status_list` until the deployment was overridden with
  `{"application":{"accepted-registry-hosts":["trust.secuveri.com"]}}`.
- Negative test: a live federal Beta-ID verification request (verifier DID on
  `identifier-reg.trust-infra.swiyu-int.admin.ch`) is blocked by the
  `trustEnvironmentDidRegex` gate in `ValidatePresentationRequestImpl` with
  `unknown_registry` ("Request blocked").
- Deeplink invocation note: `swiyu-verify://?client_id=<url-encoded>&
  request_uri=<url-encoded>`; `client_id` must match the request object's
  `client_id` exactly, including the `decentralized_identifier:` prefix.

## Phase 3 rebrand decisions

Mirrors the iOS fork's rebrand (see its `docs/FORK_NOTES.md`).

- Display name: "secuveri" (per-flavor labels in `app/build.gradle.kts`
  manifest placeholders). Prose uses "secuveri app" / "secuveri wallet";
  German keeps "App"/"Wallet" capitalized (noun grammar).
- Strings ×5 (`values`, `values-de`, `values-fr`, `values-it`, `values-rm`):
  product name swiyu -> secuveri, federal actors (Confederation, Federal
  Office of Justice, FOITT/BIT/UFIT/OFIT) -> Secuveri, publisher -> Secuveri
  GmbH (link www.secuveri.com), privacy/terms links -> secuveri.com/privacy
  and /terms (placeholders), GitHub -> github.com/secuveri.
  Kept unchanged: still-resolving federal help URLs (help-swiyu-safety,
  swiyu-informs), Play Store links (`ch.admin.foitt.swiyu`), federal e-ID
  OTP test-phase strings (disabled feature, inherently federal), and all
  technical identifiers (URL schemes, package names).
- Colors: warm swiyu palette -> secuveri blue in `theme/WalletColors.kt`
  (`purple18/27/91`, `accentPurple`, `gradientPink`; constant names kept)
  and in 55 vector drawables (`#F01ADB` -> `#1A65E5`, `#500A5A` ->
  `#0A2A5A`). Semantic reds (`#EA1C21`, `#C00012` family) and the
  per-language federal logos in `wallet_ic_bj_info.xml` (disabled e-ID flow)
  untouched.
- Gradient PNGs (`wallet_background_gradient_04..07`,
  `wallet_ic_nocredential_bg`, hdpi/xhdpi/xxhdpi) hue-remapped into the blue
  band with the same script as the iOS recolor (greys/whites preserved).
- Launcher icons (all 5 flavors): flat `#1A65E5` background + white secuveri
  check foreground. Splash (`splash_icon.xml`): blue check on the white
  splash background.
- Not rebranded: `network_security_config.xml` federal pin domains (pins are
  additive, they do not block secuveri hosts), the online-verification SDK
  license text (`res/raw`, third-party legal text), upstream
  `SandboxEnvironmentSetupRepositoryImpl` (outranked by IntKey(10)).

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
- `app/build.gradle.kts` (again, Phase 3) - `appLabel` manifest placeholders
  swiyu -> secuveri per flavor.
- `app/src/main/res/values{,-de,-fr,-it,-rm}/strings.xml` - content rebrand,
  see "Phase 3 rebrand decisions".
- `theme/src/main/kotlin/ch/admin/foitt/wallet/theme/WalletColors.kt` - warm
  brand constants remapped to secuveri blue (names kept).
- 55 vector drawables under `app/src/main/res/drawable*/` - `#F01ADB` ->
  `#1A65E5`, `#500A5A` -> `#0A2A5A`.
- `app/src/{main,dev,ref,abn,sandbox}/res/drawable/ic_launcher_background.xml`
  + `ic_launcher_foreground.xml` - secuveri launcher icon (blue + white
  check).
- `app/src/main/res/drawable/splash_icon.xml` - Swiss cross shield -> blue
  secuveri check.
- `app/src/main/res/drawable/wallet_ic_dotted_cross.xml` (white dotted swiyu
  cross, login/lock screens and fallback actor logo),
  `ic_swiss_cross_small.xml` (red Swiss shield, home navigation icon),
  `wallet_ic_swiss_cross.xml` (white cross, credential logo placeholder and
  notification small icon) - all -> secuveri check, same footprint/colors
  (white/white/blue).
- `app/src/main/res/drawable{,-night}/wallet_ic_bj_info.xml` - the federal
  "Schweizerische Eidgenossenschaft / Bundesamt für Justiz" logo shown on
  the Legal notice screen -> secuveri lockup (black/white wordmark from the
  iOS fork's logo SVGs). The 8 per-language variants
  (`drawable-{de,fr,it,rm}{,-night}`) are deleted: the lockup is
  language-independent, base + night suffice.
- `app/src/main/res/drawable-{hdpi,xhdpi,xxhdpi}/wallet_background_gradient_{04..07}.png`
  + `wallet_ic_nocredential_bg.png` - hue-remapped to the blue band.
- `theme/src/main/kotlin/ch/admin/foitt/wallet/theme/WalletColors.kt` +
  `WalletColorScheme.kt` - new `link` scheme color (`linkBlue` #1A65E5 light,
  `linkBlueLight` #8AB6F5 dark); upstream styled links with the semantic
  `error` red.
- `app/.../platform/composables/Buttons.kt` (TextLink),
  `app/.../feature/settings/presentation/composables/SettingsItem.kt`,
  `app/.../platform/badges/presentation/BadgesBottomSheet.kt`,
  `app/.../platform/nonCompliance/presentation/NonComplianceInfoScreen.kt` -
  link text/chevron color `colorScheme.error` -> `colorScheme.link`. Real
  error usages (passphrase errors, suspended badge, delete buttons) keep red.
- `README.md` - rewritten for the fork; `resources/swiyuBanner.jpg` replaced
  by `resources/secuveriBanner.jpg`.
