import React, {useState} from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  ScrollView,
  ActivityIndicator,
  PermissionsAndroid,
  Platform,
} from 'react-native';
import {useSpeechToText} from 'react-native-turbo-stt';

function App(): React.JSX.Element {
  const {result, error, isListening, start, stop} = useSpeechToText();
  const [locale] = useState('en-US');

  const requestPermission = async () => {
    if (Platform.OS !== 'android') {
      return true;
    }

    try {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
        {
          title: 'Microphone Permission',
          message:
            'This app needs access to your microphone to recognize speech.',
          buttonNeutral: 'Ask Me Later',
          buttonNegative: 'Cancel',
          buttonPositive: 'OK',
        },
      );
      return granted === PermissionsAndroid.RESULTS.GRANTED;
    } catch (err) {
      console.warn(err);
      return false;
    }
  };

  const handleToggleListening = async () => {
    if (isListening) {
      stop();
    } else {
      const hasPermission = await requestPermission();
      if (hasPermission) {
        start(locale);
      } else {
        console.log('Permission denied');
      }
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Turbo STT ⚡️</Text>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.statusContainer}>
          <Text style={styles.label}>Status:</Text>
          <View style={styles.row}>
            {isListening && <ActivityIndicator size="small" color="#007AFF" />}
            <Text style={[styles.value, isListening && styles.activeText]}>
              {isListening ? ' Listening...' : ' Idle'}
            </Text>
          </View>
        </View>

        {error && (
          <View style={styles.errorBox}>
            <Text style={styles.errorTitle}>Error:</Text>
            <Text style={styles.errorText}>
              {error.code}: {error.message}
            </Text>
          </View>
        )}

        <View style={styles.resultBox}>
          <Text style={styles.label}>Recognized Text:</Text>
          <Text style={styles.resultText}>
            {result?.text || 'Press start and say something...'}
          </Text>
          {result?.isFinal && (
            <Text style={styles.finalBadge}>Final Result</Text>
          )}
        </View>
      </ScrollView>

      <View style={styles.footer}>
        <TouchableOpacity
          style={[
            styles.button,
            isListening ? styles.stopButton : styles.startButton,
          ]}
          onPress={handleToggleListening}>
          <Text style={styles.buttonText}>
            {isListening ? 'Stop Listening' : 'Start Listening'}
          </Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  header: {
    padding: 20,
    backgroundColor: '#FFFFFF',
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
    alignItems: 'center',
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
  },
  content: {
    padding: 20,
  },
  statusContainer: {
    marginBottom: 20,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#FFF',
    padding: 15,
    borderRadius: 10,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  label: {
    fontSize: 14,
    color: '#666',
    fontWeight: '600',
    marginBottom: 5,
  },
  value: {
    fontSize: 16,
    color: '#333',
    marginLeft: 5,
  },
  activeText: {
    color: '#007AFF',
    fontWeight: 'bold',
  },
  errorBox: {
    backgroundColor: '#FFEBEE',
    padding: 15,
    borderRadius: 10,
    marginBottom: 20,
    borderLeftWidth: 4,
    borderLeftColor: '#D32F2F',
  },
  errorTitle: {
    color: '#D32F2F',
    fontWeight: 'bold',
    marginBottom: 5,
  },
  errorText: {
    color: '#B71C1C',
  },
  resultBox: {
    backgroundColor: '#FFFFFF',
    padding: 20,
    borderRadius: 12,
    minHeight: 200,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 1},
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },
  resultText: {
    fontSize: 18,
    color: '#333',
    lineHeight: 26,
    marginTop: 10,
  },
  finalBadge: {
    marginTop: 10,
    alignSelf: 'flex-start',
    backgroundColor: '#E8F5E9',
    color: '#2E7D32',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
    fontSize: 12,
    fontWeight: 'bold',
  },
  footer: {
    padding: 20,
    backgroundColor: '#FFFFFF',
    borderTopWidth: 1,
    borderTopColor: '#E0E0E0',
  },
  button: {
    paddingVertical: 16,
    borderRadius: 12,
    alignItems: 'center',
  },
  startButton: {
    backgroundColor: '#007AFF',
  },
  stopButton: {
    backgroundColor: '#FF3B30',
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default App;
