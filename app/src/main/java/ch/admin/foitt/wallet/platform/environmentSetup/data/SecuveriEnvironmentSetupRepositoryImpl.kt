package ch.admin.foitt.wallet.platform.environmentSetup.data

import ch.admin.foitt.wallet.BuildConfig
import ch.admin.foitt.wallet.platform.environmentSetup.domain.repository.EnvironmentSetupRepository
import ch.admin.foitt.wallet.platform.trustRegistry.domain.model.IdentityV2TrustStatement
import ch.admin.foitt.wallet.platform.trustRegistry.domain.model.NonComplianceTrustListStatement
import ch.admin.foitt.wallet.platform.trustRegistry.domain.model.ProtectedIssuanceAuthorizationTrustStatement
import ch.admin.foitt.wallet.platform.trustRegistry.domain.model.ProtectedIssuanceTrustListStatement
import ch.admin.foitt.wallet.platform.trustRegistry.domain.model.VerificationAuthorizationTrustStatement
import ch.admin.foitt.wallet.platform.trustRegistry.domain.model.VerificationQueryPublicStatement
import javax.inject.Inject

/**
 * Secuveri environment setup.
 *
 * Upstream, the dev/ref/abn flavors fall back to [MainEnvironmentSetupRepositoryImpl]
 * (federal production trust infrastructure) because only sandbox provides a
 * flavor override. This repository is provided with a higher priority by the
 * dev/ref/abn/sandbox flavors and repoints the wallet at the Secuveri trust
 * infrastructure (trust.secuveri.com); prod keeps the federal defaults.
 *
 * - [trustEnvironmentDidRegex] accepts DIDs anchored on trust.secuveri.com;
 *   anything else classifies as EXTERNAL and is rejected by the verifier hard
 *   gate in ValidatePresentationRequestImpl.
 * - [trustRegistryMapping] / [statusListMapping] route trust and status
 *   lookups to trust.secuveri.com.
 * - [attestationServiceMapping] and [attestationsServiceTrustedDids] are empty:
 *   there is no Secuveri attestation service yet, and verifiers are validated
 *   over the DID path.
 * - Federal e-ID request, beta-ID/OTP, non-compliance and version-enforcement
 *   flows are switched off; their base URLs are unused placeholders kept only
 *   because the interface requires values.
 */
class SecuveriEnvironmentSetupRepositoryImpl @Inject constructor() : EnvironmentSetupRepository {
    private val secuveriTrustDomain = "trust.secuveri.com"

    private val secuveriIssuerDid =
        "did:tdw:QmPFzq6b7NfJrWhgU7EoE6Z1t9aiGgVsuKHeW3BXYC7YoA:trust.secuveri.com:dids:issuer"

    override val userAgent: String = "secuveri-wallet"

    override val appVersionEnforcementUrl: String =
        "https://wallet.secuveri.com/api/versioning?platform=android&app_id=wallet"

    override val defaultAttestationServiceUrl = "https://wallet.secuveri.com/unused/attestations"

    override val attestationServiceMapping: Map<String, String> = emptyMap()

    override val attestationsServiceTrustedDids: List<String> = emptyList()

    override val trustRegistryMapping: Map<String, String> = mapOf(
        secuveriTrustDomain to secuveriTrustDomain,
    )

    override val statusListMapping: Map<String, String> = mapOf(
        secuveriTrustDomain to secuveriTrustDomain,
    )

    override val trustV1TrustRegistryTrustedDids: Map<String, List<String>> = mapOf(
        secuveriTrustDomain to listOf(secuveriIssuerDid),
    )

    override val trustRegistryTrustedDids: Map<String, Map<String, List<String>>> = mapOf(
        secuveriTrustDomain to mapOf(
            VerificationQueryPublicStatement.TYPE to listOf(secuveriIssuerDid),
            IdentityV2TrustStatement.TYPE to listOf(secuveriIssuerDid),
            ProtectedIssuanceTrustListStatement.TYPE to listOf(secuveriIssuerDid),
            ProtectedIssuanceAuthorizationTrustStatement.TYPE to listOf(secuveriIssuerDid),
            NonComplianceTrustListStatement.TYPE to listOf(secuveriIssuerDid),
            VerificationAuthorizationTrustStatement.TYPE to listOf(secuveriIssuerDid),
        ),
    )

    override val trustEnvironmentDidRegex: String = "^did:(?:tdw|webvh):[^:]+:trust\\.secuveri\\.com:.*"

    override val demoTrustEnvironmentDidRegex: String = "^did:example:never-matches$"

    override val baseTrustDomainRegex =
        Regex("^did:tdw:[^:]+:([^:]+\\.secuveri\\.com):[^:]+", setOf(RegexOption.MULTILINE))

    override val notificationBackendUrl = "https://wallet.secuveri.com/unused/push"

    override val betaIdRequestEnabled = false

    override val eIdRequestEnabled = false

    override val eIdMockMrzEnabled = false

    override val sidBackendUrl: String = "https://wallet.secuveri.com/unused/sid"

    override val avBackendUrl: String = "https://wallet.secuveri.com/unused/av"

    override val eIdNfcWebSocketUrl: String = "wss://wallet.secuveri.com/unused/av-socket"

    override val appId: String = BuildConfig.APPLICATION_ID

    override val avBeamLoggingEnabled: Boolean = false

    override val nonComplianceEnabled: Boolean = false

    override val nonComplianceBaseUrl: String = "https://wallet.secuveri.com/unused/non-compliance"

    override val batchIssuanceEnabled: Boolean = true

    override val allowBypassOtp: Boolean = false

    override val isLottieViewerEnabled: Boolean = false

    override val devsSettingsEnabled: Boolean = false

    override val isProximityEngagementEnabled: Boolean = false

    override val isVersionEnforcementEnabled: Boolean = false

    override val terminateOnInvalidIdTSEnabled: Boolean = false
}
