package io.github.aedev.flow.ui.components.donation

import androidx.annotation.StringRes

internal const val OPEN_COLLECTIVE_URL = ""
internal const val PATREON_URL = ""

internal class CryptoWallet(
    @StringRes val name: Int,
    val ticker: String,
    val address: String,
)

/** The wallets list is cleared in SuperTube. */
internal val DonationWallets: List<CryptoWallet> = emptyList()
