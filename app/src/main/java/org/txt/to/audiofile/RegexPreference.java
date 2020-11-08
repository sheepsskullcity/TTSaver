package org.txt.to.audiofile;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;


import androidx.preference.DialogPreference;

public class RegexPreference extends DialogPreference {

	private final int mDefaultValue = 0;
    private int mCurrentValue;


	public RegexPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public int getVal() {
		return mCurrentValue;
	}
	public void setVal(int val) {
		final boolean wasBlocking = shouldDisableDependents();
		mCurrentValue = val;
		persistInt(val);
		final boolean isBlocking = shouldDisableDependents();
		if (isBlocking != wasBlocking) {
			notifyDependencyChange(isBlocking);
		}
		notifyChanged();
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		// Default value from attribute. Fallback value is set to 0.
		return a.getInt(index, 0);
	}

	@Override
	protected void onSetInitialValue(Object defaultValue) {
		// Read the value. Use the default value if it is not possible.
		setVal(getPersistedInt(mDefaultValue));
	}

	@Override
	public CharSequence getSummary() {
		// Format summary string with current value
		String summary = super.getSummary().toString();
		int value = getPersistedInt(mDefaultValue);
		return summary + " " + value;
	}

}
