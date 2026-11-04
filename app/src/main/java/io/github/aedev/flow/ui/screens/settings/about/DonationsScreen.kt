package io.github.aedev.flow.ui.screens.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import io.github.aedev.flow.ui.components.settings.SettingsPage

private val HeaderPadding = 24.dp
private val HeaderSpacing = 12.dp
private val HeroIconSize = 48.dp

/** SuperTube appreciation screen: Updated with Saim. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DonationsScreen(onNavigateBack: () -> Unit) {
    SettingsPage(
        title = stringResource(R.string.support_donations_title),
        onBack = onNavigateBack,
    ) {
        item("donations.header") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = HeaderPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HeaderSpacing),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_notification_logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(HeroIconSize),
                )
                Text(
                    text = stringResource(R.string.support_flow_dev_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "SuperTube is a high-performance, privacy-respecting client for Android, updated and maintained with Saim.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        group(key = "donations.info", header = R.string.section_app) {
            row("donations.status") { shape ->
                SegmentedListItem(
                    onClick = {},
                    verticalAlignment = Alignment.CenterVertically,
                    shapes = ListItemDefaults.shapes(shape = shape),
                    colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    leadingContent = {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    overlineContent = { Text(stringResource(R.string.app_name)) },
                    supportingContent = { Text("Ad-free, tracking-free streaming with on-device recommendations.") },
                ) {
                    Text(stringResource(R.string.support_donations_title), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
