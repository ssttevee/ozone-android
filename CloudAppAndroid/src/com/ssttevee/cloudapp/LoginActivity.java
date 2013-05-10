package com.ssttevee.cloudapp;

import com.actionbarsherlock.app.SherlockActivity;
import com.cloudapp.api.CloudApp;
import com.cloudapp.api.CloudAppException;
import com.cloudapp.impl.CloudAppImpl;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends SherlockActivity {

	private CAApplication mApp;
	private SharedPreferences mPrefs;
	private Handler mHandler;
	private ProgressDialog mProgDiag;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF2F5577));
		getSupportActionBar().setTitle(getString(R.string.login_title));
		getSupportActionBar().setDisplayUseLogoEnabled(false);   
		getSupportActionBar().setDisplayShowHomeEnabled(false);
		getSupportActionBar().setDisplayShowTitleEnabled(true);
		
		mApp = (CAApplication) getApplication();
		mHandler = new Handler();
		mPrefs = getSharedPreferences("CloudAppLoginCreds", 0);
		
		mProgDiag = new ProgressDialog(LoginActivity.this);
		mProgDiag.setMessage(getString(R.string.ui_loading));
		mProgDiag.setCancelable(false);

		((Button) findViewById(R.id.login_button))
			.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new LoginToCloudApp(
							((EditText) findViewById(R.id.login_username)).getText().toString(),
							((EditText) findViewById(R.id.login_password)).getText().toString()).start();
				}
			});
	}

	@Override
	protected void onStart() {
		if(mPrefs.getBoolean("isLoggedIn", false) && !mPrefs.getString("username", "").equals("") && !mPrefs.getString("password", "").equals("")) {
			CloudApp api = new CloudAppImpl(mPrefs.getString("username", ""), mPrefs.getString("password", ""));
			mApp.api = api;
			
			Intent intent = new Intent(LoginActivity.this, MainActivity.class);
			startActivity(intent);
			overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
			finish();
		}
		super.onStart();
	}

	class LoginToCloudApp extends Thread {
		String username;
		String password;

		public LoginToCloudApp(String username, String password) {
			this.username = username;
			this.password = password;
			mProgDiag.show();
		}

		@Override
		public void run() {
			CloudApp api = new CloudAppImpl(this.username, this.password);
			try {
				if(this.username.equals("") || this.password.equals("")) throw new CloudAppException(0, "", new Throwable());
				api.getAccountStats();
				mApp.api = api;
				
				SharedPreferences.Editor editor = mPrefs.edit();
				editor.putBoolean("isLoggedIn", true);
				editor.putString("username", this.username);
				editor.putString("password", this.password);
			    editor.commit();
			    
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						mProgDiag.hide();
						Intent intent = new Intent(LoginActivity.this, MainActivity.class);
						startActivity(intent);
						overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
						finish();
					}
				});
			} catch (final CloudAppException e) {
				e.printStackTrace();
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						mProgDiag.hide();
						new AlertDialog.Builder(LoginActivity.this)
								.setTitle(
										getString(R.string.login_failed_title))
								.setMessage(e.getMessage())
								.setPositiveButton(
										getString(R.string.ui_close),
										new DialogInterface.OnClickListener() {

											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												dialog.dismiss();
											}

										}).create().show();
					}
				});
			}
			super.run();
		}

	}

}
