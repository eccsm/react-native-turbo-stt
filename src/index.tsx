import { useEffect, useState, useCallback } from 'react';
import { NativeEventEmitter } from 'react-native';
import TurboStt from './NativeTurboStt';

const eventEmitter = new NativeEventEmitter(TurboStt);

export interface SpeechResult {
  text: string;
  isFinal: boolean;
}

export interface SpeechError {
  code: string;
  message: string;
}

export function useSpeechToText() {
  const [result, setResult] = useState<SpeechResult | null>(null);
  const [error, setError] = useState<SpeechError | null>(null);
  const [isListening, setIsListening] = useState(false);

  useEffect(() => {
    const onPartial = eventEmitter.addListener(
      'onPartialResults',
      (event: any) => {
        setResult({ text: event.text, isFinal: false });
      }
    );

    const onFinal = eventEmitter.addListener('onFinalResult', (event: any) => {
      setResult({ text: event.text, isFinal: true });
      setIsListening(false);
    });

    const onError = eventEmitter.addListener('onError', (event: any) => {
      setError(event);
      setIsListening(false);
    });

    return () => {
      onPartial.remove();
      onFinal.remove();
      onError.remove();
    };
  }, []);

  const start = useCallback(async (locale: string = 'en-US') => {
    try {
      setError(null);
      setResult(null);
      setIsListening(true);
      await TurboStt.startListening(locale);
    } catch (e: any) {
      setIsListening(false);
      setError({ code: 'START_FAILED', message: e.message });
    }
  }, []);

  const stop = useCallback(async () => {
    try {
      await TurboStt.stopListening();
      setIsListening(false);
    } catch (e: any) {
      setError({ code: 'STOP_FAILED', message: e.message });
    }
  }, []);

  const destroy = useCallback(() => {
    TurboStt.destroy();
    setIsListening(false);
  }, []);

  return {
    result,
    error,
    isListening,
    start,
    stop,
    destroy,
  };
}

export default TurboStt;
