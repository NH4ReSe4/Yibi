package com.dubiao.yibi.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.dubiao.yibi.domain.VoiceParser

internal fun speechRecognitionIntent(preferOffline: Boolean): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
    }

class OnDeviceSpeechController(
    private val context: Context,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
) : RecognitionListener {
    private var recognizer: SpeechRecognizer? = null
    private var usingOnDevice = false
    private var onlineFallbackAttempted = false

    val isAvailable: Boolean
        get() = hasOnDeviceRecognizer() || SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        if (!isAvailable) {
            onError("当前设备没有可用的系统语音识别服务")
            return
        }
        onlineFallbackAttempted = false
        startRecognizer(preferOnDevice = hasOnDeviceRecognizer())
    }

    private fun hasOnDeviceRecognizer(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    private fun startRecognizer(preferOnDevice: Boolean) {
        recognizer?.destroy()
        usingOnDevice = preferOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        recognizer = if (usingOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }.also { it.setRecognitionListener(this) }
        recognizer?.startListening(speechRecognitionIntent(preferOffline = usingOnDevice))
    }

    fun stop() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    override fun onReadyForSpeech(params: Bundle?) = onListeningChanged(true)
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = onListeningChanged(false)
    override fun onPartialResults(partialResults: Bundle?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    override fun onResults(results: Bundle?) {
        onListeningChanged(false)
        val text = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.let(VoiceParser::selectBestCandidate)
        if (text.isNullOrBlank()) onError("没有听清，请再说一次") else onResult(text)
    }

    override fun onError(error: Int) {
        onListeningChanged(false)
        if (usingOnDevice && !onlineFallbackAttempted && shouldUseOnlineFallback(error) &&
            SpeechRecognizer.isRecognitionAvailable(context)
        ) {
            onlineFallbackAttempted = true
            onError("离线语音暂不可用，已切换系统语音，请再说一次")
            startRecognizer(preferOnDevice = false)
            return
        }
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "录音服务异常，请重试"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "需要麦克风权限才能使用语音记账"
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络语音识别不可用，请检查网络后重试"
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "系统语音服务暂不支持普通话（中国大陆）"
            SpeechRecognizer.ERROR_NO_MATCH -> "没有听清，请放慢一些再说一次"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "语音识别正忙，请稍后再试"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "未检测到有效语音"
            else -> "系统语音识别暂不可用"
        }
        onError(message)
    }

    private fun shouldUseOnlineFallback(error: Int): Boolean = when (error) {
        SpeechRecognizer.ERROR_CLIENT,
        SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> true
        else -> false
    }
}
