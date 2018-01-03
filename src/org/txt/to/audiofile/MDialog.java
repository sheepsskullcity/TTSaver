package org.txt.to.audiofile;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;

public class MDialog extends DialogFragment implements OnClickListener {
	
	LinearLayout llview;
	EditText et1;
	EditText et2;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	    llview = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.dialog, null);
	    et1 = (EditText) llview.findViewById(R.id.eText1);
	    et2 = (EditText) llview.findViewById(R.id.eText2);
		AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
		.setTitle(R.string.dtitle).setPositiveButton(R.string.ok, this)
		.setNegativeButton(R.string.cancel, this)
		.setView(llview);
		return adb.create();
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case Dialog.BUTTON_POSITIVE:
			MainActivity callingActivity = (MainActivity) getActivity();
			callingActivity.onDialogReturnedValues(et1.getText().toString()
					, et2.getText().toString());
			break;
		case Dialog.BUTTON_NEGATIVE:
			break;
		}
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
	}
}