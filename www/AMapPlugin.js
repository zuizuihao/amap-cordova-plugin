
var AMapPlugin = function () {
};

AMapPlugin.prototype.call_native = function (name, args, callback, error_callback) {
  var ret = cordova.exec(callback, error_callback, 'AMapPlugin', name, args);
  return ret;
}

AMapPlugin.prototype.configure = function (apiKey, distanceFilter, callback, error_callback) {
  this.call_native("configure", [apiKey, distanceFilter], callback, error_callback);
}

AMapPlugin.prototype.start = function (callback, error_callback) {
  this.call_native("start", [], callback, error_callback);
}

AMapPlugin.prototype.stop = function (callback, error_callback) {
  this.call_native("stop", [], callback, error_callback);
}

AMapPlugin.prototype.getLocationWithReGeocode = function (callback, error_callback) {
  this.call_native("getLocationWithReGeocode", [], callback, error_callback);
}

module.exports = new AMapPlugin(); 

