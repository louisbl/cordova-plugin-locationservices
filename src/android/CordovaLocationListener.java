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

import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import org.apache.cordova.CallbackContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CordovaLocationListener implements LocationListener {
    public static int PERMISSION_DENIED = 1;
    public static int POSITION_UNAVAILABLE = 2;
    public static int TIMEOUT = 3;

    private LocationClient mClient;
    private LocationRequest mLocationRequest;
    private GeoBroker owner;
    protected boolean running = false;

    public HashMap<String, CallbackContext> watches = new HashMap<String, CallbackContext>();
    private List<CallbackContext> callbacks = new ArrayList<CallbackContext>();

    private Timer timer = null;

    private String TAG = "[Cordova Location Listener]";

    public CordovaLocationListener(LocationClient client, GeoBroker broker, String tag) {
        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        mClient = client;
        this.owner = broker;
        this.TAG = tag;
    }

    protected void fail(int code, String message) {
        this.cancelTimer();
        for (CallbackContext callbackContext : this.callbacks) {
            this.owner.fail(code, message, callbackContext, false);
        }
        if (this.watches.size() == 0) {
            Log.d(TAG, "Stopping global listener");
            this.stop();
        }
        this.callbacks.clear();

        Iterator<CallbackContext> it = this.watches.values().iterator();
        while (it.hasNext()) {
            this.owner.fail(code, message, it.next(), true);
        }
    }

    private void win(Location loc) {
        this.cancelTimer();
        for (CallbackContext callbackContext : this.callbacks) {
            this.owner.win(loc, callbackContext, false);
        }
        if (this.watches.size() == 0) {
            Log.d(TAG, "Stopping global listener");
            this.stop();
        }
        this.callbacks.clear();

        Iterator<CallbackContext> it = this.watches.values().iterator();
        while (it.hasNext()) {
            this.owner.win(loc, it.next(), true);
        }
    }

    /**
     * Location Listener Methods
     */

    /**
     * Called when the location has changed.
     *
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "The location has been updated!");
        this.win(location);
    }

    // PUBLIC

    public int size() {
        return this.watches.size() + this.callbacks.size();
    }

    public void addWatch(String timerId, CallbackContext callbackContext) {
        this.watches.put(timerId, callbackContext);
        if (this.size() == 1) {
            this.start();
        }
    }

    public void addCallback(CallbackContext callbackContext, int timeout) {
        Log.d(TAG, "addCallback with timeout: " + timeout);
        if (this.timer == null) {
            this.timer = new Timer();
        }
        this.timer.schedule(new LocationTimeoutTask(callbackContext, this), timeout);
        this.callbacks.add(callbackContext);
        if (this.size() == 1) {
            this.start();
        }
    }

    public void clearWatch(String timerId) {
        if (this.watches.containsKey(timerId)) {
            this.watches.remove(timerId);
        }
        if (this.size() == 0) {
            this.stop();
        }
    }

    /**
     * Destroy listener.
     */
    public void destroy() {
        this.stop();
    }

    protected void start() {
        Log.d(TAG, "start!");
        if (!this.running) {
            this.running = true;
            Log.d(TAG, "requestLocationUpdates started");
            mClient.requestLocationUpdates(mLocationRequest, this);
        }
    }

    /**
     * Stop receiving location updates.
     */
    private void stop() {
        Log.d(TAG, "stop!");
        this.cancelTimer();
        if (this.running) {
            if (mClient.isConnected()) {
                mClient.removeLocationUpdates(this);
                mClient.disconnect();
            }
            this.running = false;
        }
    }

    private void cancelTimer() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
            this.timer = null;
        }
    }

    private class LocationTimeoutTask extends TimerTask {

        private CallbackContext callbackContext = null;
        private CordovaLocationListener listener = null;

        public LocationTimeoutTask(CallbackContext callbackContext, CordovaLocationListener listener) {
            this.callbackContext = callbackContext;
            this.listener = listener;
        }

        @Override
        public void run() {
            Log.d(TAG, "LocationTimeoutTask#run");
            listener.fail(TIMEOUT, "Unable to retrieve position");
            for (CallbackContext callbackContext : listener.callbacks) {
                if (this.callbackContext == callbackContext) {
                    listener.callbacks.remove(callbackContext);
                    break;
                }
            }

            if (listener.size() == 0) {
                listener.stop();
            }
        }
    }
}
