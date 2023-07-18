package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * Use case for notifying a logout.
 */
class BroadcastLogoutUseCase @Inject constructor(private val loginRepository: LoginRepository) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() = loginRepository.broadcastLogout()
}