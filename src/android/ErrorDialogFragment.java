package fr.louisbl.cordova.locationservices;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

/**
 * Define a DialogFragment to display the error dialog generated in
 * showErrorDialog.
 */
public class ErrorDialogFragment extends DialogFragment {

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
	 * @param dialog
	 *            An error dialog
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
