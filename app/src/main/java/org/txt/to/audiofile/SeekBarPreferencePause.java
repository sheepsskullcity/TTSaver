package org.txt.to.audiofile;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarPreferencePause extends SeekBarPreference implements OnSeekBarChangeListener {

    // Attribute names
	protected static final String ATTR_DEFAULT_VALUE = "defaultValue";
	protected static final String ATTR_MIN_VALUE = "minPValue";
	protected static final String ATTR_MAX_VALUE = "maxPValue";

    // Default values for defaults
    protected static final int DEFAULT_CURRENT_VALUE = 5;
    protected static final int DEFAULT_MIN_VALUE = 0;
    protected static final int DEFAULT_MAX_VALUE = 20;

    public SeekBarPreferencePause(Context context, AttributeSet attrs) {
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
		String s = 50 + " ms";
    	((TextView) view.findViewById(R.id.min_value)).setText(s);
    	s = mMaxValue * 100 + " ms";
    	((TextView) view.findViewById(R.id.max_value)).setText(s);
    	
    	// Setup SeekBar
    	mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
    	mSeekBar.setMax(mMaxValue - mMinValue);
    	mSeekBar.setProgress(mCurrentValue - mMinValue);
    	mSeekBar.setOnSeekBarChangeListener(this);
    	
    	// Setup text label for current value
    	mValueText = (TextView) view.findViewById(R.id.current_value);
    	s = (mCurrentValue == 0 ? 50 : mCurrentValue * 100) + " ms";
    	mValueText.setText(s);
    	
    	return view;
    }
    
    @Override
    public String getSummaryStrValue() {
    	int value = getPersistedInt(mDefaultValue);
    	return " " + (value == 0 ? 50 : value * 100) + " ms";
    }
    
    @Override
    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
    	// Update current value
    	mCurrentValue = value;
    	// Update label with current value
		String s = (mCurrentValue == 0 ? 50 : mCurrentValue * 100) + " ms";
    	mValueText.setText(s);
    }
	
}
