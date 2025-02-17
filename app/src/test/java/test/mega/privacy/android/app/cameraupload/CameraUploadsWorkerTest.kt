package test.mega.privacy.android.app.cameraupload

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.foreground.ForegroundProcessor
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.WorkProgressUpdater
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.cameraupload.CameraUploadsWorker
import mega.privacy.android.data.mapper.transfer.CompletedTransferMapper
import mega.privacy.android.data.wrapper.ApplicationWrapper
import mega.privacy.android.data.wrapper.CameraUploadsNotificationManagerWrapper
import mega.privacy.android.data.wrapper.CookieEnabledCheckWrapper
import mega.privacy.android.domain.usecase.BroadcastCameraUploadProgress
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.CompressVideos
import mega.privacy.android.domain.usecase.CompressedVideoPending
import mega.privacy.android.domain.usecase.CreateCameraUploadFolder
import mega.privacy.android.domain.usecase.CreateCameraUploadTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.CreateTempFileAndRemoveCoordinatesUseCase
import mega.privacy.android.domain.usecase.DeleteSyncRecord
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetPendingSyncRecords
import mega.privacy.android.domain.usecase.GetVideoSyncRecordsByStatus
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsNotEnoughQuota
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.IsWifiNotSatisfiedUseCase
import mega.privacy.android.domain.usecase.MonitorBatteryInfo
import mega.privacy.android.domain.usecase.MonitorChargingStoppedState
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import mega.privacy.android.domain.usecase.SetSyncRecordPendingByPath
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.BroadcastCameraUploadsSettingsActionUseCase
import mega.privacy.android.domain.usecase.camerauploads.BroadcastStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.camerauploads.DeleteCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.DisableCameraUploadsUseCase
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetDefaultNodeHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadFolderHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.HandleLocalIpChangeUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsPrimaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderSetUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.camerauploads.ProcessMediaForUploadUseCase
import mega.privacy.android.domain.usecase.camerauploads.SendBackupHeartBeatSyncUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCoordinatesUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetOriginalFingerprintUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetSecondaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupPrimaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateCameraUploadsBackupHeartbeatStatusUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateCameraUploadsBackupStatesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.monitoring.StartTracePerformanceUseCase
import mega.privacy.android.domain.usecase.monitoring.StopTracePerformanceUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DeletePreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DeleteThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.completed.AddCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.CancelAllUploadTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.ResetTotalUploadsUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.StartUploadUseCase
import mega.privacy.android.domain.usecase.workers.ScheduleCameraUploadUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Test class of [CameraUploadsWorker]
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class CameraUploadsWorkerTest {

    private lateinit var underTest: CameraUploadsWorker

    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val isNotEnoughQuota: IsNotEnoughQuota = mock()
    private val ioDispatcher = UnconfinedTestDispatcher()
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase = mock()
    private val isPrimaryFolderPathValidUseCase: IsPrimaryFolderPathValidUseCase = mock()
    private val isSecondaryFolderSetUseCase: IsSecondaryFolderSetUseCase = mock()
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled = mock()
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase = mock()
    private val isWifiNotSatisfiedUseCase: IsWifiNotSatisfiedUseCase = mock()
    private val deleteSyncRecord: DeleteSyncRecord = mock()
    private val deleteSyncRecordByLocalPath: DeleteSyncRecordByLocalPath = mock()
    private val deleteSyncRecordByFingerprint: DeleteSyncRecordByFingerprint = mock()
    private val setPrimaryFolderLocalPathUseCase: SetPrimaryFolderLocalPathUseCase = mock()
    private val shouldCompressVideo: ShouldCompressVideo = mock()
    private val setSecondaryFolderLocalPathUseCase: SetSecondaryFolderLocalPathUseCase = mock()
    private val clearSyncRecords: ClearSyncRecords = mock()
    private val areLocationTagsEnabledUseCase: AreLocationTagsEnabledUseCase = mock()
    private val getPendingSyncRecords: GetPendingSyncRecords = mock()
    private val compressedVideoPending: CompressedVideoPending = mock()
    private val getVideoSyncRecordsByStatus: GetVideoSyncRecordsByStatus = mock()
    private val setSyncRecordPendingByPath: SetSyncRecordPendingByPath = mock()
    private val isChargingRequired: IsChargingRequired = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val processMediaForUploadUseCase: ProcessMediaForUploadUseCase = mock()
    private val getUploadFolderHandleUseCase: GetUploadFolderHandleUseCase = mock()
    private val setPrimarySyncHandle: SetPrimarySyncHandle = mock()
    private val setSecondarySyncHandle: SetSecondarySyncHandle = mock()
    private val getDefaultNodeHandleUseCase: GetDefaultNodeHandleUseCase = mock()
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase = mock()
    private val monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val monitorBatteryInfo: MonitorBatteryInfo = mock()
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase = mock()
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase = mock()
    private val monitorChargingStoppedState: MonitorChargingStoppedState = mock()
    private val handleLocalIpChangeUseCase: HandleLocalIpChangeUseCase = mock()
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase = mock()
    private val cancelAllUploadTransfersUseCase: CancelAllUploadTransfersUseCase = mock()
    private val copyNodeUseCase: CopyNodeUseCase = mock()
    private val setOriginalFingerprintUseCase: SetOriginalFingerprintUseCase = mock()
    private val startUploadUseCase: StartUploadUseCase = mock()
    private val createCameraUploadFolder: CreateCameraUploadFolder = mock()
    private val setupPrimaryFolderUseCase: SetupPrimaryFolderUseCase = mock()
    private val setupSecondaryFolderUseCase: SetupSecondaryFolderUseCase = mock()
    private val establishCameraUploadsSyncHandlesUseCase: EstablishCameraUploadsSyncHandlesUseCase =
        mock()
    private val resetTotalUploadsUseCase: ResetTotalUploadsUseCase = mock()
    private val disableCameraUploadsUseCase: DisableCameraUploadsUseCase = mock()
    private val compressVideos: CompressVideos = mock()
    private val resetMediaUploadTimeStamps: ResetMediaUploadTimeStamps = mock()
    private val disableMediaUploadSettings: DisableMediaUploadSettings = mock()
    private val createCameraUploadTemporaryRootDirectoryUseCase: CreateCameraUploadTemporaryRootDirectoryUseCase =
        mock()
    private val deleteCameraUploadsTemporaryRootDirectoryUseCase: DeleteCameraUploadsTemporaryRootDirectoryUseCase =
        mock()
    private val broadcastCameraUploadProgress: BroadcastCameraUploadProgress = mock()
    private val scheduleCameraUploadUseCase: ScheduleCameraUploadUseCase = mock()
    private val createTempFileAndRemoveCoordinatesUseCase: CreateTempFileAndRemoveCoordinatesUseCase =
        mock()
    private val updateCameraUploadsBackupStatesUseCase: UpdateCameraUploadsBackupStatesUseCase =
        mock()
    private val sendBackupHeartBeatSyncUseCase: SendBackupHeartBeatSyncUseCase = mock()
    private val updateCameraUploadsBackupHeartbeatStatusUseCase: UpdateCameraUploadsBackupHeartbeatStatusUseCase =
        mock()
    private val addCompletedTransferUseCase: AddCompletedTransferUseCase = mock()
    private val completedTransferMapper: CompletedTransferMapper = mock()
    private val setCoordinatesUseCase: SetCoordinatesUseCase = mock()
    private val isChargingUseCase: IsChargingUseCase = mock()
    private val monitorStorageOverQuotaUseCase: MonitorStorageOverQuotaUseCase = mock()
    private val broadcastStorageOverQuotaUseCase: BroadcastStorageOverQuotaUseCase = mock()
    private val createImageOrVideoPreviewUseCase: CreateImageOrVideoPreviewUseCase = mock()
    private val createImageOrVideoThumbnailUseCase: CreateImageOrVideoThumbnailUseCase = mock()
    private val deletePreviewUseCase: DeletePreviewUseCase = mock()
    private val deleteThumbnailUseCase: DeleteThumbnailUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val cameraUploadsNotificationManagerWrapper: CameraUploadsNotificationManagerWrapper =
        mock()
    private val hasMediaPermissionUseCase: HasMediaPermissionUseCase = mock()
    private val applicationWrapper: ApplicationWrapper = mock()
    private val cookieEnabledCheckWrapper: CookieEnabledCheckWrapper = mock()
    private val broadcastCameraUploadsSettingsActionUseCase: BroadcastCameraUploadsSettingsActionUseCase =
        mock()
    private val startTracePerformanceUseCase: StartTracePerformanceUseCase = mock()
    private val stopTracePerformanceUseCase: StopTracePerformanceUseCase = mock()
    private var isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()


    @Before
    fun setUp() {
        Dispatchers.setMain(ioDispatcher)

        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase = WorkDatabase.create(context, workExecutor.serialTaskExecutor, true)
        underTest = CameraUploadsWorker(
            context = ApplicationProvider.getApplicationContext(),
            workerParams = WorkerParameters(
                UUID.randomUUID(),
                workDataOf(),
                emptyList(),
                WorkerParameters.RuntimeExtras(),
                1,
                1,
                executor,
                workExecutor,
                WorkerFactory.getDefaultWorkerFactory(),
                WorkProgressUpdater(workDatabase, workExecutor),
                WorkForegroundUpdater(workDatabase, object : ForegroundProcessor {
                    override fun startForeground(
                        workSpecId: String,
                        foregroundInfo: ForegroundInfo,
                    ) {
                    }

                    override fun stopForeground(workSpecId: String) {}
                    override fun isEnqueuedInForeground(workSpecId: String): Boolean = true
                }, workExecutor)
            ),
            isNotEnoughQuota = isNotEnoughQuota,
            getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase,
            isPrimaryFolderPathValidUseCase = isPrimaryFolderPathValidUseCase,
            isSecondaryFolderSetUseCase = isSecondaryFolderSetUseCase,
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            isWifiNotSatisfiedUseCase = isWifiNotSatisfiedUseCase,
            deleteSyncRecord = deleteSyncRecord,
            deleteSyncRecordByLocalPath = deleteSyncRecordByLocalPath,
            deleteSyncRecordByFingerprint = deleteSyncRecordByFingerprint,
            setPrimaryFolderLocalPathUseCase = setPrimaryFolderLocalPathUseCase,
            shouldCompressVideo = shouldCompressVideo,
            setSecondaryFolderLocalPathUseCase = setSecondaryFolderLocalPathUseCase,
            clearSyncRecords = clearSyncRecords,
            areLocationTagsEnabledUseCase = areLocationTagsEnabledUseCase,
            getPendingSyncRecords = getPendingSyncRecords,
            compressedVideoPending = compressedVideoPending,
            getVideoSyncRecordsByStatus = getVideoSyncRecordsByStatus,
            setSyncRecordPendingByPath = setSyncRecordPendingByPath,
            isChargingRequired = isChargingRequired,
            getNodeByIdUseCase = getNodeByIdUseCase,
            processMediaForUploadUseCase = processMediaForUploadUseCase,
            getUploadFolderHandleUseCase = getUploadFolderHandleUseCase,
            setPrimarySyncHandle = setPrimarySyncHandle,
            setSecondarySyncHandle = setSecondarySyncHandle,
            getDefaultNodeHandleUseCase = getDefaultNodeHandleUseCase,
            areTransfersPausedUseCase = areTransfersPausedUseCase,
            ioDispatcher = ioDispatcher,
            monitorPausedTransfersUseCase = monitorPausedTransfersUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            monitorBatteryInfo = monitorBatteryInfo,
            backgroundFastLoginUseCase = backgroundFastLoginUseCase,
            isNodeInRubbishOrDeletedUseCase = isNodeInRubbishOrDeletedUseCase,
            monitorChargingStoppedState = monitorChargingStoppedState,
            handleLocalIpChangeUseCase = handleLocalIpChangeUseCase,
            cancelTransferByTagUseCase = cancelTransferByTagUseCase,
            cancelAllUploadTransfersUseCase = cancelAllUploadTransfersUseCase,
            copyNodeUseCase = copyNodeUseCase,
            setOriginalFingerprintUseCase = setOriginalFingerprintUseCase,
            startUploadUseCase = startUploadUseCase,
            createCameraUploadFolder = createCameraUploadFolder,
            setupPrimaryFolderUseCase = setupPrimaryFolderUseCase,
            setupSecondaryFolderUseCase = setupSecondaryFolderUseCase,
            establishCameraUploadsSyncHandlesUseCase = establishCameraUploadsSyncHandlesUseCase,
            resetTotalUploadsUseCase = resetTotalUploadsUseCase,
            disableCameraUploadsUseCase = disableCameraUploadsUseCase,
            compressVideos = compressVideos,
            resetMediaUploadTimeStamps = resetMediaUploadTimeStamps,
            disableMediaUploadSettings = disableMediaUploadSettings,
            createCameraUploadTemporaryRootDirectoryUseCase = createCameraUploadTemporaryRootDirectoryUseCase,
            deleteCameraUploadsTemporaryRootDirectoryUseCase = deleteCameraUploadsTemporaryRootDirectoryUseCase,
            broadcastCameraUploadProgress = broadcastCameraUploadProgress,
            scheduleCameraUploadUseCase = scheduleCameraUploadUseCase,
            createTempFileAndRemoveCoordinatesUseCase = createTempFileAndRemoveCoordinatesUseCase,
            updateCameraUploadsBackupStatesUseCase = updateCameraUploadsBackupStatesUseCase,
            sendBackupHeartBeatSyncUseCase = sendBackupHeartBeatSyncUseCase,
            updateCameraUploadsBackupHeartbeatStatusUseCase = updateCameraUploadsBackupHeartbeatStatusUseCase,
            addCompletedTransferUseCase = addCompletedTransferUseCase,
            completedTransferMapper = completedTransferMapper,
            setCoordinatesUseCase = setCoordinatesUseCase,
            isChargingUseCase = isChargingUseCase,
            monitorStorageOverQuotaUseCase = monitorStorageOverQuotaUseCase,
            broadcastStorageOverQuotaUseCase = broadcastStorageOverQuotaUseCase,
            createImageOrVideoPreviewUseCase = createImageOrVideoPreviewUseCase,
            createImageOrVideoThumbnailUseCase = createImageOrVideoThumbnailUseCase,
            deletePreviewUseCase = deletePreviewUseCase,
            deleteThumbnailUseCase = deleteThumbnailUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            hasMediaPermissionUseCase = hasMediaPermissionUseCase,
            cameraUploadsNotificationManagerWrapper = cameraUploadsNotificationManagerWrapper,
            applicationWrapper = applicationWrapper,
            cookieEnabledCheckWrapper = cookieEnabledCheckWrapper,
            broadcastCameraUploadsSettingsActionUseCase = broadcastCameraUploadsSettingsActionUseCase,
            startTracePerformanceUseCase = startTracePerformanceUseCase,
            stopTracePerformanceUseCase = stopTracePerformanceUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that worker returns error when quota is not enough`() = runTest {
        whenever(monitorConnectivityUseCase.invoke()).thenReturn(MutableStateFlow(true))
        whenever(monitorChargingStoppedState.invoke()).thenReturn(MutableStateFlow(false))
        whenever(monitorBatteryInfo.invoke()).thenReturn(MutableStateFlow(mock()))
        whenever(isChargingRequired.invoke(any())).thenReturn(false)
        whenever(isWifiNotSatisfiedUseCase.invoke()).thenReturn(false)
        whenever(isNotEnoughQuota.invoke()).thenReturn(false)
        val result = underTest.doWork()
        Truth.assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }
}
