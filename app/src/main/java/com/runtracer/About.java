package com.runtracer;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageSwitcher;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class About extends AppCompatActivity implements View.OnClickListener {

	UserData user_bio;

	private String about_you = "";
	private TextToSpeech tts;

	private final double conv_kg_lbs= 2.2046226185;

	//GUI elements
	FloatingActionButton mSpeakAbout;
	private TextView mAboutInfo;
	private TextView mDisclaimer;
	private ScrollView mScrollable;
	private boolean playing;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		user_bio = (UserData) getIntent().getSerializableExtra("UserData");

		about_you = getAboutInfo();

		mAboutInfo = (TextView) findViewById(R.id.about_you);
		mAboutInfo.setText(about_you);

		mScrollable= (ScrollView) findViewById(R.id.scrollableContent);
		mScrollable.scrollTo(0, 0);

		//mDisclaimer = (TextView) findViewById(R.id.disclaimer);
		//mDisclaimer.setText(this.getDisclaimer());

		mSpeakAbout = (FloatingActionButton) findViewById(R.id.speak_about);
		mSpeakAbout.setOnClickListener(this);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(this);

		getSupportActionBar().setDisplayHomeAsUpEnabled(false);

		tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if(status != TextToSpeech.ERROR) {
					tts.setLanguage(Locale.US);
				}
			}
		});

	}

	private String getAboutInfo() {
		String info="";
		info= String.format("%s, %d years old.", user_bio.full_name, user_bio.age);
		about_you=info;
		info= String.format("\n\nBody Mass Index: %.2f%%", user_bio.bmi);
		about_you+=info;
		info= String.format("\nBody Adiposity Index: %.2f%%", user_bio.bai);
		about_you+=info;
		info= String.format("\nResting Metabolic Rate: %.0f KCal/day", user_bio.rmr);
		about_you+=info;
		info= String.format("\nBasal Metabolic Rate: %.0f KCal/day", user_bio.bmr);
		about_you+=info;
		info= String.format("\nMaximum Heart Rate: %.0f Hz", user_bio.maximum_hr);
		about_you+=info;
		info= "\n\nWeight Loss Info";
		about_you+=info;
		double max_cal_per_day= (user_bio.bmi * user_bio.current_weight_v * 69 )/100;
		double max_weight_loss_day_lb= max_cal_per_day / 3500;
		double max_weight_loss_day_kg= max_weight_loss_day_lb / conv_kg_lbs;
		if(user_bio.bMetricSystem) {
			info= String.format("\nMaximum Calorie Deficit per day: %.2f, which is equivalent to about %.2f kg of fat.", max_cal_per_day, max_weight_loss_day_kg );
		} else {
			info= String.format("\nMaximum Calorie Deficit per day: %.2f, which is equivalent to about %.2f pounds of fat.", max_cal_per_day, max_weight_loss_day_lb );
		}
		about_you+=info;
		info="\nThis equivalence varies widely depending on a number of factors and is provided only as a rough estimate.";
		about_you+=info;
		info= "\n\nCalorie deficits above these levels are dangerous and will likely result in lean mass loss (muscle) which in turn may reduce your metabolism and impact your weight loss.";
		about_you+=info;
		info= "\n\nInformation provided based on interpretation of the National Institute of Health guidelines and data provided by the user input and / or external devices.";
		about_you+=info;
		info= "\n\nPlease be sure to check the accuracy and precision of your external medical devices and be precise when filling your information as this will impact the results you see here.";
		about_you+=info;
		info= "\n\nPlease report any errors or miscalculations you observe while using the App and feel free to send us your suggestions by clicking the email button above.";
		about_you+=info;
		return about_you;
	}

	protected String getDisclaimer() {
		String disclaimer = "\n\nDisclaimer: please pay attention to the following information.";
		disclaimer += "\nYou must not rely on the information on this App as an alternative to medical advice from your doctor or other professional healthcare provider.";
		disclaimer += "\nIf you have any specific questions about any medical matter, you should consult your doctor or other professional healthcare provider. ";
		disclaimer += "\nIf you think you may be suffering from any medical condition, you should seek immediate medical attention.";
		disclaimer += "\nYou should never delay seeking medical advice, disregard medical advice or discontinue medical treatment because of information on this App.";
		disclaimer += "\nOur app includes interactive features that allow users to communicate with us.";
		disclaimer += "\nYou acknowledge that, because of the limited nature of communication through our App's interactive features,";
		disclaimer += "\nany assistance you may receive using any such features is likely to be incomplete and may even be misleading.\n\n";

		return (disclaimer);
	}

	protected int speakText(String toSpeak) {
		tts.speak(toSpeak, TextToSpeech.QUEUE_ADD, null);
		return(0);
	}

	private void stopSpeaking() {
		if (tts.isSpeaking()) {
			tts.stop();
		}
	}

	protected int sendEmail() {

		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"admin@runtracer.com"});
		i.putExtra(Intent.EXTRA_SUBJECT, "Suggestion");
		i.putExtra(Intent.EXTRA_TEXT, "");
		try {
			startActivity(Intent.createChooser(i, "Send mail..."));
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(About.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
		}

		return(0);
	}

	/**
	 * Called when a view has been clicked.
	 *
	 * @param v The view that was clicked.
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.speak_about:
				this.playing= !this.playing;
				if (this.playing ) {
					speakText(about_you);
					//get the drawable
					Drawable myFabSrc = getResources().getDrawable(android.R.drawable.ic_media_pause);
					//copy it in a new one
					Drawable willBeWhite = myFabSrc.getConstantState().newDrawable();
					//set the color filter, you can use also Mode.SRC_ATOP
					willBeWhite.mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
					//set it to your fab button initialized before
					Snackbar.make(v, "Speaking the text now.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
					mSpeakAbout.setImageDrawable(willBeWhite);
				} else {
					stopSpeaking();
					//get the drawable
					Drawable myFabSrc = getResources().getDrawable(android.R.drawable.ic_media_play);
					//copy it in a new one
					Drawable willBeWhite = myFabSrc.getConstantState().newDrawable();
					//set the color filter, you can use also Mode.SRC_ATOP
					willBeWhite.mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
					//set it to your fab button initialized before
					mSpeakAbout.setImageDrawable(willBeWhite);
				}
				break;

			case R.id.fab:
				sendEmail();
				Snackbar.make(v, "Sending email", Snackbar.LENGTH_LONG).setAction("Action", null).show();
				break;


		}
	}

}
