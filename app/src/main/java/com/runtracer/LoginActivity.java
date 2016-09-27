package com.runtracer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity implements LoaderCallbacks<Cursor> {

	protected UserLoginTask mAuthTask = null;

	// UI references.
	private EditText mEmailView;
	private EditText mPasswordView;
	private View mProgressView;
	private View mLoginFormView;

	private Button mEmailSignInButton;

	protected String retrievedFullName = null;
	protected String retrievedEmail = null;
	protected String retrievedPassword = null;

	protected static final String TAG = "runtracer";
	protected static JSONObject userData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		retrievedFullName="";

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String value = extras.getString("user_info");
			writeLog("Starting LoginActivity: value: " + value);
			try {
				userData = new JSONObject(value);
				writeLog("LoginActivity: processed JSONObjectvalue: " +userData.toString());
				retrievedFullName=userData.getString("full_name");
				retrievedEmail=userData.getString("email");
				writeLog("LoginActivity: found user full name: " + retrievedFullName );
				writeLog("LoginActivity: found user email: " + retrievedEmail);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		// Set up the login form.
		mEmailView = (EditText) findViewById(R.id.email);
		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.login || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});

		mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
		mEmailSignInButton.setVisibility(Button.VISIBLE);
		mEmailSignInButton.setHapticFeedbackEnabled(true);
		mEmailSignInButton.setEnabled(true);
		mEmailSignInButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin();
			}
		});

		mLoginFormView = findViewById(R.id.login_form);
		mProgressView = findViewById(R.id.login_progress);
	}

	private void populateAutoComplete() {
		getLoaderManager().initLoader(0, null, this);
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		String email = mEmailView.getText().toString();
		String password = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password, if the user entered one.
		if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(email)) {
			mEmailView.setError(getString(R.string.error_field_required));
			focusView = mEmailView;
			cancel = true;
		} else if (!isEmailValid(email)) {
			mEmailView.setError(getString(R.string.error_invalid_email));
			focusView = mEmailView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			showProgress(true);
			mAuthTask = new UserLoginTask(email, password);
			mAuthTask.execute((Void) null);
		}

		JSONObject jsonData= null;
		try {
			jsonData = new JSONObject("{\"key\":\"data\"}");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		retrievedEmail= String.valueOf(mEmailView.getText());
		retrievedPassword=String.valueOf(mPasswordView.getText());

		if( isEmailValid(retrievedEmail) ) {
		} else {
			String errorString= "email is invalid: " + retrievedEmail;
			Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show();
			return;
		}

		Intent data = new Intent();
		try {
			assert jsonData != null;
			jsonData.accumulate("full_name", retrievedFullName);
			jsonData.accumulate("email", retrievedEmail);
			writeLog("LoginActivity: Adding: email: " + retrievedEmail);
			jsonData.accumulate("passwd", retrievedPassword);
			writeLog("LoginActivity: Adding: passwd: " + retrievedPassword);
			writeLog("LoginActivity: jsonData: " + jsonData.toString());

			writeLog("LoginActivity: Testing JSON Object: key: " + jsonData.getString("key"));
			writeLog("LoginActivity: Testing JSON Object: email: " + jsonData.getString("email"));
			writeLog("LoginActivity: Testing JSON Object: passwd: " + jsonData.getString("passwd"));

		} catch (JSONException e) {
			writeLog("Got a JSONException: " + e.toString());
			e.printStackTrace();
		}
		Bundle returnValue= new Bundle();
		returnValue.putString("user_data", jsonData.toString());
		data.putExtra("data", returnValue);
		// Activity finished ok, return the data
		setResult(RESULT_OK, data);
		this.finish();
	}

	public boolean isEmailValid(String email)
	{
		String regExpn = "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
			+"((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
			+"[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
			+"([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
			+"[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
			+"([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$";

		Pattern pattern = Pattern.compile(regExpn, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(email);

		return matcher.matches();
	}

	private boolean isPasswordValid(String password) {
		return password.length() > 4;
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	public void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime).alpha(
				show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
				}
			});

			mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
			mProgressView.animate().setDuration(shortAnimTime).alpha(
				show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
				}
			});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		return new CursorLoader(this,
			// Retrieve data rows for the device user's 'profile' contact.
			Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
				ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

			// Select only email addresses.
			ContactsContract.Contacts.Data.MIMETYPE +
				" = ?", new String[]{ContactsContract.CommonDataKinds.Email
			.CONTENT_ITEM_TYPE},

			// Show primary email addresses first. Note that there won't be
			// a primary email address if the user hasn't specified one.
			ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {

	}

	private interface ProfileQuery {
		String[] PROJECTION = {
			ContactsContract.CommonDataKinds.Email.ADDRESS,
			ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
		};

		int ADDRESS = 0;
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

		private final String mEmail;
		private final String mPassword;

		UserLoginTask(String email, String password) {
			mEmail = email;
			mPassword = password;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			return true;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			mAuthTask = null;
			showProgress(false);

			if (success) {
				finish();
			} else {
				mPasswordView.setError(getString(R.string.error_incorrect_password));
				mPasswordView.requestFocus();
			}
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			showProgress(false);
		}
	}

	public void writeLog(String msg) {
		String date = (DateFormat.format("dd-MM-yyyy hh:mm:ss", new java.util.Date()).toString());
	}
}

