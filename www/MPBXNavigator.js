

var mpbx_nav =
        {
            showNavigator: function (mpbxNavigatorSuccess, mpbxNavigatorError, action, args)
            {
                cordova.exec(
                        mpbxNavigatorSuccess,
                        mpbxNavigatorError,
                        'MPBXNavigator',
                        'showNavigator',
                        [args]
                            );
            }
        };
        
        module.exports = mpbx_nav;