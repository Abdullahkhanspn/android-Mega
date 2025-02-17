package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import javax.inject.Inject

/**
 * Set current user status use case
 *
 * @property chatParticipantsRepository
 */
class SetCurrentUserStatusUseCase @Inject constructor(
    private val chatParticipantsRepository: ChatParticipantsRepository,
) {

    /**
     * Invoke
     *
     */
    suspend operator fun invoke(status: UserStatus) =
        chatParticipantsRepository.setOnlineStatus(status)
}
