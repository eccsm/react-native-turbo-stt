# react-native-turbo-stt

A high-performance React Native Speech-to-Text library built with the new Architecture (Turbo Modules) for React Native 0.76+.

## Features

- ⚡️ **Turbo Module**: Built from the ground up for the new React Native architecture.
- 📱 **Cross-Platform**: Supports both iOS (SFSpeechRecognizer) and Android (SpeechRecognizer).
- 🎯 **Real-time Results**: Get partial and final speech recognition results instantly.
- 🌍 **Multi-language**: Support for system locales.
- 🔒 **Type Safe**: Written in TypeScript with full type definitions.

## Installation

```sh
npm install react-native-turbo-stt
# or
yarn add react-native-turbo-stt
```

## Permissions

### iOS

Add the following keys to your `Info.plist` file:

```xml
<key>NSMicrophoneUsageDescription</key>
<string>We need access to your microphone for speech recognition.</string>
<key>NSSpeechRecognitionUsageDescription</key>
<string>We need permission to recognize your speech.</string>
```

### Android

The library automatically adds the `RECORD_AUDIO` permission to your `AndroidManifest.xml`. However, you still need to request runtime permission from the user.

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

## Usage

The library provides a simple `useSpeechToText` hook to manage speech recognition.

```tsx
import { useSpeechToText } from 'react-native-turbo-stt';
import { Button, Text, View, Platform, PermissionsAndroid } from 'react-native';

export default function App() {
  const { result, error, isListening, start, stop } = useSpeechToText();

  const handleStart = async () => {
    // Request permission on Android
    if (Platform.OS === 'android') {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.RECORD_AUDIO
      );
      if (granted !== PermissionsAndroid.RESULTS.GRANTED) {
        console.log('Permission denied');
        return;
      }
    }
    
    // Start listening (defaults to 'en-US' if not specified)
    await start('en-US');
  };

  return (
    <View style={{ padding: 20 }}>
      <Text>Status: {isListening ? 'Listening...' : 'Idle'}</Text>
      
      {error && <Text style={{ color: 'red' }}>Error: {error.message}</Text>}
      
      <View style={{ marginVertical: 20 }}>
        <Text style={{ fontWeight: 'bold' }}>Result:</Text>
        <Text>{result?.text}</Text>
        <Text style={{ fontSize: 12, color: 'gray' }}>
          {result?.isFinal ? '(Final)' : '(Partial)'}
        </Text>
      </View>

      <Button 
        title={isListening ? "Stop" : "Start Listening"} 
        onPress={isListening ? stop : handleStart} 
      />
    </View>
  );
}
```

## API Reference

### `useSpeechToText()`

Returns an object with the following properties:

| Property | Type | Description |
|----------|------|-------------|
| `result` | `{ text: string, isFinal: boolean } \| null` | The current recognition result. |
| `error` | `{ code: string, message: string } \| null` | Error object if something went wrong. |
| `isListening` | `boolean` | Whether the recognizer is currently active. |
| `start(locale?: string)` | `(locale?: string) => Promise<void>` | Starts listening. Optional locale (e.g., 'en-US', 'tr-TR'). |
| `stop()` | `() => Promise<void>` | Stops listening. |
| `destroy()` | `() => void` | Cleans up resources. |

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
