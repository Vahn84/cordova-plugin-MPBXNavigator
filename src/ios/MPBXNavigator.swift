//
//  MPBXNavigator.swift
//
//
//  Created by Fabio Cingolani on 12/06/2017.
//
//
import UIKit
import MapboxCoreNavigation
import MapboxNavigation
import MapboxDirections
import Mapbox


@objc(MPBXNavigator) class MPBXNavigator : CDVPlugin, MGLMapViewDelegate, NavigationMapViewDelegate,
NavigationViewControllerDelegate, CLLocationManagerDelegate {


    var destination: CLLocationCoordinate2D?
    var origin: CLLocationCoordinate2D?
    var locationManager: CLLocationManager?
    var mapView: NavigationMapView!
    var originJSON:[String:Any] = ["": ""]
    var destinationJSON:[String: Any] = ["": ""]
    var originLat:Double = -1
    var originLng:Double = -1
    var destinationLat:Double = -1
    var destinationLng:Double = -1

    func showNavigator(_ command:CDVInvokedUrlCommand) {

        let json = command.arguments[0] as? [String: Any]
        if(json != nil) {
            originJSON = (json?["origin"] as? [String:Any])!
            destinationJSON = (json?["destination"]  as? [String:Any])!
            originLat = originJSON["latitude"] as! Double
            originLng = originJSON["longitude"] as! Double
            destinationLat = destinationJSON["latitude"] as! Double
            destinationLng = destinationJSON["longitude"] as! Double
        }

        if(destinationLat > -1) {
            destination = CLLocationCoordinate2D(latitude: destinationLat, longitude: destinationLng)
        }

        if(originLat > -1) {
            origin = CLLocationCoordinate2D(latitude: originLat, longitude: originLng)
            getRoute();
        } else {
            locationManager = CLLocationManager()
            locationManager?.delegate = self
            locationManager?.requestWhenInUseAuthorization();
            locationManager?.startUpdatingLocation()
        }


        print("NAVIGATION POINT:", originLat)
        mapView = NavigationMapView();
        mapView.delegate = self
        mapView.navigationMapDelegate = self
        mapView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: 44, right: 0)
        mapView.userTrackingMode = .follow




    }

    func getRoute(didFinish: (()->())? = nil) {


        let options = RouteOptions(coordinates: [self.origin!, self.destination!])
        options.includesSteps = true
        options.routeShapeResolution = .full
        options.profileIdentifier = .automobileAvoidingTraffic

        _ = Directions.shared.calculate(options) { [weak self] (waypoints, routes, error) in
            guard error == nil else {
                print(error!)
                return
            }
            guard let route = routes?.first else {
                return
            }

            // Open method for adding and updating the route line
            self?.mapView.showRoute(route)
            self?.startNavigation(along: route)
            didFinish?()
        }
    }

    func startNavigation(along route: Route) {
        // Pass through a
        // 1. the route the user will take
        // 2. A `Directions` class, used for rerouting.
        let navigationViewController = NavigationViewController(for: route)

        // If you'd like to use AWS Polly, provide your IdentityPoolId below
        // `identityPoolId` is a required value for using AWS Polly voice instead of iOS's built in AVSpeechSynthesizer
        // You can get a token here: http://docs.aws.amazon.com/mobile/sdkforios/developerguide/cognito-auth-aws-identity-for-ios.html
        // viewController.voiceController?.identityPoolId = "<#Your AWS IdentityPoolId. Remove Argument if you do not want to use AWS Polly#>"


        navigationViewController.routeController.snapsUserLocationAnnotationToRoute = true
        navigationViewController.voiceController?.volume = 0.5
        navigationViewController.navigationDelegate = self

        // Uncomment to apply custom styles
        //        styleForRegular().apply()
        //        styleForCompact().apply()
        //        styleForiPad().apply()
        //        styleForCarPlay().apply()

        let camera = self.mapView.camera
        camera.pitch = 45
        camera.altitude = 1_000
        navigationViewController.pendingCamera = camera

        self.viewController.present(navigationViewController, animated: true, completion: nil)
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        locationManager?.stopUpdatingLocation();
        self.origin = locations[0].coordinate
        getRoute()
    }
}
