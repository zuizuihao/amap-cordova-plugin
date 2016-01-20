#import "AMapPlugin.h"
#import <MAMapKit/MAMapKit.h>
#import <AMapLocationKit/AMapLocationKit.h>
#import <UIKit/UIKit.h>

@implementation AMapPlugin {
    AMapLocationManager *locationManager;
}

/**
 * configure plugin
 * @param {Number} stationaryRadius
 * @param {Number} distanceFilter
 * @param {Number} locationTimeout
 */
- (void) configure:(CDVInvokedUrlCommand*)command
{
    [AMapLocationServices sharedServices].apiKey = [[command.arguments objectAtIndex: 0] stringValue];
    // background location cache, for when no network is detected.
    self.locationManager = [[AMapLocationManager alloc] init];
    self.locationManager.delegate = self;
    //设置允许后台定位参数，保持不会被系统挂起
    [self.locationManager setPausesLocationUpdatesAutomatically:NO];
    [self.locationManager setAllowsBackgroundLocationUpdates:YES];//iOS9(含)以上系统需设置
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void) start:(CDVInvokedUrlCommand*)command
{
    NSLog(@"- AMap start");
    [self.locationManager startUpdatingLocation];
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

/**
 * Turn it off
 */
- (void) stop:(CDVInvokedUrlCommand*)command
{
    NSLog(@"- AMap stop");
    [self.locationManager stopUpdatingLocation];
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)amapLocationManager:(MALocationManager *)manager didUpdateLocation:(CLLocation *)location
{
    NSLog(@"location:{lat:%f; lon:%f; accuracy:%f}", location.coordinate.latitude, location.coordinate.longitude, location.horizontalAccuracy);
    dispatch_async(dispatch_get_main_queue(), ^{
                        [self.commandDelegate evalJs:[NSString stringWithFormat:@"cordova.fireDocumentEvent('AMap.updateLocation',%@)",location]];
    });
}

- (void) getLocationLocationWithReGeocode:(CDVInvokedUrlCommand *)command
{
  // 带逆地理（返回坐标和地址信息）
    [self.locationManager requestLocationWithReGeocode:YES completionBlock:^(CLLocation *location, AMapLocationReGeocode *regeocode, NSError *error) {
          // Build a resultset for javascript callback.
          CDVPluginResult* result = nil; 
          if (error)
          {
              NSLog(@"locError:{%ld - %@};", (long)error.code, error.localizedDescription);
              result = [self pluginResultForValue : error];
              if (error.code == AMapLocatingErrorLocateFailed)
              {
                  return;
              }
          }
          
          NSLog(@"location:%@", location);
          result = [self pluginResultForValue:location];
          if (regeocode)
          {
              NSLog(@"reGeocode:%@", regeocode);
          }
          
          if (result) {
            [self succeedWithPluginResult:pushResult withCallbackID:command.callbackId];
          } else {
            [self failWithCallbackID:command.callbackId];
          }
    }];
}

- (CDVPluginResult *)pluginResultForValue:(id)value {
    
    CDVPluginResult *result;
    if ([value isKindOfClass:[NSString class]]) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                   messageAsString:[value stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
    } else if ([value isKindOfClass:[NSNumber class]]) {
        CFNumberType numberType = CFNumberGetType((CFNumberRef)value);
        //note: underlyingly, BOOL values are typedefed as char
        if (numberType == kCFNumberIntType || numberType == kCFNumberCharType) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:[value intValue]];
        } else  {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDouble:[value doubleValue]];
        }
    } else if ([value isKindOfClass:[NSArray class]]) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:value];
    } else if ([value isKindOfClass:[NSDictionary class]]) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:value];
    } else if ([value isKindOfClass:[NSNull class]]) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        NSLog(@"Cordova callback block returned unrecognized type: %@", NSStringFromClass([value class]));
        return nil;
    }
    return result;
}
@end
