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
        if (speechRecognizer == null) {
          speechRecognizer = SpeechRecognizer.createSpeechRecognizer(reactApplicationContext)
          speechRecognizer?.setRecognitionListener(TurboSttRecognitionListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        // Fix early cutting: Allow 60 seconds of silence for hold-to-speak
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)

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
        val params = Arguments.createMap()
        params.putString("text", matches[0])
        sendEvent("onFinalResult", params)
      }
    }

    override fun onPartialResults(partialResults: Bundle?) {
      val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
      if (!matches.isNullOrEmpty()) {
        val params = Arguments.createMap()
        params.putString("text", matches[0])
        sendEvent("onPartialResults", params)
      }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
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
