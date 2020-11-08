package org.txt.to.audiofile;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

public class SeekBarPreferencePause extends DialogPreference {

	// Namespaces to read attributes
	protected static final String PREFERENCE_NS = "http://schemas.android.com/apk/res-auto";
	protected static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

	// Attribute names
	protected static final String ATTR_DEFAULT_VALUE = "defaultValue";
	protected static final String ATTR_MIN_VALUE = "minValue";
	protected static final String ATTR_MAX_VALUE = "maxValue";

    // Default values for defaults
    protected static final int DEFAULT_CURRENT_VALUE = 5;
    protected static final int DEFAULT_MIN_VALUE = 0;
    protected static final int DEFAULT_MAX_VALUE = 20;

	// Real defaults
	protected int mDefaultValue;
	protected int mMaxValue;
	protected int mMinValue;

	// Current value
	protected int mCurrentValue;

	public SeekBarPreferencePause(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Read parameters from attributes
		mMinValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MIN_VALUE, DEFAULT_MIN_VALUE);
		mMaxValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MAX_VALUE, DEFAULT_MAX_VALUE);
		mDefaultValue = attrs.getAttributeIntValue(ANDROID_NS, ATTR_DEFAULT_VALUE, DEFAULT_CURRENT_VALUE);
	}

	public int getVal() {
		return mCurrentValue;
	}
	public int getMinVal() {
		return mMinValue;
	}
	public int getMaxVal() {
		return mMaxValue;
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
		return a.getInt(index, mDefaultValue);
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
		return summary + getSummaryStrValue();
	}

	public String getSummaryStrValue() {
    	int value = getPersistedInt(mDefaultValue);
    	return " " + (value == 0 ? 50 : value * 100) + " ms";
    }
	
}
