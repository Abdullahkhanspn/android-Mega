package mega.privacy.android.app.presentation.manager.model

/**
 * Manager UI state
 *
 * @param browserParentHandle current browser parent handle
 * @param rubbishBinParentHandle current rubbish bin parent handle
 * @param incomingParentHandle current incoming parent handle
 * @param outgoingParentHandle current outgoing parent handle
 * @param linksParentHandle current links parent handle
 * @param inboxParentHandle current inbox parent handle
 * @param isFirstNavigationLevel true if the navigation level is the first level
 * @param incomingTreeDepth current incoming tree depth
 * @param outgoingTreeDepth current outgoing tree depth
 * @param linksTreeDepth current links tree depth
 */
data class ManagerState(
    val browserParentHandle: Long = -1L,
    val rubbishBinParentHandle: Long = -1L,
    val incomingParentHandle: Long = -1L,
    val outgoingParentHandle: Long = -1L,
    val linksParentHandle: Long = -1L,
    val inboxParentHandle: Long = -1L,
    val isFirstNavigationLevel: Boolean = true,
    val incomingTreeDepth: Int = 0,
    val outgoingTreeDepth: Int = 0,
    val linksTreeDepth: Int = 0,
)