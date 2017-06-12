

var mpbx_nav =
        {
            showNavigator: function (mpbxNavigatorSuccess, mpbxNavigatorError, action, args)
            {
                cordova.exec(
                        phoneStateSuccessCallback,
                        phoneStateErrorCallback,
                        'MPBXNavigator',
                        action,
                        [args]
                            );
            }
        };
        
        module.exports = mpbx_nav;