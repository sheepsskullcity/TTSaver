package org.txt.to.audiofile;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

public class SeekBarPreferenceBaseDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    // View elements
    protected SeekBar mSeekBar;
    protected TextView mValueText;

    protected int mCurrentValue;
    protected int mMaxValue;
    protected int mMinValue;

    public static SeekBarPreferenceBaseDialogFragmentCompat newInstance(String key) {
        final SeekBarPreferenceBaseDialogFragmentCompat fragment = new SeekBarPreferenceBaseDialogFragmentCompat();
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
        if (preference instanceof SeekBarPreferenceBase) {
            mCurrentValue = ((SeekBarPreferenceBase) preference).getVal();
            mMinValue = ((SeekBarPreferenceBase) preference).getMinVal();
            mMaxValue = ((SeekBarPreferenceBase) preference).getMaxVal();
        }
    }

    @Override
    protected View onCreateDialogView(Context context) {
        // Inflate layout
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_slider, null);

        // Setup minimum and maximum text labels
        String s = mMinValue + "%";
        ((TextView) view.findViewById(R.id.min_value)).setText(s);
        s = mMaxValue + "%";
        ((TextView) view.findViewById(R.id.max_value)).setText(s);

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
                String s = mCurrentValue + "%";
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
        s = mCurrentValue + "%";
        mValueText.setText(s);

        return view;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        // Return if change was cancelled
        if (!positiveResult) {
            return;
        }

        final SeekBarPreferenceBase preference = (SeekBarPreferenceBase) getPreference();
        if (preference.callChangeListener(mCurrentValue)) {
            preference.setVal(mCurrentValue);
        }
    }
}
