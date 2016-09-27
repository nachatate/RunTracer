package com.runtracer;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RunData implements Serializable {
	private static final long serialVersionUID = 100L;
	private static final String TAG = "run_data";
	private static final SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA);
	public String run_date_start;
	public String run_date_end;
	public int run_id_v;
	public final double conv_m_s_km_h= 3.6;
	public final double conv_km_miles= 0.621371192237;
	private static final int ERROR= -1001;
	public static final int colsz= 12;
	public Date run_date_start_v;
	public Date run_date_end_v;

	public double average_speed_km_h_v;
	public double average_speed_miles_h_v;
	public double current_speed_m_s_v;
	public double current_speed_km_h_v;
	public double current_speed_miles_h_v;
	public double calories_v_distance;
	public double calories_v_heart_beat;
	public double current_weight_v;
	public double current_fat_v;
	public int inclination;
	public double threadmill_factor;
	public int current_heart_rate;
	public int recovery_hr;
	public int resting_hr;
	public long granularity_time= 2000;
	public double distance_m_v= 0.0;
	public double distance_km_v= 0.0;
	public double distance_miles_v= 0.0;
	public double gps_distance_km= 0.0;
	public double gps_distance_miles= 0.0;
	public long current_time= 0;
	public HashMap<Long, RunInstant> runtrace;
	public String runtrace_md5sum;

	public int getNoPoints() {
		int nopoints=0;
		if (this.runtrace != null) {
			nopoints= this.runtrace.size();
		} else {
			writeLog("getNoPoints returning 0");
		}
		return nopoints;
	}

	public boolean pushInstant(double mspeed, double mdistance, double gpsspeed, double gpsdistance, double caloriesdistance, double calorieshr, int heartrate, double longitude, double latitude, double altitude)
	{
		this.current_time= new Date().getTime();
		RunInstant tmp= new RunInstant();
		tmp.current_motion_speed_km_h_v= mspeed;
		tmp.current_motion_distance_km_v= mdistance;
		tmp.current_gps_speed_km_h= gpsspeed;
		tmp.current_gps_distance_km= gpsdistance;
		tmp.calories_v_distance= caloriesdistance;
		tmp.calories_v_heart_beat= calorieshr;
		tmp.current_heart_rate= heartrate;
		tmp.longitude= longitude;
		tmp.latitude= latitude;
		tmp.altitude= altitude;
		runtrace.put(this.current_time, tmp);
		return true;
	}

	String md5sum (byte[] object_bytes) throws NoSuchAlgorithmException, IOException {
		MessageDigest messageDigest= MessageDigest.getInstance("MD5");
		int byteCount;
		byteCount=object_bytes.length;
		messageDigest.update(object_bytes, 0, byteCount);
		writeLog(String.format("md5sum byteCount: %d", byteCount));

		byte[] digest = messageDigest.digest();
		String digeststring = Arrays.toString(digest);
		writeLog(String.format("md5sum: %s", digeststring));

		return (digeststring);
	}

	byte[] writeObject (Object object) {
		byte[] object_bytes = new byte[0];
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(object);
			object_bytes= bos.toByteArray();
			writeLog(String.format("writeObject: %d", bos.size()));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException ex) {
				// ignore close exception
			}
			try {
				bos.close();
			} catch (IOException ex) {
				// ignore close exception
			}
		}
		return object_bytes;
	}

	Object readObject (byte[] object_bytes) {
		Object object = null;
		ByteArrayInputStream bis = new ByteArrayInputStream(object_bytes);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			object = in.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bis.close();
			} catch (IOException ex) {
				// ignore close exception
			}
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				// ignore close exception
			}
		}
		return object;
	}

	public void writeLog(String msg) {
		SimpleDateFormat datef1= new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.CANADA);
		Date datenow= new Date();
		String date= datef1.format(datenow);
		//Log.e(TAG, date + ": " + msg);
	}

	public RunData() throws JSONException {
		run_date_start= "";
		run_date_end= "";
		run_date_start_v= new Date(0);
		run_date_end_v= new Date(0);
		runtrace= new HashMap <Long, RunInstant>();
		distance_km_v= -1;
		distance_miles_v=-1;
		current_speed_km_h_v= -1;
		current_speed_miles_h_v= -1;
		average_speed_km_h_v= -1;
		average_speed_miles_h_v= -1;
		calories_v_distance= -1;
		calories_v_heart_beat= -1;
		current_heart_rate= -1;
		current_weight_v= -1;
		current_fat_v= -1;
		inclination= -1;
		threadmill_factor= -1;
		gps_distance_km= 0.0;
		gps_distance_miles= 0.0;
		runtrace_md5sum=null;
	}

	public String getKeyName(int colidx) {
		// this is the order in which data is received from the database.
		String keyname="";
		if (colidx <= colsz || colidx > 0) {
			switch (colidx) {
				case 0:
					keyname= "uid";
					break;
				case 1:
					keyname= "run_id";
					break;
				case 2:
					keyname= "distance";
					break;
				case 3:
					keyname= "distance_gps";
					break;
				case 4:
					keyname= "average_speed";
					break;
				case 5:
					keyname= "calories_distance";
					break;
				case 6:
					keyname= "calories_heart_beat";
					break;
				case 7:
					keyname= "current_weight";
					break;
				case 8:
					keyname= "current_fat";
					break;
				case 9:
					keyname= "runtrace";
					break;
				case 10:
					keyname= "runtrace_md5sum";
					break;
				case 11:
					keyname= "date_start";
					break;
				case 12:
					keyname= "date_end";
					break;

				default:
			}
		}
		return keyname;
	}

	boolean checkRunData() throws ParseException, NoSuchAlgorithmException, JSONException, IOException {
		boolean check= false;
		String md5sum_calculated= this.runtrace_md5sum;
		String md5sum_received;
		JSONObject jsonRunData= this.createJSON();
		if(!jsonRunData.isNull("runtrace") ) {
			if (!jsonRunData.isNull("runtrace_md5sum")) {
				md5sum_received = (String) jsonRunData.get("runtrace_md5sum");
				check= (md5sum_calculated==null) || (md5sum_received.compareTo(md5sum_calculated) == 0);
			}
		}
		return check;
	}

	JSONObject createJSON () throws JSONException, IOException, NoSuchAlgorithmException, ParseException {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		DecimalFormat df = (DecimalFormat)nf;
		JSONObject jsonRunData= new JSONObject("{\"key\":\"data\"}");
		jsonRunData.accumulate("distance", df.format(this.distance_km_v));
		jsonRunData.accumulate("distance_gps", df.format(this.gps_distance_km));
		jsonRunData.accumulate("run_id",  df.format(this.run_id_v));
		jsonRunData.accumulate("average_speed",  df.format(this.average_speed_km_h_v));
		jsonRunData.accumulate("calories_distance", df.format(this.calories_v_distance));
		jsonRunData.accumulate("calories_heart_beat", df.format(this.calories_v_heart_beat));
		jsonRunData.accumulate("current_weight",  df.format(this.current_weight_v));
		jsonRunData.accumulate("current_fat", df.format(this.current_fat_v));
		jsonRunData.accumulate("date_start",  date_format.format(this.run_date_start_v));
		jsonRunData.accumulate("date_end", date_format.format(this.run_date_end_v));
		String encoded_runtrace_string;
		byte[] objectstream_wr= writeObject(this.runtrace);
		encoded_runtrace_string= Base64.encodeToString(objectstream_wr, Base64.DEFAULT);
		jsonRunData.accumulate("runtrace", encoded_runtrace_string);
		if (!jsonRunData.isNull("runtrace")) {
			byte[] objectstream = new byte[0];
			encoded_runtrace_string= (String) jsonRunData.get("runtrace");
			String pattern= "(^([A-Za-z0-9+\\/\\\\]{4})*([A-Za-z0-9+\\/\\\\]{4}|[A-Za-z0-9+\\/\\\\]{3}=|[A-Za-z0-9+\\/\\\\]{2}==).*$)";
			Pattern p = Pattern.compile(pattern, Pattern.DOTALL);
			Matcher m = p.matcher(encoded_runtrace_string);
			if (m.matches()) {
				String encoded_runtrace_cleaned = m.group(0);
				encoded_runtrace_cleaned.replace("\n", "").replace("\r", "");
				objectstream= Base64.decode(encoded_runtrace_cleaned, Base64.DEFAULT);
				this.runtrace_md5sum= md5sum(objectstream);
				writeLog(String.format("createJSON: run_id: %d runtrace_md5sum: %s encoded_sz: %d start_date: %s", this.run_id_v, this.runtrace_md5sum, encoded_runtrace_string.length(), this.run_date_start));
				jsonRunData.accumulate("runtrace_md5sum", this.runtrace_md5sum);
			}
		}
		// public static final int colsz= 10;  << check this when modifying.
		return jsonRunData;
	}

	@SuppressWarnings("unchecked")
	int writeJSON (JSONObject json_run_data) throws JSONException, ParseException, IOException, NoSuchAlgorithmException {
		if (! json_run_data.isNull("run_id")) {
			this.run_id_v= json_run_data.getInt("run_id");
		} else {
			this.run_id_v= ERROR;
		}
		if (! json_run_data.isNull("distance")) {
			this.distance_km_v= json_run_data.getDouble("distance");
		}
		if(!json_run_data.isNull("distance_gps")) {
			this.gps_distance_km= json_run_data.getDouble("distance_gps");
		}
		if(!json_run_data.isNull("run_id")) {
			this.run_id_v= Integer.parseInt((String) json_run_data.get("run_id"));
		}
		if(!	json_run_data.isNull("average_speed")) {
			this.average_speed_km_h_v= json_run_data.getDouble("average_speed");
		}
		if(!json_run_data.isNull("calories_distance")) {
			this.calories_v_distance= json_run_data.getDouble("calories_distance");
		}
		if(!json_run_data.isNull("calories_heart_beat")) {
			this.calories_v_heart_beat= json_run_data.getDouble("calories_heart_beat");
		}
		if(!json_run_data.isNull("current_weight")) {
			this.current_weight_v= json_run_data.getDouble("current_weight");
		}
		if(!json_run_data.isNull("current_fat")) {
			this.current_fat_v= json_run_data.getDouble("current_fat");
		}
		if(!json_run_data.isNull("date_start")) {
			this.run_date_start= (String) json_run_data.get("date_start");
		}
		if(!json_run_data.isNull("date_end") ) {
			this.run_date_end= (String) json_run_data.get("date_end");
		}
		this.getValues();
		this.run_date_start_v= new Date();
		this.run_date_start_v= date_format.parse(this.run_date_start);
		this.run_date_end_v= new Date();
		this.run_date_end_v= date_format.parse(this.run_date_end);

		if(!json_run_data.isNull("runtrace") ) {
			byte[] objectstream = new byte[0];
			String encoded_runtrace= (String) json_run_data.get("runtrace");

			//writeLog(String.format("writeJSON: received: encoded_runtrace: %s", encoded_runtrace));
			String pattern= "(^([A-Za-z0-9+\\/\\\\]{4})*([A-Za-z0-9+\\/\\\\]{4}|[A-Za-z0-9+\\/\\\\]{3}=|[A-Za-z0-9+\\/\\\\]{2}==).*$)";
			Pattern p = Pattern.compile(pattern, Pattern.DOTALL);
			Matcher m = p.matcher(encoded_runtrace);
			if (m.matches()) {
				String encoded_runtrace_cleaned = m.group(0);
				//writeLog(String.format("writeJSON: received: encoded_runtrace_cleaned: %s and matched against the base64 pattern", encoded_runtrace_cleaned));
				encoded_runtrace_cleaned.replace("\n", "").replace("\r", "");
				try {
					objectstream = Base64.decode(encoded_runtrace_cleaned, Base64.DEFAULT);
				} catch (Exception e) {
					writeLog(String.format("RunData: Exception: %s", e.toString()));
					//writeLog(String.format("RunData: encoded_runtrace_cleaned: %s\nm: %s\nencoded_runtrace: %s \tsize: %d", encoded_runtrace_cleaned, m.group(0), encoded_runtrace, encoded_runtrace.length()));
					e.printStackTrace();
				}
				String md5sum_calculated= md5sum(objectstream);
				String md5sum_received;
				if(!json_run_data.isNull("runtrace_md5sum") ) {
					md5sum_received= (String) json_run_data.get("runtrace_md5sum");
					if (md5sum_received.compareTo(md5sum_calculated) == 0) {
						this.runtrace= (HashMap<Long, RunInstant>) readObject(objectstream);
					}
				}
			}
		}
		return (0);
	}

	String getStartTime() {
		SimpleDateFormat dformat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault());
		return (dformat.format(this.run_date_start_v));
	}

	String getEndTime() {
		SimpleDateFormat dformat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault());
		return (dformat.format(this.run_date_end_v));
	}

	Long getStartTime_v() {
		return (this.run_date_start_v.getTime());
	}

	Long getEndTime_v() {
		return (this.run_date_start_v.getTime());
	}

	boolean setStartTime() {
		this.run_date_start_v= new Date();
		Calendar ccdate= Calendar.getInstance();
		ccdate.setTime(this.run_date_start_v);
		this.run_date_start= String.format("%04d-%02d-%02d %02d:%02d:%02d %s",
			ccdate.get(Calendar.YEAR),
			ccdate.get(Calendar.MONTH) +1,
			ccdate.get(Calendar.DAY_OF_MONTH),
			ccdate.get(Calendar.HOUR),
			ccdate.get(Calendar.MINUTE),
			ccdate.get(Calendar.SECOND),
			(ccdate.get(Calendar.AM_PM)==0)?"am":"pm");

		writeLog(String.format("setting start time to: %s, should be %s", this.run_date_start, this.run_date_start_v.toString()));

		return (true);
	}

	boolean setEndTime() {
		this.run_date_end_v= new Date();
		Calendar ccdate= Calendar.getInstance();
		ccdate.setTime(this.run_date_end_v);
		this.run_date_end= String.format("%04d-%02d-%02d %02d:%02d:%02d %s",
			ccdate.get(Calendar.YEAR),
			ccdate.get(Calendar.MONTH) +1,
			ccdate.get(Calendar.DAY_OF_MONTH),
			ccdate.get(Calendar.HOUR),
			ccdate.get(Calendar.MINUTE),
			ccdate.get(Calendar.SECOND),
			(ccdate.get(Calendar.AM_PM)==0)?"am":"pm");

		writeLog(String.format("setting end time to: %s, should be %s", this.run_date_end, this.run_date_end_v.toString()));
		return (true);
	}

	int getValues()
	{
		this.distance_miles_v= this.distance_km_v * this.conv_km_miles;
		this.average_speed_miles_h_v= this.average_speed_km_h_v * this.conv_km_miles;
		this.current_speed_miles_h_v= this.current_speed_km_h_v * this.conv_km_miles;
		this.gps_distance_miles= this.gps_distance_km * this.conv_km_miles;
		return (0);
	}

	int getDateValues() throws ParseException {
		this.getValues();
		this.run_date_start_v= new Date();
		this.run_date_start_v= date_format.parse(this.run_date_start);
		this.run_date_end_v= new Date();
		this.run_date_end_v= date_format.parse(this.run_date_end);
		int no_xpoints= (int) ((this.run_date_end_v.getTime() - this.run_date_start_v.getTime()));
		return no_xpoints/1000;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(this.run_date_start);
		out.writeObject(this.run_date_end);
		out.writeObject(this.run_id_v);
		out.writeObject(this.run_date_start_v);
		out.writeObject(this.run_date_end_v);
		out.writeObject(this.average_speed_km_h_v);
		out.writeObject(this.average_speed_miles_h_v);
		out.writeObject(this.current_speed_m_s_v);
		out.writeObject(this.current_speed_km_h_v);
		out.writeObject(this.current_speed_miles_h_v);
		out.writeObject(this.calories_v_distance);
		out.writeObject(this.calories_v_heart_beat);
		out.writeObject(this.current_weight_v);
		out.writeObject(this.current_fat_v);
		out.writeObject(this.inclination);
		out.writeObject(this.threadmill_factor);
		out.writeObject(this.current_heart_rate);
		out.writeObject(this.recovery_hr);
		out.writeObject(this.resting_hr);
		out.writeObject(this.granularity_time);
		out.writeObject(this.distance_m_v);
		out.writeObject(this.distance_km_v);
		out.writeObject(this.distance_miles_v);
		out.writeObject(this.gps_distance_km);
		out.writeObject(this.gps_distance_miles);
		out.writeObject(this.current_time);
		out.writeObject(this.runtrace);
		out.writeObject(this.runtrace_md5sum);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		this.run_date_start= (String) in.readObject();
		this.run_date_end= (String) in.readObject();
		this.run_id_v= (int) in.readObject();
		this.run_date_start_v= (Date) in.readObject();
		this.run_date_end_v= (Date) in.readObject();
		this.average_speed_km_h_v= (double) in.readObject();
		this.average_speed_miles_h_v= (double) in.readObject();
		this.current_speed_m_s_v= (double) in.readObject();
		this.current_speed_km_h_v= (double) in.readObject();
		this.current_speed_miles_h_v= (double) in.readObject();
		this.calories_v_distance= (double) in.readObject();
		this.calories_v_heart_beat= (double) in.readObject();
		this.current_weight_v= (double) in.readObject();
		this.current_fat_v= (double) in.readObject();
		this.inclination= (int) in.readObject();
		this.threadmill_factor= (double) in.readObject();
		this.current_heart_rate= (int) in.readObject();
		this.recovery_hr= (int) in.readObject();
		this.resting_hr= (int) in.readObject();
		this.granularity_time= (long) in.readObject();
		this.distance_m_v= (double) in.readObject();
		this.distance_km_v= (double) in.readObject();
		this.distance_miles_v= (double) in.readObject();
		this.gps_distance_km= (double) in.readObject();
		this.gps_distance_miles= (double) in.readObject();
		this.current_time= (long) in.readObject();
		this.runtrace= (HashMap<Long, RunInstant>) in.readObject();
		this.runtrace_md5sum= (String) in.readObject();
	}
}
