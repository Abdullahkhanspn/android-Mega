package mega.privacy.android.app.presentation.settings.camerauploads

import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.SqliteDatabaseHandler
import mega.privacy.android.app.presentation.settings.camerauploads.model.SettingsCameraUploadsState
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.ResetCameraUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.RestorePrimaryTimestamps
import mega.privacy.android.domain.usecase.RestoreSecondaryTimestamps
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsPrimaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsSettingsActionsUseCase
import mega.privacy.android.domain.usecase.camerauploads.PreparePrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetDefaultPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoSyncStatusUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupDefaultSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupPrimaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.workers.RescheduleCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadAndHeartbeatUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] class for SettingsCameraUploadsFragment
 *
 * @property isCameraUploadsEnabledUseCase Retrieves the enable status of Camera Uploads
 * @property areLocationTagsEnabledUseCase When uploading Photos, this checks whether Location Tags should be embedded in each Photo or not
 * @property areUploadFileNamesKeptUseCase Checks whether the File Names are kept or not when uploading content
 * @property checkEnableCameraUploadsStatus Checks the Camera Uploads status before enabling
 * @property clearCacheDirectory Clears all the contents of the internal cache directory
 * @property disableMediaUploadSettings Disables Media Uploads by manipulating a certain value in the database
 * @property getPrimaryFolderPathUseCase Retrieves the Primary Folder path
 * @property getUploadOptionUseCase Retrieves the upload option of Camera Uploads
 * @property getUploadVideoQualityUseCase Retrieves the Video Quality of Videos to be uploaded
 * @property getVideoCompressionSizeLimitUseCase Retrieves the maximum video file size that can be compressed
 * @property isCameraUploadsByWifiUseCase Checks whether Camera Uploads can only be run on Wi-Fi / Wi-Fi or Mobile Data
 * @property isChargingRequiredForVideoCompressionUseCase Checks whether compressing videos require the device to be charged or not
 * @property isPrimaryFolderPathValidUseCase Checks whether the Primary Folder path is valid or not
 * @property monitorConnectivityUseCase Monitors the device online status
 * @property preparePrimaryFolderPathUseCase Prepares the Primary Folder path
 * @property resetCameraUploadTimeStamps Resets the Primary and Secondary Timestamps
 * @property resetMediaUploadTimeStamps Resets the Secondary Timestamps
 * @property restorePrimaryTimestamps Restores the Primary Timestamps
 * @property restoreSecondaryTimestamps Restores the Secondary Timestamps
 * @property setCameraUploadsByWifiUseCase Sets whether Camera Uploads can only run through Wi-Fi / Wi-Fi or Mobile Data
 * @property setChargingRequiredForVideoCompressionUseCase Sets whether compressing videos require the device to be charged or not
 * @property setDefaultPrimaryFolderPathUseCase Sets the default Primary Folder path
 * @property setLocationTagsEnabledUseCase Sets whether Location Tags should be embedded in each Photo to be uploaded or not
 * @property setPrimaryFolderPathUseCase Sets the new Primary Folder path
 * @property setUploadFileNamesKeptUseCase Sets whether the File Names of files to be uploaded will be kept or not
 * @property setUploadOptionUseCase Sets the new upload option of Camera Uploads
 * @property setUploadVideoQualityUseCase Sets the new Video Quality of Videos to be uploaded
 * @property setUploadVideoSyncStatusUseCase Sets the new Sync Status of Videos to be uploaded
 * @property setVideoCompressionSizeLimitUseCase Sets the maximum video file size that can be compressed
 * @property setupDefaultSecondaryFolderUseCase Sets up a default Secondary Folder of Camera Uploads
 * @property setupPrimaryFolderUseCase Sets up the Primary Folder of Camera Uploads
 * @property setupSecondaryFolderUseCase Sets up the Secondary Folder of Camera Uploads
 * @property startCameraUploadUseCase Start the camera upload
 * @property stopCameraUploadsUseCase Stop the camera upload
 * @property rescheduleCameraUploadUseCase Reschedule the camera upload
 * @property stopCameraUploadAndHeartbeatUseCase Stop the camera upload and heartbeat
 */
@HiltViewModel
class SettingsCameraUploadsViewModel @Inject constructor(
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val areLocationTagsEnabledUseCase: AreLocationTagsEnabledUseCase,
    private val areUploadFileNamesKeptUseCase: AreUploadFileNamesKeptUseCase,
    private val checkEnableCameraUploadsStatus: CheckEnableCameraUploadsStatus,
    private val clearCacheDirectory: ClearCacheDirectory,
    private val disableMediaUploadSettings: DisableMediaUploadSettings,
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val getUploadOptionUseCase: GetUploadOptionUseCase,
    private val getUploadVideoQualityUseCase: GetUploadVideoQualityUseCase,
    private val getVideoCompressionSizeLimitUseCase: GetVideoCompressionSizeLimitUseCase,
    private val isCameraUploadsByWifiUseCase: IsCameraUploadsByWifiUseCase,
    private val isChargingRequiredForVideoCompressionUseCase: IsChargingRequiredForVideoCompressionUseCase,
    private val isPrimaryFolderPathValidUseCase: IsPrimaryFolderPathValidUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val preparePrimaryFolderPathUseCase: PreparePrimaryFolderPathUseCase,
    private val resetCameraUploadTimeStamps: ResetCameraUploadTimeStamps,
    private val resetMediaUploadTimeStamps: ResetMediaUploadTimeStamps,
    private val restorePrimaryTimestamps: RestorePrimaryTimestamps,
    private val restoreSecondaryTimestamps: RestoreSecondaryTimestamps,
    private val setCameraUploadsByWifiUseCase: SetCameraUploadsByWifiUseCase,
    private val setChargingRequiredForVideoCompressionUseCase: SetChargingRequiredForVideoCompressionUseCase,
    private val setDefaultPrimaryFolderPathUseCase: SetDefaultPrimaryFolderPathUseCase,
    private val setLocationTagsEnabledUseCase: SetLocationTagsEnabledUseCase,
    private val setPrimaryFolderPathUseCase: SetPrimaryFolderPathUseCase,
    private val setUploadFileNamesKeptUseCase: SetUploadFileNamesKeptUseCase,
    private val setUploadOptionUseCase: SetUploadOptionUseCase,
    private val setUploadVideoQualityUseCase: SetUploadVideoQualityUseCase,
    private val setUploadVideoSyncStatusUseCase: SetUploadVideoSyncStatusUseCase,
    private val setVideoCompressionSizeLimitUseCase: SetVideoCompressionSizeLimitUseCase,
    private val setupDefaultSecondaryFolderUseCase: SetupDefaultSecondaryFolderUseCase,
    private val setupPrimaryFolderUseCase: SetupPrimaryFolderUseCase,
    private val setupSecondaryFolderUseCase: SetupSecondaryFolderUseCase,
    private val startCameraUploadUseCase: StartCameraUploadUseCase,
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase,
    private val rescheduleCameraUploadUseCase: RescheduleCameraUploadUseCase,
    private val stopCameraUploadAndHeartbeatUseCase: StopCameraUploadAndHeartbeatUseCase,
    private val hasMediaPermissionUseCase: HasMediaPermissionUseCase,
    monitorCameraUploadsSettingsActionsUseCase: MonitorCameraUploadsSettingsActionsUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val dbh: SqliteDatabaseHandler,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsCameraUploadsState())

    /**
     * State of Settings Camera Uploads
     */
    val state: StateFlow<SettingsCameraUploadsState> = _state.asStateFlow()

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent = monitorConnectivityUseCase()

    /**
     * Monitor Camera Upload Settings Actions
     */
    val monitorCameraUploadsSettingsActions = monitorCameraUploadsSettingsActionsUseCase()

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    init {
        initializeSettings()
    }

    /**
     * Handle specific behavior when permissions are granted / denied
     *
     * @param permissions A [Map] of permissions that were requested
     */
    fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        if (hasMediaPermissionUseCase()) {
            handleEnableCameraUploads()
        } else {
            setMediaPermissionsRationaleState(shouldShow = true)
        }
        if (!isNotificationPermissionGranted(permissions)) {
            setNotificationPermissionRationaleState(shouldShow = true)
        }
    }

    /**
     * Checks whether the Notification Permission been granted. For Devices running Android 12
     * and below, this is automatically granted.
     *
     * @param permissions A [Map] of permissions that were requested
     *
     * @return Boolean value
     */
    private fun isNotificationPermissionGranted(permissions: Map<String, Boolean>) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[POST_NOTIFICATIONS] == true
        } else {
            true
        }

    /**
     *
     * Checks whether Camera Uploads can be enabled and handles the Status accordingly, as
     * determined by the Use Case [checkEnableCameraUploadsStatus]
     */
    fun handleEnableCameraUploads() = viewModelScope.launch {
        runCatching { checkEnableCameraUploadsStatus() }.onSuccess { status ->
            when (status) {
                EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS -> {
                    _state.update { it.copy(shouldTriggerCameraUploads = true) }
                }

                EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT -> {
                    _state.update { it.copy(shouldShowBusinessAccountPrompt = true) }
                }

                EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT -> {
                    _state.update { it.copy(shouldShowBusinessAccountSuspendedPrompt = true) }
                }
            }
        }.onFailure { Timber.w("Exception checking CU status: $it") }
    }

    /**
     * Resets the value of [SettingsCameraUploadsState.shouldShowBusinessAccountPrompt] to False
     */
    fun resetBusinessAccountPromptState() =
        _state.update { it.copy(shouldShowBusinessAccountPrompt = false) }

    /**
     * Resets the value of [SettingsCameraUploadsState.shouldShowBusinessAccountSuspendedPrompt] to False
     */
    fun resetBusinessAccountSuspendedPromptState() =
        _state.update { it.copy(shouldShowBusinessAccountSuspendedPrompt = false) }

    /**
     * Sets the value of [SettingsCameraUploadsState.shouldTriggerCameraUploads]
     * @param updatedValue the updated Boolean value of the parameter
     */
    fun setTriggerCameraUploadsState(updatedValue: Boolean) =
        _state.update { it.copy(shouldTriggerCameraUploads = updatedValue) }

    /**
     * If the handle matches the previous primary folder's handle, restore the time stamp from stamps
     * if not clean the sync record from previous primary folder
     */
    fun restorePrimaryTimestampsAndSyncRecordProcess() {
        viewModelScope.launch {
            restorePrimaryTimestamps()
        }
    }

    /**
     * on Enable MediaUpload
     * @param mediaUploadsName
     */
    fun onMediaUploadsEnabled(mediaUploadsName: String) {
        viewModelScope.launch {
            runCatching {
                // Sets up a Secondary Folder with a Media Uploads folder name
                setupDefaultSecondaryFolderUseCase(mediaUploadsName)
                //If the handle matches the previous secondary folder's handle, restore the time stamp from stamps
                //if not clean the sync record from previous primary folder
                restoreSecondaryTimestamps()
                dbh.setSecondaryUploadEnabled(true)
            }.onFailure {
                Timber.e(it)
                setErrorState(shouldShow = true)
            }
        }
    }

    /**
     * If the handle matches the previous secondary folder's handle, restore the time stamp from stamps
     * if not clean the sync record from previous primary folder
     */
    fun restoreSecondaryTimestampsAndSyncRecordProcess() {
        viewModelScope.launch {
            runCatching {
                restoreSecondaryTimestamps()
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Sets up the Primary Folder with a given folder handle
     */
    fun setupPrimaryCameraUploadFolder(primaryHandle: Long) = viewModelScope.launch {
        runCatching {
            setupPrimaryFolderUseCase(primaryHandle)
        }.onFailure {
            Timber.w(it)
            setErrorState(shouldShow = true)
        }
    }

    /**
     * Sets up the Secondary Folder with a given folder handle
     */
    fun setupSecondaryCameraUploadFolder(secondaryHandle: Long) = viewModelScope.launch {
        runCatching {
            setupSecondaryFolderUseCase(secondaryHandle)
        }.onFailure {
            Timber.w(it)
            setErrorState(shouldShow = true)
        }
    }

    /**
     * Sets the value of [SettingsCameraUploadsState.shouldShowError]
     * @param shouldShow The new state value
     */
    fun setErrorState(shouldShow: Boolean) {
        _state.update { it.copy(shouldShowError = shouldShow) }
    }

    /**
     * Sets the value of [SettingsCameraUploadsState.shouldShowMediaPermissionsRationale]
     * @param shouldShow The new state value
     */
    fun setMediaPermissionsRationaleState(shouldShow: Boolean) {
        _state.update { it.copy(shouldShowMediaPermissionsRationale = shouldShow) }
    }

    /**
     * Sets the value of [SettingsCameraUploadsState.shouldShowNotificationPermissionRationale]
     * @param shouldShow The new state value
     */
    fun setNotificationPermissionRationaleState(shouldShow: Boolean) {
        _state.update { it.copy(shouldShowNotificationPermissionRationale = shouldShow) }
    }

    /**
     * Shows / hides the Access Media Location Permission rationale by updating the
     * value of [SettingsCameraUploadsState.accessMediaLocationRationaleText]
     *
     * @param showRationale true if the rationale should be shown, and false if otherwise
     */
    fun setAccessMediaLocationRationaleShown(showRationale: Boolean) =
        _state.update {
            it.copy(accessMediaLocationRationaleText = if (showRationale) R.string.on_refuse_storage_permission else null)
        }

    /**
     * Resets all Timestamps and cleans the Cache Directory
     */
    fun resetTimestampsAndCacheDirectory() {
        viewModelScope.launch {
            runCatching {
                resetCameraUploadTimeStamps(clearCamSyncRecords = true)
                clearCacheDirectory()
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Call several Use Cases to disable Media Uploads
     */
    fun disableMediaUploads() {
        runCatching {
            viewModelScope.launch {
                resetAndDisableMediaUploads()
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    private suspend fun resetAndDisableMediaUploads() {
        resetMediaUploadTimeStamps()
        disableMediaUploadSettings()
    }

    /**
     * onCameraUploadsEnabled
     * @param shouldDisableMediaUploads
     */
    fun onCameraUploadsEnabled(shouldDisableMediaUploads: Boolean) {
        viewModelScope.launch {
            runCatching {
                setCameraUploadsEnabled(true)
                restorePrimaryTimestamps()
                // this will be replaced with [SetupCameraUploadSettingUseCase]
                dbh.setCamSyncEnabled(true)
                if (shouldDisableMediaUploads) {
                    resetAndDisableMediaUploads()
                }
            }.onFailure {
                Timber.e(it)
                setErrorState(shouldShow = true)
            }
        }
    }

    /**
     * Sets whether Camera Uploads is enabled or not
     *
     * @param isEnabled True if Camera Uploads is enabled, and false if otherwise
     */
    fun setCameraUploadsEnabled(isEnabled: Boolean) =
        _state.update { it.copy(isCameraUploadsEnabled = isEnabled) }

    /**
     * Change the Upload Connection Type for Camera Uploads
     *
     * @param wifiOnly If true, Camera Uploads will only run through Wi-Fi
     * If false, Camera Uploads can run through either Wi-Fi or Mobile Data
     */
    fun changeUploadConnectionType(wifiOnly: Boolean) = viewModelScope.launch {
        setCameraUploadsByWifiUseCase(wifiOnly)
        refreshUploadConnectionType()
    }

    /**
     * Change the Upload Option of Camera Uploads
     *
     * @param uploadOption The new [UploadOption]
     */
    fun changeUploadOption(uploadOption: UploadOption) = viewModelScope.launch {
        setUploadOptionUseCase(uploadOption)
        refreshUploadOption()
    }

    /**
     * Sets whether to include Location Tags in Photos or not
     *
     * @param include If true, Location Tags will be included for every Photo upload
     * If false, no Location Tags will be included when uploading Photos
     */
    fun includeLocationTags(include: Boolean) = viewModelScope.launch {
        setLocationTagsEnabledUseCase(include)
        refreshLocationTags()
    }

    /**
     * Change the Video Quality for videos to be uploaded. The Video Sync Status will also
     * be updated depending on the new Video Quality selected
     *
     * @param value The new Video Quality, represented as an Integer from the list
     */
    fun changeUploadVideoQuality(value: Int) = viewModelScope.launch {
        VideoQuality.values().find { it.value == value }?.let { videoQuality ->
            setUploadVideoQualityUseCase(videoQuality)
            setUploadVideoSyncStatusUseCase(
                if (videoQuality == VideoQuality.ORIGINAL) {
                    SyncStatus.STATUS_PENDING
                } else {
                    SyncStatus.STATUS_TO_COMPRESS
                }
            )
            refreshUploadVideoQuality()
        }
    }

    /**
     * Sets whether charging is required for video compression or not
     *
     * @param chargingRequired True if charging is required for video compression, and false
     * if otherwise
     */
    fun changeChargingRequiredForVideoCompression(chargingRequired: Boolean) =
        viewModelScope.launch {
            setChargingRequiredForVideoCompressionUseCase(chargingRequired)
            refreshChargingRequiredForVideoCompression()
        }

    /**
     * Sets the new video compression size limit
     *
     * @param size The new video compression size limit
     */
    fun changeVideoCompressionSizeLimit(size: Int) = viewModelScope.launch {
        setVideoCompressionSizeLimitUseCase(size)
        refreshVideoCompressionSizeLimit()
    }

    /**
     * Sets whether the File Names of files to be uploaded will be kept or not
     *
     * @param keepFileNames true if the File Names should now be left as is, and false if otherwise
     */
    fun keepUploadFileNames(keepFileNames: Boolean) = viewModelScope.launch {
        setUploadFileNamesKeptUseCase(keepFileNames)
        refreshUploadFilesNamesKept()
    }

    /**
     * Sets the new Primary Folder path, once a Folder has been selected from the File Explorer
     *
     * @param newPath The new Primary Folder path, which may be nullable
     * @param isFolderInSDCard true if the local Folder is now located in the SD Card, and
     * false if otherwise
     */
    fun changePrimaryFolderPath(newPath: String?, isFolderInSDCard: Boolean) =
        viewModelScope.launch {
            if (isPrimaryFolderPathValidUseCase(newPath)) {
                newPath?.let {
                    setPrimaryFolderPathUseCase(
                        newFolderPath = it,
                        isPrimaryFolderInSDCard = isFolderInSDCard,
                    )
                }
                refreshPrimaryFolderPath()
            } else {
                setInvalidFolderSelectedPromptShown(true)
            }
        }

    /**
     * Shows / hides the Invalid Folder Selected prompt by updating the
     * value of [SettingsCameraUploadsState.invalidFolderSelectedTextId]
     *
     * @param showPrompt true if the prompt should be shown, and false if otherwise
     */
    fun setInvalidFolderSelectedPromptShown(showPrompt: Boolean) =
        _state.update {
            it.copy(invalidFolderSelectedTextId = if (showPrompt) R.string.error_invalid_folder_selected else null)
        }

    /**
     * When [SettingsCameraUploadsViewModel] is instantiated, initialize the UI Elements
     */
    private fun initializeSettings() {
        viewModelScope.launch {
            preparePrimaryFolderPathUseCase()

            val isCameraUploadsEnabled = async { isCameraUploadsEnabledUseCase() }
            val areLocationTagsIncluded = async { areLocationTagsEnabledUseCase() }
            val areUploadFileNamesKept = async { areUploadFileNamesKeptUseCase() }
            val isChargingRequiredForVideoCompression =
                async { isChargingRequiredForVideoCompressionUseCase() }
            val uploadConnectionType = async { getUploadConnectionType() }
            val getUploadOption = async { getUploadOptionUseCase() }
            val primaryFolderPath = async { getPrimaryFolderPathUseCase() }
            val videoCompressionSizeLimit = async { getVideoCompressionSizeLimitUseCase() }
            val videoQuality = async { getUploadVideoQualityUseCase() }
            _state.update {
                it.copy(
                    isCameraUploadsEnabled = isCameraUploadsEnabled.await(),
                    areLocationTagsIncluded = areLocationTagsIncluded.await(),
                    areUploadFileNamesKept = areUploadFileNamesKept.await(),
                    isChargingRequiredForVideoCompression = isChargingRequiredForVideoCompression.await(),
                    primaryFolderPath = primaryFolderPath.await(),
                    uploadConnectionType = uploadConnectionType.await(),
                    uploadOption = getUploadOption.await(),
                    videoCompressionSizeLimit = videoCompressionSizeLimit.await(),
                    videoQuality = videoQuality.await(),
                )
            }
        }
    }

    /**
     * Updates the value of [SettingsCameraUploadsState.uploadConnectionType] whenever a new Upload
     * Connection type is set
     */
    private suspend fun refreshUploadConnectionType() {
        val uploadConnectionType = getUploadConnectionType()
        _state.update { it.copy(uploadConnectionType = uploadConnectionType) }
    }

    /**
     * Updates the value of [SettingsCameraUploadsState.uploadOption] whenever a new
     * Upload Connection type is set
     */
    private suspend fun refreshUploadOption() {
        val uploadOption = getUploadOptionUseCase()
        _state.update { it.copy(uploadOption = uploadOption) }
    }

    /**
     * Updates the value of [SettingsCameraUploadsState.areLocationTagsIncluded] whenever changes
     * to include / exclude Location Tags for Photo uploads are found
     */
    private suspend fun refreshLocationTags() {
        val areLocationTagsIncluded = areLocationTagsEnabledUseCase()
        _state.update { it.copy(areLocationTagsIncluded = areLocationTagsIncluded) }
    }

    /**
     * Updates the value of [SettingsCameraUploadsState.videoQuality] whenever a new upload
     * Video Quality has been set
     */
    private suspend fun refreshUploadVideoQuality() {
        val videoQuality = getUploadVideoQualityUseCase()
        _state.update { it.copy(videoQuality = videoQuality) }
    }

    /**
     * Updates the value of [SettingsCameraUploadsState.isChargingRequiredForVideoCompression] whenever
     * a change to require charging for video compression is found
     */
    private suspend fun refreshChargingRequiredForVideoCompression() {
        val isChargingRequiredForVideoCompression = isChargingRequiredForVideoCompressionUseCase()
        _state.update {
            it.copy(isChargingRequiredForVideoCompression = isChargingRequiredForVideoCompression)
        }
    }

    /**
     * Updates the value of [SettingsCameraUploadsState.videoCompressionSizeLimit] whenever the
     * maximum video compression size limit changes
     */
    private suspend fun refreshVideoCompressionSizeLimit() {
        val videoCompressionSizeLimit = getVideoCompressionSizeLimitUseCase()
        _state.update { it.copy(videoCompressionSizeLimit = videoCompressionSizeLimit) }
    }

    /**
     * Updates the value of [SettingsCameraUploadsState.areUploadFileNamesKept] whenever File Name
     * changes for uploads are found
     */
    private suspend fun refreshUploadFilesNamesKept() {
        val areUploadFileNamesKept = areUploadFileNamesKeptUseCase()
        _state.update { it.copy(areUploadFileNamesKept = areUploadFileNamesKept) }
    }

    /**
     * Updates the value of [SettingsCameraUploadsState.primaryFolderPath] whenever a valid Primary
     * Folder path is set
     */
    private suspend fun refreshPrimaryFolderPath() {
        val primaryFolderPath = getPrimaryFolderPathUseCase()
        _state.update { it.copy(primaryFolderPath = primaryFolderPath) }
    }

    /**
     * Retrieves the current Upload Connection Type
     *
     * @return [UploadConnectionType.WIFI] if Camera Uploads will only upload content over Wi-Fi
     * [UploadConnectionType.WIFI_OR_MOBILE_DATA] if Camera Uploads can upload content either on
     * Wi-Fi or Mobile Data
     */
    private suspend fun getUploadConnectionType() =
        if (isCameraUploadsByWifiUseCase()) UploadConnectionType.WIFI else UploadConnectionType.WIFI_OR_MOBILE_DATA

    /**
     *  Start camera upload
     */
    fun startCameraUpload() = viewModelScope.launch {
        startCameraUploadUseCase()
    }

    /**
     * Stop camera uploads
     * Cancel camera upload and heartbeat workers
     */
    fun stopCameraUploads() = viewModelScope.launch {
        stopCameraUploadsUseCase(shouldReschedule = false)
        stopCameraUploadAndHeartbeatUseCase()
    }

    /**
     * Reschedule camera upload
     */
    fun rescheduleCameraUpload() = viewModelScope.launch {
        rescheduleCameraUploadUseCase()
    }

    /**
     * Set Invalid Camera Uploads Sync Handle
     */
    fun setInvalidCameraUploadsHandle() {
        viewModelScope.launch {
            dbh.setCamSyncHandle(-1L)
        }
    }
}
