package org.txt.to.audiofile;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
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
		getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
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

	public static class PrefsFragment extends PreferenceFragmentCompat {
		PrefsFragment() {
		}

		@Override

		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.prefs, rootKey);
		}
		@Override
		public void onDisplayPreferenceDialog(Preference preference) {
			if (preference instanceof RegexPreference) {
				DialogFragment f;
				f = RegexPreferenceDialogFragmentCompat.newInstance(preference.getKey());
				f.setTargetFragment(this, 0);
				f.show(getParentFragmentManager(), null);
			} else if (preference instanceof SeekBarPreferenceBase) {
				DialogFragment f;
				f = SeekBarPreferenceBaseDialogFragmentCompat.newInstance(preference.getKey());
				f.setTargetFragment(this, 0);
				f.show(getParentFragmentManager(), null);
			} else if (preference instanceof SeekBarPreferencePause) {
				DialogFragment f;
				f = SeekBarPreferencePauseDialogFragmentCompat.newInstance(preference.getKey());
				f.setTargetFragment(this, 0);
				f.show(getParentFragmentManager(), null);
			} else if (preference instanceof SeekBarPreferencePitch) {
				DialogFragment f;
				f = SeekBarPreferencePitchDialogFragmentCompat.newInstance(preference.getKey());
				f.setTargetFragment(this, 0);
				f.show(getParentFragmentManager(), null);
			} else {
				super.onDisplayPreferenceDialog(preference);
			}
		}
	}
}
