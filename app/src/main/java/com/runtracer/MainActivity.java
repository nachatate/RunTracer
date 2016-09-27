package com.runtracer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.Builder;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.PersonBuffer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

public class MainActivity extends AppCompatActivity implements OnClickListener, ConnectionCallbacks, OnConnectionFailedListener, SensorEventListener, ResultCallback<People.LoadPeopleResult> {

	private static final boolean DEVELOPER_MODE= false;

	public static final int MAX_AVAILABLE= 1;
	private static final int MAX_ATTEMPTS = 10;

	public static final Semaphore available = new Semaphore(MAX_AVAILABLE, true);

	public static DataBaseExchange dbExchange;
	public static UserData user_bio;
	public static String lastHash= null;

	File userdatafile;
	File runmapfile;
	File runinfofile;

	FileOutputStream f_out;

	private BluetoothLeService mBluetoothLeService;
	private String mDeviceAddress;

	private HashMap activityListMap;
	private HashMap activityInfoMap;

	/* RequestCode for resolutions involving sign-in */
	private static final int RC_SIGN_IN = 0;
	private static final int NEW_USER_DATA = 1;       // The request code
	private static final int RUN_USER_DATA = 2;       // The request code
	private static final int LOGIN_USER_DATA = 3;     // The request code
	private static final int BLUETOOTH_LE = 4;        // The request code
	private static final int USER_PROFILE = 5;        // The request code
	private static final int ACTIVITIES_DATA = 6;     // The request code
	private static final int ABOUT_YOU= 7;            // The request code

	/* Keys for persisting instance variables in savedInstanceState */
	private static final String KEY_IS_RESOLVING = "is_resolving";
	private static final String KEY_SHOULD_RESOLVE = "should_resolve";

	private static final int MEASURING= 1;          // RHR Measuring state
	private static final int READY= 2;              // RHR Measuring state
	private static final int ACQUIRED= 3;           // RHR Measuring state

	private static final int NETWORK_TIMEOUT= 80000;

	//method signature for response at onPostExecute
	private static final int get_user_data= 1001;
	private static final int send_user_data= 1002;
	private static final int change_user_data= 1003;
	private static final int auth_user= 1004;
	private static final int send_run_data= 1005;
	private static final int get_run_ids= 1006;
	private static final int get_run_info= 1007;
	private static final int get_all_run_info= 1008;

	/* Client for accessing Google APIs */
	private GoogleApiClient mGoogleApiClient;


	/* View to display current status (signed-in, signed-out, disconnected, etc) */
	private TextView mStatus;
	private TextView mUsername;

	private Switch mMetricSystem;

	private TextView mCalories;
	private TextView mDistance;
	private TextView mTotalRuns;

	private TextView mRestingHeartRate;
	private TextView mRecoveryHeartRate;
	private TextView mHeartRateReserve;

	private TextView mVO2max;
	private TextView mBodyMassIndex;
	private TextView mBodyAdiposityIndex;

	private TextView mUserInfo;
	private TextView mUserMaxHR;

	private TextView mUserCurrentWeight;
	private TextView mUserTargetWeight;
	private TextView mUserCurrentFat;
	private TextView mUserTargetFat;

	private Button mMeasureRHR;
	private Button mSignOutButton;
	private SignInButton mGooglePlusSignIn;

	/* Is there is a ConnectionResult resolution in progress? */
	private boolean mIsResolving = false;
	/* Is google plus user successfully signed in */
	private boolean mIsSignedIn = false;
	private boolean mIsEmailSignedIn = false;
	private boolean mIsAuthenticated= false;

	/* Should we automatically resolve ConnectionResults when possible? */
	private boolean mShouldResolve = false;
	private boolean isBluetoothLeRegistered;

	private boolean isUpdated;
	private Uri mAppUri;
	private Uri mWebUrl;

	public MainActivity() {
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (DEVELOPER_MODE) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectDiskReads()
				.detectDiskWrites()
				.detectNetwork()   // or .detectAll() for all detectable problems
				.penaltyLog()
				.build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
				.detectLeakedSqlLiteObjects()
				.detectLeakedClosableObjects()
				.penaltyLog()
				.penaltyDeath()
				.build());
		}
		super.onCreate(savedInstanceState);

		new SimpleEula(this).show();

		try {
			available.acquire();
			dbExchange= DataBaseExchange.createDataBaseExchange();
			dbExchange.clear();
			available.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		user_bio = new UserData();
		user_bio.getValues();
		user_bio.status = "0";
		user_bio.bMetricSystem = false;

		isUpdated = false;

		activityListMap = new HashMap<Long, Long>();
		activityListMap.clear();

		activityInfoMap = new HashMap<Long, RunData>();
		activityInfoMap.clear();
		local_registerReceiver();

		// [START create_google_api_client]
		// Build GoogleApiClient with access to basic profile
		// ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		mGoogleApiClient = new Builder(this)
			.addConnectionCallbacks(this)
			.addOnConnectionFailedListener(this)
			.addApi(Plus.API)
			.addScope(new Scope(Scopes.PROFILE))
			.addScope(new Scope(Scopes.PLUS_LOGIN))
			.addScope(new Scope(Scopes.PLUS_ME))
			.addScope(new Scope(Scopes.PLUS_MOMENTS))
			.addApi(AppIndex.APP_INDEX_API).build();
		// [END create_google_api_client]

		this.setContentView(R.layout.activity_main);
		this.setupGui();
		this.readFile();
		this.updateUI();

		// Restore from saved instance state
		// [START restore_saved_instance_state]
		if (savedInstanceState != null) {
			mIsResolving = savedInstanceState.getBoolean(KEY_IS_RESOLVING);
			mShouldResolve = savedInstanceState.getBoolean(KEY_SHOULD_RESOLVE);
		}
		// [END restore_saved_instance_state]
	}

	protected boolean writeFile() {
		try {
			user_bio.getValues();
			String path = getPackageDirectory();
			userdatafile = new File(path, "userdata.user");
			f_out = new FileOutputStream(userdatafile.getAbsolutePath(), false);
			ObjectOutputStream obj_out = new ObjectOutputStream(f_out);
			obj_out.writeObject(user_bio);
			writeLog(String.format("saved file: %s", userdatafile.getAbsolutePath()));
			f_out.close();

			runmapfile = new File(path, "runmap.data");
			f_out = new FileOutputStream(runmapfile.getAbsolutePath(), false);
			ObjectOutputStream objmap_out = new ObjectOutputStream(f_out);
			objmap_out.writeObject(activityListMap);
			writeLog(String.format("saved file: %s", runmapfile.getAbsolutePath()));
			f_out.close();

			runinfofile = new File(path, "runinfo.data");
			f_out = new FileOutputStream(runinfofile.getAbsolutePath(), false);
			ObjectOutputStream objinfo_out = new ObjectOutputStream(f_out);
			objinfo_out.writeObject(activityInfoMap);
			writeLog(String.format("saved file: %s", runinfofile.getAbsolutePath()));
			f_out.close();

		} catch (IOException | PackageManager.NameNotFoundException e) {
			writeLog(String.format("writeFile(): Exception: %s", e.toString()));
			e.printStackTrace();
		}

		return true;
	}

	protected boolean readFile() {
		boolean userdata_ok= false;
		boolean runmapdata_ok= false;
		boolean runinfodata_ok= false;
		try {
			String path = getPackageDirectory();
			userdatafile = new File(path, "userdata.user");
			if (userdatafile.exists()) {
				// Read from disk using FileInputStream
				FileInputStream f_in = new FileInputStream(userdatafile.getAbsolutePath());
				// Read object using ObjectInputStream
				ObjectInputStream obj_in = new ObjectInputStream(f_in);
				// Read an object
				writeLog("File found, reading data now: ");
				user_bio= (UserData) obj_in.readObject();
				writeLog(String.format("00 tmp.full_name: %s", user_bio.full_name));
				writeLog(String.format("00 tmp.email: %s", user_bio.email));
				writeLog(String.format("00 tmp.bMetricSystem: %b", user_bio.bMetricSystem));
				user_bio.getValues();
				writeLog(String.format("01 tmp.full_name: %s", user_bio.full_name));
				writeLog(String.format("01 tmp.email: %s", user_bio.email));
				writeLog(String.format("01 tmp.bMetricSystem: %b", user_bio.bMetricSystem));
				writeLog(String.format("01 tmp.created: %s", user_bio.created));
				writeLog(String.format("01 tmp.created_at: %s", user_bio.created_at));
				writeLog(String.format("01 tmp.created_v: %s", user_bio.created_v.toString()));
				mMetricSystem.setChecked(user_bio.bMetricSystem);
				userdata_ok= true;
				f_in.close();
			} else {
				writeLog("File not found: ");
			}

			activityListMap.clear();
			activityInfoMap.clear();

			runmapfile = new File(path, "runmap.data");
			if (runmapfile.exists()) {
				writeLog("found runmapfile...");
				FileInputStream f_in = new FileInputStream(runmapfile.getAbsolutePath());
				// Read object using ObjectInputStream
				ObjectInputStream obj_in = new ObjectInputStream(f_in);
				// Read an object
				activityListMap=  (HashMap<Long, Long>)obj_in.readObject();
				runmapdata_ok= true;
				f_in.close();
			} else {
				writeLog("File not found: ");
			}

			runinfofile = new File(path, "runinfo.data");
			if (runinfofile.exists()) {
				writeLog("found runinfofile...");
				FileInputStream f_in = new FileInputStream(runinfofile.getAbsolutePath());
				// Read object using ObjectInputStream
				ObjectInputStream obj_in = new ObjectInputStream(f_in);
				// Read an object
				activityInfoMap=  (HashMap<Long, RunData>)obj_in.readObject();
				runinfodata_ok= true;
				f_in.close();
			} else {
				writeLog("File not found: ");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (userdata_ok && runmapdata_ok && runinfodata_ok) {
			int mapsz=activityListMap.size();
			int infosz=activityInfoMap.size();
			writeLog(String.format("readFile: checking basic file consistency: mapsz: %d, infosz: %d", mapsz, infosz));
			if (mapsz>0 && infosz>0 && mapsz == infosz) {
				isUpdated= true;
			}
		}
		updateUI();
		return true;
	}

	@Override
	protected void onStop() {
		super.onStop();
		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		Action viewAction = Action.newAction(
			Action.TYPE_VIEW, // TODO: choose an action type.
			"Main Page", // TODO: Define a title for the content shown.
			// TODO: If you have web page content that matches this app activity's content,
			// make sure this auto-generated web page URL is correct.
			// Otherwise, set the URL to null.
			Uri.parse("http://host/path"),
			// TODO: Make sure this auto-generated app deep link URI is correct.
			Uri.parse("android-app://com.runtracer/http/host/path")
		);
		AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction);
		writeLog("onStop()");
		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		mGoogleApiClient.disconnect();
	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			changeUserData(user_bio.createJSON());
		} catch (JSONException | ParseException | NoSuchAlgorithmException | IOException | InterruptedException e) {
			e.printStackTrace();
		}
		writeLog("onPause()");
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
	protected void onDestroy() {
		super.onDestroy();
		try {
			changeUserData(user_bio.createJSON());
		} catch (JSONException | ParseException | NoSuchAlgorithmException | IOException | InterruptedException e) {
			e.printStackTrace();
		}
		this.writeFile();
		if (mBluetoothLeService != null && isBluetoothLeRegistered) {
			unregisterReceiver(mGattUpdateReceiver);
		}
		writeLog("onDestroy()");
		updateUI();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
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

	public void newRun() {
		Intent intent = new Intent(this, RunActivity.class);
		intent.putExtra("UserData", user_bio);
		startActivityForResult(intent, RUN_USER_DATA);
	}

	public void loginUser(JSONObject userInfo) {
		Intent intent = new Intent(this, LoginActivity.class);
		intent.putExtra("user_info", userInfo.toString());
		startActivityForResult(intent, LOGIN_USER_DATA);
	}

	public void newUser(JSONObject userInfo) throws JSONException {
		if (userInfo.isNull("metric")) {
			userInfo.accumulate("metric", user_bio.bMetricSystem?1:0);
		}
		writeLog(String.format("userInfo: %s", userInfo.toString()));
		Intent intent = new Intent(this, NewUserActivity.class);
		intent.putExtra("user_info", userInfo.toString());
		startActivityForResult(intent, NEW_USER_DATA);
	}

	public void userProfile(JSONObject userInfo) {
		try {
			userInfo.accumulate("is_signed_in", mIsSignedIn);
			userInfo.accumulate("full_name", user_bio.full_name);
			userInfo.accumulate("email", user_bio.email);
			userInfo.accumulate("gender", user_bio.gender);
			userInfo.accumulate("birthday", user_bio.birthday);
			userInfo.accumulate("height", user_bio.height_v);
			userInfo.accumulate("hip_circumference", user_bio.hip_circumference_v);
			userInfo.accumulate("weight", user_bio.current_weight_v);
			userInfo.accumulate("target_weight", user_bio.target_weight_v);
			userInfo.accumulate("target_fat", user_bio.target_fat_v);
			userInfo.accumulate("fat_percentage", user_bio.current_fat_v);
			userInfo.accumulate("metric", user_bio.bMetricSystem?1:0);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Intent intent = new Intent(this, ProfileActivity.class);
		intent.putExtra("user_info", userInfo.toString());
		startActivityForResult(intent, USER_PROFILE);
	}

	public void userActivity() {
		Intent intent = new Intent(this, ActivitiesActivity.class);
		intent.putExtra("RunInfo", activityInfoMap);
		intent.putExtra("UserData", user_bio);
		startActivityForResult(intent, ACTIVITIES_DATA);
	}

	public void searchHRM(View view) throws ActivityNotFoundException {
		Intent scan_intent = new Intent(this, DeviceScanActivity.class);
		startActivityForResult(scan_intent, BLUETOOTH_LE);
	}

	private void setupGui() {

		mGooglePlusSignIn=(SignInButton) findViewById(R.id.sign_in_button);
		mGooglePlusSignIn.setOnClickListener(this);
		mGooglePlusSignIn.setSize(SignInButton.SIZE_WIDE);
		mGooglePlusSignIn.setEnabled(false);
		mGooglePlusSignIn.setVisibility(Button.VISIBLE);

		mSignOutButton=(Button) findViewById(R.id.sign_out_button);
		mSignOutButton.setOnClickListener(this);
		mSignOutButton.setEnabled(false);
		mSignOutButton.setVisibility(Button.INVISIBLE);

		findViewById(R.id.new_user_button).setOnClickListener(this);
		findViewById(R.id.new_user_button).setEnabled(true);
		findViewById(R.id.new_user_button).setVisibility(Button.VISIBLE);

		findViewById(R.id.email_login).setOnClickListener(this);
		findViewById(R.id.email_login).setEnabled(true);
		findViewById(R.id.email_login).setVisibility(Button.VISIBLE);

		findViewById(R.id.new_run).setOnClickListener(this);
		findViewById(R.id.new_run).setEnabled(true);
		findViewById(R.id.new_run).setVisibility(Button.VISIBLE);

		findViewById(R.id.btn_scan).setOnClickListener(this);
		findViewById(R.id.btn_scan).setEnabled(true);
		findViewById(R.id.btn_scan).setVisibility(Button.VISIBLE);

		findViewById(R.id.user_activity_button).setOnClickListener(this);
		findViewById(R.id.user_activity_button).setEnabled(true);
		findViewById(R.id.user_activity_button).setVisibility(Button.VISIBLE);

		findViewById(R.id.user_profile_button).setOnClickListener(this);
		findViewById(R.id.user_profile_button).setEnabled(true);
		findViewById(R.id.user_profile_button).setVisibility(Button.VISIBLE);

		mMeasureRHR=(Button)findViewById(R.id.user_resting_hr_button);
		mMeasureRHR.setOnClickListener(this);

		mMeasureRHR.setEnabled(false);
		mMeasureRHR.setVisibility(Button.VISIBLE);

		Button mAboutYou = (Button) findViewById(R.id.user_about_button);
		mAboutYou.setOnClickListener(this);

		mAboutYou.setEnabled(true);
		mAboutYou.setVisibility(Button.VISIBLE);

		// Set up view instances
		mMetricSystem = (Switch) findViewById(R.id.user_unit_system);

		mMetricSystem.setOnClickListener(this);
		mMetricSystem.setEnabled(true);
		mMetricSystem.setVisibility(Switch.VISIBLE);

		mMetricSystem.setText(R.string.user_unit_system_metric);

		mStatus = (TextView) findViewById(R.id.status);
		mCalories = (TextView) findViewById(R.id.total_calories_value);
		mDistance = (TextView) findViewById(R.id.total_distance_value);
		mTotalRuns = (TextView) findViewById(R.id.total_runs_value);

		mUsername = (TextView) findViewById(R.id.user_name);
		mUserInfo = (TextView) findViewById(R.id.user_info);
		mUserMaxHR = (TextView) findViewById(R.id.max_heart_rate_value);

		mRestingHeartRate = (TextView) findViewById(R.id.resting_hear_rate_value);
		mRecoveryHeartRate= (TextView) findViewById(R.id.recovery_heart_rate_value);
		mHeartRateReserve = (TextView) findViewById(R.id.reserve_hear_rate_value);
		mVO2max = (TextView) findViewById(R.id.vo2max_value);
		mBodyMassIndex = (TextView) findViewById(R.id.body_mass_index_value);
		mBodyAdiposityIndex= (TextView) findViewById(R.id.body_adiposity_index_value);
		mUserCurrentWeight = (TextView) findViewById(R.id.user_weight_value);
		mUserTargetWeight = (TextView) findViewById(R.id.goal_weight_value);
		mUserCurrentFat = (TextView) findViewById(R.id.current_fat_value);
		mUserTargetFat = (TextView) findViewById(R.id.goal_fat_value);

		TextView mLink = (TextView) findViewById(R.id.runtracer_web_page);
		String linkText = "Visit the <a href='https://runtracer.com'>RunTracer</a> web page.";
		mLink.setText(Html.fromHtml(linkText));
		mLink.setMovementMethod(LinkMovementMethod.getInstance());
	}

	private String printValue (Date value) {
		String stringtoprint="";
		//SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		SimpleDateFormat date_format = new SimpleDateFormat("MMMM/yyyy", Locale.getDefault());
		if (value.after(new Date(1000000))) {
			stringtoprint= date_format.format(value);
		}
		return stringtoprint;
	}

	private String printValue (double value) {
		NumberFormat nf= NumberFormat.getInstance(Locale.getDefault());
		String	stringtoprint= "--";
		if (value > 0) {
			stringtoprint= nf.format(value);
		}
		return stringtoprint;
	}

	private String printValue(String value) {
		String	stringtoprint= "";
		if (!(value.contains("empty")) && !value.isEmpty()) {
			stringtoprint= value;
		}
		return stringtoprint;
	}

	public boolean isEmailValid(String email)
	{
		String regExpn = "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
			+"((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
			+"[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
			+"([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
			+ "[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
			+"([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$";

		Pattern pattern = Pattern.compile(regExpn, Pattern.CASE_INSENSITIVE);
		Matcher matcher;
		matcher = pattern.matcher(email);

		return matcher.matches();
	}

	private void updateUI() {
		user_bio.getValues();
		user_bio.bMetricSystem= mMetricSystem.isChecked();
		if (user_bio.bMetricSystem) {
			mMetricSystem.setText(R.string.user_unit_system_metric);
		} else {
			mMetricSystem.setText(R.string.user_unit_system_imperial);
		}
		if ( user_bio.bMetricSystem ) {
			mUserTargetWeight.setText(printValue(user_bio.target_weight_v));
			mUserCurrentWeight.setText(printValue(user_bio.current_weight_v));
		} else {
			mUserTargetWeight.setText(printValue(user_bio.target_weight_v_imperial));
			mUserCurrentWeight.setText(printValue(user_bio.current_weight_v_imperial));
		}
		if (user_bio.bMetricSystem) {
			mDistance.setText(printValue(user_bio.total_distance_km));
		} else {
			mDistance.setText(printValue(user_bio.total_distance_miles));
		}
		mCalories.setText(printValue(user_bio.total_calories));
		mTotalRuns.setText(printValue(user_bio.total_runs));

		mUsername.setText(printValue(user_bio.full_name));
		mUserCurrentFat.setText(printValue(user_bio.current_fat_v));
		mUserTargetFat.setText(printValue(user_bio.target_fat_v));

		mBodyMassIndex.setText(printValue(user_bio.bmi));
		mBodyAdiposityIndex.setText(printValue(user_bio.bai));
		int minimum_age = 10;
		if (user_bio.age > minimum_age) {
			user_bio.getValues();
			mUserMaxHR.setText(printValue(user_bio.maximum_hr));
			mVO2max.setText(printValue(user_bio.vo2max));
			mHeartRateReserve.setText(printValue(user_bio.hr_reserve));
		}
		mRecoveryHeartRate.setText(printValue(user_bio.recovery_hr));
		mRestingHeartRate.setText(printValue(user_bio.resting_hr));

		mIsSignedIn= mGoogleApiClient.isConnected();
		mIsEmailSignedIn= (!mIsSignedIn && isEmailValid(user_bio.email));

		if (mIsSignedIn || mIsEmailSignedIn) {
			mUserInfo.setText(String.format("Member since %s", printValue(user_bio.created_v)));
			try {
				if (mIsSignedIn) {
					if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
						String google_account_name = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient).getDisplayName();
						mUsername.setText(google_account_name);
						mStatus.setText(getString(R.string.signed_in_fmt, google_account_name));
					} else {
						Snackbar.make(findViewById(android.R.id.content), "Google+ could not connect, aborting.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
					}
				}
				if (mIsEmailSignedIn) {
					mStatus.setText(getString(R.string.signed_in_fmt, user_bio.full_name));
				}
				mGooglePlusSignIn.setEnabled(false);
				mGooglePlusSignIn.setVisibility(View.INVISIBLE);

				ViewGroup layout = (ViewGroup) mGooglePlusSignIn.getParent();
				if(null!=layout) //for safety only  as you are doing onClick
					layout.removeView(mGooglePlusSignIn);

				mSignOutButton.setText(R.string.sign_out);
				mSignOutButton.setBackgroundColor(Color.DKGRAY);
				mSignOutButton.setTextColor(Color.LTGRAY);
				mSignOutButton.setEnabled(true);
				mSignOutButton.setVisibility(View.VISIBLE);

				findViewById(R.id.new_user_button).setEnabled(false);
				findViewById(R.id.new_user_button).setVisibility(View.VISIBLE);
				findViewById(R.id.email_login).setEnabled(false);
				findViewById(R.id.email_login).setVisibility(View.VISIBLE);
				if (mIsAuthenticated) {
					getRunData(!isUpdated);
				}
			} catch (JSONException | MalformedURLException | InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			// Show signed-out message
			mStatus.setText(R.string.signed_out);
			// Set button visibility
			mGooglePlusSignIn.setEnabled(true);
			mGooglePlusSignIn.setVisibility(View.VISIBLE);

			mSignOutButton.setEnabled(false);
			mSignOutButton.setVisibility(View.INVISIBLE);
		}
	}

	private boolean sendServerDataServiceRequest(String hash) throws InterruptedException {
		boolean result= false;
		if (isServerReady()) {
			available.acquire();
			dbExchange.pending = true;
			available.release();
			Intent mServiceIntent = new Intent(this, ServerDataService.class);
			mServiceIntent.setAction(ServerDataService.ACTION_QUERY_SERVER);
			mServiceIntent.putExtra("hash", hash);
			lastHash= hash;
			this.startService(mServiceIntent);
			result= true;
		}
		return result;
	}

	private boolean isServerReady() throws InterruptedException {
		boolean result= false;
		for (int attempts= 0; attempts < MAX_ATTEMPTS && !result; attempts++) {
			available.acquire();
			result= !dbExchange.pending;
			available.release();
		}
		return result;
	}

	private boolean authUser(JSONObject jsonUserData) throws MalformedURLException, InterruptedException, JSONException {
		boolean authok= false;
		if (isServerReady()) {
			available.acquire();
			dbExchange.clear();

			dbExchange.url = new URL("https://www.runtracer.com/select.php");
			dbExchange.command = "auth_user";
			dbExchange.accountEmail = (String) jsonUserData.get("email");
			dbExchange.full_name = (String) jsonUserData.get("full_name");
			if (dbExchange.full_name.compareTo("") == 0) {
				dbExchange.full_name = "empty";
			}
			dbExchange.json_data_in.accumulate("command", dbExchange.command);
			dbExchange.json_data_in.accumulate("email", jsonUserData.get("email"));
			dbExchange.json_data_in.accumulate("passwd", jsonUserData.get("passwd"));
			dbExchange.json_data_in.accumulate("logged", jsonUserData.get("logged"));
			String hash = dbExchange.getHash();
			available.release();
			sendServerDataServiceRequest(hash);
			authok= true;
		}
		return authok;
	}

	private int sendUserData(JSONObject jsonUserData) throws MalformedURLException, InterruptedException, JSONException {
		boolean dataok= false;
		if (isServerReady()) {
			available.acquire();
			dbExchange.clear();
			dbExchange.url = new URL("https://www.runtracer.com/select.php");
			dbExchange.command = "send_user_data";
			dbExchange.json_data_in.accumulate("command", dbExchange.command);
			dbExchange.json_data_in.accumulate("full_name", jsonUserData.get("full_name"));
			dbExchange.json_data_in.accumulate("email", jsonUserData.get("email"));
			dbExchange.json_data_in.accumulate("dob", jsonUserData.get("dob"));
			dbExchange.json_data_in.accumulate("gender", jsonUserData.get("gender"));
			dbExchange.json_data_in.accumulate("height", jsonUserData.get("height"));
			dbExchange.json_data_in.accumulate("hip_circumference", jsonUserData.get("hip_circumference"));
			dbExchange.json_data_in.accumulate("weight", jsonUserData.get("weight"));
			dbExchange.json_data_in.accumulate("target_weight", jsonUserData.get("target_weight"));
			dbExchange.json_data_in.accumulate("fat", jsonUserData.get("fat"));
			dbExchange.json_data_in.accumulate("target_fat", jsonUserData.get("target_fat"));
			dbExchange.json_data_in.put("metric", user_bio.bMetricSystem ? 1 : 0);
			String hash = dbExchange.getHash();
			available.release();
			for (int attempts = 0; attempts < 10 && !dataok; attempts++) {
				dataok = sendServerDataServiceRequest(hash);
			}
		}
		return 0;
	}

	private int changeUserData(JSONObject jsonUserData) throws MalformedURLException, InterruptedException, JSONException {
		boolean dataok= false;
		int retval=0;
		if (isServerReady() && (mIsSignedIn || mIsEmailSignedIn) ) {
			available.acquire();
			dbExchange.clear();
			dbExchange.url = new URL("https://www.runtracer.com/select.php");
			dbExchange.command = "change_user_data";
			dbExchange.json_data_in.accumulate("command", dbExchange.command);
			dbExchange.json_data_in.accumulate("full_name", jsonUserData.get("full_name"));
			dbExchange.json_data_in.accumulate("email", jsonUserData.get("email"));
			dbExchange.json_data_in.accumulate("logged", "true");
			dbExchange.json_data_in.accumulate("dob", jsonUserData.get("dob"));
			dbExchange.json_data_in.accumulate("gender", jsonUserData.get("gender"));
			dbExchange.json_data_in.accumulate("height", jsonUserData.get("height"));
			dbExchange.json_data_in.accumulate("hip_circumference", jsonUserData.get("hip_circumference"));
			dbExchange.json_data_in.accumulate("weight", jsonUserData.get("weight"));
			dbExchange.json_data_in.accumulate("target_weight", jsonUserData.get("target_weight"));
			dbExchange.json_data_in.accumulate("fat", jsonUserData.get("fat"));
			dbExchange.json_data_in.accumulate("target_fat", jsonUserData.get("target_fat"));
			dbExchange.json_data_in.accumulate("metric", user_bio.bMetricSystem ? 1 : 0);
			dbExchange.json_data_in.accumulate("recovery_heart_rate", user_bio.recovery_hr);
			dbExchange.json_data_in.accumulate("resting_heart_rate", user_bio.resting_hr);
			String hash= dbExchange.getHash();
			available.release();
			for (int attempts=0; attempts < 10 && !dataok; attempts++) {
				dataok= sendServerDataServiceRequest(hash);
			}
		} else {
			retval= -1;
		}
		return retval;
	}

	private int getRunInfo(int run_id) throws MalformedURLException, JSONException, InterruptedException {
		boolean dataok= false;
		if (isServerReady() && (mIsSignedIn || mIsEmailSignedIn) ) {
			available.acquire();
			dbExchange.clear();
			dbExchange.url = new URL("https://www.runtracer.com/select.php");
			dbExchange.command = "get_run_info";
			Date dnow = new Date();
			dbExchange.json_data_in.accumulate("command", dbExchange.command);
			dbExchange.json_data_in.accumulate("uid", user_bio.uid);
			dbExchange.json_data_in.accumulate("session_id", user_bio.session_id);
			dbExchange.json_data_in.accumulate("runid", run_id);
			String hash = dbExchange.getHash();
			available.release();
			if (activityListMap.containsKey(run_id)) {
				Long nowtime = dnow.getTime();
				Long timeatimeout = (Long) activityListMap.get(run_id);
				timeatimeout += NETWORK_TIMEOUT;
				if (nowtime > timeatimeout) {
					activityListMap.put(run_id, dnow.getTime());
					for (int attempts = 0; attempts < 10 && !dataok; attempts++) {
						writeLog(String.format("getRunInfo: attempt: %d", attempts));
						dataok = sendServerDataServiceRequest(hash);
					}
				}
			} else {
				activityListMap.put(run_id, dnow.getTime());
				for (int attempts = 0; attempts < 10 && !dataok; attempts++) {
					dataok = sendServerDataServiceRequest(hash);
				}
			}
		}
		return 0;
	}

	private int getAllRunInfo() throws MalformedURLException, JSONException, InterruptedException {
		if (mIsSignedIn || mIsEmailSignedIn) {
			if (isServerReady() ) {
				available.acquire();
				dbExchange.clear();
				dbExchange.url = new URL("https://www.runtracer.com/select.php");
				dbExchange.command = "get_all_run_info";
				dbExchange.json_data_in.accumulate("command", dbExchange.command);
				dbExchange.json_data_in.accumulate("uid", user_bio.uid);
				dbExchange.json_data_in.accumulate("session_id", user_bio.session_id);
				String hash = dbExchange.getHash();
				available.release();
				sendServerDataServiceRequest(hash);
			}
		}
		return 0;
	}

	@Override
	protected void onStart() {
		super.onStart();
		mGoogleApiClient.connect();
		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		Action viewAction = Action.newAction(
			Action.TYPE_VIEW, // TODO: choose an action type.
			"Main Page", // TODO: Define a title for the content shown.
			// TODO: If you have web page content that matches this app activity's content,
			// make sure this auto-generated web page URL is correct.
			// Otherwise, set the URL to null.
			mWebUrl= Uri.parse("http://www.runtracer.com/"),
			// TODO: Make sure this auto-generated app deep link URI is correct.
			mAppUri= Uri.parse("android-app://com.runtracer/http/host/path")
		);
		AppIndex.AppIndexApi.start(mGoogleApiClient, viewAction);
	}

	//Bluetooth support
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}
	//end of bluetooth support

	// [RESUME on_start_on_stop]
	@Override
	protected void onResume() {
		super.onResume();
		mGoogleApiClient.reconnect();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		isBluetoothLeRegistered= true;
		if (mBluetoothLeService != null) {
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
		}
	}

	// [START on_save_instance_state]
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_IS_RESOLVING, mIsResolving);
		outState.putBoolean(KEY_SHOULD_RESOLVE, mIsResolving);
	}
	// [END on_save_instance_state]

	// [START on_activity_result]
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RC_SIGN_IN) {
			mIsResolving = false;
			if (!mGoogleApiClient.isConnected()) {
				mGoogleApiClient.reconnect();
			}
		}
		if (requestCode == NEW_USER_DATA && resultCode == RESULT_OK) {
			Bundle userData;
			JSONObject jsonUserData;
			try {
				if (data.getExtras().getBundle("data") != null) {
					userData = data.getExtras().getBundle("data");
					assert userData != null;
					if (userData.getString("user_data") != null) {
						jsonUserData = new JSONObject(userData.getString("user_data"));
						sendUserData(jsonUserData);
					}
				}
			} catch (Exception e) {
				writeLog(e.toString());
			}
		}
		if (requestCode == USER_PROFILE && resultCode == RESULT_OK) {
			Bundle userData;
			JSONObject jsonUserData;
			try {
				if (data.getExtras().getBundle("data") != null) {
					userData = data.getExtras().getBundle("data");
					assert userData != null;
					if (userData.getString("user_data") != null) {
						jsonUserData = new JSONObject(userData.getString("user_data"));
						user_bio.writeJSON(jsonUserData);
						//TODO: check this authentication behaviour
						//loginUser(jsonUserData);
						changeUserData(jsonUserData);
					}
				}
			} catch (Exception e) {
				writeLog(e.toString());
			}
		}
		if (requestCode == LOGIN_USER_DATA && resultCode == RESULT_OK) {
			Bundle userData;
			JSONObject jsonUserData;
			try {
				if (data.getExtras().getBundle("data") != null) {
					userData = data.getExtras().getBundle("data");
					assert userData != null;
					if (userData.getString("user_data") != null) {
						jsonUserData = new JSONObject(userData.getString("user_data"));
						jsonUserData.accumulate("logged", "false");
						authUser(jsonUserData);
					}
				}
			} catch (Exception e) {
				writeLog(e.toString());
			}
		}
		if (requestCode == RUN_USER_DATA && resultCode == RESULT_OK && mIsAuthenticated) {
			updateUI();
		}
		if (requestCode == ACTIVITIES_DATA) {
			switch (resultCode) {
				case RESULT_CANCELED:
					break;
				case RESULT_FIRST_USER:
					break;
				case RESULT_OK:
					break;
			}
			updateUI();
		}
		if (requestCode == BLUETOOTH_LE && resultCode == RESULT_OK) {
			String bluetoothDeviceName = "";
			bluetoothDeviceName = data.getStringExtra("BluetoothDeviceName");
			try {
				final BluetoothManager bluetoothManager =
					(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
				BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
				if (mBluetoothAdapter == null) {
					Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
					finish();
					return;
				}
				BluetoothDevice btdevice = mBluetoothAdapter.getRemoteDevice(bluetoothDeviceName);
				mDeviceAddress = btdevice.getAddress();
				Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
				bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
			} catch (Exception e) {
				e.getStackTrace();
			}
		}
	}

	private int getRunData(boolean bNeedsUpdate) throws MalformedURLException, JSONException, InterruptedException {
		boolean updateOk= !bNeedsUpdate;
		int returnvalue= 0;
		if ((mIsEmailSignedIn || mIsSignedIn) && mIsAuthenticated) {
			if (updateOk) {
				long ltime;
				Date cnow= new Date();
				Collection runactivities= activityListMap.values();
				Iterator itv= runactivities.iterator();
				if (itv.hasNext() && activityListMap.size() > 1) {
					updateOk=false;
				}
				for (; itv.hasNext();) {
					ltime= (Long) itv.next();
					updateOk = !(ltime > cnow.getTime());
				}
			}
			if (!updateOk) {
				getAllRunInfo();
			}
		} else {
			returnvalue =-1;
		}
		return 0;
	}

	@Override
	public void onConnected(Bundle bundle) {
		String accountEmail="";
		String name="";
		// onConnected indicates that an account was selected on the device, that the selected
		// account has granted any requested permissions to our app and that we were able to
		// establish a service connection to Google Play services.
		if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
			accountEmail = Plus.AccountApi.getAccountName(mGoogleApiClient);
			name = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient).getDisplayName();
		}
		try {
			JSONObject jsonData = new JSONObject("{\"key\":\"data\"}");
			try {
				jsonData.accumulate("full_name", name);
				jsonData.accumulate("email", accountEmail);
				jsonData.accumulate("passwd", "");
				jsonData.accumulate("logged", "true");
			} catch (JSONException e) {
				writeLog("onConnected:01: " + "JSONException: " + e.toString());
			}
			authUser(jsonData);
			updateUI();
		} catch (Exception e) {
			e.fillInStackTrace();
		}
	}

	@Override
	public void onConnectionSuspended(int i) {
		// The connection to Google Play services was lost. The GoogleApiClient will automatically
		// attempt to re-connect. Any UI elements that depend on connection to Google APIs should
		// be hidden or disabled until onConnected is called again.
		mGoogleApiClient.connect();
	}

	// [START on_connection_failed]
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		// Could not connect to Google Play Services.  The user needs to select an account, grant permissions or resolve an error in order to sign in.
		// Refer to the javadoc for ConnectionResult to see possible error codes.
		if (!mIsResolving && mShouldResolve) {
			if (connectionResult.hasResolution()) {
				try {
					connectionResult.startResolutionForResult(this, RC_SIGN_IN);
					mIsResolving = true;
				} catch (IntentSender.SendIntentException e) {
					mIsResolving = false;
					mGoogleApiClient.connect();
				}
			} else {
				// Could not resolve the connection result, show the user an error dialog.
				showErrorDialog(connectionResult);
			}
		} else {
			// Show the signed-out UI
			updateUI();
		}
	}
	// [END on_connection_failed]

	private void showErrorDialog(ConnectionResult connectionResult) {
		int errorCode = connectionResult.getErrorCode();

		if (GooglePlayServicesUtil.isUserRecoverableError(errorCode)) {
			// Show the default Google Play services error dialog which may still start an intent
			// on our behalf if the user can resolve the issue.
			GooglePlayServicesUtil.getErrorDialog(errorCode, this, RC_SIGN_IN,
				new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						mShouldResolve = false;
						updateUI();
					}
				}).show();
		} else {
			// No default Google Play Services error, display a message to the user.
			String errorString = getString(R.string.play_services_error_fmt, errorCode);
			Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show();

			mShouldResolve = false;
			updateUI();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.sign_in_button:
				// User clicked the sign-in button, so begin the sign-in process and automatically
				// attempt to resolve any errors that occur.
				mStatus.setText(R.string.signing_in);
				// [START sign_in_clicked]
				mShouldResolve = true;
				mGoogleApiClient.connect();
				// [END sign_in_clicked]
				break;

			case R.id.new_user_button:
				JSONObject userinfo = null;
				try {
					userinfo = new JSONObject("{\"key\":\"data\"}");
					this.newUser(userinfo);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;

			case R.id.user_profile_button:
				try {
					userinfo = new JSONObject("{\"key\":\"data\"}");
					this.userProfile(userinfo);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;

			case R.id.email_login:
				try {
					userinfo = new JSONObject("{\"key\":\"data\"}");
					this.loginUser(userinfo);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;

			case R.id.btn_scan:
				try {
					this.searchHRM(v);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;

			case R.id.new_run:
				this.newRun();
				break;

			case R.id.user_activity_button:
				if (isUpdated) {
					this.userActivity();
				} else {
					Snackbar.make(findViewById(android.R.id.content), "Please wait until all activities are loaded from server.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
				}

				break;

			case R.id.user_resting_hr_button:
				writeLog(String.format("clicked button: user_resting_hr_button: user_bio.rhr_state: %d", user_bio.rhr_state ));
				if (user_bio.rhr_state == MEASURING) {
					user_bio.rhr_state= READY;
				} else {
					user_bio.rhr_state = MEASURING;
				}
				break;

			case R.id.user_about_button:
				if (isUpdated) {
					this.aboutYou();
					} else {
					Snackbar.make(findViewById(android.R.id.content), "Profile not ready.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
				}
				break;

			case R.id.user_unit_system:
				user_bio.bMetricSystem= mMetricSystem.isChecked();
				updateUI();
				break;

			case R.id.sign_out_button:
				if (mIsSignedIn && mGoogleApiClient.isConnected()) {
					Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
					mGoogleApiClient.disconnect();
				}
				user_bio.clean();
				activityListMap.clear();
				activityInfoMap.clear();
				updateUI();
				this.finish();
				break;

		}
	}

	public int aboutYou() {
		Intent intent = new Intent(this, About.class);
		intent.putExtra("UserData", user_bio);
		startActivityForResult(intent, ABOUT_YOU);
		return(0);
	}

	@Override
	public void onResult(People.LoadPeopleResult peopleData) {
		if (peopleData.getStatus().getStatusCode() == CommonStatusCodes.SUCCESS) {
			PersonBuffer personBuffer = peopleData.getPersonBuffer();
			try {
				int count = personBuffer.getCount();
				for (int i = 0; i < count; i++) {
					new RetrieveTokenTask().execute(personBuffer.get(i).getId());
				}
			} finally {
				personBuffer.close();
			}
		}
	}

	public void writeLog(String msg) {
		Date cdate;
		String date = (DateFormat.format("dd-MM-yyyy hh:mm:ss a", cdate= new Date()).toString());
		String msg2= String.format("<%d>", cdate.getTime());
		//Log.e(TAG, date + msg2 + ": "  + msg);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				finish();
			}
			// Automatically connects to the device upon successful start-up initialization.
			mBluetoothLeService.connect(mDeviceAddress);
		}
		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
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
		ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
		ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();
			writeLog("new BluetoothGattService: BLE: " + uuid);
			String LIST_NAME = "NAME";
			currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
			String LIST_UUID = "UUID";
			currentServiceData.put(LIST_UUID, uuid);
			gattServiceData.add(currentServiceData);
			writeLog("new gattServiceData: BLE: " + currentServiceData.toString());
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
				writeLog("new item for currentCharaData: BLE " + currentCharaData.toString());
				if (gattCharacteristic.getUuid().toString().matches("00002a37-0000-1000-8000-00805f9b34fb")) {
					mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
					writeLog("BLE found item for gattCharacteristic matching: " + "00002a37-0000-1000-8000-00805f9b34fb: " + gattCharacteristic.getUuid().toString());
				}
			}
			mGattCharacteristics.add(charas);
			gattCharacteristicData.add(gattCharacteristicGroupData);
		}
	}

	void local_registerReceiver() {
		IntentFilter mServerDBfilter = new IntentFilter(ResponseReceiver.ACTION_RESP);
		mServerDBfilter.addCategory(Intent.CATEGORY_DEFAULT);
		ResponseReceiver receiver = new ResponseReceiver();
		registerReceiver(receiver, mServerDBfilter);
	}


	protected class ResponseReceiver extends BroadcastReceiver
	{
		public static final String ACTION_RESP= "com.runtracer.intent.action.MESSAGE_PROCESSED";

		/**
		 * This method is called when the BroadcastReceiver is receiving an Intent broadcast.
		 * During this time you can use the other methods on BroadcastReceiver to view/modify the current result values.
		 * This method is always called within the main thread of its process, unless you explicitly asked for it to be scheduled on a different thread using
		 * {@link Context#registerReceiver(BroadcastReceiver,
		 * IntentFilter, String, Handler)}. When it runs on the main thread you should never perform long-running operations in it (there is a timeout of
		 * 10 seconds that the system allows before considering the receiver to be blocked and a candidate to be killed). You cannot launch a popup dialog
		 * in your implementation of onReceive().
		 * If this BroadcastReceiver was launched through a &lt;receiver&gt; tag, then the object is no longer alive after returning from this function.
		 * This means you should not perform any operations that return a result to you asynchronously -- in particular, for interacting with services, you should use
		 * {@link Context#startService(Intent)} instead of {@link Context#bindService(Intent, ServiceConnection, int)}.
		 * If you wish to interact with a service that is already running, you can use {@link #peekService}.
		 * The Intent filters used in {@link Context#registerReceiver} and in application manifests are not guaranteed to be exclusive. They are hints to the operating system
		 * about how to find suitable recipients. It is possible for senders to force delivery to specific recipients, bypassing filter resolution.
		 * For this reason, {@link #onReceive(Context, Intent) onReceive()} implementations should respond only to known actions, ignoring any unexpected Intents that they may receive.
		 * @param context The Context in which the receiver is running.
		 * @param intent  The Intent being received.
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			String response;
			response= intent.getStringExtra("param_out_msg");
			if (response.compareTo(lastHash) == 0) {
				dbExchange.pending= false;
			}
			try {
				processResponse(dbExchange);
			} catch (InterruptedException | JSONException | IOException | ParseException | NoSuchAlgorithmException | CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}

		private boolean updateRunInfo(int run_id, JSONObject json_run_info) throws IOException, JSONException, ParseException, NoSuchAlgorithmException {
			boolean update_result= false;
			RunData run_info= new RunData();
			Date dnow= new Date();
			if(activityListMap.containsKey(run_id)) {
				if(dnow.getTime()>=(long)activityListMap.get(run_id)) {
					if(!activityInfoMap.containsKey(run_id)) {
						if (!json_run_info.isNull("calories_distance")) {
							run_info.writeJSON(json_run_info);
							activityInfoMap.put(run_id, run_info);
							activityListMap.put(run_id, dnow.getTime() * 2); //unix time now x 2
							user_bio.total_runs= activityInfoMap.size();
							user_bio.getValues();
							update_result= true;
						}
					} else {
						run_info= (RunData) activityInfoMap.get(run_id);
						user_bio.total_runs= activityInfoMap.size();
					}
				}
			}
			user_bio.total_distance_km+= run_info.distance_km_v;
			user_bio.total_calories+= run_info.calories_v_distance;
			return update_result;
		}

		private int updateAllRunInfo(JSONObject json_all_run_info) throws IOException, JSONException, ParseException, NoSuchAlgorithmException {
			int run_id;
			int colidx, rowidx;
			JSONObject json_run_info;
			boolean eof= false;
			RunData crun= new RunData();
			user_bio.total_distance_km= 0;
			user_bio.total_distance_miles=0;
			user_bio.total_calories= 0;
			isUpdated= true;
			for (rowidx=0; !eof ; rowidx++) {
				json_run_info= new JSONObject("{\"key\":\"data\"}");
				for (colidx=0; colidx<(RunData.colsz+1); colidx ++) {
					String key=String.format("(%d:%d)", colidx, rowidx);
					if (!json_all_run_info.isNull(key)) {
						String newkey= crun.getKeyName(colidx);
						json_run_info.accumulate(newkey, json_all_run_info.get(key));
					} else {
						if (colidx==0) {
							eof= true;
						}
					}
				}
				if (!json_run_info.isNull("run_id")) {
					run_id= Integer.parseInt((String) json_run_info.get("run_id"));
					Date dnow= new Date();
					activityListMap.put(run_id, dnow.getTime());
					isUpdated= !updateRunInfo(run_id, json_run_info);
				}
			}
			return 0;
		}

		public void processResponse(DataBaseExchange dbEx) throws InterruptedException, JSONException, IOException, ParseException, NoSuchAlgorithmException, CloneNotSupportedException {
			Boolean bUserAlreadyCreated;
			Boolean bUserStatusReady;
			Boolean bUserValidated = false;
			int sender= 0;
			if ( dbEx.error_no > 0 ) {
				final DataBaseExchange retry= (DataBaseExchange) dbEx.clone();
				Handler handler = new Handler();
				handler.postDelayed(new Runnable(){
					@Override
					public void run(){
						available.release();
						try {
							sendServerDataServiceRequest(retry.hash);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}, 20000);
				Snackbar.make(findViewById(android.R.id.content), "Connectivity problem, could not update the server, retrying in 20 secs.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
			}

			if ( dbEx.json_data_out.isNull("status") || dbEx.json_data_out.isNull("created") || dbEx.json_data_out.isNull("sender")) {
				user_bio.status= "0";
				user_bio.created= "0";
				return;
			} else {
				try {
					if (!dbEx.json_data_out.isNull("status") ) {
						user_bio.status = dbEx.json_data_out.get("status").toString();
					}
					if (!dbEx.json_data_out.isNull("created") ) {
						user_bio.created = dbEx.json_data_out.get("created").toString();
					}
					if (!dbEx.json_data_out.isNull("sender") && dbEx.json_data_out.get("sender") instanceof Integer) {
						sender= dbEx.json_data_out.getInt("sender");
					}
				} catch (JSONException e) {
					e.printStackTrace();
					return;
				}
			}
			try {
				if (!dbEx.json_data_out.isNull("status") && !dbEx.json_data_out.isNull("sender")) {
					user_bio.status= dbEx.json_data_out.get("status").toString();
				}
				if (!dbEx.json_data_out.isNull("created") && !dbEx.json_data_out.isNull("sender")) {
					user_bio.created= dbEx.json_data_out.get("created").toString();
				}
				if (!dbEx.json_data_out.isNull("validated") && !dbEx.json_data_out.isNull("sender")) {
					bUserValidated= 1== dbEx.json_data_out.getInt("validated");
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			user_bio.getValues();
			bUserAlreadyCreated = (user_bio.created_v.after(new Date(0)) || user_bio.created.compareTo("1")==0);
			bUserStatusReady = !(user_bio.status.compareTo("0") == 0);

			mIsEmailSignedIn = bUserAlreadyCreated && bUserStatusReady && !mIsSignedIn;
			if (!bUserAlreadyCreated) {
				Snackbar.make(findViewById(android.R.id.content), "User not created, select New User and create your profile.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
			}
			if (bUserAlreadyCreated && !bUserStatusReady && !bUserValidated) {
				Snackbar.make(findViewById(android.R.id.content), "User email not validated, check your email and create a password.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
			}

			switch (sender) {
				case get_user_data:
					break;

				case send_user_data:
					break;

				case change_user_data:
					break;

				case auth_user:
					if (bUserAlreadyCreated && bUserStatusReady) {
						mIsEmailSignedIn= !mIsSignedIn;
						mIsAuthenticated= true;
						try {
							user_bio.writeJSON(dbEx.json_data_out);
							mMetricSystem.setChecked(user_bio.bMetricSystem);
							getRunData(!isUpdated);
							updateUI();

						} catch (JSONException | NoSuchAlgorithmException | IOException | ParseException | InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						if (!bUserAlreadyCreated) {
							JSONObject jsonData;
							try {
								jsonData = new JSONObject("{\"key\":\"data\"}");
								jsonData.accumulate("full_name", dbEx.full_name);
								jsonData.accumulate("email", dbEx.accountEmail);
								jsonData.accumulate("passwd", "");
								jsonData.accumulate("logged", "true");
								jsonData.accumulate("is_signed_in", mIsSignedIn);
								jsonData.accumulate("gender", user_bio.gender);
								jsonData.accumulate("birthday", user_bio.birthday);
								jsonData.accumulate("height", user_bio.height);
								jsonData.accumulate("hip_circumference", user_bio.hip_circumference);
								jsonData.accumulate("weight", user_bio.current_weight);
								jsonData.accumulate("target_weight", user_bio.target_weight);
								jsonData.accumulate("target_fat", user_bio.target_fat);
								jsonData.accumulate("fat_percentage", user_bio.current_fat);
								jsonData.accumulate("metric", user_bio.bMetricSystem?1:0);
								writeLog(String.format("jsonData: %s", jsonData.toString()));
								newUser(jsonData);
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
					break;

				case send_run_data:
					getRunData(true);
					changeUserData(user_bio.createJSON());
					break;

				case get_run_ids:
					int max_run_history_sz = 2000;
					for (int i=0; i< max_run_history_sz; i++) {
						String key = (String.format("value_%d", i));
						if (!dbEx.json_data_out.isNull(key)) {
							try {
								String value = (String) dbEx.json_data_out.get(String.format("value_%d", i));
								getRunInfo(Integer.parseInt(value));
							} catch (JSONException | MalformedURLException | InterruptedException e) {
								e.printStackTrace();
							}
						} else {
							i= max_run_history_sz;
						}
					}
					break;

				case get_run_info:
					try {
						int runid_v;
						String runid= (String) dbEx.json_data_out.get("runid");
						runid_v= Integer.parseInt(runid);
						updateRunInfo(runid_v, dbEx.json_data_out);
					} catch (JSONException | ParseException | IOException | NoSuchAlgorithmException e) {
						e.printStackTrace();
					}
					break;

				case get_all_run_info:
					try {
						updateAllRunInfo(dbEx.json_data_out);
						updateUI();
					} catch (JSONException | ParseException | IOException | NoSuchAlgorithmException e) {
						e.printStackTrace();
					}
					break;

				default:
			}
		}
	}

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
				mMeasureRHR.setText(getString(R.string.user_resting_hr_button));
				mMeasureRHR.setEnabled(false);
				writeLog(String.format("hr data... \nuser_bio.hr_reading: %d \nuser_bio.current_hr: %f \nuser_bio.last_hr: %f", user_bio.hr_reading, user_bio.current_hr, user_bio.last_hr));
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				// Show all the supported services and characteristics on the user interface.
				displayGattServices(mBluetoothLeService.getSupportedGattServices());
				writeLog("BLE" + (mBluetoothLeService.getSupportedGattServices()).toString());
				user_bio.hr_reading=0;
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				String data = (intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
				TextView t = (TextView) findViewById(R.id.heart_rate_value);
				t.setText(data);
				user_bio.current_hr= Double.parseDouble(data);
				user_bio.hr_reading++;
				if (user_bio.hr_reading > user_bio.RESTING_NO_READINGS && user_bio.current_hr < user_bio.RESTING_HR_MAX && user_bio.current_hr > user_bio.RESTING_HR_MIN ) {
					mMeasureRHR.setEnabled(true);
					mMeasureRHR.setVisibility(Button.VISIBLE);
				} else {
					mMeasureRHR.setText(getString(R.string.user_resting_hr_button));
				}
				if ( (user_bio.last_hr < (user_bio.current_hr - user_bio.RESTING_HR_MARGIN))  || (user_bio.last_hr > ( user_bio.current_hr + user_bio.RESTING_HR_MARGIN)) ) {
					user_bio.last_hr= -1;
					user_bio.hr_reading= 0;
				} else {
					if (user_bio.rhr_state == READY) {
						user_bio.resting_hr= user_bio.current_hr;
						user_bio.rhr_state = ACQUIRED;
						user_bio.getValues();
						updateUI();
					}
				}
				if (user_bio.rhr_state== MEASURING) {
					mMeasureRHR.setText(String.format("OK? (%.0f)", user_bio.current_hr));
				} else {
					mMeasureRHR.setText(getString(R.string.user_resting_hr_button));
				}
				user_bio.last_hr= user_bio.current_hr;
			}
		}
	};

	private class RetrieveTokenTask extends AsyncTask<String, Void, String> {
		private static final int REQ_SIGN_IN_REQUIRED = 11910;
		@Override
		protected String doInBackground(String... params) {
	          /*
            String accountName = params[0];
            String scopes = "oauth2:profile email";
            //String scopes = "oauth2:email " + Scopes.PLUS_LOGIN;
            String token = null;
            writeLog("doInBackground running...");
            try {
                writeLog("doInBackground getToken( ..., " + accountName + " ,..);" );
                token = GoogleAuthUtil.getToken(getApplicationContext(), accountName, scopes);
            } catch (IOException e) {
                writeLog(e.toString());
            } catch (UserRecoverableAuthException e) {
                startActivityForResult(e.getIntent(), REQ_SIGN_IN_REQUIRED);
            } catch (GoogleAuthException e) {
                writeLog(e.toString());
            }
            return token;
            */
			return null;
		}
	}
}