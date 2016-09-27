package com.runtracer;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class RunChartActivity extends AppCompatActivity implements View.OnClickListener {

	private static final String TAG = "runtracer";

	String jscript = "";
	int no_xpoints = 0;
	int orientation = 0;
	boolean showingMap;

	//GUI elements
	WebView mChartView;
	WebSettings webSettings;

	FloatingActionButton mDrawMap;
	private RunData run_data;
	private UserData user_data;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_run_chart);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		mDrawMap= (FloatingActionButton) findViewById(R.id.fab_draw_map);
		mDrawMap.setOnClickListener(this);
		/*
		mDrawChart= (FloatingActionButton) findViewById(R.id.fab_draw_chart);
		mDrawChart.setOnClickListener(this);
		*/

		mChartView = (WebView) findViewById(R.id.webView);
		mChartView.setBackgroundColor(Color.LTGRAY);
		mChartView.setPadding(0, 0, 0, 0);
		webSettings = mChartView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		mChartView.addJavascriptInterface(new WebAppInterface(this), "Android");
		mChartView.getSettings().setBuiltInZoomControls(true);
		mChartView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.CLOSE);

		orientation= getResources().getConfiguration().orientation;

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			run_data = (RunData) extras.getSerializable("run_data");
			user_data = (UserData) extras.getSerializable("user_data");
			updateWebview();
		}
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
	}

	boolean updateWebview() {
		if (run_data != null) {
			try {
				no_xpoints = run_data.getDateValues();
				if (showingMap) {
					jscript = generateJSMapCode(1200 + no_xpoints, 800);
					mChartView.setInitialScale(getScaleY(1200));
				} else {
					jscript = generateJSCode(1200 + no_xpoints, 800);
					mChartView.setInitialScale(getScaleY(1200 * orientation));
				}
				mChartView.getSettings().setLoadWithOverviewMode(true);
				mChartView.getSettings().setUseWideViewPort(true);
				mChartView.loadData(jscript, "text/html", null);
			} catch (ParseException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return (true);
	}

	private String generateJSCode(int c_width, int c_height) throws UnsupportedEncodingException {
		String code = "";
		long starting_time = run_data.run_date_start_v.getTime();
		NumberFormat nf= NumberFormat.getInstance(Locale.CANADA);
		code += "\n";
		code += "<html>";
		code += "\n";
		code += "<head>";
		code += "\n";
		code += "<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>";
		code += "\n";
		code += "	<script type=\"text/javascript\">";
		code += "\n";
		code += "		google.load('visualization', '1.1', {packages: ['line']});";
		code += "\n";
		code += "	google.setOnLoadCallback(drawChart);";
		code += "\n";
		code += "	function drawChart() {";
		code += "\n";
		code += "		var data = new google.visualization.DataTable();";
		code += "\n";
		code += "		data.addColumn('number', 'Time (seconds)');";
		code += "\n";
		code += "		data.addColumn('number', 'Heart Beat (Hz)');";
		code += "\n";
		if (user_data.bMetricSystem) {
			code += "		data.addColumn('number', 'Speed (km/h)');";
		} else {
			code += "		data.addColumn('number', 'Speed (miles/h)');";
		}
		code += "\n";
		code += "		data.addColumn('number', 'Calories (distance) (KCal)');";
		code += "\n";
		code += "		data.addColumn('number', 'Calories (heart rate) (KCal)');";
		code += "\n";
		code += "		data.addRows([";
		code += "\n";
		writeLog(String.format("RunChartActivity: received: runtrace:"));
		if (run_data.runtrace != null && run_data.runtrace.size() >0) {
			List keys = (List<Long>) new ArrayList(run_data.runtrace.keySet());
			Collections.sort(keys);
			Iterator it = keys.iterator();
			for (; it.hasNext(); ) {
				double speed;
				RunInstant tmp_instant = new RunInstant();
				tmp_instant = run_data.runtrace.get(it.next());
				if (user_data.bMetricSystem) {
					speed=tmp_instant.current_motion_speed_km_h_v * run_data.conv_km_miles / run_data.conv_km_miles;
				} else {
					speed=tmp_instant.current_motion_speed_km_h_v * run_data.conv_km_miles;
				}
				code += String.format("\n[%d, %d, %s, %s, %s],", (tmp_instant.current_time - starting_time) / 1000, tmp_instant.current_heart_rate, nf.format(speed), nf.format(tmp_instant.calories_v_distance), nf.format(tmp_instant.calories_v_heart_beat));
			}
		} else {
			writeLog(String.format("RunChartActivity: error: run_data.runtrace: %b", run_data.runtrace!=null));
		}
		code += "\n";
		code += "		]);";
		code += "\n";
		code += "		var options = {";
		code += "\n";
		code += "			chart: {";
		code += "\n";
		code += String.format("			title: 'Details of your run on %s', ", run_data.run_date_start);
		code += "\n";
		code += "   curveType: 'function'";
		code += "\n";
		code += "		},";
		code += "\n";
		code += String.format("		width:%d,", c_width);
		code += "\n";
		code += String.format("		height:%d,", c_height);
		code += "\n";
		code += "			axes: {";
		code += "\n";
		code += "			x: {";
		code += "\n";
		code += "				0: {side: 'bottom'}";
		code += "\n";
		code += "			}";
		code += "\n";
		code += "		}";
		code += "\n";
		code += "		};";
		code += "\n";
		code += "		var chart = new google.charts.Line(document.getElementById('line_top_x'));";
		code += "\n";
		code += "		chart.draw(data, options);";
		code += "\n";
		code += "	}";
		code += "\n";
		code += "	</script>";
		code += "\n";
		code += "	</head>";
		code += "\n";
		code += "	<body>";
		code += "\n";
		code += "	<div id=\"line_top_x\"></div>";
		code += "\n";
		code += "	</body>";
		code += "\n";
		code += "	</html>";
		code += "\n";

		writeLog(String.format("code: %s", code));
		return (code);
	}


	private String generateJSMapCode(int c_width, int c_height) throws UnsupportedEncodingException {
		String code = "";
		NumberFormat nf= NumberFormat.getInstance(Locale.CANADA);
		List keys = (List<Long>) new ArrayList(run_data.runtrace.keySet());
		Collections.sort(keys);
		Iterator it = keys.iterator();

		code += "\n";
		code += "	<html> ";
		code += "\n";
		code += "	<head> ";
		code += "\n";
		code += "	<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script> ";
		code += "\n";
		code += "	<script type=\"text/javascript\"> ";
		code += "\n";
		code += "		google.load(\"visualization\", \"1\", {packages:[\"map\"]}); ";
		code += "\n";
		code += "	google.setOnLoadCallback(drawChart); ";
		code += "\n";
		code += "	function drawChart() { ";
		code += "\n";
		code += "		var data = google.visualization.arrayToDataTable([ ";
		code += "\n";
		code += "		['Lat', 'Long', 'Name'], ";
		code += "\n";

		for (; it.hasNext(); ) {
			RunInstant tmp_instant = new RunInstant();
			tmp_instant = (RunInstant) run_data.runtrace.get(it.next());
			code += String.format("\n[%s, %s, 'time: %d" +
				"s'],", nf.format(tmp_instant.latitude), nf.format(tmp_instant.longitude), (tmp_instant.current_time - run_data.getStartTime_v())/1000);
		}

	//	code += "		[37.4422, -122.1731, 'Shopping'] ";
		code += "\n";
		code += "		]); ";
		code += "\n";
		code += "		var map = new google.visualization.Map(document.getElementById('map_div')); ";
		code += "\n";
		code += "		map.draw(data, {showTip: true}); ";
		code += "\n";
		code += "	} ";
		code += "\n";
		code += "	</script> ";
		code += "\n";
		code += "	</head> ";
		code += "\n";
		code += "	<body> ";
		code += "\n";
		code += String.format("	<div id=\"map_div\" style=\"width: %dpx; height: %dpx\"></div> ", c_width, c_height);
		code += "\n";
		code += "	</body> ";
		code += "\n";
		code += "	</html> ";
		code += "\n";

		writeLog(String.format("code: %s", code));
		return(code);
	}

	private int getScaleY(int pic_height){
		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int height= display.getHeight();
		Double val;
		val = new Double(height)/new Double(pic_height);
		val = val * 100d;
		return val.intValue();
	}

	private int getScaleX(int pic_width){
		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int width = display.getWidth();
		Double val;
		val = new Double(width)/new Double(pic_width);
		val = val * 100d;
		return val.intValue();
	}

	public void writeLog(String msg) {
		String date = (DateFormat.format("dd-MM-yyyy hh:mm:ss", new java.util.Date()).toString());
		//Log.e(TAG, date + ": " + msg);
	}

	/**
	 * Called when a view has been clicked.
	 *
	 * @param v The view that was clicked.
	 */
	@Override
	public void onClick(View v) {
		try {
			switch (v.getId()) {
				/*
				case R.id.fab_draw_chart:
					jscript = generateJSCode(1200 + no_xpoints, 800);
					mChartView.setInitialScale(getScaleY(1200));
					mChartView.loadData(jscript, "text/html", null);
					break;
				*/
				case R.id.fab_draw_map:
					showingMap= !showingMap;
					updateWebview();
					break;

				default:
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class WebAppInterface {
		Context mContext;

		/** Instantiate the interface and set the context */
		WebAppInterface(Context c) {
			mContext = c;
		}

		/** Show a toast from the web page */
		@JavascriptInterface
		public void showToast(String toast) {
			Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
		}
	}

}
