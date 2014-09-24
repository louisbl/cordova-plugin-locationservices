/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package fr.louisbl.cordova.nativegeolocation;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
 * This class is the interface to the Geolocation.  It's bound to the geo object.
 *
 * This class only starts and stops various GeoListeners, which consist of a GPS and a Network Listener
 */

public class GeoBroker extends CordovaPlugin implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;
    private CordovaLocationListener mListener;
    private boolean wantLastLocation = false;
    private boolean wantUpdates = false;
    private JSONArray prevArgs;
    private CallbackContext cbContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        // your init code here
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST:

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // Log the result
                        Log.d(LocationUtils.APPTAG, "Error resolved. Please re-try operation.");
                        break;

                    // If any other result was returned by Google Play services
                    default:
                        // Log the result
                        Log.d(LocationUtils.APPTAG, "Google Play services: unable to resolve connection error.");
                        break;
                }

                // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode
                Log.d(LocationUtils.APPTAG, "Received an unknown activity request code " + requestCode + " in onActivityResult.");
                break;
        }
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action          The action to execute.
     * @param args            JSONArry of arguments for the plugin.
     * @param callbackContext The callback id used when calling back into JavaScript.
     * @return True if the action was valid, or false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (isGPSdisabled()) {
            PluginResult.Status status = PluginResult.Status.ERROR;
            String message = "GPS is disabled on this device.";
            PluginResult result = new PluginResult(status, message);
            callbackContext.sendPluginResult(result);

            return true;
        }

        if (servicesConnected()) {
            if (mLocationClient == null) {
                create();
            }
            if (!mLocationClient.isConnected() && !mLocationClient.isConnecting()) {
                mLocationClient.connect();
            }
            if (action.equals("getLocation")) {
                if (mLocationClient.isConnected()) {
                    getLastLocation(args, callbackContext);
                } else {
                    setWantLastLocation(args, callbackContext);
                }
            } else if (action.equals("addWatch")) {
                String id = args.getString(0);
                wantUpdates = true;
                this.addWatch(id, callbackContext);
            } else if (action.equals("clearWatch")) {
                String id = args.getString(0);
                this.clearWatch(id);
            } else {
                return false;
            }
        } else {
            PluginResult.Status status = PluginResult.Status.ERROR;
            String message = "Google Play Services is not available for this device.";
            PluginResult result = new PluginResult(status, message);
            callbackContext.sendPluginResult(result);
        }
        return true;
    }

    private boolean isGPSdisabled() {
        LocationManager lm = null;
        boolean gps_enabled;
        if (lm == null)
            lm = (LocationManager) this.cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            Log.d(LocationUtils.APPTAG, "GPS enabled: " + gps_enabled);
        } catch(Exception ex){
            ex.printStackTrace();
            gps_enabled = false;
        }

        return !gps_enabled;
    }

    private void getLastLocation() {
        try {
            getLastLocation(prevArgs, cbContext);
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            cbContext = null;
            prevArgs = null;
        }
    }

    private void getLastLocation(JSONArray args, CallbackContext callbackContext) throws JSONException {
        int maximumAge = args.getInt(1);
        Log.d(LocationUtils.APPTAG, "Maximum age: " + maximumAge);
        Location last = mLocationClient.getLastLocation();
        // Check if we can use lastKnownLocation to get a quick reading and use less battery
        if (last != null && (System.currentTimeMillis() - last.getTime()) <= maximumAge) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, returnLocationJSON(last));
            callbackContext.sendPluginResult(result);
        } else {
            getCurrentLocation(callbackContext, args.optInt(2, 60000));
        }
    }

    private void setWantLastLocation(JSONArray args, CallbackContext callbackContext) {
        prevArgs = args;
        cbContext = callbackContext;
        wantLastLocation = true;
    }

    private void clearWatch(String id) {
        getListener().clearWatch(id);
    }

    private void getCurrentLocation(CallbackContext callbackContext, int timeout) {
        getListener().addCallback(callbackContext, timeout);
    }

    private void addWatch(String timerId, CallbackContext callbackContext) {
        getListener().addWatch(timerId, callbackContext);
    }

    private CordovaLocationListener getListener() {
        if (mListener == null) {
            mListener = new CordovaLocationListener(mLocationClient, this, LocationUtils.APPTAG);
        }
        return mListener;
    }

    /**
     * Called when the activity is to be shut down.
     * Stop listener.
     */
    public void onDestroy() {
        if (mListener != null) {
            mListener.destroy();
        }
        if (mLocationClient != null && mLocationClient.isConnected()) {
            // After disconnect() is called, the client is considered "dead".
            mLocationClient.disconnect();
        }
    }

    /**
     * Called when the view navigates.
     * Stop the listeners.
     */
    public void onReset() {
        this.onDestroy();
    }

    public JSONObject returnLocationJSON(Location loc) {
        JSONObject o = new JSONObject();

        try {
            o.put("latitude", loc.getLatitude());
            o.put("longitude", loc.getLongitude());
            o.put("altitude", (loc.hasAltitude() ? loc.getAltitude() : null));
            o.put("accuracy", loc.getAccuracy());
            o.put("heading", (loc.hasBearing() ? (loc.hasSpeed() ? loc.getBearing() : null) : null));
            o.put("velocity", loc.getSpeed());
            o.put("timestamp", loc.getTime());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return o;
    }

    public void win(Location loc, CallbackContext callbackContext, boolean keepCallback) {
        PluginResult result = new PluginResult(PluginResult.Status.OK, this.returnLocationJSON(loc));
        result.setKeepCallback(keepCallback);
        callbackContext.sendPluginResult(result);
    }

    /**
     * Location failed.  Send error back to JavaScript.
     *
     * @param code The error code
     * @param msg  The error message
     * @throws JSONException
     */
    public void fail(int code, String msg, CallbackContext callbackContext, boolean keepCallback) {
        JSONObject obj = new JSONObject();
        String backup = null;
        try {
            obj.put("code", code);
            obj.put("message", msg);
        } catch (JSONException e) {
            obj = null;
            backup = "{'code':" + code + ",'message':'" + msg.replaceAll("'", "\'") + "'}";
        }
        PluginResult result;
        if (obj != null) {
            result = new PluginResult(PluginResult.Status.ERROR, obj);
        } else {
            result = new PluginResult(PluginResult.Status.ERROR, backup);
        }

        result.setKeepCallback(keepCallback);
        callbackContext.sendPluginResult(result);
    }

    private void create() {
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this.cordova.getActivity(), this, this);
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.cordova.getActivity());

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d(LocationUtils.APPTAG, "Google Play Services is available");

            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this.cordova.getActivity(), 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(this.cordova.getActivity().getFragmentManager(), LocationUtils.APPTAG);
            }
            return false;
        }
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LocationUtils.APPTAG, "Location Services connected");
        if (wantLastLocation) {
            wantLastLocation = false;
            getLastLocation();
        }
        if (mListener != null && wantUpdates) {
            wantUpdates = false;
            mListener.start();
        }
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        Log.d(LocationUtils.APPTAG, "Location Services disconnected");
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {

                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this.cordova.getActivity(),
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {

                // Log the error
                e.printStackTrace();
            }
        } else {

            // If no resolution is available, display a dialog to the user with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this.cordova.getActivity(),
                LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(this.cordova.getActivity().getFragmentManager(), LocationUtils.APPTAG);
        }
    }

    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
}
