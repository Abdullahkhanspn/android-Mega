package mega.privacy.android.app.usecase.call

import android.util.Pair
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.CustomCountDownTimer
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_WAITING_FOR_OTHERS
import mega.privacy.android.app.data.extensions.observeOnce
import mega.privacy.android.app.meeting.CallSoundType
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.utils.Constants.SECONDS_TO_WAIT_FOR_OTHERS_TO_JOIN_THE_CALL
import mega.privacy.android.app.utils.Constants.TYPE_JOIN
import mega.privacy.android.app.utils.Constants.TYPE_LEFT
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.domain.entity.CallsSoundNotifications
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatSession
import timber.log.Timber
import javax.inject.Inject

/**
 * Main use case to control when a call-related sound should be played.
 *
 * @property megaChatApi   Mega Chat API needed to get call information.
 * @property getParticipantsChangesUseCase GetParticipantsChangesUseCase
 */
class GetCallSoundsUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
    private val getParticipantsChangesUseCase: GetParticipantsChangesUseCase,
    private val getSessionStatusChangesUseCase: GetSessionStatusChangesUseCase,
    private val getCallStatusChangesUseCase: GetCallStatusChangesUseCase,
    private val endCallUseCase: EndCallUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val callsPreferencesGateway: CallsPreferencesGateway,
    private val monitorChatCallUpdates: MonitorChatCallUpdates,
    @ApplicationScope private val sharingScope: CoroutineScope,
) {

    companion object {
        const val SECONDS_TO_WAIT_TO_RECOVER_CONTACT_CONNECTION: Long = 10
        const val ONE_PARTICIPANT: Int = 1
    }

    /**
     * Participant info
     *
     * @property peerId     Peer ID of participant
     * @property clientId   Client ID of participant
     */
    data class ParticipantInfo(
        val peerId: Long,
        val clientId: Long,
    )

    var finishCallCountDownTimer: CustomCountDownTimer? = null
    var waitingForOthersCountDownTimer: CustomCountDownTimer? = null

    val participants = ArrayList<ParticipantInfo>()
    val disposable = CompositeDisposable()
    var disposableCountDownTimer: Disposable? = null

    /**
     * Method to get the appropriate sound
     *
     * @return CallSoundType
     */
    fun get(): Flowable<CallSoundType> =
        Flowable.create({ emitter ->

            val outgoingRingingStatusObserver = Observer<MegaChatCall> { call ->
                if (MegaApplication.getChatManagement()
                        .isRequestSent(call.callId) && call.numParticipants == ONE_PARTICIPANT
                ) {
                    endCallUseCase.hangCall(call.callId)
                        .subscribeBy(
                            onError = { error ->
                                Timber.e(error.stackTraceToString())
                            })
                }
            }

            getCallStatusChangesUseCase.getReconnectingStatus()
                .subscribeBy(
                    onNext = {
                        if (it) {
                            Timber.d("Call reconnecting")
                            emitter.onNext(CallSoundType.CALL_RECONNECTING)
                        }
                    },
                    onError = { error ->
                        Timber.e(error.stackTraceToString())
                    }
                ).addTo(disposable)

            getSessionStatusChangesUseCase.getSessionChanged()
                .subscribeBy(
                    onNext = { (call, sessionStatus, isRecoverable, peerId, clientId) ->
                        val participant =
                            ParticipantInfo(peerId = peerId, clientId = clientId)

                        if (call == null) {
                            stopCountDown(INVALID_HANDLE, participant)
                        } else {
                            megaChatApi.getChatRoom(call.chatid)?.let { chat ->
                                if (!chat.isGroup && !chat.isMeeting) {
                                    when (sessionStatus) {
                                        MegaChatSession.SESSION_STATUS_IN_PROGRESS -> {
                                            Timber.d("Session in progress")
                                            stopCountDown(call.chatid, participant)
                                        }
                                        MegaChatSession.SESSION_STATUS_DESTROYED -> {
                                            isRecoverable?.let { isRecoverableSession ->
                                                if (isRecoverableSession) {
                                                    Timber.d("Session destroyed, recoverable session. Wait 10 seconds to hang up")
                                                    startFinishCallCountDown(
                                                        call,
                                                        participant,
                                                        SECONDS_TO_WAIT_TO_RECOVER_CONTACT_CONNECTION
                                                    )

                                                } else {
                                                    Timber.d("Session destroyed, unrecoverable session.")
                                                    stopCountDown(
                                                        call.chatid,
                                                        participant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onError = { error ->
                        Timber.e(error.stackTraceToString())
                    }
                )
                .addTo(disposable)

            getParticipantsChangesUseCase.checkIfIAmAloneOnAnyCall()
                .subscribeBy(
                    onNext = { (chatId, onlyMeInTheCall, waitingForOthers) ->
                        removeWaitingForOthersCountDownTimer()
                        MegaApplication.getChatManagement().stopCounterToFinishCall()

                        if (onlyMeInTheCall) {
                            if (waitingForOthers) {
                                val liveD: MutableLiveData<Boolean> = MutableLiveData()
                                waitingForOthersCountDownTimer = CustomCountDownTimer(liveD)

                                liveD.observeOnce { counterState ->
                                    counterState?.let { isFinished ->
                                        if (isFinished) {
                                            MegaApplication.getChatManagement()
                                                .startCounterToFinishCall(chatId)

                                            LiveEventBus.get(
                                                EVENT_UPDATE_WAITING_FOR_OTHERS,
                                                Pair::class.java
                                            ).post(Pair.create(chatId, onlyMeInTheCall))

                                            megaChatApi.getChatCall(chatId)?.let { call ->
                                                if (call.hasLocalAudio()) {
                                                    Timber.d("I am the only participant in the group call/meeting, muted micro")
                                                    megaChatApi.disableAudio(call.chatid, null)
                                                }
                                            }

                                            removeWaitingForOthersCountDownTimer()
                                        }
                                    }
                                }

                                waitingForOthersCountDownTimer?.start(
                                    SECONDS_TO_WAIT_FOR_OTHERS_TO_JOIN_THE_CALL)
                            } else {
                                MegaApplication.getChatManagement()
                                    .startCounterToFinishCall(chatId)
                            }
                        } else {
                            MegaApplication.getChatManagement().hasEndCallDialogBeenIgnored = false
                        }
                    },
                    onError = { error ->
                        Timber.e(error.stackTraceToString())
                    }
                )
                .addTo(disposable)

            sharingScope.launch {
                monitorChatCallUpdates()
                    .collectLatest { call ->
                        call.changes?.apply {
                            Timber.d("Monitor chat call updated, changes $this")
                            if (contains(ChatCallChanges.Status)) {
                                when (call.status) {
                                    ChatCallStatus.TerminatingUserParticipation -> {
                                        Timber.d("Terminating user participation")
                                        removeWaitingForOthersCountDownTimer()
                                        MegaApplication.getChatManagement()
                                            .stopCounterToFinishCall()
                                        rtcAudioManagerGateway.removeRTCAudioManager()
                                        emitter.onNext(CallSoundType.CALL_ENDED)
                                    }

                                    else -> {}

                                }
                            }
                        }
                    }
            }

            getParticipantsChangesUseCase.getChangesFromParticipants()
                .subscribeBy(
                    onNext = { result ->
                        sharingScope.launch {
                            callsPreferencesGateway
                                .getCallsSoundNotificationsPreference()
                                .collectLatest { soundNotifications ->
                                    val isEnabled =
                                        soundNotifications == CallsSoundNotifications.Enabled
                                    if (isEnabled) {
                                        when (result.typeChange) {
                                            TYPE_JOIN -> emitter.onNext(CallSoundType.PARTICIPANT_JOINED_CALL)
                                            TYPE_LEFT -> emitter.onNext(CallSoundType.PARTICIPANT_LEFT_CALL)
                                        }
                                    }
                                    this.cancel()
                                }
                        }

                    },
                    onError = { error ->
                        Timber.e(error.stackTraceToString())
                    }
                )
                .addTo(disposable)

            LiveEventBus.get(EventConstants.EVENT_CALL_OUTGOING_RINGING_CHANGE,
                MegaChatCall::class.java)
                .observeForever(outgoingRingingStatusObserver)

            emitter.setCancellable {
                LiveEventBus.get(EventConstants.EVENT_CALL_OUTGOING_RINGING_CHANGE,
                    MegaChatCall::class.java)
                    .removeObserver(outgoingRingingStatusObserver)

                removeWaitingForOthersCountDownTimer()
                disposable.clear()
            }

        }, BackpressureStrategy.LATEST)

    /**
     * Method to start the countdown to hang up the call
     *
     * @param call      MegaChatCall
     * @param seconds   Seconds to wait
     */
    private fun startFinishCallCountDown(
        call: MegaChatCall,
        participant: ParticipantInfo,
        seconds: Long,
    ) {
        megaChatApi.getChatRoom(call.chatid)?.let { chat ->
            if (!chat.isGroup && !chat.isMeeting && participants.contains(participant)) {
                if (finishCallCountDownTimer == null) {
                    participants.remove(participant)

                    val countDownTimerLiveData: MutableLiveData<Boolean> = MutableLiveData()
                    finishCallCountDownTimer = CustomCountDownTimer(countDownTimerLiveData)

                    countDownTimerLiveData.observeOnce { counterState ->
                        counterState?.let { isFinished ->
                            if (isFinished) {
                                Timber.d("Count down timer ends. Hang call")
                                endCallUseCase.hangCall(call.callId)
                                    .subscribeBy(
                                        onComplete = {
                                            removeFinishCallCountDownTimer()
                                        },
                                        onError = { error ->
                                            Timber.e(error.stackTraceToString())
                                        })
                                    .addTo(disposable)
                            }
                        }
                    }
                }

                Timber.d("Count down timer starts")
                finishCallCountDownTimer?.start(seconds)
            }
        }
    }

    /**
     * Method to stop the countdown
     *
     * @param chatId        Chat ID
     * @param participant   ParticipantInfo
     */
    private fun stopCountDown(chatId: Long, participant: ParticipantInfo) {
        megaChatApi.getChatRoom(chatId)?.let { chat ->
            if (!chat.isGroup && !chat.isMeeting) {
                var participantToRemove: ParticipantInfo? = null
                participants.forEach { participantToCheck ->
                    if (participantToCheck.peerId == participant.peerId) {
                        participantToRemove = participantToCheck
                    }
                }

                if (participantToRemove != null) {
                    participants.remove(participantToRemove)
                }

                participants.add(participant)
                removeFinishCallCountDownTimer()
            }
        }
    }

    /**
     * Remove Finish call Count down timer
     */
    private fun removeFinishCallCountDownTimer() {
        finishCallCountDownTimer?.apply {
            Timber.d("Count down timer stops")
            stop()
        }
        finishCallCountDownTimer = null
    }

    /**
     * Remove Waiting for others Count down timer
     */
    private fun removeWaitingForOthersCountDownTimer() {
        waitingForOthersCountDownTimer?.apply {
            Timber.d("Count down timer stops")
            stop()
        }
        waitingForOthersCountDownTimer = null
    }
}