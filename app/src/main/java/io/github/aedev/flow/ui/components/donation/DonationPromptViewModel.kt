package io.github.aedev.flow.ui.components.donation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aedev.flow.data.local.PlayerPreferences
import io.github.aedev.flow.ui.startup.LaunchPrompts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SHOW_DELAY_MS = 1_500L

/** Decides once per activity whether to ask for support, and records the answer. */
@HiltViewModel
class DonationPromptViewModel
    @Inject
    constructor(
        private val preferences: PlayerPreferences,
        private val prompts: LaunchPrompts,
    ) : ViewModel() {
        private var evaluated = false
        private val _visible = MutableStateFlow(false)
        val visible: StateFlow<Boolean> = _visible.asStateFlow()

        fun evaluate() {
            evaluated = true
            _visible.value = false
        }

        fun dismiss(dontAskAgain: Boolean) {
            _visible.value = false
            if (dontAskAgain) viewModelScope.launch { preferences.setDonationPromptDisabled(true) }
        }
    }
