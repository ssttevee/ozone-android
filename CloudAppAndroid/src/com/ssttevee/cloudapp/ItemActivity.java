package com.ssttevee.cloudapp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.ClipboardManager;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cloudapp.api.CloudAppException;
import com.cloudapp.api.model.CloudAppItem.Type;

public class ItemActivity extends SherlockActivity {

	private CAApplication mApp;
	private Handler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_item);
		getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF2F5577));
		getSupportActionBar().setTitle(getString(R.string.item_title));
		getSupportActionBar().setDisplayUseLogoEnabled(false);   
		getSupportActionBar().setDisplayShowHomeEnabled(false);
		getSupportActionBar().setDisplayShowTitleEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		mApp = (CAApplication) getApplication();
		mHandler = new Handler();

		if(mApp.itemForViewing == null) finish();
		
		String details = "";
		
		try {
			details += mApp.itemForViewing.getName() + "\n";
			details += mApp.itemForViewing.getItemType() + "\n";
			details += DateFormat.format("MMM dd, yyyy", mApp.itemForViewing.getCreatedAt()) + "\n";
			details += mApp.itemForViewing.getViewCounter() + "\n";
			details += (mApp.itemForViewing.getRedirectUrl() == "null" ? "N/A" : mApp.itemForViewing.getRedirectUrl()) + "\n";
			details += mApp.itemForViewing.getSource() + "\n";
			details += (mApp.itemForViewing.isPrivate() ? "Private" : "Public") + "\n";
			details += (mApp.itemForViewing.isTrashed() ? DateFormat.format("MMM dd, yyyy", mApp.itemForViewing.getDeletedAt()) : "Never") + "\n";
			
			((TextView) findViewById(R.id.item_details)).setText(details);
			((TextView) findViewById(R.id.item_details)).setHorizontallyScrolling(true);
			
			if(mApp.itemForViewing.getItemType().equals(Type.IMAGE))
				new Thread(new Runnable() {
					public void run() {
						try {

							InputStream is = (InputStream) new URL(mApp.itemForViewing.getThumbnailUrl()).getContent();
							final Drawable d = Drawable.createFromStream(is, "src name");

						    mHandler.post(new Runnable() {
								public void run() {
								    ((ImageView) findViewById(R.id.item_thumb)).setImageDrawable(d);
								    LayoutParams p = ((ImageView) findViewById(R.id.item_thumb)).getLayoutParams();
								    p.height = (int) (((ImageView) findViewById(R.id.item_thumb)).getWidth() * 0.75);
								    ((ImageView) findViewById(R.id.item_thumb)).setLayoutParams(p);
								}
							});
						} catch (CloudAppException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}).start();
			else if(mApp.itemForViewing.getItemType().equals(Type.BOOKMARK)) ((ImageView) findViewById(R.id.item_thumb)).setImageResource(R.drawable.thumb_bookmark);
			else if(mApp.itemForViewing.getItemType().equals(Type.ARCHIVE)) ((ImageView) findViewById(R.id.item_thumb)).setImageResource(R.drawable.thumb_archive);
			else if(mApp.itemForViewing.getItemType().equals(Type.AUDIO)) ((ImageView) findViewById(R.id.item_thumb)).setImageResource(R.drawable.thumb_music);
			else if(mApp.itemForViewing.getItemType().equals(Type.VIDEO)) ((ImageView) findViewById(R.id.item_thumb)).setImageResource(R.drawable.thumb_video);
		} catch (CloudAppException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(R.string.main_ui_ctx_view)
			.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					try {
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mApp.itemForViewing.getUrl()));
						startActivity(browserIntent);
					} catch (CloudAppException e) {
						e.printStackTrace();
					}
					return false;
				}
			}).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		menu.add(R.string.main_ui_ctx_copy)
			.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					try {
						String linkforclip = mApp.itemForViewing.getUrl();
						ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				        cm.setText(linkforclip);
				        Toast.makeText(ItemActivity.this, getString(R.string.main_ui_message_copied), Toast.LENGTH_SHORT).show();
					} catch (CloudAppException e) {
				        Toast.makeText(ItemActivity.this, getString(R.string.main_ui_message_not_copied), Toast.LENGTH_SHORT).show();
					}
					return false;
				}
			});
		
		menu.add(R.string.main_ui_ctx_delete)
			.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					try {
			            new AlertDialog.Builder(ItemActivity.this)
			        	.setTitle(getString(R.string.ui_confirm))
			        	.setMessage("Delete " + mApp.itemForViewing.getName() + "?")
			        	.setPositiveButton(getString(R.string.ui_yes), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								new Thread(new Runnable() {
									public void run() {
										try {
											mApp.api.delete(mApp.itemForViewing);
											mHandler.post(new Runnable() {
												public void run() {
													Toast.makeText(ItemActivity.this, "Deleted 1 item", Toast.LENGTH_SHORT).show();
													setResult(RESULT_CANCELED, new Intent());     
											    	finish();
													overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
												}
											});
										} catch (CloudAppException e) {
											e.printStackTrace();
										}
									}
								}).start();
							}
						})
						.setNegativeButton(getString(R.string.ui_cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						}).create().show();
					} catch (CloudAppException e) {
						e.printStackTrace();
					}
					return false;
				}
			});
		
		menu.add(R.string.main_ui_ctx_rename)
		.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				try {
					final EditText input = new EditText(ItemActivity.this);
					input.setText(mApp.itemForViewing.getName());
		            new AlertDialog.Builder(ItemActivity.this)
		            .setView(input)
		        	.setTitle(getString(R.string.main_ui_ctx_rename))
		        	.setPositiveButton(getString(R.string.ui_ok), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							new Thread(new Runnable() {
								public void run() {
									try {
										mApp.api.rename(mApp.itemForViewing, input.getText().toString());
										mHandler.post(new Runnable() {
											public void run() {
												Toast.makeText(ItemActivity.this, "Renamed to " + input.getText().toString(), Toast.LENGTH_SHORT).show();
												setResult(RESULT_CANCELED, new Intent());
											}
										});
									} catch (CloudAppException e) {
										e.printStackTrace();
									}
								}
							}).start();
						}
					})
					.setNegativeButton(getString(R.string.ui_cancel), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					}).create().show();
				} catch (CloudAppException e) {
					e.printStackTrace();
				}
				return false;
			}
		});
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	finish();
			overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		mApp.itemForViewing = null;
		super.onDestroy();
	}
	
}
