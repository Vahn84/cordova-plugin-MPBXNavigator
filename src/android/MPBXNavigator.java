package com.vahn.cordova.mpbxnavigator;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;
import android.content.Context;
import android.util.Log;


public class MPBXNavigator extends CordovaPlugin {

        public void initialize(CordovaInterface cordova, CordovaWebView webView) {
            super.initialize(cordova, webView);
        }

        @Override
        public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
            Context context = cordova.getActivity().getApplicationContext();
            Intent intent = new Intent(context, com.vahn.cordova.mpbxnavigator.NavigationActivity.class);
            Log.d("JSON OBJECT", args.toString());
            if(action.equalsIgnoreCase("showNavigator")) {
                double originLat = 0;
                double originLng = 0;
                double destinationLat = 0;
                double destinationLng = 0;

                try {
                    JSONObject jsonRequest = new JSONObject(args.get(0).toString());
                    originLat = jsonRequest.getJSONObject("origin").getDouble("latitude");
                    Log.d("OLAT", String.valueOf(originLat));
                    destinationLat = jsonRequest.getJSONObject("destination").getDouble("latitude");
                    originLng =jsonRequest.getJSONObject("origin").getDouble("longitude");
                    destinationLng = jsonRequest.getJSONObject("destination").getDouble("longitude");

                    intent.putExtra("originLat", originLat);
                    intent.putExtra("destinationLat", destinationLat);
                    intent.putExtra("originLng", originLng);
                    intent.putExtra("destinationLng", destinationLng);

                } catch (Exception e) {

                }

            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        }


}
