package mega.privacy.android.data.wrapper

/**
 * Mega Application Wrapper
 */
interface ApplicationWrapper {
    /**
     * Sets the isLoggingIn variable in MegaApplication
     */
    fun setLoggingIn(isLoggingIn: Boolean)

    /**
     * Gets the isLoggingIn variable in MegaApplication
     */
    fun isLoggingIn(): Boolean

    /**
     * Sets the isIsHeartBeatAlive variable in MegaApplication
     * @param isAlive
     */
    fun setHeartBeatAlive(isAlive: Boolean)
}
