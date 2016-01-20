//
//  PushTalkPlugin.h
//  PushTalk
//
//  Created by zhangqinghe on 13-12-13.
//
//

#import <Cordova/CDV.h>

#define kAMapPluginReceiveNotification @"AMapPluginReceiveNofication"

@interface AMapPlugin : CDVPlugin{
  
}

- (void) configure:(CDVInvokedUrlCommand*)command;
- (void) start:(CDVInvokedUrlCommand*)command;
- (void) stop:(CDVInvokedUrlCommand*)command;
- (void) getLocationLocationWithReGeocode:(CDVInvokedUrlCommand *)command;

@end
