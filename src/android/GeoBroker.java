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
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

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
        GooglePlayServicesClient.ConnectionCallbacks {

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;
    private CordovaLocationListener mListener;
    private boolean mWantLastLocation = false;
    private boolean mWantUpdates = false;
    private JSONArray mPrevArgs;
    private CallbackContext mCbContext;
    private GooglePlayServicesUtils mGooglePlayServicesUtils;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
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

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LocationUtils.APPTAG, "Location Services connected");
        if (mWantLastLocation) {
            mWantLastLocation = false;
            getLastLocation();
        }
        if (mListener != null && mWantUpdates) {
            mWantUpdates = false;
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

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action          The action to execute.
     * @param args            JSONArry of arguments for the plugin.
     * @param callbackContext The callback id used when calling back into JavaScript.
     * @return True if the action was valid, or false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("clearWatch")) {
            String id = args.getString(0);
            this.clearWatch(id);

            return true;
        }

        if (args.getBoolean(1) && isGPSdisabled()) {
            PluginResult.Status status = PluginResult.Status.ERROR;
            String message = "GPS is disabled on this device.";
            PluginResult result = new PluginResult(status, message);
            callbackContext.sendPluginResult(result);

            return true;
        }

        if (getGooglePlayServicesUtils().servicesConnected()) {
            if (!getLocationClient().isConnected() && !getLocationClient().isConnecting()) {
                getLocationClient().connect();
            }
            if (action.equals("getLocation")) {
                if (getLocationClient().isConnected()) {
                    getLastLocation(args, callbackContext);
                } else {
                    setWantLastLocation(args, callbackContext);
                }
            } else if (action.equals("addWatch")) {
                String id = args.getString(0);
                int priority = args.getInt(1);
                long interval = args.getLong(2);
                long fastInterval = args.getLong(3);
                getListener().setLocationRequestParams(priority, interval, fastInterval);
                mWantUpdates = true;
                this.addWatch(id, callbackContext);
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

    /**
     * Called when the activity is to be shut down.
     * Stop listener.
     */
    public void onDestroy() {
        if (mListener != null) {
            mListener.destroy();
        }
        if (getLocationClient().isConnected()) {
            // After disconnect() is called, the client is considered "dead".
            getLocationClient().disconnect();
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

    private boolean isGPSdisabled() {
        LocationManager lm = null;
        boolean gps_enabled;
        if (lm == null)
            lm = (LocationManager) this.cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            Log.d(LocationUtils.APPTAG, "GPS enabled: " + gps_enabled);
        } catch (Exception ex) {
            ex.printStackTrace();
            gps_enabled = false;
        }

        return !gps_enabled;
    }

    private void getLastLocation() {
        try {
            getLastLocation(mPrevArgs, mCbContext);
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            mCbContext = null;
            mPrevArgs = null;
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
        mPrevArgs = args;
        mCbContext = callbackContext;
        mWantLastLocation = true;
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
            mListener = new CordovaLocationListener(getLocationClient(), this, LocationUtils.APPTAG);
        }
        return mListener;
    }

    private LocationClient getLocationClient() {
        if (mLocationClient == null) {
            mLocationClient = new LocationClient(cordova.getActivity(), this, getGooglePlayServicesUtils());
        }
        return mLocationClient;
    }

    private GooglePlayServicesUtils getGooglePlayServicesUtils() {
        if (mGooglePlayServicesUtils == null) {
            mGooglePlayServicesUtils = new GooglePlayServicesUtils(cordova);
        }
        return mGooglePlayServicesUtils;
    }
}
