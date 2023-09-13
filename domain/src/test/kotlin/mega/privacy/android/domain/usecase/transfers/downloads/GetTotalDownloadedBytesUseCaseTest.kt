package mega.privacy.android.domain.usecase.transfers.downloads

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetTotalDownloadedBytesUseCaseTest {

    private lateinit var underTest: GetTotalDownloadedBytesUseCase

    private lateinit var transferRepository: TransferRepository

    @BeforeAll
    fun setup() {
        transferRepository = mock()
        underTest = GetTotalDownloadedBytesUseCase(transferRepository)
    }

    @AfterAll
    fun resetMocks() {
        reset(transferRepository)
    }

    @ParameterizedTest(name = " repository call returns {0}")
    @ValueSource(longs = [0, 7, 200])
    fun `test that GetTotalDownloadedBytesUseCase returns correctly if`(
        bytes: Long,
    ) = runTest {
        whenever(transferRepository.getTotalDownloadedBytes()).thenReturn(bytes)
        Truth.assertThat(underTest()).isEqualTo(bytes)
    }
}