package com.runtracer;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.text.format.DateFormat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.util.Date;
import java.util.Iterator;

import static android.os.Process.myTid;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in a service on a separate handler thread.
 * TODO: Customize class - update intent actions, extra parameters and static helper methods.
 */
public class ServerDataService extends IntentService {

	protected static final String ACTION_QUERY_SERVER= "com.runtracer.query_server";
	protected static final String ACTION_REPORT_STATUS= "com.runtracer.report_status";
	protected static final String PARAM_OUT_MSG = "param_out_msg";

	private static final String EXTRA_PARAM1 = "com.runtracer.extra.PARAM1";
	private static final String EXTRA_PARAM2 = "com.runtracer.extra.PARAM2";

	private static final String TAG = "runtracer";
	private static final int NETWORK_TIMEOUT = 8000;

	private static DataBaseExchange localDbExchange;

	public ServerDataService() {
		super("ServerDataService");
		localDbExchange= DataBaseExchange.createDataBaseExchange();
		writeLog("\n\n\n\n\n\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\nServerDataService: starting ServerDataService...");
	}

	public void writeLog(String msg) {
		Date cdate;
		String date = (DateFormat.format("dd-MM-yyyy hh:mm:ss a", cdate= new Date()).toString());
		int threadID;
		threadID = Process.getThreadPriority(myTid());
		String msg2= String.format("<%d> thread id: %d \t>> ", cdate.getTime(), threadID);
	}

	/**
	 * Starts this service to perform action QueryServer with the given parameters.
	 * If the service is already performing a task this action will be queued.
	 * @see IntentService
	 */
	public static void startActionQueryServer(Context context, String param1, String param2) {
		Intent intent = new Intent(context, ServerDataService.class);
		intent.setAction(ACTION_QUERY_SERVER);
		intent.putExtra(EXTRA_PARAM1, param1);
		intent.putExtra(EXTRA_PARAM2, param2);
		context.startService(intent);
	}

	/** Starts this service to perform action ReportStatus with the given parameters.
	 * If the service is already performing a task this action will be queued.
	 * @see IntentService
	 */
	public static void startActionReportStatus(Context context, String param1, String param2) {
		Intent intent = new Intent(context, ServerDataService.class);
		intent.setAction(ACTION_REPORT_STATUS);
		intent.putExtra(EXTRA_PARAM1, param1);
		intent.putExtra(EXTRA_PARAM2, param2);
		context.startService(intent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String hash= null;
		if (intent != null) {
			try {
				final String action = intent.getAction();
				hash= intent.getStringExtra("hash");
				if (ACTION_QUERY_SERVER.equals(action)) {
					if (MainActivity.dbExchange != null && (MainActivity.dbExchange.hash.compareTo(hash) == 0)) {
						MainActivity.available.acquire();
						localDbExchange.clear();
						localDbExchange= (DataBaseExchange) MainActivity.dbExchange.clone();
						MainActivity.available.release();
						localDbExchange.json_data_out = new JSONObject("{\"key\":\"data\"}");
						//writeLog(String.format("ServerDataService: localDbExchange received full_name: %s", localDbExchange.full_name));
						//writeLog(String.format("ServerDataService: localDbExchange received accountEmail: %s", localDbExchange.accountEmail));
						//writeLog(String.format("ServerDataService: localDbExchange received command: %s", localDbExchange.command));
						//writeLog(String.format("ServerDataService: localDbExchange received url: %s", localDbExchange.url));
						doExchange(localDbExchange);
						handleActionQueryServer(hash);
					}
				} else if (ACTION_REPORT_STATUS.equals(action)) {
					final String param1 = intent.getStringExtra(EXTRA_PARAM1);
					final String param2 = intent.getStringExtra(EXTRA_PARAM2);
					handleActionReportStatus(param1, param2);
				}
			} catch (InterruptedException | JSONException | CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
	}

	/** Handle action QueryServer in the provided background thread with the provided parameters. */
	private void handleActionQueryServer(String response) throws InterruptedException, CloneNotSupportedException, JSONException {
		MainActivity.available.acquire();
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(MainActivity.ResponseReceiver.ACTION_RESP);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		MainActivity.dbExchange.clear();
		MainActivity.dbExchange= (DataBaseExchange) localDbExchange.clone();
		MainActivity.available.release();
		broadcastIntent.putExtra(PARAM_OUT_MSG, response);
		sendBroadcast(broadcastIntent);
	}

	/** Handle action ReportStatus in the provided background thread with the provided parameters. */
	private void handleActionReportStatus(String param1, String param2) {
		String response= "<msg>message FROM ServerDataService: data from handleActionReportStatus.</msg>";
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(MainActivity.ResponseReceiver.ACTION_RESP);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		broadcastIntent.putExtra(PARAM_OUT_MSG, response);
		sendBroadcast(broadcastIntent);
	}

	protected synchronized DataBaseExchange doExchange(DataBaseExchange dbEx) throws JSONException {
		int json_sz= dbEx.json_data_in.toString().length();
		int dbgidx=0;
		dbEx.json_data_in.put("json_size", json_sz);
		HttpURLConnection httpConnection;
		try {
			assert dbEx.url != null;
			httpConnection = (HttpURLConnection) dbEx.url.openConnection();
		} catch (IOException e) {
			writeLog(String.format("ServerDataService:doExchange: %d :IOException: %s", dbgidx, e.toString()));
			dbgidx++;
			dbEx.error_no= dbgidx;
			return dbEx;
		}
		try {
			assert httpConnection != null;
			httpConnection.setConnectTimeout(NETWORK_TIMEOUT);
			httpConnection.setReadTimeout(NETWORK_TIMEOUT);
			httpConnection.setDoInput(true);
			httpConnection.setDoOutput(true);
			httpConnection.setUseCaches(false);
			httpConnection.setRequestMethod("POST");
		} catch (ProtocolException e) {
			writeLog(String.format("ServerDataService:doExchange:%d :IOException: %s", dbgidx, e.toString()));
			dbgidx++;
			dbEx.error_no= dbgidx;
			return dbEx;
		}
		int delay;
		try {
			delay=0;
			OutputStream os;
			os = httpConnection.getOutputStream();
			BufferedWriter br;
			assert os != null;
			br = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			br.write(dbEx.json_data_in.toString());
			br.flush();
			br.close();
			os.flush();
			os.close();
		} catch (IOException e) {
			writeLog(String.format("ServerDataService:doExchange:%d :IOException: %s", dbgidx, e.toString()));
			dbgidx++;
			dbEx.error_no= dbgidx;
			httpConnection.disconnect();
			e.printStackTrace();
			return dbEx;
		}
		try {
			httpConnection.connect();
		} catch (IOException e) {
			writeLog(String.format("ServerDataService:doExchange:%d :IOException: %s", dbgidx, e.toString()));
			dbgidx++;
			dbEx.error_no= dbgidx;
			httpConnection.disconnect();
			e.printStackTrace();
			return dbEx;
		}
		try {
			InputStream is= httpConnection.getInputStream();
			InputStreamReader isr=new InputStreamReader(is);
			BufferedReader in= new BufferedReader(isr);
			String inputLine;
			JSONObject json_data;
			json_data = new JSONObject("{\"key\":\"data\"}");
			StringBuilder sb = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				sb.append(inputLine);
				json_data = new JSONObject(sb.toString());
				Iterator<String> itr = json_data.keys();
				while (itr.hasNext()) {
					String key = itr.next();
					dbEx.json_data_out.accumulate(key, json_data.get(key));
				}
			}
			httpConnection.disconnect();
			is.close();
			isr.close();
			in.close();
		} catch ( IOException | JSONException e) {
			writeLog(String.format("ServerDataService:doExchange:%d :IOException: %s", dbgidx, e.toString()));
			dbgidx++;
			dbEx.error_no= dbgidx;
			httpConnection.disconnect();
			e.printStackTrace();
			return dbEx;
		}
		try {
			if (dbEx.error_no > 0) {
				delay= 80000;
			}
			if (delay>0) {
				Thread.sleep(delay);
			}
		} catch (InterruptedException e) {
			writeLog(String.format("ServerDataService:doExchange:%d :IOException: %s", dbgidx, e.toString()));
			dbgidx++;
			dbEx.error_no= dbgidx;
			httpConnection.disconnect();
			e.printStackTrace();
		} return dbEx;
	}
}
