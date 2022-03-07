package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.MegaAttributes
import mega.privacy.android.app.MegaPreferences

/**
 * Settings repository - class for handling all calls relating to settings
 *
 */
interface SettingsRepository{
    /**
     * Is sdk logging enabled
     *
     * @return flow that emits true if enabled else false
     */
    fun isSdkLoggingEnabled(): Flow<Boolean>

    /**
     * Set sdk logging enabled
     *
     * @param enabled
     */
    suspend fun setSdkLoggingEnabled(enabled: Boolean)

    /**
     * Is chat logging enabled
     *
     * @return flow that emits true if enabled else false
     */
    fun isChatLoggingEnabled(): Flow<Boolean>

    /**
     * Set chat logging enabled
     *
     * @param enabled
     */
    suspend fun setChatLoggingEnabled(enabled: Boolean)

    /**
     * Get attributes
     *
     * This method requires some refactoring as MegaAttributes is not a domain entity and thus violates the architecture
     *
     * @return the current MegaAttributes
     */
    fun getAttributes(): MegaAttributes?

    /**
     * Get preferences
     *
     * This method requires some refactoring as MegaPreferences is not a domain entity and thus violates the architecture
     *
     * @return the current MegaPreferences
     */
    fun getPreferences(): MegaPreferences?

    /**
     * Set passcode lock enabled/disabled
     *
     * @param enabled
     */
    fun setPasscodeLockEnabled(enabled: Boolean)

    /**
     * Fetch contact links option
     *
     * @return true if option is enabled, else false
     */
    suspend fun fetchContactLinksOption(): Boolean

    /**
     * Get start screen
     *
     * @return start screen key
     */
    fun getStartScreen(): Int

    /**
     * Should hide recent activity
     *
     * @return true if option is enabled, else false
     */
    fun shouldHideRecentActivity(): Boolean

    /**
     * Set auto accept qr requests
     *
     * @param accept
     * @return true if option is enabled, else false
     */
    suspend fun setAutoAcceptQR(accept: Boolean): Boolean

    /**
     * Monitor start screen
     *
     * @return start screen key changes as a flow
     */
    fun monitorStartScreen(): Flow<Int>

    /**
     * Monitor hide recent activity
     *
     * @return hide recent activity option enabled status as a flow
     */
    fun monitorHideRecentActivity(): Flow<Boolean>
}
