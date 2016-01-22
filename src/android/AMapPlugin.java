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
import org.json.JSONObject;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;

public class AMapPlugin extends CordovaPlugin implements AMapLocationListener{
	  private static AMapPlugin instance;
    /** LOG TAG */
    private static final String TAG = AMapPlugin.class.getSimpleName();

	  /** JS回调接口对象 */
    public static CallbackContext updateLocationCallbackContext = null;
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明mLocationOption对象
    public AMapLocationClientOption mLocationOption = null;
    
    public AMapPlugin() {
		    instance = this;
	  }

    /**
     * 插件初始化
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    	LOG.d(TAG, "AMapPlugin#initialize");

        super.initialize(cordova, webView);
    }

    /**
     * 插件主入口
     */
    @Override
    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    	LOG.d(TAG, "AMapPlugin#execute");

    	boolean ret = false;
      if ("configure".equalsIgnoreCase(action)) {
            final String apiKey = args.getString(0);
            final long interval = args.getLong(1);
            
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                  AMapLocationClient.setApiKey(apiKey);
                  //初始化定位
                  mLocationClient = new AMapLocationClient(cordova.getActivity().getApplicationContext());
                  //设置定位回调监听
                  mLocationClient.setLocationListener(instance);
                  //初始化定位参数
                  mLocationOption = new AMapLocationClientOption();
                  //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
                  mLocationOption.setLocationMode(AMapLocationMode.Battery_Saving);
                  //设置是否返回地址信息（默认返回地址信息）
                  mLocationOption.setNeedAddress(true);
                  //设置是否强制刷新WIFI，默认为强制刷新
                  mLocationOption.setWifiActiveScan(true);
                  //设置是否允许模拟位置,默认为false，不允许模拟位置
                  mLocationOption.setMockEnable(false);
                  //设置定位间隔,单位毫秒,默认为2000ms
                  mLocationOption.setInterval(interval);
                  //给定位客户端对象设置定位参数
                  mLocationClient.setLocationOption(mLocationOption);
                  
                  PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
                  pluginResult.setKeepCallback(false);
                  callbackContext.sendPluginResult(pluginResult);
                  LOG.d(TAG, "AMapPlugin#configure");
                }
            });
            ret =  true;
        }if ("start".equalsIgnoreCase(action)) {
            updateLocationCallbackContext = callbackContext;
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                	LOG.d(TAG, "AMapPlugin#start");
                  //设置是否只定位一次,默认为false
                  mLocationOption.setOnceLocation(false);
                  //启动定位
                  mLocationClient.startLocation();

                  PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
                  pluginResult.setKeepCallback(true);
                  updateLocationCallbackContext.sendPluginResult(pluginResult);
                }
            });
            ret =  true;
        } else if ("stop".equalsIgnoreCase(action)){
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                	LOG.d(TAG, "AMapPlugin#stop");
                  mLocationClient.stopLocation();
                  
                  PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
                  pluginResult.setKeepCallback(false);
                  callbackContext.sendPluginResult(pluginResult);
                }
            });
            ret =  true;
        } else if ("getLocationWithReGeocode".equalsIgnoreCase(action)) {
            updateLocationCallbackContext = callbackContext;
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            
            //设置是否只定位一次,默认为false
            mLocationOption.setOnceLocation(true);
            //启动定位
            mLocationClient.startLocation();
            ret = true;
        }
        return ret;
    }
    
     // 定位监听
	  @Override
    public void onLocationChanged(AMapLocation location) {
      try{
          JSONObject data = new JSONObject();
          if (location != null) {
              if (location.getErrorCode() == 0) {
                  String address = String.format("%s%s%s%s",location.getProvince(),location.getCity()
                                ,location.getDistrict(),location.getStreet()); 
                  setStringData(data, "latitude", String.valueOf(location.getLatitude()));
                  setStringData(data, "longitude", String.valueOf(location.getLongitude()));
                  setStringData(data, "address", address);
                  sendSuccessData(updateLocationCallbackContext, data, true);
              } else {
                  //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                  Log.e("AmapError","location Error, ErrCode:"
                      + location.getErrorCode() + ", errInfo:"
                      + location.getErrorInfo());
                  setStringData(data, "errCode", String.valueOf(location.getErrorCode()));
                  setStringData(data, "errInfo", location.getErrorInfo());
                  sendErrorData(updateLocationCallbackContext, data, true);
              }
          }
        } catch (JSONException e) {
           LOG.e(TAG, e.toString());
		    }catch (NullPointerException e) {
          
        } catch (Exception e) {}
    }
    
    /**
     * 设定字符串类型JSON对象，如值为空时不设定
     * 
     * @param jsonObject JSON对象
     * @param name 关键字
     * @param value 值
     * @throws JSONException JSON异常
     */
    private void setStringData(JSONObject jsonObject, String name, String value) throws JSONException {
    	if (value != null && !"".equals(value)) {
    		jsonObject.put(name, value);
    	}
    }
    
    /**
     * 接收推送成功内容并返回给前端JS
     * 
     * @param jsonObject JSON对象
     */
    private void sendSuccessData(CallbackContext callbackContext, JSONObject jsonObject, boolean isCallBackKeep) {
        Log.d(TAG, "BaiduPushReceiver#sendSuccessData: " + (jsonObject != null ? jsonObject.toString() : "null"));

        if (callbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, jsonObject);
            result.setKeepCallback(isCallBackKeep);
            callbackContext.sendPluginResult(result);
        }
    }
    
    /**
     * 接收推送失败内容并返回给前端JS
     * 
     * @param jsonObject JSON对象
     */
    private void sendErrorData(CallbackContext callbackContext, JSONObject jsonObject, boolean isCallBackKeep) {
        Log.d(TAG, "BaiduPushReceiver#sendErrorData: " + (jsonObject != null ? jsonObject.toString() : "null"));

        if (callbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, jsonObject);
            result.setKeepCallback(false);
            callbackContext.sendPluginResult(result);
        }
    }
}