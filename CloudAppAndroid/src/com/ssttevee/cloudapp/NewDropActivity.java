package com.ssttevee.cloudapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Window;
import com.cloudapp.api.CloudApp;
import com.cloudapp.api.CloudAppException;
import com.cloudapp.api.model.CloudAppProgressListener;
import com.cloudapp.impl.CloudAppImpl;
import com.ssttevee.cloudapp.MainActivity.GetCAData;

public class NewDropActivity extends SherlockActivity {

	private CAApplication mApp;
	private SharedPreferences mPrefs;
	private Handler mHandler;
	private ProgressDialog mProgDiag;
	private ViewFlipper mFlipper;
	
	public static final int FILE_SELECT_CODE = 0x000002;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_new_drop);
		
		mApp = (CAApplication) getApplication();
		mHandler = new Handler();
		mPrefs = getSharedPreferences("CloudAppLoginCreds", 0);
		
		mProgDiag = new ProgressDialog(this);
		mProgDiag.setProgressStyle(ProgressDialog.STYLE_SPINNER | ProgressDialog.STYLE_HORIZONTAL);
		mProgDiag.setMessage(getString(R.string.ui_loading));
		mProgDiag.setCancelable(false);
		
		mFlipper = (ViewFlipper) findViewById(R.id.new_drop_flipper);

		if(mPrefs.getBoolean("isLoggedIn", false) && !mPrefs.getString("username", "").equals("") && !mPrefs.getString("password", "").equals("")) {
			CloudApp api = new CloudAppImpl(mPrefs.getString("username", ""), mPrefs.getString("password", ""));
			mApp.api = api;
		} else {
			Toast.makeText(this, "Error: Please login first", Toast.LENGTH_SHORT).show();
			finish();
		}
		
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();

	    if (Intent.ACTION_SEND.equals(action) && type != null) {
	        if ("text/plain".equals(type)) {
	            final String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
	            if (sharedText != null) {
	            	if(URLUtil.isValidUrl(sharedText)) {
	            		((TextView) findViewById(R.id.input_bookmark_url)).setText(sharedText);
	    				mFlipper.setDisplayedChild(2);
	    				mFlipper.getChildAt(2).setVisibility(View.VISIBLE);
	            	} else {
	        			final EditText input = new EditText(this);
	            		String filename = "";
	            		try {
	            			filename += sharedText.substring(0, 15);
						} catch (IndexOutOfBoundsException e) {
							filename += sharedText;
						}
	        			input.setText(filename + ".txt");
	                    new AlertDialog.Builder(this)
	                    .setView(input)
		            	.setTitle("Confirm")
		            	.setMessage(getString(R.string.new_drop_ui_name_text_file))
	                	.setPositiveButton(getString(R.string.ui_ok), new DialogInterface.OnClickListener() {
	        				public void onClick(DialogInterface dialog, int which) {
	    						try {
	    		            		FileOutputStream fos = openFileOutput(input.getText().toString() + (input.getText().toString().endsWith(".txt") ? "" : ".txt"), Context.MODE_PRIVATE);
	    		            		fos.write(sharedText.getBytes());
	    		            		fos.close();
									new SendData(SendData.UPLOAD_FILE, getFilesDir() + "/" + input.getText().toString() + (input.getText().toString().endsWith(".txt") ? "" : ".txt")).start();
	    						} catch (FileNotFoundException e) {
	    							e.printStackTrace();
	    						} catch (IOException e) {
	    							e.printStackTrace();
	    						}
	        				}
	        			})
	        			.setNegativeButton(getString(R.string.ui_cancel), new DialogInterface.OnClickListener() {
	        				
	        				@Override
	        				public void onClick(DialogInterface dialog, int which) {
	        					dialog.dismiss();
	        					finish();
	        				}
	        			}).create().show();
	            	}
	            }
	        } else if (type.startsWith("image/")) {
	            Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
	            if (imageUri != null) {
		            final String path = getPath(imageUri);
		            
		            
		            new AlertDialog.Builder(this)
		            	.setTitle("Confirm")
		            	.setMessage("Upload " + path.substring(path.lastIndexOf('/') + 1) + "?")
		            	.setPositiveButton(getString(R.string.ui_yes), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								new SendData(SendData.UPLOAD_FILE, path).start();
								dialog.dismiss();
							}
						})
						.setNegativeButton(getString(R.string.ui_cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								finish();
							}
						})
		            	.create().show();
	            }
	        }
	    }
	    
		
		((Button) findViewById(R.id.btn_add_bookmark)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!URLUtil.isValidUrl(((TextView) findViewById(R.id.input_bookmark_url)).getText().toString()))
		            new AlertDialog.Builder(NewDropActivity.this)
		            	.setTitle("Error")
		            	.setMessage("Invalid URL")
		            	.setNeutralButton(getString(R.string.ui_ok), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						}).create().show();
					
				else
		            new AlertDialog.Builder(NewDropActivity.this)
		            	.setTitle("Confirm")
		            	.setMessage("Bookmark " + (((TextView) findViewById(R.id.input_bookmark_name)).getText().toString().trim().equals("") ? ((TextView) findViewById(R.id.input_bookmark_url)).getText().toString() : ((TextView) findViewById(R.id.input_bookmark_name)).getText().toString()) + "?")
		            	.setPositiveButton(getString(R.string.ui_yes), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								new SendData(SendData.CREATE_BOOKMARK, ((TextView) findViewById(R.id.input_bookmark_name)).getText().toString(), ((TextView) findViewById(R.id.input_bookmark_url)).getText().toString()).start();
								dialog.dismiss();
							}
						})
						.setNegativeButton(getString(R.string.ui_cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								finish();
							}
						}).create().show();
			}
		});
		((Button) findViewById(R.id.btn_new_bookmark)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mFlipper.setDisplayedChild(2);
				mFlipper.getChildAt(2).setVisibility(View.VISIBLE);
			}
		});
		((Button) findViewById(R.id.btn_new_file)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		        Intent intent = new Intent(Intent.ACTION_GET_CONTENT); 
		        intent.setType("*/*"); 
		        intent.addCategory(Intent.CATEGORY_OPENABLE);

		        try {
		            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
		        } catch (android.content.ActivityNotFoundException ex) {
		            Toast.makeText(NewDropActivity.this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
		        }
			}
		});
		((Button) findViewById(R.id.btn_cancel)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case FILE_SELECT_CODE:      
	        if (resultCode == RESULT_OK) {  
	            // Get the Uri of the selected file 
	            Uri uri = data.getData();
	            // Get the path
	            final String path = getPath(uri);
	            
	            final SendData sd = new SendData(SendData.UPLOAD_FILE, path);
	            
	            new AlertDialog.Builder(this)
	            	.setTitle("Confirm")
	            	.setMessage("Upload " + path.substring(path.lastIndexOf('/') + 1) + "?")
	            	.setPositiveButton(getString(R.string.ui_yes), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							sd.start();
							dialog.dismiss();
						}
					})
					.setNegativeButton(getString(R.string.ui_cancel), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
						}
					})
	            	.create().show();
	        }
	        break;
	    }
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	public String getPath(Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = this.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor
                .getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }

        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

	class SendData extends Thread {
		
		public static final int CREATE_BOOKMARK = 0x000001;
		public static final int CREATE_BOOKMARKS = 0x000002;
		public static final int UPLOAD_FILE = 0x000003;
		
		private int mode;
		private String[] params;

		public SendData(int mode, String... params) {
			this.mode = mode;
			this.params = params;
		}

		@Override
		public void run() {
			try {
				switch (this.mode) {
				case CREATE_BOOKMARK:
					mApp.api.createBookmark(params[0], params[1]);
					mHandler.post(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(NewDropActivity.this, "Upload Complete", Toast.LENGTH_SHORT).show();
							Intent returnIntent = new Intent();
							setResult(RESULT_OK,returnIntent);     
							finish();
						}
					});
					break;
					
				case UPLOAD_FILE:
					final DecimalFormat df = new DecimalFormat("#.0");
					mHandler.post(new Runnable() {

						@Override
						public void run() {
							mFlipper.setDisplayedChild(1);
							((View) ((Button) findViewById(R.id.btn_cancel)).getParent()).setVisibility(View.GONE);
							((TextView) findViewById(R.id.upload_progress_text)).setText("Uploading " + params[0].substring(params[0].lastIndexOf('/') + 1) + "...");
							((TextView) findViewById(R.id.upload_progress_precent)).setText("0%");
							((TextView) findViewById(R.id.upload_progress_numbers)).setText("...");
						}
					});
					mApp.api.upload(new File(params[0]), new CloudAppProgressListener() {
						
						@Override
						public void transferred(final long written, final long length) {
							mHandler.post(new Runnable() {

								@Override
								public void run() {
									((ProgressBar) findViewById(R.id.upload_progress_bar)).setMax((int) length);
									((ProgressBar) findViewById(R.id.upload_progress_bar)).setProgress((int) written);
									((TextView) findViewById(R.id.upload_progress_precent)).setText(df.format(written * 100.0 / length) + "%");
									((TextView) findViewById(R.id.upload_progress_numbers)).setText(written + "/" + length);
									if(written == length) {
									}
								}
							});
						}
					});
					mHandler.post(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(NewDropActivity.this, "Upload Complete", Toast.LENGTH_SHORT).show();
							Intent returnIntent = new Intent();
							setResult(RESULT_OK,returnIntent);     
							finish();
						}
					});
					break;

				default:
					break;
				}
			} catch (CloudAppException e) {
				e.printStackTrace();
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						new AlertDialog.Builder(NewDropActivity.this)
								.setTitle(getString(R.string.login_failed_title))
								.setMessage(getString(R.string.login_failed))
								.setPositiveButton(
										getString(R.string.ui_close),
										new DialogInterface.OnClickListener() {

											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												dialog.dismiss();
												finish();
											}

										}).create().show();
					}
				});
			}
			mHandler.post(new Runnable() {

				@Override
				public void run() {
				}
			});
			super.run();
		}

	}

}
