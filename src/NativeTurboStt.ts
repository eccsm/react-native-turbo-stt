import { TurboModuleRegistry, type TurboModule } from 'react-native';

export interface Spec extends TurboModule {
  startListening(locale: string): Promise<void>;
  stopListening(): Promise<void>;
  destroy(): void;
  addListener(eventName: string): void;
  removeListeners(count: number): void;
  requestPermission(): Promise<boolean>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('TurboStt');
