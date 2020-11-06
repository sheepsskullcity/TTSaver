package org.txt.to.audiofile;

import java.io.File;

import android.Manifest;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.IBinder;


public class MainActivity extends AppCompatActivity {
	
//	private final String LOG_TAG = "mactivity_logs";
	private final String STATE_START = "STATE_START";
	private final String STATE_END = "STATE_END";
	private final String STATE_PATH = "STATE_PATH";
	private final String STATE_DIR = "STATE_DIR";
	private final String SAVED_PATH = "SAVED_PATH";
	private final String SAVED_DIR = "SAVED_DIR";

	public static final int MODE_FILE = 1;
	public static final int MODE_TEXT = 2;
	public static final int MODE_CONVERT = 3;
	public static final int NO_ERRORS = 0;
	public static final int FILE_NOT_EXIST = 1;
	public static final int FILE_NOT_READABLE = 2;
	public static final int DIR_NOT_EXIST = 3;
	public static final int DIR_NOT_WRITABLE = 4;
	public static final int EMPTY_TEXT = 5;
	public static final int NO_PERMISSION = 6;
	public static final int REQUEST_PERMISSION_CODE_START = 00001;
	public static final int REQUEST_PERMISSION_CODE_FILES = 00002;
	public static final int REQUEST_CODE_FILES = 1;
	public static final int DARK = 1;
	public static final String INPUT_TEXT_FILE = "INPUT_TEXT_FILE";
	public static final String CONVERT = "CONVERT";
	public static final String INPUT_TEXT = "INPUT_TEXT";
	public static final String OUTPUT_PATH = "OUTPUT_PATH";
	public static final String START_FROM = "START_FROM";
	public static final String END_WITH = "END_WITH";
	public static final String NPARTS = "NPARTS";
	public static final String SPEECH_SPEED = "SPEECH_SPEED";
	public static final String PITCH = "PITCH";
	public static final String MAX_PAUSE = "MAX_PAUSE";
	public static final String OUTPUT_NAME_TEMPLATE = "OUTPUT_NAME_TEMPLATE";
	public static final String ENCODING_FORMAT = "ENCODING_FORMAT";
	public static final String MESSAGE = "MESSAGE";
	public static final String TYPE = "TYPE";
	public static final String FILE_PATH = "FILE_PATH";
	public static final String DIR_PATH = "DIR_PATH";
	public static final String CUSTOM_INTENT_FILTER = "org.txt.to.audiofile.ResultForMainActivity";
	public static final String TIME_INTENT_FILTER = "org.txt.to.audiofile.TimeForMainActivity";
	
	private boolean bound = false;
	private Intent serviceIntent;
	private ServiceConnection sConn;
	private TTSaverService myService;
	private IntentFilter cif;
	private IntentFilter tif;
	private int start;
	private int end;
	private ProgressBar mProgress;
	private ProgressBar mEncProgress;
	private LinearLayout progressLLView;
	private int mProgressStatus = 0;
	private int mEncProgressStatus = 0;
	private EditText fileTxtEdit;
	private EditText dirTxtEdit;
	private Button stButton;
	private Button rngButton;
	private Button filesButton;
	private TextView rangeTxtView;
	private TextView pathTxtView;
	private TextView timeTxtView;
	private int mtheme;
	private CountDownTimer timer = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mtheme = Integer.parseInt(prefs.getString("pref_theme", "1"));
		if (mtheme == DARK)
			setTheme(R.style.AppBaseTheme);
		else
			setTheme(R.style.AppBaseLightTheme);
		super.onCreate(savedInstanceState);
		//Log.d(LOG_TAG, "onCreate");
		serviceIntent = new Intent(this, TTSaverService.class);
		cif = new IntentFilter(CUSTOM_INTENT_FILTER);
		tif = new IntentFilter(TIME_INTENT_FILTER);
		setContentView(R.layout.activity_main);
		
		Toolbar myToolbar = (Toolbar) findViewById(R.id.ma_toolbar);
		setSupportActionBar(myToolbar);
		
		mProgress = (ProgressBar) findViewById(R.id.progress_bar);
		mEncProgress = (ProgressBar) findViewById(R.id.enc_progress_bar);
		fileTxtEdit = (EditText) findViewById(R.id.filePath_EditText);
		dirTxtEdit = (EditText) findViewById(R.id.dirPath_EditText);
		stButton = (Button) findViewById(R.id.startAndStop_Btn);
		rngButton = (Button) findViewById(R.id.range_Btn);
		filesButton = (Button) findViewById(R.id.files_Btn);
		progressLLView = (LinearLayout) findViewById(R.id.progress_ll);
		rangeTxtView = (TextView) findViewById(R.id.range_TextView);
		pathTxtView = (TextView) findViewById(R.id.filePath_TextView);
		timeTxtView = (TextView) findViewById(R.id.time_TextView);
		
		if (savedInstanceState != null) {
	        start = savedInstanceState.getInt(STATE_START);
	        end = savedInstanceState.getInt(STATE_END);
	        fileTxtEdit.setText(savedInstanceState.getString(STATE_PATH));
	        dirTxtEdit.setText(savedInstanceState.getString(STATE_DIR));
	    } else {
	    	start = 0;
	    	end = 0;
	    	loadText();
	    }
		
		
        startService(serviceIntent);
        sConn = new ServiceConnection() {
        	@Override
        	public void onServiceConnected(ComponentName className,
            		IBinder service) {
        		myService = ((TTSaverService.MyBinder) service).getService();
            	bound = true;
        		if (myService.isRunning()) {
            		if (timer != null) {
            			timer.cancel();
            		}
        			timer = new CountDownTimer(myService.getRemTime(), 1000) {
        				@Override
        				public void onTick(long millisUntilFinished) {
        					timeTxtView.setText(Utils.getTime(millisUntilFinished));
        				}
        				@Override
        				public void onFinish() {
        					String s = "0s";
        					timeTxtView.setText(s);
        				}
        			}.start();
        			setDisabledViews();
        			mProgressStatus = myService.getProgress();
        			mEncProgressStatus = myService.getEncProgress();	
        		} else {
        			setEnabledViews();
        			mProgressStatus = 0;
        			mEncProgressStatus = 0;
        		}
        		mProgress.setProgress(mProgressStatus);
        		mEncProgress.setProgress(mEncProgressStatus);
            }
        	@Override
            public void onServiceDisconnected(ComponentName name) {
            	bound = false;
        		if (timer != null) {
        			timer.cancel();
        			timer = null;
        		}
            }
        };
        
		stButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (bound && myService.isRunning()) {
					myService.stop();
					if (timer != null) {
						timer.cancel();
						timer = null;
					}
					timeTxtView.setText("");
				} else {
					boolean permission = isPermissionGranted(REQUEST_PERMISSION_CODE_START);
					if (permission) {
						onclickStart();
					}
				}
			}});
		stButton.setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				if (bound && myService.isRunning())
					myService.forceStop();
				return true;
			}});
		rngButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				onclickRange();
			}});
		filesButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				boolean permission = isPermissionGranted(REQUEST_PERMISSION_CODE_FILES);
				if (permission) {
					onclickFiles();
				}
			}});
	}
	
	private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean encPBar = intent.getExtras().getBoolean(TYPE);
			if (encPBar) {
				mEncProgressStatus = intent.getExtras().getInt(MESSAGE);
				mEncProgress.setProgress(mEncProgressStatus);
			} else {
				int x = intent.getExtras().getInt(MESSAGE);
				if (x == -1) {
	        		if (timer != null) {
	        			timer.cancel();
	        			timer = null;
	        		}
	        		timeTxtView.setText("");
					setEnabledViews();
					mProgressStatus = 0;
					mEncProgressStatus = 0;
					mEncProgress.setProgress(mEncProgressStatus);
				} else {
					if (mEncProgressStatus > 0) {
						mEncProgressStatus = 0;
						mEncProgress.setProgress(mEncProgressStatus);
					}
					mProgressStatus = x;
				}
				mProgress.setProgress(mProgressStatus);
			}
		}
	};
	
	private final BroadcastReceiver tMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (bound) {
				if (myService.isRunning()) {
					if (timer != null)
						timer.cancel();
					timer = new CountDownTimer(myService.getRemTime(), 1000) {

						@Override
						public void onTick(long millisUntilFinished) {
							timeTxtView.setText(Utils.getTime(millisUntilFinished));
						}
				
						@Override
						public void onFinish() {
							String s = "0s";
							timeTxtView.setText(s);
						}
					}.start();
				}
			}
		}
	};
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	    savedInstanceState.putInt(STATE_START, start);
	    savedInstanceState.putInt(STATE_END, end);
	    savedInstanceState.putString(STATE_PATH, fileTxtEdit.getText().toString());
	    savedInstanceState.putString(STATE_DIR, dirTxtEdit.getText().toString());
	    super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		//Log.d(LOG_TAG, "onStart");
		bindService(serviceIntent, sConn, 0);
		
		Resources res = getResources();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean rangeVisible = prefs.getBoolean("pref_checkbox", false);
		int mode = Integer.parseInt(prefs.getString("pref_mode", "1"));
		if (mode != MODE_FILE && mode != MODE_CONVERT) {
			pathTxtView.setText(R.string.text_title);
		} else {
			pathTxtView.setText(R.string.file_path_title);
		}
		if (!rangeVisible || mode != MODE_FILE) {
			rngButton.setVisibility(View.GONE);
			start = 0;
			end = 0;
			String s = res.getString(R.string.range_text) + " " + start + " - " + end;
			rangeTxtView.setText(s);
			rangeTxtView.setVisibility(View.GONE);
		} else {
			rngButton.setVisibility(View.VISIBLE);
			String s = res.getString(R.string.range_text) + " " + start + " - " + end;
			rangeTxtView.setText(s);
			rangeTxtView.setVisibility(View.VISIBLE);
		}
		if (mtheme != Integer.parseInt(prefs.getString("pref_theme", "1"))) {
			finish();
			startActivity(getIntent());
		}
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		//Log.d(LOG_TAG, "onRestart");
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		//Log.d(LOG_TAG, "onPause");
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(tMessageReceiver);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		//Log.d(LOG_TAG, "onResume");
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, cif);
		LocalBroadcastManager.getInstance(this).registerReceiver(tMessageReceiver, tif);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		//Log.d(LOG_TAG, "onStop");
        if (bound) {
        	unbindService(sConn);
        	bound = false;
        }
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Log.d(LOG_TAG, "onDestroy");
		saveText();
	}
	
	private void saveText() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor ed = prefs.edit();
		int mode = Integer.parseInt(prefs.getString("pref_mode", "1"));
		if ((mode == MODE_FILE || mode == MODE_CONVERT) && new File(fileTxtEdit.getText().toString()).exists()) {
			ed.putString(SAVED_PATH, fileTxtEdit.getText().toString());
		}
		if (new File(dirTxtEdit.getText().toString()).exists()) {
			ed.putString(SAVED_DIR, dirTxtEdit.getText().toString());
		}
		ed.apply();
	}
	
	private void loadText() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int mode = Integer.parseInt(prefs.getString("pref_mode", "1"));
		if (mode == MODE_FILE || mode == MODE_CONVERT) {
			fileTxtEdit.setText(prefs.getString(SAVED_PATH, ""));
		}
		dirTxtEdit.setText(prefs.getString(SAVED_DIR, ""));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_settings) {
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void onclickStart() {
		Intent taskIntent = new Intent(this, TTSaverService.class);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String eformat = prefs.getString("pref_audioEncodingType", "OGG");
		String nameTemplate =  prefs.getString("pref_outputTemplate", "output_");
		int nparts = Integer.parseInt(prefs.getString("pref_audioParts", "10"));
		int mode = Integer.parseInt(prefs.getString("pref_mode", "1"));
		int speed = prefs.getInt("seekBarPreference", 100);
		int pitch = prefs.getInt("seekBarPreferencePitch", 100);
		int pause;
		if (prefs.getBoolean("pref_maxPause", false)) {
			pause = 100 * prefs.getInt("seekBarPreferencePause", 5);
			if (pause == 0) {
				pause = 50;
			}
		} else {
			pause = -1;
		}
			
		String text = fileTxtEdit.getText().toString();
		String outputPath = dirTxtEdit.getText().toString();
		outputPath = outputPath.replaceAll("([^/])$", "$1/");
		int x = checkForErrors(mode, text, outputPath);	
			
		if (x == NO_ERRORS) {
			taskIntent.putExtra((mode == MODE_FILE || mode == MODE_CONVERT) ? INPUT_TEXT_FILE : INPUT_TEXT, text)
			.putExtra(OUTPUT_PATH, outputPath)
			.putExtra(START_FROM, start)
			.putExtra(END_WITH, end)
			.putExtra(NPARTS, nparts == 0 ? 1 : nparts)
			.putExtra(OUTPUT_NAME_TEMPLATE, nameTemplate)
			.putExtra(SPEECH_SPEED, speed)
			.putExtra(PITCH, pitch)
			.putExtra(MAX_PAUSE, pause)
			.putExtra(CONVERT, mode == MODE_CONVERT);
			switch (eformat) {
				case "OGG":
					taskIntent.putExtra(ENCODING_FORMAT, Encodings.OGG_FORMAT);
					break;
				case "MP3":
					taskIntent.putExtra(ENCODING_FORMAT, Encodings.MP3_FORMAT);
					break;
				case "WAVE":
					taskIntent.putExtra(ENCODING_FORMAT, Encodings.WAV_FORMAT);
					break;
			}
			if (bound && !myService.isRunning()) {
				mProgress.setProgress(0);
				mEncProgress.setProgress(0);
				setDisabledViews();
				timeTxtView.setText("");
				myService.runTask(taskIntent);
			}
		}
		showErrors(x);
		saveText();
	}
	
	private int checkForErrors(int mode, String text, String outputPath) {
		File dir = new File(outputPath);
		if (mode == MODE_FILE || mode == MODE_CONVERT) {
			File file = new File(text);
			if (!file.exists() || !file.isFile())
				return FILE_NOT_EXIST;
			if (!file.canRead())
				return FILE_NOT_READABLE;
		}
		if (mode == MODE_TEXT && text.isEmpty()) {
			return EMPTY_TEXT;
		}
		if (!dir.exists() || !dir.isDirectory())
			return DIR_NOT_EXIST;
		if (!dir.canWrite())
			return DIR_NOT_WRITABLE;
		return NO_ERRORS;
	}
	
	public void onclickFiles() {
		Intent filesIntent = new Intent(this, FilesActivity.class);
		startActivityForResult(filesIntent, REQUEST_CODE_FILES);
	}

	public  boolean isPermissionGranted(int code) {
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
					== PackageManager.PERMISSION_GRANTED) {
				return true;
			} else {
				requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, code);
				return false;
			}
		}
		else {
			return true;
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case REQUEST_PERMISSION_CODE_START:
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					onclickStart();
				} else {
					showErrors(NO_PERMISSION);
				}
				break;
			case REQUEST_PERMISSION_CODE_FILES:
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					onclickFiles();
				} else {
					showErrors(NO_PERMISSION);
				}
				break;
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.d(LOG_TAG, "onResult");
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_CODE_FILES) {
				String f_path = data.getStringExtra(FILE_PATH);
				String d_path = data.getStringExtra(DIR_PATH);
				if (!f_path.isEmpty())
					fileTxtEdit.setText(f_path);
				if (!d_path.isEmpty())
					dirTxtEdit.setText(d_path);
			}
		} else {
			Toast.makeText(this, "Wrong result from FilesActivity", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void showErrors(int code) {
		switch(code) {
			case NO_PERMISSION:
				Toast.makeText(this, getText(R.string.no_permission), Toast.LENGTH_LONG).show();
				break;
			case FILE_NOT_EXIST:
				Toast.makeText(this, getText(R.string.file_error), Toast.LENGTH_LONG).show();
				break;
			case FILE_NOT_READABLE:
				Toast.makeText(this, getText(R.string.file_notreadable_error), Toast.LENGTH_LONG).show();
				break;
			case DIR_NOT_EXIST:
				Toast.makeText(this, getText(R.string.dir_error), Toast.LENGTH_LONG).show();
				break;
			case DIR_NOT_WRITABLE:
				Toast.makeText(this, getText(R.string.dir_notwritable_error), Toast.LENGTH_LONG).show();
				break;
			case EMPTY_TEXT:
				Toast.makeText(this, getText(R.string.empty_text_error), Toast.LENGTH_LONG).show();
				break;
			default:
				break;
		}
	}
	
	public void onclickRange() {
		DialogFragment dlg = new MDialog();
		dlg.show(getFragmentManager(), "dlg");
	}
	
	public void onDialogReturnedValues(String start_value, String end_value) {
		Resources res = getResources();
		if (!start_value.isEmpty())
			start = Integer.parseInt(start_value);
		else
			start = 0;
		if (!end_value.isEmpty())
			end = Integer.parseInt(end_value);
		else
			end = 0;
		String s = res.getString(R.string.range_text) + start + "-" + end;
		rangeTxtView.setText(s);
	}
	
	private void setDisabledViews() {
		stButton.setText(R.string.stopBtn);
		filesButton.setVisibility(View.INVISIBLE);
		progressLLView.setVisibility(View.VISIBLE);
		findViewById(R.id.filePath_EditText).setEnabled(false);
		findViewById(R.id.dirPath_EditText).setEnabled(false);
		findViewById(R.id.range_Btn).setEnabled(false);
	}
	
	private void setEnabledViews() {
		stButton.setText(R.string.startBtn);
		progressLLView.setVisibility(View.INVISIBLE);
		filesButton.setVisibility(View.VISIBLE);
		findViewById(R.id.filePath_EditText).setEnabled(true);
		findViewById(R.id.dirPath_EditText).setEnabled(true);
		findViewById(R.id.range_Btn).setEnabled(true);
	}
}
