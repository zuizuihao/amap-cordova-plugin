#import "AMapPlugin.h"
#import <AMapSearchKit/AMapSearchKit.h>
#import <AMapLocationKit/AMapLocationKit.h>
#import <UIKit/UIKit.h>

@implementation AMapPlugin {
    AMapLocationManager *locationManager;
    AMapSearchAPI *searchAPI;
}

/**
 * configure plugin
 * @param {Number} stationaryRadius
 * @param {Number} distanceFilter
 * @param {Number} locationTimeout
 */
- (void) configure:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        NSString * apiKey = [command.arguments objectAtIndex: 0];
        NSLog(@"- AMap configure %@", apiKey);
        [AMapLocationServices sharedServices].apiKey = apiKey;
        [AMapSearchServices sharedServices].apiKey = apiKey;

        searchAPI = [[AMapSearchAPI alloc] init];
        searchAPI.delegate = self;
        // background location cache, for when no network is detected.
        locationManager = [[AMapLocationManager alloc] init];
        locationManager.delegate = self;
        //设定定位的最小更新距离，默认为 kCLDistanceFilterNone 。
        locationManager.distanceFilter = [[command.arguments objectAtIndex: 1] intValue];
        // 带逆地理信息的一次定位（返回坐标和地址信息）
        [locationManager setDesiredAccuracy:kCLLocationAccuracyHundredMeters];
        //设置允许后台定位参数，保持不会被系统挂起
        [locationManager setPausesLocationUpdatesAutomatically:NO];
        [locationManager setAllowsBackgroundLocationUpdates:YES];//iOS9(含)以上系统需设置
    
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

- (void) start:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^ {
        NSLog(@"- AMap start");
        [locationManager startUpdatingLocation];
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

/**
 * Turn it off
 */
- (void) stop:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^ {
        NSLog(@"- AMap stop");
        [locationManager stopUpdatingLocation];
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

- (void)amapLocationManager:(AMapLocationManager *)manager didUpdateLocation:(CLLocation *)location
{
    //NSLog(@"Update Location");
    //构造AMapReGeocodeSearchRequest对象
    AMapReGeocodeSearchRequest *regeo = [[AMapReGeocodeSearchRequest alloc] init];
    regeo.location = [AMapGeoPoint locationWithLatitude:location.coordinate.latitude longitude:location.coordinate.longitude];
    regeo.radius = 10000;
    regeo.requireExtension = YES;

    //发起逆地理编码
    [searchAPI AMapReGoecodeSearch: regeo];
}

- (void)onReGeocodeSearchDone:(AMapReGeocodeSearchRequest *)request response:(AMapReGeocodeSearchResponse *)response
{
    if(response.regeocode != nil)
    {
        //NSLog(@"ReGeocodeSearch Done");
        //通过AMapReGeocodeSearchResponse对象处理搜索结果
        AMapAddressComponent *regeocode = response.regeocode.addressComponent;
        NSString *address = [NSString stringWithFormat:@"%@%@%@%@",regeocode.province, regeocode.city, regeocode.district, regeocode.township];
        //NSLog(@"location:{lat:%f; lon:%f; accuracy:%f}", location.coordinate.latitude, location.coordinate.longitude, location.horizontalAccuracy);
        NSMutableDictionary* dict = [NSMutableDictionary dictionaryWithCapacity:2];
        [dict setObject: [NSNumber numberWithFloat: request.location.latitude] forKey:@"latitude"];
        [dict setObject: [NSNumber numberWithFloat: request.location.longitude] forKey:@"longitude"];
        [dict setObject: address forKey:@"address"];
        
        NSError  *error;
        
        NSData   *jsonData   = [NSJSONSerialization dataWithJSONObject:dict options:0 error:&error];
        NSString *jsonString = [[NSString alloc]initWithData:jsonData encoding:NSUTF8StringEncoding];
        
        dispatch_async(dispatch_get_main_queue(), ^{
            [self.commandDelegate evalJs:[NSString stringWithFormat:@"cordova.fireDocumentEvent('AMap.updateLocation',%@)",jsonString]];
        });
    }
}

- (void) getLocationWithReGeocode:(CDVInvokedUrlCommand *)command
{
        [self.commandDelegate runInBackground:^ {
            //NSLog(@"- AMap getLocationWithReGeocode");
            // 带逆地理（返回坐标和地址信息）
            [locationManager requestLocationWithReGeocode:YES completionBlock:^(CLLocation *location, AMapLocationReGeocode *regeocode, NSError *error) {
                NSMutableDictionary* dict = [NSMutableDictionary dictionaryWithCapacity:2];
                // Build a resultset for javascript callback.
                CDVPluginResult* result = nil;
                if (error)
                {
                    NSLog(@"locError:{%ld - %@};", (long)error.code, error.localizedDescription);
                    [self failWithCallbackID:command.callbackId];
                    return ;
                }
                
                [dict setObject: [NSNumber numberWithFloat: location.coordinate.latitude] forKey:@"latitude"];
                [dict setObject: [NSNumber numberWithFloat: location.coordinate.longitude] forKey:@"longitude"];
                if(regeocode){
                    if(regeocode.province){
                      NSString *address = [NSString stringWithFormat:@"%@%@%@%@",regeocode.province, regeocode.city, regeocode.district, regeocode.township];
                      [dict setObject: address forKey:@"address"];
                    }
                }
                result = [self pluginResultForValue: dict];
                [self succeedWithPluginResult:result withCallbackID:command.callbackId];
            }];
        }];
}

-(void)failWithCallbackID:(NSString *)callbackID {
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    [self.commandDelegate sendPluginResult:result callbackId:callbackID];
}
- (void)succeedWithPluginResult:(CDVPluginResult *)result withCallbackID:(NSString *)callbackID {
    [self.commandDelegate sendPluginResult:result callbackId:callbackID];
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