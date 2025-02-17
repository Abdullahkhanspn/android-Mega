package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.backup.SetupCameraUploadsBackupUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

/**
 * Test class for [SetupCameraUploadSettingUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetupCameraUploadSettingUseCaseTest {

    private lateinit var underTest: SetupCameraUploadSettingUseCase

    private val cameraUploadRepository: CameraUploadRepository = mock()
    private val setupCameraUploadsBackupUseCase: SetupCameraUploadsBackupUseCase = mock()
    private val removeBackupFolderUseCase: RemoveBackupFolderUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = SetupCameraUploadSettingUseCase(
            cameraUploadRepository = cameraUploadRepository,
            setupCameraUploadsBackupUseCase = setupCameraUploadsBackupUseCase,
            removeBackupFolderUseCase = removeBackupFolderUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
            setupCameraUploadsBackupUseCase,
            removeBackupFolderUseCase
        )
    }

    @ParameterizedTest(name = "with {0}")
    @ValueSource(booleans = [true, false])
    fun `test that camera upload setting is set when invoked`(isEnabled: Boolean) = runTest {
        val cameraUploadName = "Camera Uploads"
        underTest(isEnabled, cameraUploadName)
        verify(cameraUploadRepository).setCameraUploadsEnabled(isEnabled)
        if (isEnabled) {
            verify(setupCameraUploadsBackupUseCase).invoke(cameraUploadName)
            verifyNoInteractions(removeBackupFolderUseCase)
        } else {
            verify(removeBackupFolderUseCase).invoke(CameraUploadFolderType.Primary)
            verifyNoInteractions(setupCameraUploadsBackupUseCase)
        }
    }
}
