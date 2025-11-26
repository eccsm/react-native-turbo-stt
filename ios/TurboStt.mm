#import "TurboStt.h"

#import <React/RCTBridge+Private.h>
#import <React/RCTBridge.h>
#import <React/RCTUtils.h>
#import <ReactCommon/RCTTurboModule.h>

#if __has_include("TurboStt-Swift.h")

#else
#import <TurboStt/TurboStt-Swift.h>
#endif

@implementation TurboStt {
  TurboSttImpl *_impl;
}

@synthesize bridge = _bridge;

- (instancetype)init {
  if (self = [super init]) {
    _impl = [[TurboSttImpl alloc] init];
    __weak typeof(self) weakSelf = self;
    _impl.onEvent = ^(NSString *eventName, id body) {
      [weakSelf sendEvent:eventName body:body];
    };
  }
  return self;
}

- (void)sendEvent:(NSString *)eventName body:(id)body {
  if (self.bridge) {
    [self.bridge enqueueJSCall:@"RCTDeviceEventEmitter"
                        method:@"emit"
                          args:body ? @[ eventName, body ] : @[ eventName ]
                    completion:NULL];
  }
}

- (void)startListening:(NSString *)locale
               resolve:(RCTPromiseResolveBlock)resolve
                reject:(RCTPromiseRejectBlock)reject {
  [_impl startListening:locale resolver:resolve rejecter:reject];
}

- (void)stopListening:(RCTPromiseResolveBlock)resolve
               reject:(RCTPromiseRejectBlock)reject {
  [_impl stopListening:resolve rejecter:reject];
}

- (void)destroy {
  [_impl destroy];
}

- (void)addListener:(NSString *)eventName {
  // Required for TurboModule compatibility
}

- (void)removeListeners:(double)count {
  // Required for TurboModule compatibility
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params {
  return std::make_shared<facebook::react::NativeTurboSttSpecJSI>(params);
}

+ (NSString *)moduleName {
  return @"TurboStt";
}

@end
