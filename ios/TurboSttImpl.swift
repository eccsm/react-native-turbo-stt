import Foundation
import Speech
import React

@objc(TurboSttImpl)
public class TurboSttImpl: NSObject, SFSpeechRecognizerDelegate {
  private var speechRecognizer: SFSpeechRecognizer?
  private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
  private var recognitionTask: SFSpeechRecognitionTask?
  private let audioEngine = AVAudioEngine()
  
  @objc public var onEvent: ((String, Any?) -> Void)?

  @objc public func startListening(_ locale: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    let audioSession = AVAudioSession.sharedInstance()
    do {
      try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
      try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
    } catch {
      reject("START_FAILED", "Failed to setup audio session", error)
      return
    }

    speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: locale))
    speechRecognizer?.delegate = self

    recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
    guard let recognitionRequest = recognitionRequest else {
      reject("START_FAILED", "Unable to create recognition request", nil)
      return
    }
    recognitionRequest.shouldReportPartialResults = true
    recognitionRequest.taskHint = .unspecified

    let inputNode = audioEngine.inputNode
    recognitionTask = speechRecognizer?.recognitionTask(with: recognitionRequest) { [weak self] result, error in
      guard let self = self else { return }
      
      var isFinal = false
      
      if let result = result {
        isFinal = result.isFinal
        let text = result.bestTranscription.formattedString
        if isFinal {
          self.onEvent?("onFinalResult", ["text": text])
        } else {
          self.onEvent?("onPartialResults", ["text": text])
        }
      }
      
      if error != nil || isFinal {
        self.audioEngine.stop()
        inputNode.removeTap(onBus: 0)
        self.recognitionRequest = nil
        self.recognitionTask = nil
        
        if let error = error {
            self.onEvent?("onError", ["code": "RECOGNITION_ERROR", "message": error.localizedDescription])
        }
      }
    }

    let recordingFormat = inputNode.outputFormat(forBus: 0)
    inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { (buffer, when) in
      self.recognitionRequest?.append(buffer)
    }

    audioEngine.prepare()
    
    do {
      try audioEngine.start()
      resolve(nil)
    } catch {
      reject("START_FAILED", "Audio engine failed to start", error)
    }
  }

  @objc public func stopListening(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    if audioEngine.isRunning {
      audioEngine.stop()
      recognitionRequest?.endAudio()
      resolve(nil)
    } else {
      resolve(nil)
    }
  }

  @objc public func requestPermission(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    SFSpeechRecognizer.requestAuthorization { authStatus in
      OperationQueue.main.addOperation {
        switch authStatus {
        case .authorized:
          resolve(true)
        default:
          resolve(false)
        }
      }
    }
  }

  @objc public func destroy() {
    audioEngine.stop()
    recognitionTask?.cancel()
    recognitionTask = nil
    recognitionRequest = nil
    speechRecognizer = nil
  }
}
