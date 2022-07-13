package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.FolderVersionInfo
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaNode

/**
 * Files repository
 *
 */
interface FilesRepository {
    /**
     * Get folder version info
     *
     * @return info
     */
    @Throws(MegaException::class)
    suspend fun getRootFolderVersionInfo(): FolderVersionInfo

    /**
     * Monitor node updates
     *
     * @return a flow of all global node updates
     */
    fun monitorNodeUpdates(): Flow<List<MegaNode>>

    /**
     * Get the root node
     *
     * @return A node corresponding to the root node, null if cannot be retrieved
     */
    suspend fun getRootNode(): MegaNode?

    /**
     * Get the rubbish root node
     *
     * @return A node corresponding to the rubbish bin node, null if cannot be retrieved
     */
    suspend fun getRubbishBinNode(): MegaNode?

    /**
     * Get children of a parent node
     *
     * @param parentNode parent node
     * @param order order for the returned list
     * @return Children nodes of a parent node
     */
    suspend fun getChildrenNode(parentNode: MegaNode, order: Int? = null): List<MegaNode>

    /**
     * Get the node corresponding to a handle
     *
     * @param handle
     */
    suspend fun getNodeByHandle(handle: Long): MegaNode?

    /**
     * Get cloud sort order
     * @return cloud sort order
     */
    suspend fun getCloudSortOrder(): Int

    /**
     * Get camera sort order
     * @return camera sort order
     */
    suspend fun getCameraSortOrder(): Int

    /**
     * Checks if Inbox node has children.
     *
     * @return True if Inbox has children, false otherwise.
     */
    suspend fun hasInboxChildren(): Boolean

    /**
     * Downloads a file node in background.
     *
     * @param node  File node to download.
     * @return The local path of the downloaded file.
     */
    suspend fun downloadBackgroundFile(node: MegaNode): String
}
