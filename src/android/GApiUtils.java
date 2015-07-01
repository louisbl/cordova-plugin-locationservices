package fr.louisbl.cordova.locationservices;

import org.apache.cordova.CordovaInterface;

import android.app.Dialog;
import android.content.IntentSender;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;

public class GApiUtils implements GoogleApiClient.OnConnectionFailedListener {

	private final CordovaInterface mCordova;

	public GApiUtils(CordovaInterface cordova) {
		mCordova = cordova;
	}

	/*
	 * Called by Location Services if the attempt to Location Services fails.
	 */
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(
						mCordova.getActivity(),
						LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			// If no resolution is available, display a dialog to the user with
			// the error.
			showErrorDialog(connectionResult.getErrorCode(), LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);
		}
	}

	/**
	 * Verify that Google Play services is available before making a request.
	 *
	 * @return true if Google Play services is available, otherwise false
	 */
	public boolean servicesConnected() {

		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(mCordova.getActivity());

		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d(LocationUtils.APPTAG, "Google Play Services is available");

			// Continue
			return true;
			// Google Play services was not available for some reason
		} else {
			showErrorDialog(resultCode, 0);
			return false;
		}
	}

	/**
	 * Show a dialog returned by Google Play services for the connection error
	 * code
	 *
	 * @param errorCode
	 *            An error code returned from onConnectionFailed
	 */
	private void showErrorDialog(final int errorCode, final int requestCode) {
		mCordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				// Get the error dialog from Google Play services
				Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, mCordova.getActivity(), requestCode);

				// If Google Play services can provide an error dialog
				if (errorDialog != null) {

					// Create a new DialogFragment in which to show the error dialog
					ErrorDialogFragment errorFragment = new ErrorDialogFragment();

					// Set the dialog in the DialogFragment
					errorFragment.setDialog(errorDialog);

					// Show the error dialog in the DialogFragment
					errorFragment.show(mCordova.getActivity().getFragmentManager(),
							LocationUtils.APPTAG);
				}
			}
		});
	}
}
