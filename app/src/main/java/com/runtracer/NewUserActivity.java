package com.runtracer;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.view.View.*;

public class NewUserActivity extends AppCompatActivity implements OnClickListener, OnTouchListener, TextView.OnEditorActionListener {

	private final NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
	public static String datePicked="";
	private static final String TAG = "runtracer";
	protected static JSONObject userData;

	// GUI elements
	Button mNewUser;
	Button mUserDateofBirth;

	Switch mUserGender;
	EditText mFullName;
	EditText mUserEmail;

	EditText mUserHeight;
	EditText mUserHipCircumference;
	EditText mUserWeight;
	EditText mUserTargetWeight;
	EditText mUserBodyMassIndex;
	EditText mUserBodyAdiposityIndex;
	EditText mUserFat;
	EditText mUserTargetFat;

	TextView mUserHeightUnits;
	TextView mUserHipCircumferenceUnits;
	TextView mUserWeightUnits;
	TextView mUserWeightTargetUnits;

	String retrievedFullName;
	String retrievedEmail;
	String retrievedGender;
	String retrievedDOB;
	String retrievedHeight;
	String retrievedHipCircumference;
	String retrievedWeight;
	String retrievedTargetWeight;
	String retrievedFat;
	String retrievedTargetFat;

	private final double conv_in_cm= 2.54;
	private final double conv_ft_cm= 30.48;
	private final double conv_lb_kg= 0.45359237;

	double retrievedHeight_v;
	double retrievedHipCircumference_v;
	double retrievedWeight_v;
	double retrievedTargetWeight_v;

	private boolean retrievedMetricSystem;

	private double bmi;
	private double bai;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);

		retrievedFullName= "";
		retrievedEmail= "";
		retrievedGender= "";
		retrievedDOB= "";
		retrievedHeight= "0";
		retrievedHipCircumference= "0";
		retrievedWeight= "0";
		retrievedTargetWeight= "0";
		retrievedFat= "0";
		retrievedTargetFat= "0";

		retrievedHeight_v= -1;
		retrievedHipCircumference_v= -1;
		retrievedWeight_v= -1;
		retrievedTargetWeight_v= -1;

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String value = extras.getString("user_info");
			try {
				userData= new JSONObject(value);
				writeLog(String.format("json received: %s", userData));
				if (!userData.isNull("full_name")) {
					retrievedFullName=userData.getString("full_name");
				} else {
					retrievedFullName="";
				}
				if (!userData.isNull("email")) {
					retrievedEmail=userData.getString("email");
				} else {
					retrievedEmail="";
				}
				if (!userData.isNull("metric")) {
					retrievedMetricSystem= (userData.getInt("metric")==1);
				}
				if ( retrievedFullName.compareTo("empty")  == 0 ) {
					retrievedFullName="";
				}
				if ( retrievedEmail.compareTo("empty")  == 0 ) {
					retrievedEmail="";
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		setContentView(R.layout.activity_new_user);
		this.setupGui();
	}

	private void setupGui() {
		Context context = this.getApplicationContext();

		// Set up button click listeners
		mNewUser= (Button) findViewById(R.id.sign_in_button);
		findViewById(R.id.sign_up_button).setOnClickListener(this);
		findViewById(R.id.sign_up_button).setEnabled(true);
		findViewById(R.id.sign_up_button).setVisibility(Button.VISIBLE);

		// Set up view instances
		mFullName= (EditText) findViewById(R.id.full_name);
		mUserEmail = (EditText) findViewById(R.id.email);
		mUserDateofBirth= (Button) findViewById(R.id.date_picker_button);
		mUserGender= (Switch) findViewById(R.id.user_gender_value);
		mUserGender.setOnClickListener(this);
		mUserHeight= (EditText) findViewById(R.id.user_height);

		mUserHeightUnits= (TextView) findViewById(R.id.height_units);
		mUserWeightUnits= (TextView) findViewById(R.id.weight_units);
		mUserWeightTargetUnits= (TextView) findViewById(R.id.weight_unit_target);
		mUserHipCircumferenceUnits= (TextView) findViewById(R.id.length_units);

		mUserHipCircumference= (EditText) findViewById(R.id.hip_circunference_value);
		mUserWeight= (EditText) findViewById(R.id.user_weight);
		mUserTargetWeight= (EditText) findViewById(R.id.user_weight_target);

		mUserBodyMassIndex= (EditText) findViewById(R.id.user_bmi_value);
		mUserBodyAdiposityIndex= (EditText) findViewById(R.id.user_bai_value);

		mUserBodyMassIndex.setEnabled(false);
		mUserBodyAdiposityIndex.setEnabled(false);

		mUserFat= (EditText) findViewById(R.id.user_fat_percentage);
		mUserTargetFat= (EditText) findViewById(R.id.user_fat_percentage_target);
		mUserDateofBirth.setOnClickListener(this);
		mUserDateofBirth.setOnTouchListener(this);
		mUserDateofBirth.setOnEditorActionListener(this);
		mUserDateofBirth.setText(datePicked);
		mUserHeight.setOnClickListener(this);
		mUserHipCircumference.setOnClickListener(this);
		mUserHipCircumference.setOnTouchListener(this);
		mUserHeight.setOnTouchListener(this);
		mUserWeight.setOnClickListener(this);
		mUserWeight.setOnTouchListener(this);
		mUserTargetWeight.setOnClickListener(this);
		mUserTargetWeight.setOnTouchListener(this);
		mUserFat.setOnClickListener(this);
		mUserFat.setOnTouchListener(this);
		mUserTargetFat.setOnClickListener(this);
		mUserTargetFat.setOnTouchListener(this);
		mFullName.setOnClickListener(this);
		mFullName.setOnTouchListener(this);
		mFullName.setText(retrievedFullName);
		mUserEmail.setOnClickListener(this);
		mUserEmail.setOnTouchListener(this);
		mUserEmail.setText(retrievedEmail);
		mUserGender.setText(retrievedGender);

		if (retrievedMetricSystem) {
			mUserHeightUnits.setText(R.string.unit_cm);
			mUserWeightUnits.setText(R.string.unit_kg);
			mUserWeightTargetUnits.setText(R.string.unit_kg);
			mUserHipCircumferenceUnits.setText(R.string.unit_cm);
		} else {
			mUserHeightUnits.setText(R.string.unit_ft);
			mUserWeightUnits.setText(R.string.unit_lb);
			mUserWeightTargetUnits.setText(R.string.unit_lb);
			mUserHipCircumferenceUnits.setText(R.string.unit_inches);
		}
	}

	public boolean isNumber(String str) {
		int size = str.length();
		for (int i = 0; i < size; i++) {
			Character cchar= new Character(str.charAt(i));
			if (!Character.isDigit(cchar) && (cchar != '.') && (cchar != ',')) {
				return false;
			}
		}
		return size > 0;
	}

	boolean calculateBMI() throws ParseException {
		boolean result;
		getValues(false);
		if (retrievedHeight_v > 0 && retrievedHipCircumference_v > 0 && retrievedWeight_v >0) {
			// Metric: (HC / (HM)1.5) - 18
			// BAI = Body Adiposity Index
			this.bai = retrievedHipCircumference_v / Math.pow(retrievedHeight_v / 100, 1.5) - 18;
			// BMI = Body Mass Index
			// Metric: BMI = WKG / (HM x HM)
			this.bmi = retrievedWeight_v / Math.pow(retrievedHeight_v / 100, 2);
			result = true;
		} else {
			result=false;
		}
		return result;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		mUserDateofBirth.setText(datePicked);
		try {
			if ( calculateBMI() ) {
				mUserBodyMassIndex.setText(String.format("%.2f", this.bmi));
				mUserBodyAdiposityIndex.setText(String.format("%.2f", this.bai));
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		mUserDateofBirth.setText(datePicked);
		return false;
	}

	public static class DatePickerFragment extends DialogFragment
		implements DatePickerDialog.OnDateSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current date as the default date in the picker
			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);

			// Create a new instance of DatePickerDialog and return it
			return new DatePickerDialog(getActivity(), this, year - 20, month, day);
		}

		public void onDateSet(DatePicker view, int year, int month, int day) {
			datePicked= String.format("%d-%d-%d ", year, month + 1, day );
		}

		public void writeLog(String msg) {
			String date = (DateFormat.format("dd-MM-yyyy hh:mm:ss", new java.util.Date()).toString());
			//Log.e(TAG, date + ": " + msg);
		}
	}

	public void showDatePickerDialog(View v) {
		//DialogFragment newFragment = new DatePickerFragment();
		DatePickerFragment newFragment = new DatePickerFragment();
		newFragment.show(getSupportFragmentManager(), "datePicker");
		retrievedDOB= datePicked;
		mUserDateofBirth.setText(datePicked);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_new_user, menu);
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

	public boolean isEmailValid(String email)
	{
		String regExpn = "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
			+"((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
			+"[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
			+"([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
			+ "[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
			+"([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$";

		Pattern pattern = Pattern.compile(regExpn, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(email);

		return matcher.matches();
	}

	private boolean isNameValid(String name) {
		return name.length() > 4;
	}

	private boolean isWeightValid(String weight) throws ParseException {
		double weight_v= 0.0;
		if (isNumber(weight)) {
			weight_v = nf.parse(weight).doubleValue();
		}
		if (retrievedMetricSystem) {
			return weight_v > 20;
		} else {
			return weight_v > 40;
		}
	}

	private boolean isFatValid(String fat) throws ParseException {
		double fat_v= 0.0;
		if (isNumber(fat)) {
			fat_v = nf.parse(fat).doubleValue();
		}
		return (fat_v > 2 && fat_v < 90);
	}

	private boolean isHeightValid(String height) throws ParseException {
		double height_v= 0.0;
		if (isNumber(height)) {
			height_v = nf.parse(height).doubleValue();
		}
		if (retrievedMetricSystem) {
			return (height_v > 40 && height_v < 240);
		} else {
			return (height_v > 3 && height_v < 8);
		}
	}

	private boolean isHipCircumferenceValid(String hip_circumference) throws ParseException {
		double hip_circumference_v= 0.0;
		if (isNumber(hip_circumference)) {
			hip_circumference_v = nf.parse(hip_circumference).doubleValue();
		}
		if (retrievedMetricSystem) {
			return (hip_circumference_v > 20 && hip_circumference_v < 240);
		} else {
			return (hip_circumference_v > 7 && hip_circumference_v < 80);
		}
	}

	public boolean getValues(boolean bFillJSON) throws ParseException {

		double lretrievedHeight_v =-1;
		double lretrievedHipCircumference_v =-1;
		double lretrievedWeight_v =-1;
		double lretrievedTargetWeight_v =-1;
		double lretrievedFat_v=-1;
		double lretrievedTargetFat_v=-1;

		retrievedFullName= String.valueOf(mFullName.getText());
		retrievedEmail= String.valueOf(mUserEmail.getText());
		retrievedGender= String.valueOf(mUserGender.getText());
		retrievedDOB= datePicked;
		retrievedHeight=String.valueOf(mUserHeight.getText());
		retrievedHipCircumference=String.valueOf(mUserHipCircumference.getText());
		retrievedWeight=String.valueOf(mUserWeight.getText());
		retrievedTargetWeight=String.valueOf(mUserTargetWeight.getText());
		retrievedFat=String.valueOf(mUserFat.getText());
		retrievedTargetFat=String.valueOf(mUserTargetFat.getText());

		lretrievedFat_v= nf.parse(retrievedFat).doubleValue();
		lretrievedTargetFat_v= nf.parse(retrievedTargetFat).doubleValue();

		if( isWeightValid(retrievedWeight)  && isHeightValid(retrievedHeight) && isHipCircumferenceValid(retrievedHipCircumference) && isWeightValid(retrievedTargetWeight)) {
			lretrievedHeight_v = nf.parse(retrievedHeight).doubleValue();
			lretrievedHipCircumference_v = nf.parse(retrievedHipCircumference).doubleValue();
			lretrievedWeight_v = nf.parse(retrievedWeight).doubleValue();
			lretrievedTargetWeight_v = nf.parse(retrievedTargetWeight).doubleValue();
		} else {
			return (false);
		}

		if (retrievedMetricSystem) {
			retrievedHeight_v = lretrievedHeight_v;
			retrievedHipCircumference_v = lretrievedHipCircumference_v;
			retrievedWeight_v = lretrievedWeight_v;
			retrievedTargetWeight_v = lretrievedTargetWeight_v;
		} else {
			retrievedHeight_v = lretrievedHeight_v * conv_ft_cm;
			retrievedHipCircumference_v = lretrievedHipCircumference_v * conv_in_cm;
			retrievedWeight_v = lretrievedWeight_v * conv_lb_kg;
			retrievedTargetWeight_v = lretrievedTargetWeight_v * conv_lb_kg;
		}

		if (bFillJSON) {
			if( !isWeightValid(retrievedWeight) ) {
				String errorString= "Weight is invalid: " + retrievedWeight;
				Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show();
				return false;
			}
			if( !isHeightValid(retrievedHeight) ) {
				String errorString= "Height is invalid: " + retrievedHeight;
				Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show();
				return false;
			}
			if( !isHipCircumferenceValid(retrievedHipCircumference) ) {
				String errorString= "Hip Circumference is invalid: " + retrievedHipCircumference;
				Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show();
				return false;
			}
			if( !isEmailValid(retrievedEmail) ) {
				String errorString= "email is invalid: " + retrievedEmail;
				Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show();
				return false;
			}
			if( !isNameValid(retrievedFullName) ) {
				String errorString= "Name is invalid: " + retrievedFullName;
				Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show();
				return false;
			}
			if( !isWeightValid(retrievedTargetWeight) ) {
				String errorString= "Target Weight is invalid: " + retrievedTargetWeight;
				Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show();
				return false;
			}
			if( !isFatValid(retrievedFat) ) {
				String errorString= "Body Fat % is invalid: " + retrievedFat;
				Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show();
				return false;
			}
			if( !isFatValid(retrievedTargetFat) ) {
				String errorString= "Target Body Fat % is invalid: " + retrievedTargetFat;
				Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show();
				return false;
			}
			try {
				userData.put("full_name", retrievedFullName);
				userData.put("email", retrievedEmail);
				userData.put("gender", retrievedGender);
				userData.put("dob", retrievedDOB);
				userData.put("fat", lretrievedFat_v);
				userData.put("target_fat", lretrievedTargetFat_v);
				userData.put("height", retrievedHeight_v);
				userData.put("hip_circumference", retrievedHipCircumference_v);
				userData.put("weight", retrievedWeight_v);
				userData.put("target_weight", retrievedTargetWeight_v);
				writeLog(String.format("getValues: userData: %s", userData.toString()));
			} catch (JSONException e) {
				writeLog(String.format("getValues: JSONException: %s", e.toString()));
				e.printStackTrace();
				return (false);
			}
		} else {
			writeLog(String.format("getValues: false"));
		}
		return(true);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.sign_up_button:
				Snackbar.make(v, "Please check your email and setup your password from the link provided.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
				try {
					if (getValues(true)) {
						Intent data = new Intent();
						Bundle returnValue= new Bundle();
						returnValue.putString("user_data", userData.toString());
						data.putExtra("data", returnValue);
						setResult(RESULT_OK, data);
						this.finish();
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
				break;

			case R.id.user_gender_value:
				if (mUserGender.isChecked()) {
					mUserGender.setText("Female");
					retrievedGender="female";
				} else {
					mUserGender.setText("Male");
					retrievedGender="male";
				}
				break;

			case R.id.date_picker_button:
				showDatePickerDialog(v);
				break;

			case R.id.full_name:
				break;

			case R.id.email:
				break;

			default:
				break;
		}
	}

	public void writeLog(String msg) {
		String date = (DateFormat.format("dd-MM-yyyy hh:mm:ss", new java.util.Date()).toString());
		//Log.e(TAG, date + ": " + msg);
	}
}
