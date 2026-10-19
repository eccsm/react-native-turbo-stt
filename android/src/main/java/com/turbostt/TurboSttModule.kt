package com.turbostt

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener

@ReactModule(name = TurboSttModule.NAME)
class TurboSttModule(reactContext: ReactApplicationContext) :
  NativeTurboSttSpec(reactContext) {

  private var speechRecognizer: SpeechRecognizer? = null
  private val mainHandler = Handler(Looper.getMainLooper())
  private var committedTranscript = ""
  private var lastPartial = ""
  private var stopRequested = false

  override fun getName(): String {
    return NAME
  }

  override fun requestPermission(promise: Promise) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      promise.resolve(true)
      return
    }

    val context = reactApplicationContext
    val activity = currentActivity

    if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
      promise.resolve(true)
      return
    }

    if (activity is PermissionAwareActivity) {
      activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 101, object : PermissionListener {
        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
          if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
              promise.resolve(true)
            } else {
              promise.resolve(false)
            }
            return true
          }
          return false
        }
      })
    } else {
      promise.reject("NO_ACTIVITY", "Activity not found or not PermissionAware")
    }
  }

  override fun startListening(locale: String, promise: Promise) {
    mainHandler.post {
      try {
        committedTranscript = ""
        lastPartial = ""
        stopRequested = false
        if (speechRecognizer == null) {
          speechRecognizer = SpeechRecognizer.createSpeechRecognizer(reactApplicationContext)
          speechRecognizer?.setRecognitionListener(TurboSttRecognitionListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        // Fix early cutting: Allow 60 seconds of silence for hold-to-speak
        // RecognizerIntent documents these extras as Integer values. Some
        // recognition services reject Long values and fall back to zero,
        // ending and restarting the session (and replaying their system beep).
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60000)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 60000)

        speechRecognizer?.startListening(intent)
        promise.resolve(null)
      } catch (e: Exception) {
        promise.reject("START_FAILED", e.message)
      }
    }
  }

  override fun stopListening(promise: Promise) {
    mainHandler.post {
      try {
        commitPendingPartial()
        stopRequested = true
        emitTranscript("onFinalResult", committedTranscript)
        speechRecognizer?.stopListening()
        promise.resolve(null)
      } catch (e: Exception) {
        promise.reject("STOP_FAILED", e.message)
      }
    }
  }

  override fun destroy() {
    mainHandler.post {
      try {
        speechRecognizer?.destroy()
        speechRecognizer = null
      } catch (e: Exception) {
        // Ignore
      }
    }
  }

  override fun addListener(eventName: String) {
    // Required for TurboModule compatibility
  }

  override fun removeListeners(count: Double) {
    // Required for TurboModule compatibility
  }

  private fun sendEvent(eventName: String, params: WritableMap) {
    reactApplicationContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }

  inner class TurboSttRecognitionListener : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
      val params = Arguments.createMap()
      params.putString("code", "SPEECH_ERROR")
      params.putString("message", getErrorMessage(error))
      sendEvent("onError", params)
    }

    override fun onResults(results: Bundle?) {
      val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
      if (!matches.isNullOrEmpty()) {
        committedTranscript = joinTranscript(committedTranscript, matches[0])
        lastPartial = ""
        val params = Arguments.createMap()
        params.putString("text", committedTranscript)
        // Google's Android recognizer can keep one microphone session alive
        // and call onResults once per phrase. Treat those phrase boundaries as
        // partial results; the user's explicit stop is the end of the whole
        // dictation. Otherwise every pause replaces the text in JavaScript.
        sendEvent(if (stopRequested) "onFinalResult" else "onPartialResults", params)
      }
    }

    override fun onSegmentResults(segmentResults: Bundle) {
      val matches = segmentResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
      if (!matches.isNullOrEmpty()) {
        committedTranscript = joinTranscript(committedTranscript, matches[0])
        lastPartial = ""
        val params = Arguments.createMap()
        params.putString("text", committedTranscript)
        sendEvent("onPartialResults", params)
      }
    }

    override fun onEndOfSegmentedSession() {
      if (committedTranscript.isNotEmpty()) {
        val params = Arguments.createMap()
        params.putString("text", committedTranscript)
        sendEvent("onFinalResult", params)
      }
    }

    override fun onPartialResults(partialResults: Bundle?) {
      val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
      val text = matches?.firstOrNull()?.trim().orEmpty()
      // Google's segmented recognizer on some Samsung devices reports only
      // partial results. A blank partial is its phrase boundary; keep the last
      // non-blank hypothesis before the next phrase starts.
      if (text.isEmpty()) {
        if (commitPendingPartial()) emitTranscript("onPartialResults", committedTranscript)
        return
      }
      lastPartial = text
      emitTranscript("onPartialResults", joinTranscript(committedTranscript, text))
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
  }

  private fun joinTranscript(committed: String, current: String): String {
    val before = committed.trim()
    val now = current.trim()
    if (before.isEmpty()) return now
    if (now.isEmpty() || before.endsWith(now, ignoreCase = true)) return before
    if (now.startsWith(before, ignoreCase = true)) return now
    return "$before $now"
  }

  private fun commitPendingPartial(): Boolean {
    if (lastPartial.isEmpty()) return false
    committedTranscript = joinTranscript(committedTranscript, lastPartial)
    lastPartial = ""
    return true
  }

  private fun emitTranscript(eventName: String, text: String) {
    if (text.isEmpty()) return
    val params = Arguments.createMap()
    params.putString("text", text)
    sendEvent(eventName, params)
  }

  private fun getErrorMessage(errorCode: Int): String {
    return when (errorCode) {
      SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
      SpeechRecognizer.ERROR_CLIENT -> "Client side error"
      SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
      SpeechRecognizer.ERROR_NETWORK -> "Network error"
      SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
      SpeechRecognizer.ERROR_NO_MATCH -> "No match"
      SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
      SpeechRecognizer.ERROR_SERVER -> "Server error"
      SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
      else -> "Unknown error"
    }
  }

  companion object {
    const val NAME = "TurboStt"
  }
}
