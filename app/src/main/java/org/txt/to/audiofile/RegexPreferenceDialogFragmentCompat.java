package org.txt.to.audiofile;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private int mCurrentValue;
    private LinearLayout linLayout;
    public final static String REGEX_LIST_FILE = "filter.rex";
    private final ArrayList<String[]> list = new ArrayList<>();


    public static RegexPreferenceDialogFragmentCompat newInstance(String key) {
        final RegexPreferenceDialogFragmentCompat fragment = new RegexPreferenceDialogFragmentCompat();
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
        if (preference instanceof RegexPreference) {
            mCurrentValue = ((RegexPreference) preference).getVal();
        }
    }

    @Override
    protected View onCreateDialogView(Context context) {
        // Inflate layout
        final LayoutInflater inflater = (LayoutInflater) requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = Objects.requireNonNull(inflater).inflate(R.layout.regex_list, null);
        EditText txt;
        Button btn;
        linLayout = view.findViewById(R.id.regexLayout);
        linLayout.removeAllViews();
        list.clear();

        if (mCurrentValue != 0) {
            BufferedReader bufferedReader;
            try {
                bufferedReader = new BufferedReader(new FileReader(new File(getContext().getFilesDir() + File.separator + REGEX_LIST_FILE)));
                String read;
                Pattern pattern = Pattern.compile("^(.+)#->#(.*)$");
                while ((read = bufferedReader.readLine()) != null) {
                    String[] s = new String[2];
                    Matcher matcher = pattern.matcher(read);
                    matcher.find();
                    s[0] = matcher.group(1);
                    s[1] = matcher.group(2);
                    list.add(s);
                }
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < list.size(); i++) {
                final View item = inflater.inflate(R.layout.regex_item, linLayout, false);
                txt = item.findViewById(R.id.regex_editText_left);
                txt.setText(list.get(i)[0]);
                txt = item.findViewById(R.id.regex_editText_right);
                txt.setText(list.get(i)[1]);
                btn = item.findViewById(R.id.button_regex);
                btn.setText("-");
                btn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (linLayout.getChildCount() > 1) {
                            linLayout.removeView(item);
                        } else {
                            ((EditText) item.findViewById(R.id.regex_editText_left)).setText("");
                            ((EditText) item.findViewById(R.id.regex_editText_right)).setText("");
                        }
                    }
                });
                linLayout.addView(item);
            }
        } else {
            final View item = inflater.inflate(R.layout.regex_item, linLayout, false);
            btn = item.findViewById(R.id.button_regex);
            btn.setText("-");
            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (linLayout.getChildCount() > 1) {
                        linLayout.removeView(item);
                    } else {
                        ((EditText) item.findViewById(R.id.regex_editText_left)).setText("");
                        ((EditText) item.findViewById(R.id.regex_editText_right)).setText("");
                    }
                }
            });
            linLayout.addView(item);
        }

        view.findViewById(R.id.button_plus).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final View item = inflater.inflate(R.layout.regex_item, linLayout, false);
                Button btn = item.findViewById(R.id.button_regex);
                btn.setText("-");
                btn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (linLayout.getChildCount() > 1) {
                            linLayout.removeView(item);
                        } else {
                            ((EditText) item.findViewById(R.id.regex_editText_left)).setText("");
                            ((EditText) item.findViewById(R.id.regex_editText_right)).setText("");
                        }
                    }
                });
                linLayout.addView(item);
            }
        });
        return view;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        // Return if change was cancelled
        if (!positiveResult) {
            return;
        }

        EditText txt;
        list.clear();

        for (int i = 0; i < linLayout.getChildCount(); i++) {
            String[] s = new String[2];
            txt = linLayout.getChildAt(i).findViewById(R.id.regex_editText_left);
            s[0] = txt.getText().toString();
            txt = linLayout.getChildAt(i).findViewById(R.id.regex_editText_right);
            s[1] = txt.getText().toString();
            if (!s[0].isEmpty())
                list.add(s);
        }

        mCurrentValue = list.size();

        BufferedWriter bufferedWriter;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(new File(getContext().getFilesDir() + File.separator + REGEX_LIST_FILE)));
            for (String[] s : list) {
                try {
                    Pattern.compile(s[0]);
                } catch (PatternSyntaxException exception) {
                    Toast.makeText(this.getContext(), "REGEX compile error", Toast.LENGTH_LONG).show();
                    mCurrentValue--;
                    continue;
                }
                bufferedWriter.write(s[0] + "#->#" + s[1]);
                bufferedWriter.write("\n");
            }
            bufferedWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        final RegexPreference preference = (RegexPreference) getPreference();
        if (preference.callChangeListener(mCurrentValue)) {
            preference.setVal(mCurrentValue);
        }
    }

}
