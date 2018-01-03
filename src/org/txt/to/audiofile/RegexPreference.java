package org.txt.to.audiofile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.txt.to.audiofile.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.preference.DialogPreference;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class RegexPreference extends DialogPreference {
	
    private ArrayList<String[]> list = new ArrayList<String[]>();
	private final int mDefaultValue = 0;
    private int mCurrentValue;
    private LinearLayout linLayout;
    public final static String REGEX_LIST_FILE = "filter.rex";
    

    public RegexPreference(Context context, AttributeSet attrs) {
    	super(context, attrs);
    }

    @Override
    protected View onCreateDialogView() {
    	// Get current value from preferences
    	mCurrentValue = getPersistedInt(mDefaultValue);
    	
    	// Inflate layout
    	final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View view = inflater.inflate(R.layout.regex_list, null);
    	EditText txt;
    	Button btn;
    	linLayout = (LinearLayout) view.findViewById(R.id.regexLayout);
    	linLayout.removeAllViews();
    	list.clear();
    	
    	if (mCurrentValue != 0) {
    		BufferedReader bufferedReader;
			try {
				bufferedReader = new BufferedReader(new FileReader(new File(getContext().getFilesDir() + File.separator + REGEX_LIST_FILE)));
				String read;
				Pattern pattern = Pattern.compile("^(.+)#->#(.*)$");
				while((read = bufferedReader.readLine()) != null){
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
    	    	txt = (EditText) item.findViewById(R.id.regex_editText_left);
    	    	txt.setText(list.get(i)[0]);
    	    	txt = (EditText) item.findViewById(R.id.regex_editText_right);
    	    	txt.setText(list.get(i)[1]);
    	    	btn = (Button) item.findViewById(R.id.button_regex);
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
    		btn = (Button) item.findViewById(R.id.button_regex);
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
    	
    	((Button) view.findViewById(R.id.button_plus)).setOnClickListener(new View.OnClickListener() {
    			public void onClick(View v) {
    				final View item = inflater.inflate(R.layout.regex_item, linLayout, false);
    				Button btn = (Button) item.findViewById(R.id.button_regex);
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
    protected void onDialogClosed(boolean positiveResult) {
    	super.onDialogClosed(positiveResult);
    	
    	// Return if change was cancelled
    	if (!positiveResult) {
    		return;
    	}
    	
    	EditText txt;
    	list.clear();
    	
    	for (int i = 0; i < linLayout.getChildCount(); i++) {
    		String[] s = new String[2];
    		txt = (EditText) linLayout.getChildAt(i).findViewById(R.id.regex_editText_left);
    		s[0] = txt.getText().toString();
    		txt = (EditText) linLayout.getChildAt(i).findViewById(R.id.regex_editText_right);
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
    	int value = getPersistedInt(mDefaultValue);
    	return summary + " " + value;
    }
	
}
