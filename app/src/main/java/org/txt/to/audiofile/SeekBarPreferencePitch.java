package org.txt.to.audiofile;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SeekBarPreferencePitch extends SeekBarPreference implements OnSeekBarChangeListener {

    // Attribute names
    protected static final String ATTR_DEFAULT_VALUE = "defaultValue";
    protected static final String ATTR_MIN_VALUE = "minPiValue";
    protected static final String ATTR_MAX_VALUE = "maxPiValue";

    // Default values for defaults
    protected static final int DEFAULT_CURRENT_VALUE = 10;
    protected static final int DEFAULT_MIN_VALUE = 1;
    protected static final int DEFAULT_MAX_VALUE = 19;

    public SeekBarPreferencePitch(Context context, AttributeSet attrs) {
    	super(context, attrs);
    }
	
    @Override
    protected View onCreateDialogView() {
    	// Get current value from preferences
    	mCurrentValue = getPersistedInt(mDefaultValue);
    	
    	// Inflate layout
    	LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View view = inflater.inflate(R.layout.dialog_slider, null);

    	// Setup minimum and maximum text labels
    	((TextView) view.findViewById(R.id.min_value)).setText(Integer.toString(mMinValue - 10));
    	((TextView) view.findViewById(R.id.max_value)).setText(Integer.toString(mMaxValue - 10));
    	
    	// Setup SeekBar
    	mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
    	mSeekBar.setMax(mMaxValue - mMinValue);
    	mSeekBar.setProgress(mCurrentValue - mMinValue);
    	mSeekBar.setOnSeekBarChangeListener(this);
    	
    	// Setup text label for current value
    	mValueText = (TextView) view.findViewById(R.id.current_value);
    	mValueText.setText(Integer.toString(mCurrentValue - 10));
    	
    	return view;
    }
    
    @Override
    public String getSummaryStrValue() {
    	int value = getPersistedInt(mDefaultValue);
    	return " " + Integer.toString(value - 10);
    }
    
    @Override
    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
    	// Update current value
    	mCurrentValue = value + mMinValue;
    	// Update label with current value
    	mValueText.setText(Integer.toString(mCurrentValue - 10));
    }
    
}
