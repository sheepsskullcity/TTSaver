package org.txt.to.audiofile;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

public class SeekBarPreferencePauseDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    // View elements
    protected SeekBar mSeekBar;
    protected TextView mValueText;

    protected int mCurrentValue;
    protected int mMaxValue;
    protected int mMinValue;

    public static SeekBarPreferencePauseDialogFragmentCompat newInstance(String key) {
        final SeekBarPreferencePauseDialogFragmentCompat fragment = new SeekBarPreferencePauseDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get current value from preferences
        DialogPreference preference = getPreference();
        if (preference instanceof SeekBarPreferencePause) {
            mCurrentValue = ((SeekBarPreferencePause) preference).getVal();
            mMinValue = ((SeekBarPreferencePause) preference).getMinVal();
            mMaxValue = ((SeekBarPreferencePause) preference).getMaxVal();
        }
    }

    @Override
    protected View onCreateDialogView(Context context) {
        // Inflate layout
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_slider, null);

        // Setup minimum and maximum text labels
        String s = 50 + " ms";
        ((TextView) view.findViewById(R.id.min_value)).setText(s);
        s = mMaxValue * 100 + " ms";
        ((TextView) view.findViewById(R.id.max_value)).setText(s);

        // Setup SeekBar
        mSeekBar = view.findViewById(R.id.seek_bar);
        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setProgress(mCurrentValue - mMinValue);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
                // Update current value
                mCurrentValue = value;
                // Update label with current value
                String s = (mCurrentValue == 0 ? 50 : mCurrentValue * 100) + " ms";
                mValueText.setText(s);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Setup text label for current value
        mValueText = view.findViewById(R.id.current_value);
        s = (mCurrentValue == 0 ? 50 : mCurrentValue * 100) + " ms";
        mValueText.setText(s);

        return view;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        // Return if change was cancelled
        if (!positiveResult) {
            return;
        }

        final SeekBarPreferencePause preference = (SeekBarPreferencePause) getPreference();
        if (preference.callChangeListener(mCurrentValue)) {
            preference.setVal(mCurrentValue);
        }
    }
}
