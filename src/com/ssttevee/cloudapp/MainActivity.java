package com.ssttevee.cloudapp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cloudapp.api.CloudAppException;
import com.cloudapp.api.model.CloudAppItem;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingActivity;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.ClipboardManager;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;

@SuppressWarnings("deprecation")
public class MainActivity extends SlidingActivity {
	
	public static final int FILE_UPLOAD_CODE = 0x000001;
	public static final int FILE_SELECT_CODE = 0x000002;
	public static final int VIEW_ITEM_CODE = 0x000003;

	private CAApplication mApp;
	private Handler mHandler;
	private SharedPreferences mPrefs;
	private ProgressDialog mProgDiag;
	private List<CloudAppItem> list = new ArrayList<CloudAppItem>();
	
	private int page = 0;
	private boolean isLastPage = false;
	private boolean isTrashSelected = false;
	private ItemListAdapter adapter;
	
	private TextView lastFilter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setBehindContentView(R.layout.slider_main);

		// action bar config/settings
		getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF2F5577));
		getSupportActionBar().setTitle(R.string.main_title);
		getSupportActionBar().setDisplayUseLogoEnabled(false);   
		getSupportActionBar().setDisplayShowHomeEnabled(false);
		getSupportActionBar().setDisplayShowTitleEnabled(true);

		// sliding menu config/settings
		getSlidingMenu().setBehindWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics()));
		getSlidingMenu().setMode(SlidingMenu.LEFT);
		getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		setSlidingActionBarEnabled(false);
		
		// set fields
		mApp = (CAApplication) getApplication();
		mHandler = new Handler();
		mPrefs = getSharedPreferences("CloudAppLoginCreds", 0);
		
		mProgDiag = new ProgressDialog(MainActivity.this);
		mProgDiag.setMessage(getString(R.string.ui_loading));
		mProgDiag.setCancelable(false);
        
		lastFilter = ((TextView) ((ViewGroup) findViewById(R.id.filter_all)).getChildAt(1));
		
		// the listeners ~
		RadioGroup rg = (RadioGroup) findViewById(R.id.view_mode);
		rg.check(R.id.view_mode_list);
		rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				Toast.makeText(MainActivity.this, "Selected " + checkedId, Toast.LENGTH_SHORT).show();
			}
		});
//		footerView = (ListFooter) getLayoutInflater().inflate(R.layout.footer_more, null);
//		footerView.setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				((ListFooter) v).onClick(v);
//				new GetCAData(GetCAData.GET_ITEMS, mApp.itemsPerPage + "", "null", "false").start();
//			}
//		});
		((Button) findViewById(R.id.button_delete)).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String itemNames = "";
				for (Iterator<CloudAppItem> iterator = adapter.getCheckedItems().iterator(); iterator.hasNext();) {
					try {
						CloudAppItem type = iterator.next();
						itemNames += type.getName();
						if(iterator.hasNext()) itemNames += ", ";
					} catch (CloudAppException e) {
						e.printStackTrace();
					}
				}
	            new AlertDialog.Builder(MainActivity.this)
	            	.setTitle("Confirm")
	            	.setMessage("Delete " + itemNames + "?")
	            	.setPositiveButton(getString(R.string.ui_yes), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							new GetCAData(GetCAData.DELETE_ITEMS).start();
						}
					})
					.setNegativeButton(getString(R.string.ui_cancel), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					}).create().show();
			}
		});
		final PullToRefreshListView lv = (PullToRefreshListView) findViewById(R.id.data_list_view);
		registerForContextMenu(lv.getRefreshableView());
//		lv.addFooterView(footerView);
		lv.getRefreshableView().setAdapter(adapter = new ItemListAdapter(this, list));
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mApp.itemForViewing = adapter.getItem(position - 1);
				Intent intent = new Intent(MainActivity.this, ItemActivity.class);
				startActivityForResult(intent, VIEW_ITEM_CODE);
				overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
			}
		});
		lv.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				// TODO Auto-generated method stub
				
			}
		});
//		lv.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
		lv.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				refreshData();
			}
		});
		lv.setOnLastItemVisibleListener(new PullToRefreshBase.OnLastItemVisibleListener() {
			public void onLastItemVisible() {
				if(!isLastPage) {
					if(!isTrashSelected)
						new GetCAData(GetCAData.GET_ITEMS, mApp.itemsPerPage + "", "null", "false").start();
					else 
						new GetCAData(GetCAData.GET_ITEMS, mApp.itemsPerPage + "", "null", "true").start();
				}
			}
		});
		adapter.setListener(new ItemListAdapter.CBListener() {
			public void onCheckBoxChanged(boolean checked) {
				if(checked)
					((Button) findViewById(R.id.button_delete)).setVisibility(View.VISIBLE);
				else
					((Button) findViewById(R.id.button_delete)).setVisibility(View.GONE);
			}
		});

		new GetCAData(GetCAData.GET_ITEMS, mApp.itemsPerPage + "", "null", "false").start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(R.string.main_ui_new_drop)
			.setIcon(R.drawable.ic_menu_plus)
			.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					Intent intent = new Intent(MainActivity.this, NewDropActivity.class);
					startActivityForResult(intent, FILE_UPLOAD_CODE);
					return false;
				}
			}).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		menu.add(R.string.main_ui_donate)
			.setIcon(R.drawable.ic_menu_donate)
			.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					Intent intent = new Intent(MainActivity.this, DonateActivity.class);
					startActivity(intent);
					overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
					return false;
				}
			}).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		menu.add(R.string.main_ui_log_out)
			.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					SharedPreferences.Editor editor = mPrefs.edit();
					editor.putBoolean("isLoggedIn", false);
					editor.putString("username", "");
					editor.putString("password", "");
				    editor.commit();

					Intent intent = new Intent(MainActivity.this, LoginActivity.class);
					startActivity(intent);
					overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
					finish();
					return false;
				}
			});
		
//		menu.add(R.string.main_ui_settings)
//			.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//				@Override
//				public boolean onMenuItemClick(MenuItem item) {
					// TODO add settings activity
//					Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
//					startActivity(intent);
//					overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
//					finish();
//					return false;
//				}
//			});
		
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case FILE_UPLOAD_CODE:
	        if (resultCode == RESULT_OK)
	        	refreshData();
        	break;
        	
        case VIEW_ITEM_CODE:
        	if(resultCode == RESULT_CANCELED)
	        	refreshData();
        	break;
	    }
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		
	    try {
			menu.setHeaderTitle(adapter.getItem(info.position - 1).getName());
		} catch (CloudAppException e) {
			e.printStackTrace();
		}  
	    menu.add(0, info.position - 1, 0, getString(R.string.main_ui_ctx_view));
	    menu.add(0, info.position - 1, 0, getString(R.string.main_ui_ctx_download));
	    menu.add(0, info.position - 1, 0, getString(R.string.main_ui_ctx_copy));
	    menu.add(0, info.position - 1, 0, getString(R.string.main_ui_ctx_delete));
	    menu.add(0, info.position - 1, 0, getString(R.string.main_ui_ctx_rename));
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(final android.view.MenuItem item) {
		if(item.getTitle().equals(getString(R.string.main_ui_ctx_view))) {
			try {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(adapter.getItem(item.getItemId()).getUrl()));
				startActivity(browserIntent);
			} catch (CloudAppException e) {
			}
		} else if(item.getTitle().equals(getString(R.string.main_ui_ctx_download))) {
			try {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(adapter.getItem(item.getItemId()).getContentUrl()));
				startActivity(browserIntent);
			} catch (CloudAppException e) {
			}
		} else if(item.getTitle().equals(getString(R.string.main_ui_ctx_copy))) {
			try {
				String linkforclip = adapter.getItem(item.getItemId()).getUrl();
				ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		        cm.setText(linkforclip);
		        Toast.makeText(this, getString(R.string.main_ui_message_copied), Toast.LENGTH_SHORT).show();
			} catch (CloudAppException e) {
		        Toast.makeText(this, getString(R.string.main_ui_message_not_copied), Toast.LENGTH_SHORT).show();
			}
		} else if(item.getTitle().equals(getString(R.string.main_ui_ctx_delete))) {
			String itemName = "";
			try {
				itemName = adapter.getItem(item.getItemId()).getName();
			} catch (CloudAppException e) {
			}
            new AlertDialog.Builder(MainActivity.this)
        	.setTitle(getString(R.string.ui_confirm))
        	.setMessage("Delete " + itemName + "?")
        	.setPositiveButton(getString(R.string.ui_yes), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new GetCAData(GetCAData.DELETE_ITEM, item.getItemId() + "").start();
				}
			})
			.setNegativeButton(getString(R.string.ui_cancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			}).create().show();
		} else if(item.getTitle().equals(getString(R.string.main_ui_ctx_rename))) {
			final EditText input = new EditText(this);
			String itemName = "";
			try {
				itemName = adapter.getItem(item.getItemId()).getName();
			} catch (CloudAppException e) {
			}
			input.setText(itemName);
            new AlertDialog.Builder(MainActivity.this)
            .setView(input)
        	.setTitle(getString(R.string.main_ui_ctx_rename))
        	.setPositiveButton(getString(R.string.ui_ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					new GetCAData(GetCAData.RENAME_ITEM, item.getItemId() + "", input.getText().toString()).start();
				}
			})
			.setNegativeButton(getString(R.string.ui_cancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			}).create().show();
		}
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		mProgDiag.dismiss();
		super.onPause();
	}

	private void refreshData() {
    	list.clear();
    	page = 0;
    	isLastPage = false;
		if(!isTrashSelected)
			new GetCAData(GetCAData.GET_ITEMS, mApp.itemsPerPage + "", "null", "false").start();
		else 
			new GetCAData(GetCAData.GET_ITEMS, mApp.itemsPerPage + "", "null", "true").start();
	}
	
	private void showData(PullToRefreshListView lv) {
		adapter.setItems(list);
		adapter.notifyDataSetChanged();
		lv.onRefreshComplete();
	}
	
	public void onFilterClicked(View v) {
		lastFilter.setTextAppearance(this, R.style.normText);
		lastFilter = ((TextView) ((ViewGroup) v).getChildAt(1));
		lastFilter.setTextAppearance(this, R.style.boldText);

		if(v.getId() == R.id.filter_trash) {
	    	isTrashSelected = true;
	    	refreshData();
		} else {
			if(isTrashSelected) {
				isTrashSelected = false;
				refreshData();
			}
			if(v.getId() == R.id.filter_all) adapter.setFilter(null);
			if(v.getId() == R.id.filter_image) adapter.setFilter(CloudAppItem.Type.IMAGE);
			if(v.getId() == R.id.filter_bookmark) adapter.setFilter(CloudAppItem.Type.BOOKMARK);
			if(v.getId() == R.id.filter_text) adapter.setFilter(CloudAppItem.Type.TEXT);
			if(v.getId() == R.id.filter_archive) adapter.setFilter(CloudAppItem.Type.ARCHIVE);
			if(v.getId() == R.id.filter_audio) adapter.setFilter(CloudAppItem.Type.AUDIO);
			if(v.getId() == R.id.filter_video) adapter.setFilter(CloudAppItem.Type.VIDEO);
			if(v.getId() == R.id.filter_unknown) adapter.setFilter(CloudAppItem.Type.UNKNOWN);
		}
		
		adapter.notifyDataSetChanged();
	}
	
	public void clearCheckedBoxes() {
		
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

	class GetCAData extends Thread {
		
		public static final int CREATE_BOOKMARK = 0x000001;
		public static final int CREATE_BOOKMARKS = 0x000002;
		public static final int GET_ITEM = 0x000003;
		public static final int GET_ITEMS = 0x000004;
		public static final int DELETE_ITEMS = 0x000005;
		public static final int DELETE_ITEM = 0x000006;
		public static final int RECOVER_ITEM = 0x000007;
		public static final int SET_SECURITY = 0x000008;
		public static final int RENAME_ITEM = 0x000009;
		
		private int mode;
		private String[] params;
		private int itemCount = 0;

		public GetCAData(int mode, String... params) {
			this.mode = mode;
			this.params = params;
			mProgDiag.show();
		}

		@Override
		public void run() {
			try {
				switch (this.mode) {
				case CREATE_BOOKMARK:
					
					break;
					
				case DELETE_ITEMS:
					for (CloudAppItem cloudAppItem : adapter.getCheckedItems()) {
						mApp.api.delete(cloudAppItem);
						itemCount++;
					}
					mHandler.post(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(MainActivity.this, "Deleted " + itemCount + " items", Toast.LENGTH_SHORT).show();
							refreshData();
						}
					});
					break;
					
				case DELETE_ITEM:
					mApp.api.delete(adapter.getItem(Integer.parseInt(params[0])));
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(MainActivity.this, "Deleted 1 item", Toast.LENGTH_SHORT).show();
							refreshData();
						}
					});
					break;
					
				case RECOVER_ITEM:
					int number_recovered = 0;
					for (CloudAppItem cloudAppItem : adapter.getCheckedItems()) {
						mApp.api.delete(cloudAppItem);
						number_recovered++;
					}
					final int numberRecovered = number_recovered;
					mHandler.post(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(MainActivity.this, "Recovered " + numberRecovered + " items", Toast.LENGTH_SHORT).show();
							refreshData();
						}
					});
					break;
					
				case GET_ITEMS:
					page++;
					final List<CloudAppItem> items = mApp.api.getItems(
							page, 
							Integer.parseInt(params[0]), 
							(params[1].equals("null") ? null : CloudAppItem.Type.valueOf(params[1])), 
							Boolean.parseBoolean(params[2]), null);
					list.addAll(items);
					mHandler.post(new Runnable() {

						@Override
						public void run() {
							if(items.size() < Integer.parseInt(params[0])) isLastPage = true;
							
							showData((PullToRefreshListView) findViewById(R.id.data_list_view));
						}
					});
					break;
					
				case RENAME_ITEM:
					mApp.api.rename(adapter.getItem(Integer.parseInt(params[0])), params[1]);
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(MainActivity.this, "Renamed to " + params[1], Toast.LENGTH_SHORT).show();
							refreshData();
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
						mProgDiag.hide();
						new AlertDialog.Builder(MainActivity.this)
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
											}

										}).create().show();
					}
				});
			}
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					mProgDiag.hide();
				}
			});
			super.run();
		}

	}

}
