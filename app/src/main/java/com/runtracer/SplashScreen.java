package com.runtracer;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class SplashScreen extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash_screen);

		final ImageView iv = (ImageView) findViewById(R.id.splash_image);
		final Animation an = AnimationUtils.loadAnimation(this, R.anim.abc_fade_in);
		final Animation an2 = AnimationUtils.loadAnimation(this, R.anim.abc_fade_out);

		iv.startAnimation(an);
		an.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}
			@Override
			public void onAnimationEnd(Animation animation) {
				new Handler().postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						iv.startAnimation(an2);
						finish();
						Intent i = new Intent(getBaseContext(), MainActivity.class);
						startActivity(i);
					}
				}, 2000/* 1sec delay */);
			}
			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});
	}
}
