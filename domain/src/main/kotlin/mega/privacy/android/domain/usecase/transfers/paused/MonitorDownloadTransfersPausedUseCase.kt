package mega.privacy.android.domain.usecase.transfers.paused

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to get a flow that determines whether the transfer queue is paused or if all individual Download transfers in progress are paused.
 */
class MonitorDownloadTransfersPausedUseCase @Inject constructor(
    override val transferRepository: TransferRepository,
) : MonitorTypeTransfersPausedUseCase() {

    override fun isCorrectType(transfer: Transfer) =
        transfer.transferType == TransferType.TYPE_DOWNLOAD && !transfer.isFolderTransfer

    override suspend fun totalPendingIndividualTransfers() =
        transferRepository.getCurrentActiveTransferTotalsByType(TransferType.TYPE_DOWNLOAD).pendingFileTransfers

    override suspend fun totalPausedIndividualTransfers() =
        transferRepository.getCurrentActiveTransferTotalsByType(TransferType.TYPE_DOWNLOAD).pausedFileTransfers
}