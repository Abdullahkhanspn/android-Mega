package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferType

/**
 * Mega local room gateway
 *
 */
interface MegaLocalRoomGateway {
    /**
     * Save contact
     *
     * @param contact
     */
    suspend fun insertContact(contact: Contact)

    /**
     * Set contact name
     *
     * @param firstName
     * @param email
     * @return
     */
    suspend fun updateContactNameByEmail(firstName: String?, email: String?)

    /**
     * Set contact last name
     *
     * @param lastName
     * @param email
     */
    suspend fun updateContactLastNameByEmail(lastName: String?, email: String?)

    /**
     * Set contact mail
     *
     * @param handle
     * @param email
     */
    suspend fun updateContactMailByHandle(handle: Long, email: String?)

    /**
     * Set contact fist name
     *
     * @param handle
     * @param firstName
     */
    suspend fun updateContactFistNameByHandle(handle: Long, firstName: String?)

    /**
     * Set contact last name
     *
     * @param handle
     * @param lastName
     */
    suspend fun updateContactLastNameByHandle(handle: Long, lastName: String?)

    /**
     * Set contact nickname
     *
     * @param handle
     * @param nickname
     */
    suspend fun updateContactNicknameByHandle(handle: Long, nickname: String?)

    /**
     * Find contact by handle
     *
     * @param handle
     * @return
     */
    suspend fun getContactByHandle(handle: Long): Contact?

    /**
     * Find contact by email
     *
     * @param email
     * @return
     */
    suspend fun getContactByEmail(email: String?): Contact?

    /**
     * Clear contacts
     *
     */
    suspend fun deleteAllContacts()

    /**
     * Get contact count
     *
     * @return
     */
    suspend fun getContactCount(): Int

    /**
     * Get all contacts
     *
     */
    suspend fun getAllContacts(): List<Contact>

    /**
     * Get all completed transfers
     *
     * @param size the limit size of the list. If null, the limit does not apply
     */
    fun getAllCompletedTransfers(size: Int? = null): Flow<List<CompletedTransfer>>

    /**
     * Add a completed transfer
     *
     * @param transfer the completed transfer to add
     */
    suspend fun addCompletedTransfer(transfer: CompletedTransfer)

    /**
     * Get the completed transfers count
     */
    suspend fun getCompletedTransfersCount(): Int

    /**
     * Delete all completed transfers
     */
    suspend fun deleteAllCompletedTransfers()

    /**
     * Get completed transfers by state
     *
     * @param states
     * @return list of transfers match the state
     */
    suspend fun getCompletedTransfersByState(states: List<Int>): List<CompletedTransfer>

    /**
     * Delete completed transfers by state
     *
     * @param states
     * @return deleted completed transfer list
     */
    suspend fun deleteCompletedTransfersByState(states: List<Int>): List<CompletedTransfer>

    /**
     * Delete completed transfer
     *
     * @param completedTransfer
     */
    suspend fun deleteCompletedTransfer(completedTransfer: CompletedTransfer)

    /**
     * Get active transfer by tag
     */
    suspend fun getActiveTransferByTag(tag: Int): ActiveTransfer?

    /**
     * Get active transfers by type
     * @return a flow of all active transfers list
     */
    fun getActiveTransfersByType(transferType: TransferType): Flow<List<ActiveTransfer>>

    /**
     * Get active transfers by type
     * @return all active transfers list
     */
    suspend fun getCurrentActiveTransfersByType(transferType: TransferType): List<ActiveTransfer>

    /**
     * Insert a new active transfer or replace it if there's already an active transfer with the same tag
     */
    suspend fun insertOrUpdateActiveTransfer(activeTransfer: ActiveTransfer)

    /**
     * Delete all active transfer by type
     */
    suspend fun deleteAllActiveTransfersByType(transferType: TransferType)

    /**
     * Delete an active transfer by its tag
     */
    suspend fun setActiveTransferAsFinishedByTag(tags: List<Int>)

    /**
     * Get active transfer totals by type
     * @return a flow of [ActiveTransferTotals]
     */
    fun getActiveTransferTotalsByType(transferType: TransferType): Flow<ActiveTransferTotals>

    /**
     * Get active transfer totals by type
     * @return current [ActiveTransferTotals]
     */
    suspend fun getCurrentActiveTransferTotalsByType(transferType: TransferType): ActiveTransferTotals

    /**
     * Save sync record
     */
    suspend fun saveSyncRecord(record: SyncRecord)

    /**
     * Save sync records
     */
    suspend fun saveSyncRecords(records: List<SyncRecord>)

    /**
     * Sets the new Video Sync Status for Camera Uploads
     *
     * @param syncStatus The new Video Sync Status, represented as an [Int]
     */
    suspend fun setUploadVideoSyncStatus(syncStatus: Int)

    /**
     * Does file name exist
     */
    suspend fun doesFileNameExist(
        fileName: String,
        isSecondary: Boolean,
    ): Boolean

    /**
     * Does local path exist
     */
    suspend fun doesLocalPathExist(
        fileName: String,
        isSecondary: Boolean,
    ): Boolean

    /**
     * Get sync record by fingerprint
     * @return sync record
     */
    suspend fun getSyncRecordByFingerprint(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ): SyncRecord?

    /**
     * Get all pending sync records
     * @return pending sync records
     */
    suspend fun getPendingSyncRecords(): List<SyncRecord>

    /**
     * Get video sync record with status type
     */
    suspend fun getVideoSyncRecordsByStatus(syncStatusType: Int): List<SyncRecord>

    /**
     * Delete sync records by type
     */
    suspend fun deleteAllSyncRecords(syncRecordType: Int)

    /**
     * Deletes sync records.
     */
    suspend fun deleteAllSyncRecordsTypeAny()

    /**
     * Delete all Secondary Sync Records
     */
    suspend fun deleteAllSecondarySyncRecords()

    /**
     * Delete all Primary Sync Records
     */
    suspend fun deleteAllPrimarySyncRecords()

    /**
     * Get sync record by local path
     * @return sync record
     */
    suspend fun getSyncRecordByLocalPath(path: String, isSecondary: Boolean): SyncRecord?

    /**
     * Delete sync records by path
     */
    suspend fun deleteSyncRecordByPath(path: String?, isSecondary: Boolean)

    /**
     * Delete sync records by local path
     */
    suspend fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean)

    /**
     * Delete sync records by fingerprint
     */
    suspend fun deleteSyncRecordByFingerPrint(
        originalPrint: String,
        newPrint: String,
        isSecondary: Boolean,
    )

    /**
     * Update sync record status by local path
     */
    suspend fun updateSyncRecordStatusByLocalPath(
        syncStatusType: Int,
        localPath: String?,
        isSecondary: Boolean,
    )

    /**
     * Get sync record by new path
     * @return sync record
     */
    suspend fun getSyncRecordByNewPath(path: String): SyncRecord?

    /**
     * Get all syncRecord timestamps
     */
    suspend fun getAllTimestampsOfSyncRecord(isSecondary: Boolean, syncRecordType: Int): List<Long>

    /**
     * Get all sd transfers
     *
     * @return the list of sd transfers
     */
    suspend fun getAllSdTransfers(): List<SdTransfer>

    /**
     * Insert sd transfer
     *
     */
    suspend fun insertSdTransfer(transfer: SdTransfer)

    /**
     * Delete sd transfer by tag
     *
     */
    suspend fun deleteSdTransferByTag(tag: Int)

    /**
     * Get completed transfer by id
     *
     * @param id the id of the completed transfer
     */
    suspend fun getCompletedTransferById(id: Int): CompletedTransfer?
}
