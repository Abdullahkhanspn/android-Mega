package mega.privacy.android.app.presentation.offline.offlinecompose.model

import mega.privacy.android.domain.entity.offline.OfflineNodeInformation

/**
 * UI state for the OfflineComposeViewModel
 * @param isLoading UI state to show the loading state
 * @param showOfflineWarning UI state to show the offline warning
 * @param offlineNodes The offline nodes fetched from the database
 * @param selectedNodeHandles The selected nodes when the view is in the selecting mode
 *
 *
 */
data class OfflineUIState(
    val isLoading: Boolean = false,
    val showOfflineWarning: Boolean = false,
    val offlineNodes: List<OfflineNodeUIItem<OfflineNodeInformation>> = emptyList(),
    val selectedNodeHandles: List<String> = emptyList(),
)