package org.txt.to.audiofile;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.view.ActionMode.Callback;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class FilesActivity extends AppCompatActivity {

	private final String DEFAULT_PATH = "/";
	private final String ATTRIBUTE_NAME_TEXT_1 = "text_1";
	private final String ATTRIBUTE_NAME_TEXT_2 = "text_2";
	private final String ATTRIBUTE_NAME_TEXT_3 = "text_3";
	private final String ATTRIBUTE_NAME_TEXT_4 = "text_4";
	private final String ATTRIBUTE_NAME_IMAGE = "image";
	private final String PARENT_FOLDER_TITLE = "Parent folder";
	private final int PARENT_FOLDER_POSITION = 0;
	private final int EMPTY_FOLDER = 1;
	private final String STATE_CURRENT_DIR = "STATE_CURRENT_DIR";
	private final String STATE_FILE = "STATE_FILE";
	private final String STATE_DIR = "STATE_DIR";
	private final String DATE_FORMAT = "dd/MM/yyyy";
	private final int EMPTY_BACKGROUND = 0;
	
	private String SDCARDROOT_PATH;
	
	private String currentDir = "";
	private String finalFilePath = "";
	private String finalDirPath = "";
	
	private ListView lvSimple;
	private TextView txtEmpty;
	private TextView txtFile;
	private TextView txtDir;
	private TextView txtStatus;
	private SimpleAdapter sAdapter;
	private ArrayList<Map<String, Object>> data;
	private Map<String, Object> m;
	private ArrayList<File> list;
	private int mode;
	private int checkedPosition;
	private final FilesActivity mFActivity = this;
	private Toolbar myToolbar;
	
	public ActionMode actionMode;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mode = Integer.parseInt(prefs.getString("pref_mode", "1"));
		int theme = Integer.parseInt(prefs.getString("pref_theme", "1"));
		if (theme == MainActivity.DARK)
			setTheme(R.style.FilesTheme);
		else
			setTheme(R.style.FilesLightTheme);
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			currentDir = savedInstanceState.getString(STATE_CURRENT_DIR);
			finalFilePath = savedInstanceState.getString(STATE_FILE);
			finalDirPath = savedInstanceState.getString(STATE_DIR);
		}
		setContentView(R.layout.activity_files);
		
		myToolbar = (Toolbar) findViewById(R.id.fa_toolbar);
		setSupportActionBar(myToolbar);
		
		lvSimple = (ListView) findViewById(R.id.files_ListView);
		txtEmpty = (TextView) findViewById(R.id.filesEmpty_TextView);
		txtFile = (TextView) findViewById(R.id.filesFile_TextView);
		txtDir = (TextView) findViewById(R.id.filesDir_TextView);
		txtStatus =  (TextView) findViewById(R.id.filesStatus_TextView);
		
		if (!finalFilePath.isEmpty()) {
			String append = getResources().getString(R.string.files_filepath_append);
			append = append + " " + new File(finalFilePath).getName();
			txtFile.setText(append);
		} else if (mode == MainActivity.MODE_TEXT) {
			txtFile.setVisibility(View.INVISIBLE);
		}
		if (!finalDirPath.isEmpty()) {
			String append = getResources().getString(R.string.files_dirpath_append);
			append = append + " " + finalDirPath;
			txtDir.setText(append);
		}
		
		data = new ArrayList<>();
		SDCARDROOT_PATH = Objects.requireNonNull(new File(Environment.getExternalStorageDirectory().getAbsolutePath()).getParentFile()).getAbsolutePath();
		if (currentDir.isEmpty()) {
			String state = Environment.getExternalStorageState();
			if (state.equals(Environment.MEDIA_MOUNTED)) {
				currentDir = Environment.getExternalStorageDirectory().getAbsolutePath();
			} else {
				currentDir = DEFAULT_PATH;
			}
		}
		fillFList(currentDir);
		initFList();
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	    savedInstanceState.putString(STATE_CURRENT_DIR, currentDir);
	    savedInstanceState.putString(STATE_FILE, finalFilePath);
	    savedInstanceState.putString(STATE_DIR, finalDirPath);
	    super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    getMenuInflater().inflate(R.menu.fa_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.item_menu_action_mode) {
			File f = new File(currentDir);
			int position = 0;
			if (f.getParent() == null) {
				if (list.size() < 1)
					return true;
			} else {
				if (list.size() < 2)
					return true;
				position = 1;
			}
			if (actionMode == null) {
				checkedPosition = position;
				actionMode = startSupportActionMode(callback);
				lvSimple.setItemChecked(checkedPosition, true);
				sAdapter.notifyDataSetChanged();
			}
			return true;
		}
		if (id == R.id.item_menu_close) {
			Intent intent = new Intent();
			intent.putExtra(MainActivity.FILE_PATH, finalFilePath);
			intent.putExtra(MainActivity.DIR_PATH, finalDirPath);
			setResult(RESULT_OK, intent);
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		if (actionMode == null) {
			if (!list.isEmpty() && (new File(currentDir).getParentFile() != null)
					&& (!Objects.requireNonNull(new File(currentDir).getParentFile()).getAbsolutePath().equals(SDCARDROOT_PATH))) {
				File pathItem = list.get(PARENT_FOLDER_POSITION);
				currentDir = pathItem.getPath();
				fillFList(currentDir);
				sAdapter.notifyDataSetChanged();
				checkEmptyFolder(data.size());
			} else {
				Intent intent = new Intent();
				intent.putExtra(MainActivity.FILE_PATH, finalFilePath);
				intent.putExtra(MainActivity.DIR_PATH, finalDirPath);
				setResult(RESULT_OK, intent);
				super.onBackPressed();
			}
		} else {
			actionMode.finish();
		}
	}
	
	private void fillFList(String path) {
		DateFormat df = DateFormat.getDateInstance();
		String fsz, rw, date;
		txtStatus.setText(path);
		data.clear();
		File f = new File(path);
		list = new ArrayList<>(Arrays.asList(f.listFiles()));
		sortList();
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).isFile() && mode == MainActivity.MODE_TEXT)
				continue;
			m = new HashMap<>();
			m.put(ATTRIBUTE_NAME_TEXT_1, list.get(i).getName());
			if (list.get(i).isFile()) {
				fsz = Utils.getStringSizeLengthFile(list.get(i).length());
				m.put(ATTRIBUTE_NAME_TEXT_2, fsz);
			} else if (list.get(i).isDirectory() && list.get(i).canRead()) {
				fsz = "" + list.get(i).listFiles().length;
				m.put(ATTRIBUTE_NAME_TEXT_2, fsz + " item");
			} else {
				m.put(ATTRIBUTE_NAME_TEXT_2, "");
			}
			rw = (list.get(i).canRead() ? "R" : "-") +
					"/" + (list.get(i).canWrite() ? "W" : "-");
			m.put(ATTRIBUTE_NAME_TEXT_3, rw);
			date = df.format(list.get(i).lastModified());
			m.put(ATTRIBUTE_NAME_TEXT_4, date);
			if (list.get(i).isDirectory())
				m.put(ATTRIBUTE_NAME_IMAGE, R.drawable.dir);
			else
				m.put(ATTRIBUTE_NAME_IMAGE, R.drawable.file);
			data.add(m);
		}
		if (f.getParent() != null) {
			list.add(PARENT_FOLDER_POSITION, f.getParentFile());
			m = new HashMap<>();
			m.put(ATTRIBUTE_NAME_TEXT_1, PARENT_FOLDER_TITLE);
			m.put(ATTRIBUTE_NAME_TEXT_2, "");
			m.put(ATTRIBUTE_NAME_TEXT_3, "");
			m.put(ATTRIBUTE_NAME_TEXT_4, "");
			m.put(ATTRIBUTE_NAME_IMAGE, R.drawable.parent_dir);
			data.add(PARENT_FOLDER_POSITION, m);
		}
	}
	
	private void initFList () {
		String[] from = { ATTRIBUTE_NAME_IMAGE,
				ATTRIBUTE_NAME_TEXT_1, ATTRIBUTE_NAME_TEXT_2,
				ATTRIBUTE_NAME_TEXT_3, ATTRIBUTE_NAME_TEXT_4};
		int[] to = { R.id.fileItem_ImageView,
				R.id.fileItem_TextView1, R.id.fileItem_TextView2,
				R.id.fileItem_TextView3, R.id.fileItem_TextView4};
		
		sAdapter = new SimpleAdapter(this, data, R.layout.item_file, from, to) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View row = super.getView(position, convertView, parent);
				if (row == null) {
					LayoutInflater mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					row = mInflater.inflate(R.layout.item_file, parent, false);
				}
				final ListView lv = (ListView) parent;
				if(position == lv.getCheckedItemPosition() && actionMode != null) {
					row.setBackgroundColor(ContextCompat.getColor(mFActivity, R.color.selected));
				} else {
					row.setBackgroundResource(EMPTY_BACKGROUND);
				}
				return row;
			}
		};
		lvSimple.setAdapter(sAdapter);
		lvSimple.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		checkEmptyFolder(data.size());
		lvSimple.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				File pathItem = list.get(position);
				if (pathItem.isDirectory() && pathItem.canRead() && actionMode == null) {
					currentDir = pathItem.getPath();
					fillFList(currentDir);
					sAdapter.notifyDataSetChanged();
					checkEmptyFolder(data.size());
				}
				if (actionMode != null) {
					File f = new File(currentDir);
					if ((f.getParent() == null) || (f.getParent() != null && position != PARENT_FOLDER_POSITION)) {
						checkedPosition = position;
						lvSimple.setItemChecked(checkedPosition, true);
						if (!list.get(checkedPosition).isFile()) {
							actionMode.getMenu().findItem(R.id.item_delete).setVisible(false);
							actionMode.getMenu().findItem(R.id.item_edit).setVisible(false);
						} else {
							actionMode.getMenu().findItem(R.id.item_delete).setVisible(true);
							actionMode.getMenu().findItem(R.id.item_edit).setVisible(true);
						}
						sAdapter.notifyDataSetChanged();
					} else {
						lvSimple.setItemChecked(checkedPosition, true);
					}
				}
			}});
		lvSimple.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				File f = new File(currentDir);
				if ((actionMode == null && f.getParent() == null) || (f.getParent() != null && position != PARENT_FOLDER_POSITION)) {
					checkedPosition = position;
					actionMode = startSupportActionMode(callback);
					lvSimple.setItemChecked(checkedPosition, true);
					sAdapter.notifyDataSetChanged();
				}
				return true;
			}
		});
	}
	
	public Callback callback = new Callback() {

		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.context, menu);
			if (list.get(checkedPosition).isDirectory()) {
				menu.findItem(R.id.item_delete).setVisible(false);
				menu.findItem(R.id.item_edit).setVisible(false);
			}
			return true;
		}

		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			File f = list.get(checkedPosition);
	        switch (item.getItemId()) {
            	case R.id.item_close:
            		mode.finish();
					Intent intent = new Intent();
					intent.putExtra(MainActivity.FILE_PATH, finalFilePath);
					intent.putExtra(MainActivity.DIR_PATH, finalDirPath);
					setResult(RESULT_OK, intent);
					finish();
            		return true;
            	case R.id.item_delete:
            		if (f.canWrite() && f.isFile()) {
            			deleteFile(f);
            			mode.finish();
            		} else {
            			Toast.makeText(mFActivity, getText(R.string.file_notwritable_error), Toast.LENGTH_LONG).show();
            		}
            		return true;
            	case R.id.item_edit:
            		if (f.canWrite() && f.isFile()) {
            			renameFile(f);
            			mode.finish();
            		} else {
            			Toast.makeText(mFActivity, getText(R.string.file_notwritable_error), Toast.LENGTH_LONG).show();
            		}
            		return true;
            	case R.id.item_save:
            		if (list.get(checkedPosition).isFile()) {
            			finalFilePath = list.get(checkedPosition).getPath();
            			String append = getResources().getString(R.string.files_filepath_append) + " " + list.get(checkedPosition).getName();
            			txtFile.setText(append);
            		} else if (list.get(checkedPosition).isDirectory()) {
						finalDirPath = list.get(checkedPosition).getPath();
						String append = getResources().getString(R.string.files_dirpath_append) + " " + finalDirPath;
						txtDir.setText(append);
            		}
            		return true;
            	default:
            		return false;
	        }
		}

		public void onDestroyActionMode(ActionMode mode) {
			actionMode = null;
			sAdapter.notifyDataSetChanged();
		}
	};
	
	private void deleteFile(final File f) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.confirm_removing);
		builder.setMessage(getResources().getString(R.string.removing_info) + " " + f.getName() + " ?");
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
		    public void onClick(DialogInterface dialog, int item) {
				f.delete();
				fillFList(currentDir);
				sAdapter.notifyDataSetChanged();
				checkEmptyFolder(data.size());
		    }
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
		    public void onClick(DialogInterface dialog, int item) {
		    }
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void renameFile(final File f) {
		final EditText input = new EditText(this);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.confirm_rename);
		builder.setMessage(getResources().getString(R.string.rename_info) + " " + f.getName());
		input.setText(f.getName());
		builder.setView(input);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
		    public void onClick(DialogInterface dialog, int item) {
				String value = input.getText().toString().trim();
				File newFile = new File(f.getParent(), value);
				if (Utils.containsIllegalChars(value)) {
					Toast.makeText(FilesActivity.this, getText(R.string.new_bad_chars).toString() + " * \\ / \" : ? | < >", Toast.LENGTH_LONG).show();
					return;
				}
				if (newFile.exists()) {
					Toast.makeText(FilesActivity.this, value + " " + getText(R.string.file_already_exist), Toast.LENGTH_LONG).show();
					return;
				}
				if (value != null && !value.isEmpty() && !value.equals(f.getName())) {
					if (newFile.getParentFile().canWrite())
						f.renameTo(newFile);
				}
				fillFList(currentDir);
				sAdapter.notifyDataSetChanged();
				checkEmptyFolder(data.size());
		    }
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
		    public void onClick(DialogInterface dialog, int item) {
		    }
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void checkEmptyFolder(int status) {
		if (status == EMPTY_FOLDER) {
			txtEmpty.setVisibility(View.VISIBLE);
		} else {
			txtEmpty.setVisibility(View.INVISIBLE);
		}
	}
	
	private void sortList() {
		Collections.sort(list, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				if ( (o1.isFile() && o2.isFile()) || (o1.isDirectory() && o2.isDirectory()) )
					return ((Integer) o1.getName().compareTo(o2.getName()));
				else {
					if (o2.isDirectory())
						return 1;
					else
						return -1;
				}
			}});
	}
}
