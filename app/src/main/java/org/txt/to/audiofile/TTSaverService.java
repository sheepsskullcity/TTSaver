package org.txt.to.audiofile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import java.text.DecimalFormat;

import lib.textdata.array.FB2DataArray;
import lib.textdata.array.TxtBuffer;
import lib.textdata.array.TxtDataArray;
import lib.wave.merge.WaveMerge;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import androidx.core.app.NotificationCompat.Builder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;
import android.preference.PreferenceManager;

public class TTSaverService extends Service implements OnInitListener {
	
	private final String LOG_TAG = "service_logs";
	
	private final int TTS_INIT_ERROR = 1;
	private final int TTS_ERROR = 2;
	private final int BUFFER_ERROR = 3;
	private final int ENCODER_ERROR = 4;
	private final int RECORD_FINISHED = -1;
	private final String UTTERANCE_ID = "org.txt.to.audiofile.UtteranceID";
	private final int ONGOING_NOTIFICATION_ID = 1;
	private final String CH_ID_LOW = "org.txt.to.audiofile.low";
	private final String CH_ID_HIGH = "org.txt.to.audiofile.high";
	
	private TextToSpeech tts;
	private final HashMap<String, String> myHashRender = new HashMap<>();
	private ArrayList<String> list;
	private static TxtBuffer buff;
	private int currentFile = 0;
	private String outPath;
	private final Handler handler = new Handler();
	private Encodings encFormat;
	private final WaveMerge wm = new WaveMerge();
	private long startTime;
	private long endTime;
	private final TTSaverService service = this;
	private final MyBinder mBinder = new MyBinder();
	private boolean cancel;
	private boolean isRunning = false;
	private Intent mintent;
	private Intent tintent;
	private int progressStatus = 0;
	private int encProgressStatus = 0;
	private int nparts;
	private String nameTemplate;
	private int bufferParts;
	private String errorMsg;
	private long startT;
	private long endT;
	private long totalT;
	private long timeT;
	private WakeLock wakeLock;
	private boolean hasErrors;
	private int speed;
	private int pitch;
	private int max_pause;
	private Thread t;
 
  static {
	  System.loadLibrary("ogg");
	  System.loadLibrary("vorbis");
	  System.loadLibrary("mp3lame");
	  System.loadLibrary("encode");
  }
  
  public class MyBinder extends Binder {
	  public TTSaverService getService() {
		  return TTSaverService.this;
	  }
  }
  
  private final Runnable timerRunnable = new Runnable() {
      @Override
      public void run() {
    	  timeT = timeT - 1000;
          handler.postDelayed(this, 1000);
      }
  };
	
  private native int encodeOgg(String inputPath, String outputPath, int a, int b);
  private native int encodeMP3(String inputPath, String outputPath, int a, int b);
  private native void cancelEnc();

  @Override
  public void onCreate() {
	  super.onCreate();
	  //Log.d(LOG_TAG, "onCreate");
	  tintent = new Intent(MainActivity.TIME_INTENT_FILTER);
	  mintent = new Intent(MainActivity.CUSTOM_INTENT_FILTER);
	  myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID);

	  createNotificationChannel();

	  PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
	  assert powerManager != null;
	  wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
	          "TTSaver:TTSaverWakelock");
	  wakeLock.setReferenceCounted(false);
  }

	private void createNotificationChannel() {
		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = getString(R.string.foreground_service);
			String description = getString(R.string.foreground_service_desc);
			int importance_low = NotificationManager.IMPORTANCE_LOW;
			int importance_high = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel channel_low = new NotificationChannel(CH_ID_LOW, name, importance_low);
			NotificationChannel channel_high = new NotificationChannel(CH_ID_HIGH, name, importance_high);
			channel_low.setDescription(description);
			// Register the channel with the system; you can't change the importance
			// or other notification behaviors after this
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel_low);
			notificationManager.createNotificationChannel(channel_high);
		}
	}

  @Override
  public void onInit(int status) {
	  switch(status) {
	
	  case TextToSpeech.SUCCESS:
		  String strr = nextOutStr();
		  String str = buff.getBuffer();
		  list = new ArrayList<>();
		  if (str != null) {
			  currentFile++;
			  list.add(strr);
			  startT = System.currentTimeMillis();
			  tts.setSpeechRate(((float)speed) * 0.01f);
			  tts.setPitch(((float)pitch) * 0.01f);
			  toWFile(str, strr);
		  } else {
			  isRunning = false;
			  sendMessage(RECORD_FINISHED, false);
			  tts = null;
			  stopForeground(true);
			  showError(BUFFER_ERROR);
			  handler.removeCallbacks(timerRunnable);
			  wakeLock.release();
		  }
		  break;
		
	  case TextToSpeech.ERROR:
		  isRunning = false;
		  sendMessage(RECORD_FINISHED, false);
		  tts = null;
		  stopForeground(true);
		  showError(TTS_INIT_ERROR);
		  handler.removeCallbacks(timerRunnable);
		  wakeLock.release();
		  break;
	  }
  }

  private void toWFile(String text, String filename) {
	  int code;
	  if (Build.VERSION.SDK_INT < 21) {
		  code = tts.synthesizeToFile(text, myHashRender, filename);
	  } else {
		  code = tts.synthesizeToFile(text, null, new File(filename), UTTERANCE_ID);
	  }
	  if (code == TextToSpeech.ERROR) {
		  cancel = true;
		  stopForeground(true);
		  showError(TTS_ERROR);
	  }
  }
  
  private void synthesizeNext() {
	  if (currentFile % nparts == 0) {
		  String s2 = nameFromTemplate(currentFile / nparts);
		  String s1 = s2 + ".wav";
		  encodeWavFiles(s1, s2);
	  }
	  sendMessage((int)(buff.checkProgress() * 100), false);
	  String str = buff.getBuffer();
	  if ((str != null) && !cancel) {
		  String sstr = nextOutStr();
		  currentFile++;
		  list.add(sstr);
		  tts.setSpeechRate(((float)speed) * 0.01f);
		  tts.setPitch(((float)pitch) * 0.01f);
		  toWFile(str, sstr);
	  } else {
		  if (tts != null) {
			  tts.stop();
			  tts.shutdown();
		  }
		  tts = null;
		  if (!list.isEmpty()) {
		  	  String s2 = nameFromTemplate((currentFile / nparts) + 1);
		  	  String s1 = s2 + ".wav";
		  	  encodeWavFiles(s1, s2);
		  }
		  currentFile = 0;
		  endTime = System.currentTimeMillis();
		  //Log.d(LOG_TAG, "finished rec");
		  long total = endTime - startTime;
		  //Log.d(LOG_TAG, "Total time is: " + Utils.getTime(total));
		  isRunning = false;
		  sendMessage(RECORD_FINISHED, false);
		  handler.removeCallbacks(timerRunnable);
		  stopForeground(true);
		  if (!cancel && !hasErrors) {
			  show(total, wm.getNumChannels(), wm.getSampleRate());
			  finalNotif();
		  }
		  wakeLock.release();
	  }
  }
  
  private String nameFromTemplate(int i) {
	  return outPath + nameTemplate + afileNumber(i);
  }

  private String afileNumber(int i) {
	  int x = bufferParts / nparts;
	  DecimalFormat df;
	  if (x > 10000)
		  df = new DecimalFormat("00000");
	  else if (x > 1000)
		  df = new DecimalFormat("0000");
	  else if (x > 100)
		  df = new DecimalFormat("000");
	  else if (x > 10)
		  df = new DecimalFormat("00");
	  else
		  df = new DecimalFormat("0");
	  return df.format(i);
  }
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
	  //Log.d(LOG_TAG, "onStartCommand");
	  return START_NOT_STICKY;
  }
  
  public void runTask(Intent intent) {
	  SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	  boolean bool_notif = prefs.getBoolean("pref_notifsound", false);
	  Notification notification;

	  Intent notificationIntent = new Intent(this, MainActivity.class);
	  notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
			  .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

	  PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

	  Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_gray);

	  Builder mBuilder = new Builder(this, bool_notif ? CH_ID_HIGH : CH_ID_LOW)
			  .setSmallIcon(R.drawable.ic_white)
			  .setLargeIcon(bm)
			  .setContentInfo(getText(R.string.ticker_text))
			  .setContentTitle(getText(R.string.notification_title))
			  .setContentText(getText(R.string.notification_message))
			  .setContentIntent(pendingIntent);

	  if (Build.VERSION.SDK_INT >= 21)
		  mBuilder.setCategory(Notification.CATEGORY_SERVICE);

	  notification = mBuilder.build();

	  startForeground(ONGOING_NOTIFICATION_ID, notification);
	  wakeLock.acquire();
	  String fileName = "";
	  String text = "";
	  boolean conv = false;
	  hasErrors = false;
	  startTime = System.currentTimeMillis();
	  startT = 0;
	  endT = 0;
	  totalT = 0;
	  timeT = 1000;
	  cancel = false;
	  isRunning = true;
	  bufferParts = 0;
	  currentFile = 0;
	  if (intent.hasExtra(MainActivity.CONVERT)) {
		  conv = Objects.requireNonNull(intent.getExtras()).getBoolean(MainActivity.CONVERT);
	  }
	  if (intent.hasExtra(MainActivity.INPUT_TEXT_FILE) || intent.hasExtra(MainActivity.INPUT_TEXT_FILE)) {
		  fileName = Objects.requireNonNull(intent.getExtras()).getString(MainActivity.INPUT_TEXT_FILE);
	  }
	  else if (intent.hasExtra(MainActivity.INPUT_TEXT)) {
		  text = Objects.requireNonNull(intent.getExtras()).getString(MainActivity.INPUT_TEXT);
	  }
	  nameTemplate = Objects.requireNonNull(intent.getExtras()).getString(MainActivity.OUTPUT_NAME_TEMPLATE);
	  outPath = intent.getExtras().getString(MainActivity.OUTPUT_PATH);
	  int start = intent.getExtras().getInt(MainActivity.START_FROM);
	  int end = intent.getExtras().getInt(MainActivity.END_WITH);
	  nparts = intent.getExtras().getInt(MainActivity.NPARTS);
	  encFormat = (Encodings)intent.getExtras().getSerializable(MainActivity.ENCODING_FORMAT);
	  speed = intent.getExtras().getInt(MainActivity.SPEECH_SPEED);
	  pitch = intent.getExtras().getInt(MainActivity.PITCH) * 10;
	  max_pause = intent.getExtras().getInt(MainActivity.MAX_PAUSE);
	  assert fileName != null;
	  if (!fileName.isEmpty())
		  fillBuffer(fileName, start, end, true, conv);
	  else {
		  assert text != null;
		  if (!text.isEmpty())
			  fillBuffer(text, 0, 0, false, false);
	  }
  }

   private void startSynthesis() {
	  tts = new TextToSpeech(this, this);
	  tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
			  @Override
			  public void onDone(String utteranceId) {
				  try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				  synthesizeNext();
				  endT = System.currentTimeMillis();
				  calcAvrg();
				  startT = System.currentTimeMillis();
			  }

			  @Override
			  public void onError(String utteranceId) {
				  Log.d(LOG_TAG, "tts error");
				  if (tts.isSpeaking())
					  tts.stop();
				  
				  if (tts != null)
					  tts.shutdown();
				  
				  if (!list.isEmpty())
					  deleteWFiles(list);
				  
				  isRunning = false;
				  sendMessage(RECORD_FINISHED, false);
				  tts = null;
				  stopForeground(true);
				  showError(TTS_ERROR);
				  handler.removeCallbacks(timerRunnable);
				  wakeLock.release();
			  }

			  @Override
			  public void onStart(String utteranceId) {
				  //Log.d(LOG_TAG, "tts part #" + currentFile + " started");
			  }
	  });
  }

  @Override
  public void onDestroy() {
	  super.onDestroy();
	  //Log.d(LOG_TAG, "onDestroy");
	  stopForeground(true);
	  handler.removeCallbacks(timerRunnable);
	  if (tts != null) {
	  	if (tts.isSpeaking())
	  		tts.stop();
	  }
	  
	  if (tts != null)
		  tts.shutdown();
	  
	  if (!list.isEmpty())
		  deleteWFiles(list);
	  wakeLock.release();
  }

  @Override
  public IBinder onBind(Intent intent) {
	  //Log.d(LOG_TAG, "onBind");
	  return mBinder;
  }
  
  @Override
  public boolean onUnbind(Intent intent) {
	  //Log.d(LOG_TAG, "onUnbind");
	  return super.onUnbind(intent);
  }
  
  private void fillBuffer(final String str, final int a, final int b, final boolean isFile, final boolean convert) {
	  Runnable bufferRunnable = new Runnable() {
		  
		  TxtBuffer buffer;
		  
		  final Runnable messageRunnable = new Runnable() {
			  @Override
			  public void run() {
				  buff = buffer;
				  startSynthesis();
			  }
		  };
		  
		  public void run() {
			  if (convert) {
				  convertFile();
			  }
			  else if (isFile) {
				  runFile();
			  } else {
				  runText();
			  }
		  }
		  private void runFile() {
			  if (str.matches(".+\\.[Ff][Bb]2$")) {
				  FB2DataArray fd = new FB2DataArray(str, true);
				  if (rulesCount() != 0)
					  fd.useRules(readRules(), a, b);
				  buffer = new TxtBuffer(fd, a, b);
			  } else if (str.matches(".+\\.[Ff][Bb]2\\.[Zz][Ii][Pp]$")) {
				  FB2DataArray fd;
				try {
					ZipFile zf = new ZipFile(new File(str));
					fd = new FB2DataArray(zf, true);
					if (rulesCount() != 0)
						fd.useRules(readRules(), a, b);
					buffer = new TxtBuffer(fd, a, b);
				} catch (IOException e) {
					e.printStackTrace();
				}
			  } else {
				  TxtDataArray td;
				  if (str.matches(".+\\.[Ee][Pp][Uu][Bb]$")) {
					  ArrayList<String> l = new Epub(str).getParsedText();
					  td = new TxtDataArray(l);
				  } else {
					  td = new TxtDataArray(str);
				  }
				  if (rulesCount() != 0)
					  td.useRules(readRules(), a, b);
				  buffer = new TxtBuffer(td, a, b);
			  }
			  while (buffer.getBuffer() != null) {
				  bufferParts++;
			  }
			  buffer.clearState();
			  handler.post(messageRunnable);
		  }
		  private void convertFile() {
			  String sss = "";
			  if (str.matches(".+\\.[Ff][Bb]2$")) {
				  FB2DataArray fd = new FB2DataArray(str, false);
				  if (rulesCount() != 0)
					  fd.useRules(readRules(), 0, 0);
				  sss = processStr(fd.dataArray);
			  } else if (str.matches(".+\\.[Ff][Bb]2\\.[Zz][Ii][Pp]$")) {
				  FB2DataArray fd;
				try {
					ZipFile zf = new ZipFile(new File(str));
					fd = new FB2DataArray(zf, false);
					if (rulesCount() != 0)
						fd.useRules(readRules(), 0, 0);
					sss = processStr(fd.dataArray);
				} catch (IOException e) {
					e.printStackTrace();
				}
			  } else {
				  TxtDataArray td;
				  if (str.matches(".+\\.[Ee][Pp][Uu][Bb]$")) {
					  ArrayList<String> l = new Epub(str).getParsedText();
					  td = new TxtDataArray(l);
				  } else {
					  td = new TxtDataArray(str);
				  }
				  if (rulesCount() != 0)
					  td.useRules(readRules(), 0, 0);
				  sss = processStr(td.dataArray);
			  }
			  saveConvertedFile(sss);
			  isRunning = false;
			  sendMessage(RECORD_FINISHED, false);
			  tts = null;
			  stopForeground(true);
			  handler.removeCallbacks(timerRunnable);
			  wakeLock.release();
		  }
		  private String processStr(ArrayList<String> dataArray) {
				StringBuilder sb = new StringBuilder();
				int h = 0;
				int i = dataArray.size();
				if (i > 0) {
					for (String s : dataArray) {
						h++;
						sb.append(s).append("\n");
						sendMessage((int) (100.0f * (float) h / (float) i), false);
					}
					sendMessage(100, false);
				}
				return sb.toString();
		  }
		  private void saveConvertedFile(String s) {
			  String regx = "(\\[)(\\d+)(])";
			  Pattern p = Pattern.compile(regx, Pattern.MULTILINE);
			  Matcher m = p.matcher(s);
			  int i = 0;
			  while (m.find() && !t.isInterrupted()) {
				  i++;
			  }
			  m.reset();
			  int h = 0;
			  while (m.find() && !t.isInterrupted() && i > 0) {
				  h++;
				  String rep = "";
				  Pattern p1 = Pattern.compile("^" + m.group(2) + "(\\n)+(.+)", Pattern.MULTILINE);
				  Matcher m1 = p1.matcher(s);
				  if (m1.find()) {
					  rep = "(" + m1.group(2) + ")";
				  }
				  s = s.replaceFirst(regx, rep);
				  sendMessage((int) (100.0f * (float) h / (float) i), false);
			  }
			  if (!t.isInterrupted()) {
				  sendMessage(100, false);
				  FileUpdate.writeData(s, outPath + "converted.txt", false);
				  show(new File(str).getName(), h);
			  }
		  }
		  private void runText() {
			  ArrayList<String> array = new ArrayList<>();
			  array.add(str);
			  TxtDataArray td = new TxtDataArray(array);
			  if (rulesCount() != 0)
				  td.useRules(readRules(), 0, 0);
			  buffer = new TxtBuffer(td);
			  while (buffer.getBuffer() != null) {
				  bufferParts++;
			  }
			  buffer.clearState();
			  handler.post(messageRunnable);
		  }
		  private ArrayList<String[]> readRules() {
			  ArrayList<String[]> list = new ArrayList<>();
			  BufferedReader bufferedReader;
			  try {
				  bufferedReader = new BufferedReader(new FileReader(new File(getFilesDir() + File.separator + RegexPreferenceDialogFragmentCompat.REGEX_LIST_FILE)));
				  String read;
				  Pattern pattern = Pattern.compile("^(.+)#->#(.*)$");
				  while((read = bufferedReader.readLine()) != null) {
					  String[] s = new String[2];
					  Matcher matcher = pattern.matcher(read);
					  matcher.find();
					  s[0] = matcher.group(1);
					  s[1] = matcher.group(2).replaceAll("\\\\", "\\\\\\\\");
					  list.add(s);
				  }
				  bufferedReader.close();
			  } catch (IOException e) {
				  e.printStackTrace();
			  }
			  return list;
		  }
		  private int rulesCount() {
			  SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
			  return prefs.getInt("regexPreference", 0);
		  }};
	  t = new Thread(bufferRunnable);
	  t.start();
  }
  
  private String nextOutStr() {
	  return outPath + UUID.randomUUID().toString() + ".wav";
  }
  
  private void deleteWFiles(ArrayList<String> args) {
	  try{
		  for (int i = 0; i < args.size(); i++) {
			  removeWFile(args.get(i));
		  }
		  list.clear();
	  } catch(Exception e) {
		e.printStackTrace();
	  }
  }
  
  private void removeWFile(String str) throws Exception {
	  File file = new File(str);
	  if (file.exists()) {
		  if (!file.delete()) {
			  Log.d(LOG_TAG, "Delete operation failed.");
		  }
	  }
  }
  
  private void encodeWavFiles(String s1, String s2) {
	  int x = 1;
	  String[] args = list.toArray(new String[0]);
	  wm.merge(args, s1, max_pause);
	  deleteWFiles(list);
	  if (encFormat == Encodings.OGG_FORMAT && !cancel)
		  x = encodeOgg(s1, s2 + ".ogg", wm.getNumChannels(), wm.getSampleRate());
	  if (encFormat == Encodings.MP3_FORMAT && !cancel)
		  x = encodeMP3(s1, s2 + ".mp3", wm.getNumChannels(), wm.getSampleRate());
	  sendMessage(0, true);
	  if (x == 0) {
		  cancel = true;
		  stopForeground(true);
		  showError(ENCODER_ERROR);
	  }
	  try{
		  if (encFormat != Encodings.WAV_FORMAT)
			  removeWFile(s1);
	  } catch(Exception e) {
		  e.printStackTrace();
	  }
  }
  
  private void show(final long t, final int a, final int b) {
	  handler.post(new Runnable() {
		  public void run() {
			  Toast.makeText(TTSaverService.this,
					getText(R.string.total_time) + Utils.getTime(t) + "; channels: " + a + "; samplerate: " + b,
					Toast.LENGTH_LONG).show();
		  }
	  });
  }
  
  private void show(final String s, final int a) {
	  handler.post(new Runnable() {
		  public void run() {
			  Toast.makeText(TTSaverService.this,
					"" + s + " converted. Annotations replaced - " + a + ".",
					Toast.LENGTH_LONG).show();
		  }
	  });
  }
  
  private void showError(int er) {
	hasErrors = true;
	switch(er) {
	case TTS_INIT_ERROR:
		errorMsg = (String) getText(R.string.tts_init_error);
		break;
	case TTS_ERROR:
		errorMsg = (String) getText(R.string.tts_error);
		break;
	case BUFFER_ERROR:
		errorMsg = (String) getText(R.string.buffer_error);
		break;
	case ENCODER_ERROR:
		errorMsg = (String) getText(R.string.encoder_error);
		break;
	}
	handler.post(new Runnable() {
		public void run() {
			Toast.makeText(TTSaverService.this,
					errorMsg,
					Toast.LENGTH_LONG).show();
			}
		});
	showErrNotif(errorMsg);
  }

  private void showErrNotif(String msg) {
	  SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	  boolean bool_notif = prefs.getBoolean("pref_notifsound", false);

	  Intent notificationIntent = new Intent(this, MainActivity.class);
	  notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
	  	.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
	  
	  PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
	  
	  Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	  
	  Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_red);
	  
	  Builder mBuilder = new Builder(this, bool_notif ? CH_ID_HIGH : CH_ID_LOW)
	    .setSmallIcon(R.drawable.ic_white)
	    .setLargeIcon(bm)
	    .setContentTitle(getText(R.string.error_notification_title))
	    .setContentText(msg)
	    .setDefaults(Notification.DEFAULT_LIGHTS)
	    .setContentIntent(pendingIntent);
	  
	  if (Build.VERSION.SDK_INT >= 21)
		  mBuilder.setCategory(Notification.CATEGORY_ERROR);
	  if (bool_notif)
		  mBuilder.setSound(soundUri);
	  
	  mBuilder.setAutoCancel(true);
	  
	  NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	  mNotificationManager.notify(ONGOING_NOTIFICATION_ID, mBuilder.build());
  }

  private void finalNotif() {
	  SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	  boolean bool_notif = prefs.getBoolean("pref_notifsound", false);

	  Intent notificationIntent = new Intent(this, MainActivity.class);
	  notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
	  	.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
	  PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
	  
	  Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	  
	  Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
	  
	  Builder mBuilder = new Builder(this, bool_notif ? CH_ID_HIGH : CH_ID_LOW)
	    .setSmallIcon(R.drawable.ic_white)
	    .setLargeIcon(bm)
	    .setContentTitle(getText(R.string.final_notification_title))
	    .setContentText(getText(R.string.final_notification_text))
	    .setDefaults(Notification.DEFAULT_LIGHTS)
	    .setContentIntent(pendingIntent);
	  
	  if (Build.VERSION.SDK_INT >= 21)
		  mBuilder.setCategory(Notification.CATEGORY_STATUS);
	  if (bool_notif)
		  mBuilder.setSound(soundUri);
	  
	  mBuilder.setAutoCancel(true);
	  
	  NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	  mNotificationManager.notify(ONGOING_NOTIFICATION_ID, mBuilder.build());
  }
  
  public void stop() {
	  cancel = true;
	  if (t.isAlive())
		  t.interrupt();
	  cancelEnc();
  }
  
  public void forceStop() {
	  cancel = true;
	  if (t.isAlive())
		  t.interrupt();
	  cancelEnc();
	  if (tts != null)
		  tts.shutdown();
	  if (!list.isEmpty())
		  deleteWFiles(list);
	  isRunning = false;
	  sendMessage(RECORD_FINISHED, false);
	  tts = null;
	  stopForeground(true);
	  handler.removeCallbacks(timerRunnable);
	  wakeLock.release();
  }
  
  public boolean isRunning() {
	  return isRunning;
  }
  
  private void sendMessage(int x, boolean encPBar) {
	  if (x >= 0) {
		  if (encPBar)
			  encProgressStatus = x;
		  else
			  progressStatus = x;
	  } else {
		  encProgressStatus = 0;
		  progressStatus = 0;
	  }
	  mintent.putExtra(MainActivity.MESSAGE, x);
	  mintent.putExtra(MainActivity.TYPE, encPBar);
	  LocalBroadcastManager.getInstance(this).sendBroadcast(mintent);
  }
  
  private void updatePBar(int progress) {
	  sendMessage(progress, true);
  }
  
  public int getProgress() {
	  return progressStatus;
  }
  
  public int getEncProgress() {
	  return encProgressStatus;
  }
  
  public long getRemTime() {
	  return timeT;
  }
  
  private void calcAvrg() {
	  totalT += endT - startT;
	  if (((currentFile - 1) % nparts == 0 || currentFile < 6) && bufferParts > 5) {
		  timeT = (long)(((float) totalT) * ((float)(bufferParts - currentFile - 1)) / (float)(currentFile - 1));
		  LocalBroadcastManager.getInstance(this).sendBroadcast(tintent);
	  }
	  if (currentFile == 2 && bufferParts > 5)
		  handler.postDelayed(timerRunnable, 0);
  }

}
