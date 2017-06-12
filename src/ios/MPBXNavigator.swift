//
//  MPBXNavigator.swift
//  
//
//  Created by Fabio Cingolani on 12/06/2017.
//
//

import Foundation

@objc(MPBXNavigator) class MPBXNavigator : CDVPlugin {
    
    
    var destination: MGLPointAnnotation?
    var customMarker: CustomPointAnnotation!
    var navigation: RouteController?
    var userRoute: Route?
    var POIS = [CustomPointAnnotation]()
    
    @IBOutlet weak var mapView: NavigationMapView!
    @IBOutlet weak var startNavigationButton: UIButton!
    @IBOutlet weak var simulateNavigationButton: UIButton!
    @IBOutlet weak var howToBeginLabel: UILabel!
    
    func echo(command:CDVInvokeUrlCommand) {
        
        automaticallyAdjustsScrollViewInsets = false
        mapView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: 44, right: 0)
        mapView.delegate = self
        mapView.navigationMapDelegate = self
        
        mapView.userTrackingMode = .follow
        
        
        let navigationViewController = NavigationViewController(for: route)
        
        // If you'd like to use AWS Polly, provide your IdentityPoolId below
        // `identityPoolId` is a required value for using AWS Polly voice instead of iOS's built in AVSpeechSynthesizer
        // You can get a token here: http://docs.aws.amazon.com/mobile/sdkforios/developerguide/cognito-auth-aws-identity-for-ios.html
        // viewController.voiceController?.identityPoolId = "Your AWS IdentityPoolId. Remove Argument if you do not want to use AWS Polly"
        
        navigationViewController.routeController.snapsUserLocationAnnotationToRoute = true
        navigationViewController.voiceController?.volume = 0.5
        navigationViewController.navigationDelegate = self
        
        // Uncomment to apply custom styles
        //        styleForRegular().apply()
        styleForCompact().apply()
        //        styleForiPad().apply()
        //        styleForCarPlay().apply()
        
        let camera = mapView.camera
        camera.pitch = 45
        camera.altitude = 1_000
        navigationViewController.pendingCamera = camera
        
        present(navigationViewController, animated: true, completion: nil)
    }
}
