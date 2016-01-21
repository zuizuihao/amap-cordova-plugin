package com.roadshr.cordova.amap;

import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import com.amap.api.location;

public class AMapPlugin extends CordovaPlugin implements AMapLocationListener{
    /** LOG TAG */
    private static final String LOG_TAG = AMapPlugin.class.getSimpleName();

	  /** JS回调接口对象 */
    public static CallbackContext updateLocationCallbackContext = null;
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明mLocationOption对象
    public AMapLocationClientOption mLocationOption = null;
  
    /**
     * 插件初始化
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    	LOG.d(LOG_TAG, "AMapPlugin#initialize");

        super.initialize(cordova, webView);
    }

    /**
     * 插件主入口
     */
    @Override
    public boolean execute(String action, final JSONArray args, CallbackContext callbackContext) throws JSONException {
    	LOG.d(LOG_TAG, "AMapPlugin#execute");

    	boolean ret = false;
        
        if ("configure".equalsIgnoreCase(action)) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
          
            final String apiKey = args.getString(0);
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                	LOG.d(LOG_TAG, "AMapPlugin#configure");
                    AmapLocationClient.setApiKey(apiKey);
                }
            });
            ret =  true;
        }if ("start".equalsIgnoreCase(action)) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
          
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                	LOG.d(LOG_TAG, "AMapPlugin#start");
                  //初始化定位
                  mLocationClient = new AMapLocationClient(getApplicationContext());
                  //设置定位回调监听
                  mlocationClient.setLocationListener(this);
                  
                  //初始化定位参数
                  mLocationOption = new AMapLocationClientOption();
                  //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
                  mLocationOption.setLocationMode(AMapLocationMode.Battery_Saving);
                  //设置是否返回地址信息（默认返回地址信息）
                  mLocationOption.setNeedAddress(true);
                  //设置是否只定位一次,默认为false
                  mLocationOption.setOnceLocation(false);
                  //设置是否强制刷新WIFI，默认为强制刷新
                  mLocationOption.setWifiActiveScan(true);
                  //设置是否允许模拟位置,默认为false，不允许模拟位置
                  mLocationOption.setMockEnable(false);
                  //设置定位间隔,单位毫秒,默认为2000ms
                  mLocationOption.setInterval(2000);
                  //给定位客户端对象设置定位参数
                  mlocationClient.setLocationOption(mLocationOption);
                  //启动定位
                  mlocationClient.startLocation();
                }
            });
            ret =  true;
        } else if ("stop".equalsIgnoreCase(action)){
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                	LOG.d(LOG_TAG, "AMapPlugin#stop");
                    PushManager.stopWork(cordova.getActivity().getApplicationContext());
                }
            });
            ret =  true;
        } else if ("getLocationLocationWithReGeocode".equalsIgnoreCase(action)) {
            updateLocationCallbackContext = callbackContext;
            
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            ret = true;
        }

        return ret;
    }
    
    // 定位监听
	  @Override
    public void onLocationChanged(AMapLocation amapLocation) {
            PluginResult result = null;
            if (amapLocation != null) {
                if (amapLocation.getErrorCode() == 0) {
                //定位成功回调信息，设置相关消息
                amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                amapLocation.getLatitude();//获取纬度
                amapLocation.getLongitude();//获取经度
                amapLocation.getAccuracy();//获取精度信息
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(amapLocation.getTime());
                df.format(date);//定位时间
                amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                amapLocation.getCountry();//国家信息
                amapLocation.getProvince();//省信息
                amapLocation.getCity();//城市信息
                amapLocation.getDistrict();//城区信息
                amapLocation.getStreet();//街道信息
                        amapLocation.getStreetNum();//街道门牌号信息
                amapLocation.getCityCode();//城市编码
                amapLocation.getAdCode();//地区编码
                
                result = new PluginResult(PluginResult.Status.OK, amapLocation);
            } else {
                      //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError","location Error, ErrCode:"
                    + amapLocation.getErrorCode() + ", errInfo:"
                    + amapLocation.getErrorInfo());
                }
                result = new PluginResult(PluginResult.Status.ERROR, amapLocation);
            }
            
            if (updateLocationCallbackContext != null) {
                  updateLocationCallbackContext.sendPluginResult(result);
            }
      }

}
