package io.github.aedev.flow.ui.components.donation

import androidx.compose.runtime.Composable

/** Disabled in SuperTube: donation prompt never appears. */
@Composable
fun DonationPromptHost(
    enabled: Boolean,
    onNavigateToDonations: () -> Unit,
) {
    // Intentionally no-op: donation prompts are disabled in SuperTube
}
