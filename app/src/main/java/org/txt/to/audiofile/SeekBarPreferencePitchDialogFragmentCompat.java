package org.txt.to.audiofile;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

public class SeekBarPreferencePitchDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    // View elements
    protected SeekBar mSeekBar;
    protected TextView mValueText;

    protected int mCurrentValue;
    protected int mMaxValue;
    protected int mMinValue;

    public static SeekBarPreferencePitchDialogFragmentCompat newInstance(String key) {
        final SeekBarPreferencePitchDialogFragmentCompat fragment = new SeekBarPreferencePitchDialogFragmentCompat();
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
        if (preference instanceof SeekBarPreferencePitch) {
            mCurrentValue = ((SeekBarPreferencePitch) preference).getVal();
            mMinValue = ((SeekBarPreferencePitch) preference).getMinVal();
            mMaxValue = ((SeekBarPreferencePitch) preference).getMaxVal();
        }
    }

    @Override
    protected View onCreateDialogView(Context context) {
        // Inflate layout
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_slider, null);

        // Setup minimum and maximum text labels
        ((TextView) view.findViewById(R.id.min_value)).setText(Integer.toString(mMinValue - 10));
        ((TextView) view.findViewById(R.id.max_value)).setText(Integer.toString(mMaxValue - 10));

        // Setup SeekBar
        mSeekBar = view.findViewById(R.id.seek_bar);
        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setProgress(mCurrentValue - mMinValue);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
                // Update current value
                mCurrentValue = value + mMinValue;
                // Update label with current value
                mValueText.setText(Integer.toString(mCurrentValue - 10));
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
        mValueText.setText(Integer.toString(mCurrentValue - 10));

        return view;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        // Return if change was cancelled
        if (!positiveResult) {
            return;
        }

        final SeekBarPreferencePitch preference = (SeekBarPreferencePitch) getPreference();
        if (preference.callChangeListener(mCurrentValue)) {
            preference.setVal(mCurrentValue);
        }
    }
}
