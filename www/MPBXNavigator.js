var exec = require('cordova/exec');

exports.showMap = function(arg0, success, error) {
    exec(success, error, "MPBXNavigator", "coolMethod", [arg0]);
};
