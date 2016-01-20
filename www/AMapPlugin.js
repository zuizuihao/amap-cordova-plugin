
var AMapPlugin = function () {
};

AMapPlugin.prototype.error_callback = function (msg) {
  console.log("Javascript Callback Error: " + msg)
}

AMapPlugin.prototype.call_native = function (name, args, callback) {
  var ret = cordova.exec(callback, this.error_callback, 'AMapPlugin', name, args);
  return ret;
}

AMapPlugin.prototype.init = function () {
  this.call_native("init", data, null);
}

module.exports = new AMapPlugin(); 

