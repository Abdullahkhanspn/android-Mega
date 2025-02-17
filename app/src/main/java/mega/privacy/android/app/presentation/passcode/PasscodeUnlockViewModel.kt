package mega.privacy.android.app.presentation.passcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.passcode.mapper.PasscodeTypeMapper
import mega.privacy.android.app.presentation.passcode.model.PasscodeUnlockState
import mega.privacy.android.domain.entity.passcode.UnlockPasscodeRequest
import mega.privacy.android.domain.usecase.passcode.GetPasscodeTypeUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeAttemptsUseCase
import mega.privacy.android.domain.usecase.passcode.UnlockPasscodeUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Passcode unlock view model
 *
 * @property unlockPasscodeUseCase
 */
@HiltViewModel
internal class PasscodeUnlockViewModel @Inject constructor(
    private val monitorPasscodeAttemptsUseCase: MonitorPasscodeAttemptsUseCase,
    private val unlockPasscodeUseCase: UnlockPasscodeUseCase,
    private val getPasscodeTypeUseCase: GetPasscodeTypeUseCase,
    private val passcodeTypeMapper: PasscodeTypeMapper,
) : ViewModel() {

    private val _state: MutableStateFlow<PasscodeUnlockState> = MutableStateFlow(
        PasscodeUnlockState.Loading
    )

    /**
     * State
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val passCodeType = passcodeTypeMapper(getPasscodeTypeUseCase())
                monitorPasscodeAttemptsUseCase()
                    .catch {
                        Timber.e(it)
                    }.collect { count ->
                        _state.update {
                            if (it is PasscodeUnlockState.Data) {
                                it.copy(
                                    failedAttempts = count,
                                    logoutWarning = count >= 5
                                )
                            } else {
                                PasscodeUnlockState.Data(
                                    passcodeType = passCodeType,
                                    failedAttempts = count,
                                    logoutWarning = count >= 5
                                )
                            }
                        }
                    }
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Unlock with passcode
     *
     * @param passCode
     */
    fun unlockWithPasscode(passCode: String) =
        unlock(UnlockPasscodeRequest.PasscodeRequest(passCode))

    /**
     * Unlock with password
     *
     * @param password
     */
    fun unlockWithPassword(password: String) =
        unlock(UnlockPasscodeRequest.PasswordRequest(password))

    private fun unlock(request: UnlockPasscodeRequest) {
        viewModelScope.launch {
            runCatching {
                unlockPasscodeUseCase(request)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    fun unlockWithBiometrics() {
        unlock(UnlockPasscodeRequest.BiometricRequest)
    }

    fun onBiometricAuthFailed() {
        Timber.w("Biometric Authentication failed")
    }

}