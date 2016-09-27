package com.runtracer;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class UserData implements Serializable {
	private static final long serialVersionUID = 100L;

	private static final SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA);
	private static final SimpleDateFormat local_format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());

	private static final String TAG = "user_data";

	public final int RESTING_NO_READINGS= 20;
	public final int RESTING_HR_MARGIN= 5;
	public final int RESTING_HR_MIN= 40;
	public final int RESTING_HR_MAX= 100;

	public String full_name;
	public String birthday;
	public String gender;
	public String height;
	public String hip_circumference;
	public String current_weight;
	public String current_fat;
	public String target_weight;
	public String target_fat;
	public String email;
	public String status;
	public String created;
	public String created_at;

	public boolean gender_v;

	public double height_v=0;
	public double hip_circumference_v=0;
	public double current_weight_v=0;
	public double current_fat_v=0;
	public double target_weight_v=0;
	public double target_fat_v=0;

	public double height_v_imperial= -1;
	public double hip_circumference_v_imperial= -1;
	public double current_weight_v_imperial= -1;
	public double target_weight_v_imperial= -1;

	private final double conv_km_miles= 0.621371192237;
	private final double conv_kg_lbs= 2.204622618;
	private final double conv_cm_inches = 0.393700787402;


	public Date birthday_date;
	public int age=0;

	public double vo2max=0;
	public double cff=0;
	public double bmr=0;
	public double rmr=0;
	public double bmi=0;
	public double bai=0;

	//heart rate data
	public int rhr_state=0;
	public int hr_reading=0;
	public double current_hr=0;        // current reading of heart rate
	public double last_hr=0;           // last reading of heart rate
	public double resting_hr=0;        // resting heart rate
	public double hr_reserve=0;        // heart rate reserve
	public double maximum_hr=0;        // maximum heart rate.
	public double recovery_hr=0;       // recovery heart rate.

	public double target_hr_light=0;
	public double target_hr_moderate=0;
	public double target_hr_heavy=0;
	public double target_hr_very_heavy=0;

	public double total_distance_km=0;
	public double total_distance_miles=0;
	public double total_calories=0;
	public int no_runs=0;

	public boolean bMetricSystem= false;

	public String uid;
	public int uid_v=0;

	public String session_id;
	public int total_runs=0;
	private final int minimum_age=18;
	//private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
	public Date created_v;

	public UserData() {
		full_name= "empty";
		birthday= "empty";
		gender= "empty";
		height= "empty";
		hip_circumference= "empty";
		current_weight= "empty";
		current_fat= "empty";
		target_weight= "empty";
		target_fat= "empty";
		email= "empty";
		status= "empty";
		created= "empty";
		created_at= "empty";

		gender_v= false;

		height_v= -1;
		hip_circumference_v= -1;
		current_weight_v= -1;
		current_fat_v= -1;
		target_weight_v= -1;
		target_fat_v= -1;

		height_v_imperial= -1;
		hip_circumference_v_imperial= -1;
		current_weight_v_imperial= -1;
		target_weight_v_imperial= -1;

		birthday_date= new Date();
		age= -1;

		vo2max= -1;
		cff= -1;
		bmr= -1;
		rmr= -1;
		bmi= -1;
		bai= -1;

		rhr_state=-1;
		hr_reading=-1;
		current_hr= -1;        // current reading of heart rate
		last_hr= -1;           // last reading of heart rate
		resting_hr= -1;        // resting heart rate
		hr_reserve= -1;        // heart rate reserve
		maximum_hr= -1;        // maximum heart rate.
		recovery_hr= -1;       // recovery heart rate.

		total_distance_km=0;
		total_distance_miles=0;
		total_calories=0;
		no_runs=0;

		target_hr_light= -1;
		target_hr_moderate= -1;
		target_hr_heavy= -1;
		target_hr_very_heavy= -1;
		bMetricSystem= false;
		uid= "empty";
		uid_v= -1;

		session_id= "empty";
		total_runs= 0;
		created_v= new Date(0);
	}

	int getValues() {
		int result= 0;
		this.total_distance_miles= this.total_distance_km * this.conv_km_miles;
		if( this.current_weight.compareTo("empty") == 0 ||
			this.height.compareTo("empty") == 0 ||
			this.hip_circumference.compareTo("empty") == 0 ||
			this.current_fat.compareTo("empty") == 0 ||
			this.target_weight.compareTo("empty") == 0 ||
			this.target_fat.compareTo("empty") == 0 ||
			this.gender.compareTo("empty") == 0 ||
			this.full_name.compareTo("empty") == 0 ||
			this.email.compareTo("empty") == 0 ) {
		} {
			result= -1;
		}
		writeLog(String.format("created: %s, created_at: %s, created_v: %s", created, created_at, created_v));
		if( !(this.created.compareTo("0") == 0) && !(this.created.compareTo("1") == 0) && !(this.created.compareTo("empty") == 0)) {
			try {
				writeLog(String.format("inside try: created: %s, created_at: %s, created_v: %s", created, created_at, created_v));
				this.created_v= new Date();
				writeLog(String.format("inside if 03: created: %s, created_at: %s, created_v: %s", created, created_at, created_v ));
				this.created_v= date_format.parse(this.created);
				writeLog(String.format("inside if 04: created: %s, created_at: %s, created_v: %s", created, created_at, created_v));
				writeLog(String.format("UserData: created_v: %s", this.created_v.toString()));
			} catch (ParseException e) {
				writeLog(String.format("UserData: parseException: %s", e.toString()));
			}
		}
		writeLog(String.format("created: %s, created_at: %s, created_v: %s", created, created_at, created_v));

		if( !(this.current_weight.compareTo("empty") == 0) ) {
			this.current_weight_v= Double.parseDouble(this.current_weight);
		}
		if( !(this.height.compareTo("empty") == 0) ) {
			this.height_v = Double.parseDouble(this.height);
		}
		if( !(this.hip_circumference.compareTo("empty") == 0) ) {
			this.hip_circumference_v = Double.parseDouble(this.hip_circumference);
		}
		if( !(this.current_fat.compareTo("empty") == 0) ) {
			this.current_fat_v = Double.parseDouble(this.current_fat);
		}
		if( !(this.target_weight.compareTo("empty") == 0) ) {
			this.target_weight_v = Double.parseDouble(this.target_weight);
		}
		if( !(this.target_fat.compareTo("empty") == 0) ) {
			this.target_fat_v = Double.parseDouble(this.target_fat);
		}
		if( !(this.uid.compareTo("empty") == 0) ) {
			this.uid_v = Integer.parseInt(this.uid);
		}
		if( !(this.gender.compareTo("empty") == 0) ) {
			this.gender_v= (gender.compareTo("Male")==0);
		}
		this.current_weight_v_imperial= current_weight_v * conv_kg_lbs;
		this.height_v_imperial= this.height_v * conv_cm_inches;
		this.hip_circumference_v_imperial= this.hip_circumference_v * conv_cm_inches;
		this.target_weight_v_imperial= this.target_weight_v * conv_kg_lbs;

		/*For males: BMR = (13.75 x WKG) + (5 x HC) - (6.76 x age) + 66
			For females: BMR = (9.56 x WKG) + (1.85 x HC) - (4.68 x age) + 655 */
		if ( this.gender_v ) {
			this.bmr= 13.75 * this.current_weight_v + 5 * this.height_v - (6.76 * this.age) + 66 ;
		} else {
			this.bmr= 9.56 * this.current_weight_v + 1.85 * this.height_v - (4.68 * this.age) + 655 ;
		}
		this.rmr= this.bmr * 1.1;
		// BAI = Body Adiposity Index
		// Metric: (HC / (HM)1.5) - 18
		this.bai= this.hip_circumference_v / Math.pow(this.height_v / 100, 1.5) - 18;
		// BMI = Body Mass Index
		// Metric: BMI = WKG / (HM x HM)
		this.bmi= this.current_weight_v /  Math.pow( this.height_v / 100, 2) ;

		if (this.resting_hr > 0 && this.maximum_hr > this.resting_hr) {
			this.hr_reserve= this.maximum_hr - this.resting_hr;
		}
		//VO2max = 15.3 x (MHR/RHR)
		if (this.resting_hr > 0) {
			this.vo2max= 15.3 * (this.maximum_hr / this.resting_hr);
		}

		/*
		For VO2max ≥ 56 mL•kg-1•min-1:
		CFF = 1.00
		For 56 mL•kg-1•min-1 > VO2max ≥ 54 mL•kg-1•min-1:
		CFF = 1.01
		For 54 mL•kg-1•min-1 > VO2max ≥ 52 mL•kg-1•min-1:
		CFF = 1.02
		For 52 mL•kg-1•min-1 > VO2max ≥ 50 mL•kg-1•min-1:
		CFF = 1.03
		For 50 mL•kg-1•min-1 > VO2max ≥ 48 mL•kg-1•min-1:
		CFF = 1.04
		For 48 mL•kg-1•min-1 > VO2max ≥ 46 mL•kg-1•min-1:
		CFF = 1.05
		For 46 mL•kg-1•min-1 > VO2max ≥ 44 mL•kg-1•min-1:
		CFF = 1.06
		For VO2max < 44 mL•kg-1•min-1:
		CFF = 1.07
		 */
		if (this.vo2max >= 56.00) {
			this.cff= 1.00;
		}
		if (this.vo2max >= 54.00 && this.vo2max < 56.00) {
			this.cff= 1.01;
		}
		if (this.vo2max >= 52.00 && this.vo2max < 54.00) {
			this.cff= 1.02;
		}
		if (this.vo2max >= 50.00 && this.vo2max < 52.00) {
			this.cff= 1.03;
		}
		if (this.vo2max >= 48.00 && this.vo2max < 50.00) {
			this.cff= 1.04;
		}
		if (this.vo2max >= 46.00 && this.vo2max < 48.00) {
			this.cff= 1.05;
		}
		if (this.vo2max >= 44.00 && this.vo2max < 46.00) {
			this.cff= 1.06;
		}
		if (this.vo2max < 44.00) {
			this.cff= 1.07;
		}
		if (this.age > this.minimum_age) {
			this.maximum_hr= (int) (208 - (0.7 * this.age));
			this.target_hr_light= 0.35 * this.maximum_hr;
			this.target_hr_moderate= 0.55 * this.maximum_hr;
			this.target_hr_heavy= 0.7 * this.maximum_hr;
			this.target_hr_very_heavy= 0.9 * this.maximum_hr;
		}
		return (result);
	}

	JSONObject createJSON () throws JSONException, IOException, NoSuchAlgorithmException, ParseException {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		DecimalFormat df = (DecimalFormat)nf;
		JSONObject jsonuserdata= new JSONObject("{\"key\":\"data\"}");
		jsonuserdata.accumulate("uid_v", this.uid_v);
		jsonuserdata.accumulate("uid", this.uid);
		jsonuserdata.accumulate("full_name", this.full_name);
		jsonuserdata.accumulate("email", this.email);
		jsonuserdata.accumulate("gender", this.gender);
		jsonuserdata.accumulate("birthday", this.birthday);
		jsonuserdata.accumulate("dob", this.birthday);
		jsonuserdata.accumulate("height", this.height);
		jsonuserdata.accumulate("hip_circumference", this.hip_circumference);
		jsonuserdata.accumulate("weight", this.current_weight);
		jsonuserdata.accumulate("fat", this.current_fat);
		jsonuserdata.accumulate("target_weight", this.target_weight);
		jsonuserdata.accumulate("target_fat", this.target_fat);
		jsonuserdata.accumulate("resting_heart_rate", this.resting_hr);
		jsonuserdata.accumulate("recovery_heart_rate", this.recovery_hr);
		jsonuserdata.accumulate("metric", this.bMetricSystem ? 1 : 0);
		writeLog(String.format("UserData: createJSON last line returning: %s", jsonuserdata.toString()));
		return (jsonuserdata);
	}

	int writeJSON (JSONObject jsonuserdata) throws JSONException, ParseException, IOException, NoSuchAlgorithmException {
		int returnval=0;
		if (! jsonuserdata.isNull("uid_v")) {
			this.uid_v= jsonuserdata.getInt("uid_v");
			writeLog(String.format("UserData: writeJSON: received: this.uid_v: %d", this.uid_v));
		} else {
			returnval= -1;
		}
		if (!jsonuserdata.isNull("uid")) {
			this.uid= jsonuserdata.get("uid").toString();
			writeLog(String.format("UserData: writeJSON: %s", this.uid));
		} else {
			returnval= -2;
		}
		if (! jsonuserdata.isNull("full_name")) {
			this.full_name= jsonuserdata.getString("full_name");
			writeLog(String.format("UserData: writeJSON: received: this.full_name: %s", this.full_name ));
		} else {
			returnval= -3;
		}
		if (! jsonuserdata.isNull("mail")) {
			this.email= jsonuserdata.getString("mail");
			writeLog(String.format("UserData: writeJSON: received: this.email: %s", this.email));
		} else {
			returnval= -4;
		}
		if (!jsonuserdata.isNull("session_id")) {
			this.session_id= jsonuserdata.get("session_id").toString();
			writeLog(String.format("UserData: writeJSON: %s", this.session_id));
		} else {
			returnval= -5;
		}
		if (!jsonuserdata.isNull("birthday")) {
			this.birthday = jsonuserdata.get("birthday").toString();
			writeLog(String.format("UserData: writeJSON: %s", this.birthday));
			this.birthday_date = local_format.parse(this.birthday);
		} else {
			returnval= -8;
		}
		if (!jsonuserdata.isNull("gender")) {
			this.gender = jsonuserdata.get("gender").toString();
			writeLog(String.format("UserData: writeJSON: %s", this.gender));
		} else {
			returnval= -9;
		}
		if (!jsonuserdata.isNull("height")) {
			this.height = jsonuserdata.get("height").toString();
			writeLog(String.format("UserData: writeJSON: %s", this.height));
		} else {
			returnval= -10;
		}
		if (!jsonuserdata.isNull("hip_circumference")) {
			this.hip_circumference= jsonuserdata.get("hip_circumference").toString();
			writeLog(String.format("UserData: writeJSON: %s", this.hip_circumference));
		} else {
			returnval= -11;
		}
		if (!jsonuserdata.isNull("weight_current")) {
			this.current_weight = jsonuserdata.get("weight_current").toString();
			writeLog(String.format("UserData: writeJSON: %s", this.current_weight));
		} else {
			returnval= -12;
		}
		if (!jsonuserdata.isNull("fat_percentage")) {
			this.current_fat = jsonuserdata.get("fat_percentage").toString();
			writeLog(String.format("UserData: writeJSON: %s", this.current_fat));
		} else {
			returnval= -13;
		}
		if (!jsonuserdata.isNull("weight_target")) {
			this.target_weight = jsonuserdata.get("weight_target").toString();
			writeLog(String.format("UserData: writeJSON: %s", this.target_weight));
		} else {
			returnval= -14;
		}
		if (!jsonuserdata.isNull("fat_percentage_target")) {
			this.target_fat = jsonuserdata.get("fat_percentage_target").toString();
			writeLog(String.format("UserData: writeJSON: %s", this.target_fat));
		} else {
			returnval= -15;
		}
		if (!jsonuserdata.isNull("unit_system")) {
			this.bMetricSystem=(jsonuserdata.getInt("unit_system")==1);
			writeLog(String.format("UserData: writeJSON: mMetricSystem: %b", this.bMetricSystem));
		} else {
			returnval= -16;
		}
		if (!jsonuserdata.isNull("resting_heart_rate")) {
			this.resting_hr= Double.parseDouble((String) jsonuserdata.get("resting_heart_rate"));
			writeLog(String.format("UserData: writeJSON: resting_heart_rate: %.2f", this.resting_hr));
		} else {
			returnval= -17;
		}
		if (!jsonuserdata.isNull("recovery_heart_rate")) {
			this.recovery_hr = Double.parseDouble((String) jsonuserdata.get("recovery_heart_rate"));
			writeLog(String.format("UserData: writeJSON: recovery_hr: %.2f", this.recovery_hr));
		} else {
			returnval= -18;
		}
		if (!jsonuserdata.isNull("created")) {
			this.created_at= jsonuserdata.get("created").toString();
			writeLog(String.format("UserData: writeJSON: created_at: %s", this.created_at));
		}

		Calendar calendar_birth = Calendar.getInstance();
		calendar_birth.setTime(this.birthday_date);
		Calendar calendar_now = Calendar.getInstance();
		this.age = calendar_now.get(Calendar.YEAR) - calendar_birth.get(Calendar.YEAR);
		this.getValues();

		writeLog(String.format("UserData: writeJSON last line returning: %d for json: %s", returnval, jsonuserdata.toString()));
		return(returnval);
	}

	public void writeLog(String msg) {
		Date datenow= new Date();
		String date= date_format.format(datenow);
		//Log.e(TAG, date + ": " + msg);
	}

	private void writeObject(java.io.ObjectOutputStream out)
		throws IOException {
		out.writeObject(this.full_name);
		out.writeObject(this.birthday);
		out.writeObject(this.gender);
		out.writeObject(this.height);
		out.writeObject(this.hip_circumference);
		out.writeObject(this.current_weight);
		out.writeObject(this.current_fat);
		out.writeObject(this.target_weight);
		out.writeObject(this.target_fat);
		out.writeObject(this.email);
		out.writeObject(this.status);
		out.writeObject(this.created);
		out.writeObject(this.created_at);
		out.writeObject(this.gender_v);

		out.writeObject(this.height_v);
		out.writeObject(this.hip_circumference_v);
		out.writeObject(this.current_weight_v);
		out.writeObject(this.current_fat_v);
		out.writeObject(this.target_weight_v);
		out.writeObject(this.target_fat_v);

		out.writeObject(height_v_imperial);
		out.writeObject(hip_circumference_v_imperial);
		out.writeObject(current_weight_v_imperial);
		out.writeObject(target_weight_v_imperial);

		out.writeObject(this.birthday_date);
		out.writeObject(this.age);
		out.writeObject(this.vo2max);
		out.writeObject(this.cff);
		out.writeObject(this.bmr);
		out.writeObject(this.rmr);
		out.writeObject(this.bmi);
		out.writeObject(this.bai);
		out.writeObject(this.rhr_state);
		out.writeObject(this.hr_reading);
		out.writeObject(this.current_hr);
		out.writeObject(this.last_hr);
		out.writeObject(this.resting_hr);
		out.writeObject(this.hr_reserve);
		out.writeObject(this.maximum_hr);
		out.writeObject(this.recovery_hr);
		out.writeObject(this.target_hr_light);
		out.writeObject(this.target_hr_moderate);
		out.writeObject(this.target_hr_heavy);
		out.writeObject(this.target_hr_very_heavy);

		out.writeObject(this.total_distance_km);
		out.writeObject(this.total_distance_miles);
		out.writeObject(this.total_calories);
		out.writeObject(this.no_runs);

		out.writeObject(this.bMetricSystem);
		out.writeObject(this.uid);
		out.writeObject(this.uid_v);
		out.writeObject(this.session_id);
		out.writeObject(this.total_runs);
		out.writeObject(this.created_v);
	}

	private void readObject(java.io.ObjectInputStream in)
		throws IOException, ClassNotFoundException {
		// populate the fields of 'this' from the data in 'in'...
		this.full_name= (String) in.readObject();
		this.birthday= (String) in.readObject();
		this.gender= (String) in.readObject();
		this.height= (String) in.readObject();
		this.hip_circumference= (String) in.readObject();
		this.current_weight= (String) in.readObject();
		this.current_fat= (String) in.readObject();
		this.target_weight= (String) in.readObject();
		this.target_fat= (String) in.readObject();
		this.email= (String) in.readObject();
		this.status= (String) in.readObject();
		this.created= (String) in.readObject();
		this.created_at= (String) in.readObject();
		this.gender_v= (boolean) in.readObject();

		this.height_v= (double) in.readObject();
		this.hip_circumference_v= (double) in.readObject();
		this.current_weight_v= (double) in.readObject();
		this.current_fat_v= (double) in.readObject();
		this.target_weight_v= (double) in.readObject();
		this.target_fat_v= (double) in.readObject();

		this.height_v_imperial= (double) in.readObject();
		this.hip_circumference_v_imperial= (double) in.readObject();
		this.current_weight_v_imperial= (double) in.readObject();
		this.target_weight_v_imperial= (double) in.readObject();

		this.birthday_date= (Date) in.readObject();
		this.age= (int) in.readObject();
		this.vo2max= (double) in.readObject();
		this.cff= (double) in.readObject();
		this.bmr= (double) in.readObject();
		this.rmr= (double) in.readObject();
		this.bmi= (double) in.readObject();
		this.bai= (double) in.readObject();
		this.rhr_state= (int) in.readObject();
		this.hr_reading= (int) in.readObject();
		this.current_hr= (double) in.readObject();
		this.last_hr= (double) in.readObject();
		this.resting_hr= (double) in.readObject();
		this.hr_reserve= (double) in.readObject();
		this.maximum_hr= (double) in.readObject();
		this.recovery_hr= (double) in.readObject();
		this.target_hr_light= (double) in.readObject();
		this.target_hr_moderate= (double) in.readObject();
		this.target_hr_heavy= (double) in.readObject();
		this.target_hr_very_heavy= (double) in.readObject();

		this.total_distance_km= (double) in.readObject();
		this.total_distance_miles= (double) in.readObject();
		this.total_calories= (double) in.readObject();
		this.no_runs= (int) in.readObject();

		this.bMetricSystem= (boolean) in.readObject();
		this.uid= (String) in.readObject();
		this.uid_v= (int) in.readObject();
		this.session_id= (String) in.readObject();
		this.total_runs= (int) in.readObject();
		this.created_v= (Date) in.readObject();
	}

	public boolean clean() {
		full_name= "empty";
		birthday= "empty";
		gender= "empty";
		height= "empty";
		hip_circumference= "empty";
		current_weight= "empty";
		current_fat= "empty";
		target_weight= "empty";
		target_fat= "empty";
		email= "empty";
		status= "empty";
		created= "empty";
		created_at= "empty";

		gender_v= false;

		height_v= -1;
		hip_circumference_v= -1;
		current_weight_v= -1;
		current_fat_v= -1;
		target_weight_v= -1;
		target_fat_v= -1;

		height_v_imperial= -1;
		hip_circumference_v_imperial= -1;
		current_weight_v_imperial= -1;
		target_weight_v_imperial= -1;

		birthday_date= new Date();
		age= -1;

		vo2max= -1;
		cff= -1;
		bmr= -1;
		rmr= -1;
		bmi= -1;
		bai= -1;

		rhr_state=-1;
		hr_reading=-1;
		current_hr= -1;        // current reading of heart rate
		last_hr= -1;           // last reading of heart rate
		resting_hr= -1;        // resting heart rate
		hr_reserve= -1;        // heart rate reserve
		maximum_hr= -1;        // maximum heart rate.
		recovery_hr= -1;       // recovery heart rate.

		total_distance_km=0;
		total_distance_miles=0;
		total_calories=0;
		no_runs=0;

		target_hr_light= -1;
		target_hr_moderate= -1;
		target_hr_heavy= -1;
		target_hr_very_heavy= -1;
		bMetricSystem= false;
		uid= "empty";
		uid_v= -1;

		session_id= "empty";
		total_runs= 0;
		created_v= new Date(0);
		return true;
	}
}

