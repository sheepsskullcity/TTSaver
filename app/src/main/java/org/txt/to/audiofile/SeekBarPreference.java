package org.txt.to.audiofile;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.preference.DialogPreference;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SeekBarPreference extends DialogPreference implements OnSeekBarChangeListener {
	
    // Namespaces to read attributes
    protected static final String PREFERENCE_NS = "http://schemas.android.com/apk/res/org.txt.to.audiofile";
    protected static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    // Attribute names
    protected static final String ATTR_DEFAULT_VALUE = "defaultValue";
    protected static final String ATTR_MIN_VALUE = "minValue";
    protected static final String ATTR_MAX_VALUE = "maxValue";

    // Default values for defaults
    protected static final int DEFAULT_CURRENT_VALUE = 100;
    protected static final int DEFAULT_MIN_VALUE = 25;
    protected static final int DEFAULT_MAX_VALUE = 400;

    // Real defaults
    protected int mDefaultValue;
    protected int mMaxValue;
    protected int mMinValue;
    
    // Current value
    protected int mCurrentValue;
    
    // View elements
    protected SeekBar mSeekBar;
    protected TextView mValueText;

    public SeekBarPreference(Context context, AttributeSet attrs) {
    	super(context, attrs);

    	// Read parameters from attributes
    	mMinValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MIN_VALUE, DEFAULT_MIN_VALUE);
    	mMaxValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MAX_VALUE, DEFAULT_MAX_VALUE);
    	mDefaultValue = attrs.getAttributeIntValue(ANDROID_NS, ATTR_DEFAULT_VALUE, DEFAULT_CURRENT_VALUE);
    }

    @Override
    protected View onCreateDialogView() {
    	// Get current value from preferences
    	mCurrentValue = getPersistedInt(mDefaultValue);
    	
    	// Inflate layout
    	LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View view = inflater.inflate(R.layout.dialog_slider, null);

    	// Setup minimum and maximum text labels
        String s = mMinValue + "%";
    	((TextView) view.findViewById(R.id.min_value)).setText(s);
    	s = mMaxValue + "%";
    	((TextView) view.findViewById(R.id.max_value)).setText(s);
    	
    	// Setup SeekBar
    	mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
    	mSeekBar.setMax(mMaxValue - mMinValue);
    	mSeekBar.setProgress(mCurrentValue - mMinValue);
    	mSeekBar.setOnSeekBarChangeListener(this);
    	
    	// Setup text label for current value
    	mValueText = (TextView) view.findViewById(R.id.current_value);
    	s = mCurrentValue + "%";
    	mValueText.setText(s);
    	
    	return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
    	super.onDialogClosed(positiveResult);
    	
    	// Return if change was cancelled
    	if (!positiveResult) {
    		return;
    	}
	
    	// Persist current value if needed
    	if (shouldPersist()) {
    		persistInt(mCurrentValue);
    	}
    	
    	// Notify activity about changes (to update preference summary line)
    	notifyChanged();
    }

    @Override
    public CharSequence getSummary() {
    	// Format summary string with current value
    	String summary = super.getSummary().toString();
    	return summary + getSummaryStrValue();
    }
    
    public String getSummaryStrValue() {
    	int value = getPersistedInt(mDefaultValue);
    	return " " + value + "%";
    }
    
    @Override
    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
    	// Update current value
    	mCurrentValue = value + mMinValue;
    	// Update label with current value
        String s = mCurrentValue + "%";
    	mValueText.setText(s);
    }

    public void onStartTrackingTouch(SeekBar seek) {
    	// Not used
    }

    public void onStopTrackingTouch(SeekBar seek) {
    	// Not used
    }
	
}