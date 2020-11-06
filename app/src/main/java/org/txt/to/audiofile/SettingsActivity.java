package org.txt.to.audiofile;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {
	
	private final int DARK = 1;
	private SharedPreferences prefs;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(listener);
		int theme = Integer.parseInt(prefs.getString("pref_theme", "1"));
		if (theme == DARK)
			setTheme(R.style.AppBaseTheme);
		else
			setTheme(R.style.AppBaseLightTheme);
		super.onCreate(savedInstanceState);
		getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new mFragment()).commit();
    }
	
	@Override
	public void onDestroy() {
		prefs.unregisterOnSharedPreferenceChangeListener(listener);
		super.onDestroy();
	}
	
	OnSharedPreferenceChangeListener listener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged (SharedPreferences sharedPreferences, String key) {
			if (key.equals("pref_outputTemplate")) {
				if (Utils.containsIllegalChars(sharedPreferences.getString("pref_outputTemplate", "output_"))) {
					Editor editor = sharedPreferences.edit();
					editor.putString("pref_outputTemplate", "output_");
					editor.apply();
					Toast.makeText(SettingsActivity.this, getText(R.string.out_bad_chars).toString() + " * \\ / \" : ? | < >", Toast.LENGTH_LONG).show();
					finish();
					startActivity(getIntent());
				}
			}
		}
	};

	public static class mFragment extends PreferenceFragmentCompat {
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.prefs, rootKey);
		}
	}
}
