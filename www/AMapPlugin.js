
var AMapPlugin = function () {
};

AMapPlugin.prototype.error_callback = function (msg) {
  console.log("Javascript Callback Error: " + msg)
}

AMapPlugin.prototype.call_native = function (name, args, callback) {
  var ret = cordova.exec(callback, this.error_callback, 'AMapPlugin', name, args);
  return ret;
}

AMapPlugin.prototype.configure = function (apiKey) {
  this.call_native("configure", apiKey, null);
}

AMapPlugin.prototype.start = function () {
  this.call_native("start", [pageName], null);
}

AMapPlugin.prototype.stop = function () {
  this.call_native("stop", null);
}

module.exports = new AMapPlugin(); 

