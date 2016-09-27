package com.runtracer;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class RunActivity extends Activity implements View.OnClickListener, SensorEventListener, LoaderManager.LoaderCallbacks<Cursor>, View.OnLongClickListener, LocationListener {

	private static final int MAX_ATTEMPTS = 10;
	String status = "----";
	Sensor accelerometer;
	SensorManager sm;

	Vector accelerometer_vector;

	private BluetoothAdapter mBluetoothAdapter;
	private LocationManager locationManager;
	Location previousLocation;
	private GpsStatus gpsStatus;

	private static final String TAG = "runtracer";

	private UserData user_bio;
	private RunData run_data;

	private static long elapsedMillis;

	public int heart_rate_at_peak;
	public int heart_rate_at_rest;
	public boolean bMeasuredPeak;

	long last_time = 0;
	long this_time = 0;

	private long time_calories_now;
	private long time_calories_last;

	public double threshold_modulus = 2.00;
	public double threshold_begin = 0;
	public double threshold_end = 0;

	public final int fifo_sz_angle = 200;
	public final int fifo_sz_speed = 200;
	public final int fifo_sz_accel = 400;

	public final int STATE_INITIAL = 0;
	public final int STATE_OUTSIDE = 10;
	public final int STATE_INSIDE = 20;

	private final int IDLE = 0;
	private final int RUNNING = 1;
	private final int PAUSED = 2;

	boolean isRegistered;

	public int state = STATE_INITIAL;
	private int user_status;

	private final long time_threshold = 500;

	private final double human_speed_limit = 40;
	private final double accelerometer_spurious_change = 8;

	private final double time_diff_min = 40;
	private final double time_diff_max = 400;

	private long time_difference = 0;
	private long time_initial = 0;
	private long time_start = 0;
	private long time_end = 0;
	private long time_now = 0;

	private double avg_acceleration = 0;
	private double accelerometer_value = 0;

	private double accelerometer_last_value = 0;

	//TDDO: user height
	private double avg_distance = 0.3080;
	private double last_curr_speed = 0;

	//GUI elements
	private Button btn_start;
	private Button btn_measure_inclination;
	private Button btn_calibrate;
	private Button btn_done_run;

	private Switch btn_indoor;

	private Chronometer mChronometer;
	private ProgressBar mAccelerationBar;

	private TextView acceleration;

	private TextView mCurrentInclination;
	private TextView mRunInclination;

	private TextView mSpeedMotion;
	private TextView mSpeedGPS;

	private TextView mCaloriesDistance;
	private TextView mCaloriesHeartBeat;

	private TextView mDistanceGPS;
	private TextView mDistanceMotion;

	private TextView mDistanceUnitsMotion;
	private TextView mSpeedUnitsMotion;

	private TextView mDistanceUnitsGPS;
	private TextView mSpeedUnitsGPS;

	TableLayout mTableLayout0;
	TableLayout mTableLayout1;
	TableLayout mTableLayout2;
	TableLayout mTableLayout3;
	TableLayout mTableLayout4;
	TableLayout mTableLayout5;

	TableRow mMotionDistanceRow;
	TableRow mMotionSpeedRow;

	TableRow mGPSDistanceRow;
	TableRow mGPSSpeedRow;

	//private TextView mTroubleshooting;

	private String mDeviceAddress;
	private BluetoothLeService mBluetoothLeService;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

	private LinkedList<Double> fifo_grade = new LinkedList<>();
	private LinkedList<Double> fifo_acceleration = new LinkedList<>();
	private LinkedList<Double> fifo_speed = new LinkedList<>();

	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";

	private int no_satellites;
	private double longitude;
	private double latitude;
	private double altitude;
	private double gps_speed;
	private double accuracy;
	private boolean gps_on;
	private long memory_limit;

	private int countSatellites() {
		int count = 0;
		gpsStatus = locationManager.getGpsStatus(null);
		for (GpsSatellite sat : gpsStatus.getSatellites()) {
			if (sat.usedInFix()) {
				count++;
			}
		}
		return count;
	}

	public long availableMemory() {
		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(mi);
		return (mi.availMem);
	}

	public long totalMemory() {
		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(mi);
		return (mi.totalMem);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.memory_limit = this.totalMemory();
		accelerometer_vector = new Vector();

		//GPS data acquired and monitored.
		/********** get Gps location service LocationManager object ***********/
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		previousLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		/*
		  Parameters :
		     First(provider)    :  the name of the provider with which to register
		     Second(minTime)    :  the minimum time interval for notifications, in milliseconds. This field is only used as a hint to conserve power, and actual time between location updates may be greater or lesser than this value.
		     Third(minDistance) :  the minimum distance interval for notifications, in meters
		     Fourth(listener)   :  a {#link LocationListener} whose onLocationChanged(Location) method will be called for each location update
        */

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 800, 10, this);
		locationManager.getGpsStatus(gpsStatus);
		this.no_satellites = countSatellites();

		setContentView(R.layout.activity_run);

		user_status = IDLE;
		elapsedMillis = 0;
		fifo_grade.clear();
		fifo_acceleration.clear();
		fifo_speed.clear();

		try {
			run_data = new RunData();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		run_data.calories_v_distance = 0.0;
		run_data.calories_v_heart_beat = 0.0;

		run_data.distance_km_v = 0.0;
		run_data.average_speed_km_h_v = 0.0;
		run_data.average_speed_miles_h_v = 0.0;

		time_initial = new Date().getTime();
		this_time = new Date().getTime();
		last_time = new Date().getTime();

		time_calories_now = new Date().getTime();
		time_calories_last = new Date().getTime();

		user_bio = (UserData) getIntent().getSerializableExtra("UserData");

		run_data.getValues();
		run_data.current_weight_v = user_bio.current_weight_v;
		run_data.current_fat_v = user_bio.current_fat_v;

		setupGui();

		sm = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);

		return intentFilter;
	}

	private boolean measureRecoveryHeartRate() {
		boolean bok = false;
		if (run_data.current_heart_rate > 10 ) {
			this.bMeasuredPeak=true;
			this.heart_rate_at_peak= run_data.current_heart_rate;
			this.heart_rate_at_rest= run_data.current_heart_rate;
		}
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		// set title
		alertDialogBuilder.setTitle("Measure Heart Recovery Rate?");
		// set dialog message
		alertDialogBuilder
			.setMessage("Stay idle for 1 minute, measuring your heart rate.")
			.setCancelable(false)
			.setNegativeButton("NO", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// if this button is clicked, close
					// current activity
					try {
						RunActivity.this.finishRunNow();
					} catch (InterruptedException | IOException | ParseException | NoSuchAlgorithmException | JSONException e) {
						e.printStackTrace();
					}
				}
			})
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// if this button is clicked, just close
					// the dialog box and do nothing
					dialog.cancel();
				}
			});
		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();
		// show it
		alertDialog.show();
		return bok;
	}

	private void finishRun() throws InterruptedException, IOException, ParseException, NoSuchAlgorithmException, JSONException {
		writeLog("finishRun!!");
		sm.unregisterListener(this);
		locationManager.removeUpdates(this);
		//TODO: criteria to measure heart recovery rate
		if (run_data.current_heart_rate > 10 ) {
			measureRecoveryHeartRate();
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					if (bMeasuredPeak && run_data.current_heart_rate > user_bio.target_hr_moderate && ( heart_rate_at_peak - run_data.current_heart_rate) > 0  ) {
						heart_rate_at_rest= run_data.current_heart_rate;
						run_data.recovery_hr= heart_rate_at_peak - heart_rate_at_rest;
					}
					try {
						finishRunNow();
					} catch (InterruptedException | NoSuchAlgorithmException | IOException | JSONException | ParseException e) {
						e.printStackTrace();
					}
				}
			}, 60000);
		} else {
			run_data.recovery_hr= -1;
			finishRunNow();
		}
	}

	private void finishRunNow() throws InterruptedException, NoSuchAlgorithmException, ParseException, JSONException, IOException {
		writeLog("finishRunNow!!");
		sendRunData();
		if (run_data.recovery_hr > 0) {
			MainActivity.user_bio.recovery_hr = run_data.recovery_hr;
		}
		setResult(RESULT_OK);
		finish();
	}

	private boolean sendServerDataServiceRequest(String hash) throws InterruptedException {
		writeLog("sendServerDataServiceRequest!!");
		boolean result= false;
		if (isServerReady()) {
			writeLog("sendServerDataServiceRequest: server ready!!");
			MainActivity.available.acquire();
			MainActivity.dbExchange.pending = true;
			MainActivity.available.release();
			Intent mServiceIntent = new Intent(this, ServerDataService.class);
			mServiceIntent.setAction(ServerDataService.ACTION_QUERY_SERVER);
			mServiceIntent.putExtra("hash", hash);
			MainActivity.lastHash= hash;
			this.startService(mServiceIntent);
			result= true;
		}
		return result;
	}

	private boolean isServerReady() throws InterruptedException {
		boolean result= false;
		for (int attempts= 0; attempts < MAX_ATTEMPTS && !result; attempts++) {
			MainActivity.available.acquire();
			result= !MainActivity.dbExchange.pending;
			MainActivity.available.release();
		}
		return result;
	}

	private boolean sendRunData() throws InterruptedException, JSONException, ParseException, NoSuchAlgorithmException, IOException {
		boolean result= false;
		boolean data_ok= run_data.checkRunData();
		String hash;
		if (data_ok && isServerReady()) {
			MainActivity.available.acquire();
			MainActivity.dbExchange.clear();
			run_data.getValues();
			String json_run_data = run_data.createJSON().toString();
			JSONObject json_run = new JSONObject(json_run_data);
			MainActivity.dbExchange.url = new URL("https://www.runtracer.com/select.php");
			MainActivity.dbExchange.command = "send_run_data";
			MainActivity.dbExchange.full_name = user_bio.full_name;
			MainActivity.dbExchange.accountEmail = user_bio.email;
			MainActivity.dbExchange.json_data_in = json_run;
			MainActivity.dbExchange.json_data_in.accumulate("command", MainActivity.dbExchange.command);
			MainActivity.dbExchange.json_data_in.accumulate("uid", user_bio.uid);
			MainActivity.dbExchange.json_data_in.accumulate("session_id", user_bio.session_id);
			hash = MainActivity.dbExchange.getHash();
			MainActivity.available.release();
			result= sendServerDataServiceRequest(hash);
		}
		return result;
	}

	protected void onResume() {
		super.onResume();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		isRegistered = true;
		writeLog("RunActivity: registerReceiver: " + mGattUpdateReceiver);

		if (mBluetoothLeService != null) {
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
			writeLog("RunActivity: Connect request result=" + result);
		}
	}

	@Override protected void onStart() {
		super.onStart();
	}

	protected void onPause() {
		super.onPause();
		writeLog("RunActivity: onPause...");
	}

	protected void onStop() {
		super.onStop();
		if (isRegistered) {
			unregisterReceiver(mGattUpdateReceiver);
			isRegistered = false;
		}
		writeLog("RunActivity: onStop...");
	}

	@Override protected void onDestroy() {
		super.onDestroy();
		if (isRegistered) {
			unregisterReceiver(mGattUpdateReceiver);
			isRegistered = false;
		}
		writeLog("RunActivity: onDestroy...");
		return;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_run, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private double getAvgGrade (double current_angle) {
		double sum_grade=0;
		double avg_grade=0;
		if (fifo_grade.size()>fifo_sz_angle) {
			fifo_grade.removeFirst();
		}
		fifo_grade.add(current_angle);
		for (int i=0; i<(fifo_grade.size()); i++) {
			sum_grade= fifo_grade.get(i) + sum_grade;
		}
		avg_grade= sum_grade / fifo_grade.size();
		return avg_grade;
	}

	private double getAvgAcceleration(double current_acceleration) {
		double sum_acceleration=0;
		double avg_acceleration=0;
		if (fifo_acceleration.size()>fifo_sz_accel) {
			fifo_acceleration.removeFirst();
		}
		fifo_acceleration.add(current_acceleration);
		for (int i=0; i<(fifo_acceleration.size()); i++) {
			sum_acceleration= fifo_acceleration.get(i) + sum_acceleration;
		}
		avg_acceleration= sum_acceleration / fifo_acceleration.size();
		return avg_acceleration;
	}

	private double getAvgSpeed(double current_speed) {
		double sum_speed=0;
		double avg_speed=0;
		if(current_speed>human_speed_limit) {
			current_speed=human_speed_limit;
		}
		if (fifo_speed.size()>fifo_sz_speed) {
			fifo_speed.removeFirst();
		}
		fifo_speed.add(current_speed);
		for (int i=0; i<(fifo_speed.size()); i++) {
			sum_speed= fifo_speed.get(i) + sum_speed;
		}
		avg_speed= sum_speed/ fifo_speed.size();
		return avg_speed;
	}

	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				finish();
			}
			writeLog("RunActivity: Bluetooth LE Service initialized: " + mDeviceAddress);
			// Automatically connects to the device upon successful start-up initialization.
			mBluetoothLeService.connect(mDeviceAddress);
			writeLog("RunActivity: Bluetooth LE Service connected: " + mDeviceAddress);
		}
		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
			writeLog("RunActivity: Bluetooth LE Service disconnected: " + mDeviceAddress);
		}
	};

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	// ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
	//                        or notification operations.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				run_data.current_heart_rate= -1;
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				// Show all the supported services and characteristics on the user interface.
				displayGattServices(mBluetoothLeService.getSupportedGattServices());
				writeLog((mBluetoothLeService.getSupportedGattServices()).toString());
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				String data= (intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
				TextView t = (TextView) findViewById(R.id.heart_rate_value);
				t.setText(data);
				run_data.current_heart_rate= Integer.parseInt(data);
			}
		}
	};

	// Demonstrates how to iterate through the supported GATT Services/Characteristics.
	// In this sample, we populate the data structure that is bound to the ExpandableListView
	// on the UI.
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null) return;
		String uuid = null;
		String unknownServiceString = getResources().getString(R.string.unknown_service);
		String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
		ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData=  new ArrayList<ArrayList<HashMap<String, String>>>();
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();
			writeLog("new BluetoothGattService: " + uuid);
			currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
			currentServiceData.put(LIST_UUID, uuid);
			gattServiceData.add(currentServiceData);
			writeLog("new gattServiceData: " + currentServiceData.toString());
			ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
			List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();
			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				charas.add(gattCharacteristic);
				HashMap<String, String> currentCharaData = new HashMap<String, String>();
				uuid = gattCharacteristic.getUuid().toString();
				currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
				currentCharaData.put(LIST_UUID, uuid);
				gattCharacteristicGroupData.add(currentCharaData);
				writeLog("new item for currentCharaData: " + currentCharaData.toString());

				if (gattCharacteristic.getUuid().toString().matches("00002a37-0000-1000-8000-00805f9b34fb")) {
					mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
					writeLog("found item for gattCharacteristic matching: " + "00002a37-0000-1000-8000-00805f9b34fb: " + gattCharacteristic.getUuid().toString());
				}
			}
			mGattCharacteristics.add(charas);
			gattCharacteristicData.add(gattCharacteristicGroupData);
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		String bluetoothDeviceName="";
		writeLog("RunActivity: onActivityResult");
		if (resultCode == RESULT_OK) {
			bluetoothDeviceName = data.getStringExtra("BluetoothDeviceName");
			writeLog("RunActivity: bluetoothDeviceName received from scan activity: " + bluetoothDeviceName);
		}
		try {
			writeLog("RunActivity: bluetoothDeviceName being processed " + bluetoothDeviceName);
			//mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			// Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
			// BluetoothAdapter through BluetoothManager.
			final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			mBluetoothAdapter = bluetoothManager.getAdapter();
			// Checks again if Bluetooth is supported on the device.
			if (mBluetoothAdapter == null) {
				Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
			BluetoothDevice btdevice= mBluetoothAdapter.getRemoteDevice(bluetoothDeviceName);
			mDeviceAddress= btdevice.getAddress();
			writeLog("RunActivity: bluetoothDeviceName parsed from scan activity: " + btdevice.toString());
			Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
			bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
		} catch ( Exception e) {
			e.getStackTrace();
		}
		//final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(bluetoothDeviceName);
		//writeLog("RunActivity: bluetoothDeviceName parsed from scan activity: " + device.toString());
	}

	private void setupGui() {

		mTableLayout0= (TableLayout) findViewById(R.id.table_layout_0);
		mTableLayout1= (TableLayout) findViewById(R.id.table_layout_1);
		mTableLayout2= (TableLayout) findViewById(R.id.table_layout_2);
		mTableLayout3= (TableLayout) findViewById(R.id.table_layout_3);
		mTableLayout4= (TableLayout) findViewById(R.id.table_layout_4);
		mTableLayout5= (TableLayout) findViewById(R.id.table_layout_5);

		mMotionDistanceRow= (TableRow) findViewById(R.id.motion_distance_row);
		mMotionSpeedRow= (TableRow) findViewById(R.id.motion_speed_row);

		mGPSDistanceRow= (TableRow) findViewById(R.id.gps_distance_row);
		mGPSSpeedRow= (TableRow) findViewById(R.id.gps_speed_row);

		acceleration= (TextView) findViewById(R.id.acceleration);

		mSpeedMotion= (TextView) findViewById(R.id.speed_ui);
		mSpeedGPS= (TextView) findViewById(R.id.gps_speed_value);

		mCaloriesDistance= (TextView) findViewById(R.id.calories_distance_value);
		mCaloriesHeartBeat= (TextView) findViewById(R.id.calories_heart_beat_value);

		mDistanceGPS= (TextView) findViewById(R.id.gps_distance_value);
		mDistanceMotion= (TextView) findViewById(R.id.distance_value);

		mDistanceUnitsMotion= (TextView) findViewById(R.id.distance_units);
		mDistanceUnitsGPS= (TextView) findViewById(R.id.gps_distance_units);

		mSpeedUnitsMotion= (TextView) findViewById(R.id.speed_units);
		mSpeedUnitsGPS= (TextView) findViewById(R.id.gps_speed_units);

		btn_start=(Button) findViewById(R.id.btn_start_run);
		btn_start.setOnClickListener(this);
		btn_start.setOnLongClickListener(this);

		btn_start.setEnabled(true);

		btn_done_run=(Button) findViewById(R.id.btn_done_run);
		btn_done_run.setOnClickListener(this);
		btn_done_run.setEnabled(true);

		btn_indoor= (Switch) findViewById(R.id.run_indoor_value);
		btn_indoor.setOnClickListener(this);
		btn_indoor.setEnabled(true);

		btn_measure_inclination= (Button) findViewById(R.id.measure_inclination_button);
		btn_measure_inclination.setOnClickListener(this);
		btn_measure_inclination.setEnabled(true);

		btn_calibrate= (Button) findViewById(R.id.calibrate_button);
		btn_calibrate.setOnClickListener(this);
		btn_calibrate.setEnabled(true);

		mCurrentInclination = (TextView) findViewById(R.id.measure_inclination_value);
		mCurrentInclination.setOnClickListener(this);
		mCurrentInclination.setEnabled(true);

		mRunInclination= (TextView) findViewById(R.id.run_inclination_value);
		mRunInclination.setOnClickListener(this);
		mRunInclination.setEnabled(true);

		mChronometer= (Chronometer) findViewById(R.id.time_value);

		mCaloriesDistance= (TextView) findViewById(R.id.calories_distance_value);
		mDistanceMotion= (TextView) findViewById(R.id.distance_value);

		mDistanceUnitsMotion= (TextView) findViewById(R.id.distance_units);
		mDistanceUnitsMotion.setEnabled(true);

		mSpeedUnitsMotion= (TextView) findViewById(R.id.speed_units);
		mSpeedUnitsMotion.setEnabled(true);

		mAccelerationBar = (ProgressBar) findViewById(R.id.accelerationBar);
		mAccelerationBar.setMax(120);

		this.updateGui();
	}

	public void updateGui() {
		if (user_bio.bMetricSystem) {
			mDistanceUnitsMotion.setText(R.string.unit_km);
			mDistanceUnitsGPS.setText(R.string.unit_km);
			mSpeedUnitsMotion.setText(R.string.unit_km_h);
			mSpeedUnitsGPS.setText(R.string.unit_km_h);
		} else {
			mDistanceUnitsMotion.setText(R.string.unit_miles);
			mDistanceUnitsGPS.setText(R.string.unit_miles);
			mSpeedUnitsMotion.setText(R.string.unit_miles_h);
			mSpeedUnitsGPS.setText(R.string.unit_miles_h);
		}

		if (btn_indoor.isChecked()) {
			btn_indoor.setText(R.string.run_indoor_on);

			mSpeedGPS.setEnabled(false);
			mDistanceGPS.setEnabled(false);
			mSpeedMotion.setEnabled(true);
			mDistanceMotion.setEnabled(true);

			mDistanceUnitsGPS.setEnabled(false);
			mSpeedUnitsGPS.setEnabled(false);

			mDistanceUnitsMotion.setEnabled(true);
			mSpeedUnitsMotion.setEnabled(true);

			mMotionDistanceRow.setBackgroundColor(Color.YELLOW);
			mMotionSpeedRow.setBackgroundColor(Color.YELLOW);

			mGPSDistanceRow.setBackgroundColor(Color.LTGRAY);
			mGPSSpeedRow.setBackgroundColor(Color.LTGRAY);

		} else {
			btn_indoor.setText(R.string.run_indoor_off);

			mSpeedGPS.setEnabled(true);
			mDistanceGPS.setEnabled(true);
			mSpeedMotion.setEnabled(false);
			mDistanceMotion.setEnabled(false);

			mDistanceUnitsGPS.setEnabled(true);
			mSpeedUnitsGPS.setEnabled(true);

			mDistanceUnitsMotion.setEnabled(false);
			mSpeedUnitsMotion.setEnabled(false );

			mMotionDistanceRow.setBackgroundColor(Color.LTGRAY);
			mMotionSpeedRow.setBackgroundColor(Color.LTGRAY);

			mGPSDistanceRow.setBackgroundColor(Color.YELLOW);
			mGPSSpeedRow.setBackgroundColor(Color.YELLOW);
		}
	}

	public boolean onLongClick(View v) {
		try {
			switch (v.getId()) {
				case R.id.btn_start_run:
					writeLog(String.format("onLongClick..."));
					break;
			}
		} catch (Exception e) {
			writeLog("onLongClick Exception: " + e.getMessage());
		}
		return false;
	}

	public void onClick(View v) {
		try {
			switch (v.getId()) {
				case R.id.btn_start_run:
					startChronometer(v);
					break;

				case R.id.btn_done_run:
					run_data.setEndTime();
					this.finishRun();
					break;

				case R.id.run_indoor_value:
					run_data.threadmill_factor= (btn_indoor.isChecked())?0:0.84;
					writeLog(String.format("threadmill factor= %.2f", run_data.threadmill_factor));
					updateGui();
					break;

				case R.id.measure_inclination_button:
					writeLog("Pressed button measure inclination.");
					mRunInclination.setText(String.format("%.2f", accelerometer_vector.avgGrade - accelerometer_vector.gradeOffset ));
					break;

				case R.id.calibrate_button:
					accelerometer_vector.gradeOffset= accelerometer_vector.avgGrade;
					writeLog(String.format("Calibrating inclination value to %.2f", accelerometer_vector.gradeOffset ));
					break;

				case R.id.measure_inclination_value:
					writeLog("Text Inclination Clicked.");
					break;
			}
		} catch (Exception e) {
			writeLog(String.format("onClick Exception: %s", e.getMessage()));
		}
	}

	private void showElapsedTime() {
		elapsedMillis = SystemClock.elapsedRealtime() - mChronometer.getBase();
	}

	private long getElapsedTime() {
		elapsedMillis = SystemClock.elapsedRealtime() - mChronometer.getBase();
		return (elapsedMillis);
	}

	/**
	 * Called when the location has changed.
	 * <p/>
	 * <p> There are no restrictions on the use of the supplied Location object.
	 *
	 * @param location The new location, as a Location object.
	 */
	@Override
	public void onLocationChanged(Location location) {
		if (location!= null) {
			this.gps_on= true;
			this.no_satellites= countSatellites();

			this.latitude = location.getLatitude();
			this.longitude = location.getLongitude();

			if (location.hasAccuracy()) {
				this.accuracy = location.getAccuracy();
			}
			if (location.hasAltitude()) {
				this.altitude = location.getAltitude();
			}
			if (location.hasSpeed()) {
				this.gps_speed = location.getSpeed() * run_data.conv_m_s_km_h;
			} else {
				this.gps_speed = 0;
			}
			if (previousLocation != null) {
				if (previousLocation.hasSpeed()) {
					run_data.gps_distance_km += location.distanceTo(previousLocation) / 1000;
				}
			}
			previousLocation = location;
		}
	}

	/**
	 * Called when the provider status changes. This method is called when
	 * a provider is unable to fetch a location or if the provider has recently
	 * become available after a period of unavailability.
	 *
	 * @param provider the name of the location provider associated with this
	 *                 update.
	 * @param status   {@link \\LocationProvider#OUT_OF_SERVICE} if the
	 *                 provider is out of service, and this is not expected to change in the
	 *                 near future; {@link \\LocationProvider#TEMPORARILY_UNAVAILABLE} if
	 *                 the provider is temporarily unavailable but is expected to be available
	 *                 shortly; and {@link \\LocationProvider#AVAILABLE} if the
	 *                 provider is currently available.
	 * @param extras   an optional Bundle which will contain provider specific
	 *                 status variables.
	 *                 <p/>
	 *                 <p> A number of common key/value pairs for the extras Bundle are listed
	 *                 below. Providers that use any of the keys on this list must
	 *                 provide the corresponding value as described below.
	 *                 <p/>
	 *                 <ul>
	 *                 <li> satellites - the number of satellites used to derive the fix
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		/******** Called when User on Gps  *********/
		this.no_satellites= countSatellites();
		String str = String.format("Latitude: %.2f \nLongitude: %.2f \nNumber of Satellites: %d \nStatus: %d", this.latitude, this.longitude, this.no_satellites, status);
	}

	/**
	 * Called when the provider is enabled by the user.
	 *
	 * @param provider the name of the location provider associated with this
	 *                 update.
	 */
	@Override
	public void onProviderEnabled(String provider) {
		/******** Called when User on Gps  *********/
		this.gps_on= true;
	}

	/**
	 * Called when the provider is disabled by the user. If requestLocationUpdates
	 * is called on an already disabled provider, this method is called
	 * immediately.
	 *
	 * @param provider the name of the location provider associated with this
	 *                 update.
	 */
	@Override
	public void onProviderDisabled(String provider) {
		/******** Called when User off Gps *********/
		Toast.makeText(getBaseContext(), "Gps turned off ", Toast.LENGTH_LONG).show();
		this.gps_on= false;
	}

	private class Vector {
		public double x=0.0;
		public double y=0.0;
		public double z=0.0;
		public double gradeOffset= 0.0;
		public double avgGrade= 0.0;

		Vector() {
			this.x=0.0;
			this.y=0.0;
			this.z=0.0;
		}

		public double getGrade() {
			double grade= 0;
			double angle=0.0;

			angle= Math.toRadians(this.getAngle());
			if (angle < (Math.PI * 0.8) ) {
				grade = Math.sin(angle) / Math.cos(angle);
				grade*= 100.00;
			}

			return (grade);
		}

		double getAngle() {
			double modulus=0.0;

			double cosine_alpha= 0.0;
			double cosine_beta= 0.0;
			double cosine_gamma= 0.0;

			double angle_alpha= 0.0;
			double angle_beta= 0.0;
			double angle_gamma= 0.0;

			modulus= Math.sqrt(Math.pow(this.x, 2) + Math.pow(this.y, 2) + Math.pow(this.z, 2));

			cosine_alpha= this.x / modulus;
			cosine_beta= this.y / modulus;
			cosine_gamma= this.z / modulus;

			angle_alpha=  Math.toDegrees(Math.asin(cosine_alpha));
			angle_beta=   Math.toDegrees(Math.asin(cosine_beta));
			angle_gamma=  Math.toDegrees(Math.asin(cosine_gamma));

			return(angle_beta);
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		accelerometer_value=0;
		accelerometer_value = Math.sqrt(Math.pow(event.values[0], 2) + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2));

		accelerometer_vector.x= event.values[0];
		accelerometer_vector.y= event.values[1];
		accelerometer_vector.z= event.values[2];
		accelerometer_vector.avgGrade= getAvgGrade(accelerometer_vector.getGrade());

		if (user_status == IDLE) {
			if (accelerometer_vector.getAngle() > -55 && accelerometer_vector.getAngle() < 55) {
				mCurrentInclination.setText(String.format("%.2f %%", accelerometer_vector.avgGrade - accelerometer_vector.gradeOffset ));
				btn_calibrate.setEnabled(true);
				btn_measure_inclination.setEnabled(true);
			} else {
				mCurrentInclination.setText("--");
			}
			return;
		} else {
			mCurrentInclination.setText("");
			btn_calibrate.setEnabled(false);
			btn_measure_inclination.setEnabled(false);

			ViewGroup layout1 = (ViewGroup) btn_measure_inclination.getParent();
			if(null!=layout1) {
				layout1.removeView(btn_measure_inclination);
			}
			ViewGroup layout2 = (ViewGroup) btn_calibrate.getParent();
			if(null!=layout2) {
				layout2.removeView(btn_calibrate);
			}
		}

		if ( (Math.abs(accelerometer_value - accelerometer_last_value) > accelerometer_spurious_change) ) {
			//spurious value, will be discarded.
			accelerometer_value= accelerometer_last_value;
		}
		accelerometer_last_value= accelerometer_value;
		avg_acceleration = getAvgAcceleration(accelerometer_value);

		mCaloriesDistance.setText(String.format("%.2f", run_data.calories_v_distance));
		mCaloriesHeartBeat.setText(String.format("%.2f", run_data.calories_v_heart_beat));

		mAccelerationBar.setProgress((int) (accelerometer_value * 10));
		acceleration.setText(String.format("Status: %s", status));

		threshold_begin=avg_acceleration + threshold_modulus;
		threshold_end=avg_acceleration - threshold_modulus ;

		switch (state) {

			case STATE_INITIAL:
				run_data.current_speed_m_s_v = 0;
				state = STATE_OUTSIDE;
				break;

			case STATE_OUTSIDE:
				time_now= new Date().getTime();

				if ( (time_now - time_end) > time_threshold)  {
					time_difference = time_now - time_end;
					run_data.current_speed_m_s_v= (avg_distance / time_difference) * 1000;
					run_data.current_speed_km_h_v= run_data.current_speed_m_s_v * run_data.conv_m_s_km_h;
				}

				if (accelerometer_value > threshold_begin ) {
					state= STATE_INSIDE;
					time_start= new Date().getTime();
				}
				break;

			case STATE_INSIDE:
				time_now= new Date().getTime();
				if ( (time_now - time_start) > time_threshold)  {
					state= STATE_OUTSIDE;
				}

				if (accelerometer_value < threshold_end ) {
					state= STATE_OUTSIDE;
					time_end= new Date().getTime();
					if ( ((time_end - time_start) < time_diff_max) && ((time_end - time_start) > time_diff_min) )  {
						time_difference = time_end - time_start;
						run_data.current_speed_m_s_v= (avg_distance / time_difference) * 1000;
						run_data.current_speed_km_h_v= run_data.current_speed_m_s_v * run_data.conv_m_s_km_h;
						run_data.distance_m_v= run_data.distance_m_v + avg_distance;
						run_data.distance_km_v= run_data.distance_m_v / 1000;
					}
				}
				break;

			default:
				state= STATE_INITIAL;
				break;
		}
		/*  Male: ((-55.0969 + (0.6309 x HR) + (0.1988 x W) + (0.2017 x A))/4.184) x 60 x T
				Female: ((-20.4022 + (0.4472 x HR) - (0.1263 x W) + (0.074 x A))/4.184) x 60 x T */

		run_data.getValues();

		if (user_status == RUNNING) {
			time_calories_now= new Date().getTime();
			long delta_time= ( time_calories_now - time_calories_last );
			double delta_time_hours= ((double)delta_time) / 1000 / 60 / 60;

			if ( delta_time > run_data.granularity_time ) {
				if (run_data.current_heart_rate >= user_bio.resting_hr && user_bio.resting_hr > 20 && user_bio.resting_hr < 100) {
					if (user_bio.gender_v) {
						//Male: ((-55.0969 + (0.6309 x HR) + (0.1988 x W) + (0.2017 x A))/4.184) x 60 x T */
						run_data.calories_v_heart_beat += ((-55.0969 + (0.6309 * run_data.current_heart_rate) + (0.1988 * user_bio.current_weight_v) + (0.2017 * user_bio.age)) / 4.184) * 60 * delta_time_hours;
					} else {
						// Female: ((-20.4022 + (0.4472 x HR) - (0.1263 x W) + (0.074 x A))/4.184) x 60 x T */
						run_data.calories_v_heart_beat += ((-20.4022 + (0.4472 * run_data.current_heart_rate) + (0.1263 * user_bio.current_weight_v) + (0.074 * user_bio.age)) / 4.184) * 60 * delta_time_hours;
					}
				}
				//-20% ≤ % Grade ≤ - 15%:
				if (run_data.inclination >= -20 && run_data.inclination <= -15) {
					run_data.calories_v_distance = (((-0.01 * run_data.inclination) + 0.50) * user_bio.current_weight_v + run_data.threadmill_factor) * run_data.distance_km_v * user_bio.cff;
				}
				//-15% < % Grade ≤ - 10%:
				if (run_data.inclination >= -15 && run_data.inclination <= -10) {
					run_data.calories_v_distance = (((-0.02 * run_data.inclination) + 0.35) * user_bio.current_weight_v + run_data.threadmill_factor) * run_data.distance_km_v * user_bio.cff;
				}
				//10% < % Grade ≤ 0%:
				if (run_data.inclination >= -10 && run_data.inclination <= 0) {
					run_data.calories_v_distance = (((0.04 * run_data.inclination) + 0.95) * user_bio.current_weight_v + run_data.threadmill_factor) * run_data.distance_km_v * user_bio.cff;
				}
				//0% < % Grade ≤ 10%:
				if (run_data.inclination > 0 && run_data.inclination <= 10) {
					run_data.calories_v_distance = (((0.05 * run_data.inclination) + 0.95) * user_bio.current_weight_v + run_data.threadmill_factor) * run_data.distance_km_v * user_bio.cff;
				}
				//10% < % Grade ≤ 15%:
				if (run_data.inclination > 10 && run_data.inclination <= 15) {
					run_data.calories_v_distance = (((0.07 * run_data.inclination) + 0.75) * user_bio.current_weight_v + run_data.threadmill_factor) * run_data.distance_km_v * user_bio.cff;
				}
				time_calories_last = new Date().getTime();

				double usedMemoryPercentage= 100 * (double) availableMemory() / (double) totalMemory();
				if ( usedMemoryPercentage < 80 ) {
					run_data.pushInstant(run_data.average_speed_km_h_v, this.avg_distance, this.gps_speed, run_data.gps_distance_km, run_data.calories_v_distance, run_data.calories_v_heart_beat, run_data.current_heart_rate, this.longitude, this.latitude, this.altitude);
				} else {
					writeLog("RunActivity: ERROR: MEMORY USE ABOVE 80%.");
				}
			}
		}
		run_data.getValues();
		run_data.average_speed_km_h_v= getAvgSpeed(run_data.current_speed_km_h_v);
		if (user_bio.bMetricSystem) {
			mDistanceMotion.setText(String.format("%.2f", run_data.distance_km_v));
			mDistanceGPS.setText(String.format("%.2f", run_data.gps_distance_km));
			mSpeedMotion.setText(String.format("%.2f", run_data.average_speed_km_h_v));
			mSpeedGPS.setText(String.format("%.2f",  this.gps_speed));
		} else {
			mDistanceMotion.setText(String.format("%.2f", run_data.distance_miles_v));
			mDistanceGPS.setText(String.format("%.2f", run_data.gps_distance_miles));
			mSpeedMotion.setText(String.format("%.2f",  run_data.average_speed_miles_h_v));
			mSpeedGPS.setText(String.format("%.2f",  this.gps_speed));
		}
		double idle_speed= 1;
		double walking_speed= 2;
		if (run_data.average_speed_km_h_v < idle_speed) {
			status="Idle";
		} else {
			this_time=  new Date().getTime();
			last_time=  new Date().getTime();
			if (run_data.average_speed_km_h_v < walking_speed) {
				status="Walking";
			} else {
				status = "Running";
			}
		}
		if(run_data.current_speed_m_s_v > 0) {
			last_curr_speed= run_data.current_speed_m_s_v;
		}
	}

	public void writeLog(String msg) {
		String date = (DateFormat.format("dd-MM-yyyy hh:mm:ss a", new Date()).toString());
		//Log.e(TAG, date + ": " + msg);
	}

	/**
	 * Called when the accuracy of the registered sensor has changed.
	 * <p/>
	 * <p>See the SENSOR_STATUS_* constants in
	 * {@link SensorManager SensorManager} for details.
	 *
	 * @param sensor
	 * @param accuracy The new accuracy of this sensor, one of
	 *                 {@code SensorManager.SENSOR_STATUS_*}
	 */

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public void startChronometer(View view) {
		switch(user_status) {
			case IDLE:
				updateChronometer();
				showElapsedTime();
				mChronometer.setBase(SystemClock.elapsedRealtime());
				mChronometer.start();
				user_status= RUNNING;
				time_calories_last= new Date().getTime();
				run_data.setStartTime();
				btn_start.setText("STOP");
				break;

			case PAUSED:
				updateChronometer();
				showElapsedTime();
				mChronometer.start();
				user_status= RUNNING;
				time_calories_last= new Date().getTime();
				btn_start.setText("STOP");
				break;

			case RUNNING:
				updateChronometer();
				showElapsedTime();
				mChronometer.stop();
				user_status= PAUSED;
				btn_start.setText("RESUME");
				break;
		}
	}

	public void updateChronometer() {
		int stoppedMilliseconds = 0;
		String chronoText = mChronometer.getText().toString();
		String array[] = chronoText.split(":");
		if (array.length == 2) {
			stoppedMilliseconds = Integer.parseInt(array[0]) * 60 * 1000 + Integer.parseInt(array[1]) * 1000;
		} else if (array.length == 3) {
			stoppedMilliseconds = Integer.parseInt(array[0]) * 60 * 60 * 1000 + Integer.parseInt(array[1]) * 60 * 1000 + Integer.parseInt(array[2]) * 1000;
		}
		mChronometer.setBase(SystemClock.elapsedRealtime() - stoppedMilliseconds);
		mChronometer.start();
	}

	protected String getPackageDirectory() throws PackageManager.NameNotFoundException {
		String path;
		PackageManager m = getPackageManager();
		String s = getPackageName();
		PackageInfo p = m.getPackageInfo(s, 0);
		path= p.applicationInfo.dataDir;

		return (path);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}
}
