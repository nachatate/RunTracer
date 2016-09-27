package com.runtracer;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

public class RunInstant implements Serializable {
	private static final long serialVersionUID = 100L;
	public long current_time;
	public double current_motion_speed_km_h_v;
	public double current_motion_distance_km_v;
	public double current_gps_speed_km_h;
	public double current_gps_distance_km;
	public double calories_v_distance;
	public double calories_v_heart_beat;
	public int current_heart_rate;
	public double longitude;
	public double latitude;
	public double altitude;

	RunInstant() {
		current_time= new Date().getTime();
		this.current_motion_speed_km_h_v = 0.0;
		this.current_motion_distance_km_v = 0.0;
		this.current_gps_speed_km_h = 0.0;
		this.current_gps_distance_km = 0.0;
		this.calories_v_distance = 0.0;
		this.calories_v_heart_beat = 0.0;
		this.current_heart_rate = 0;
		this.longitude = 0.0;
		this.latitude = 0.0;
		this.altitude = 0.0;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(this.current_time);
		out.writeObject(this.current_motion_speed_km_h_v);
		out.writeObject(this.current_motion_distance_km_v);
		out.writeObject(this.current_gps_speed_km_h);
		out.writeObject(this.current_gps_distance_km);
		out.writeObject(this.calories_v_distance);
		out.writeObject(this.calories_v_heart_beat);
		out.writeObject(this.current_heart_rate);
		out.writeObject(this.longitude);
		out.writeObject(this.latitude);
		out.writeObject(this.altitude);
	}

	private void readObject(java.io.ObjectInputStream in)
		throws IOException, ClassNotFoundException {
		this.current_time= (long) in.readObject();
		this.current_motion_speed_km_h_v= (double) in.readObject();
		this.current_motion_distance_km_v= (double) in.readObject();
		this.current_gps_speed_km_h= (double) in.readObject();
		this.current_gps_distance_km= (double) in.readObject();
		this.calories_v_distance= (double) in.readObject();
		this.calories_v_heart_beat= (double) in.readObject();
		this.current_heart_rate= (int) in.readObject();
		this.longitude= (double) in.readObject();
		this.latitude= (double) in.readObject();
		this.altitude= (double) in.readObject();
	}
}
