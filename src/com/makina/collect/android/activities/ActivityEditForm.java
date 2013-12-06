/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.makina.collect.android.activities;

import java.util.ArrayList;
import java.util.List;

import com.WazaBe.HoloEverywhere.app.AlertDialog;
import com.WazaBe.HoloEverywhere.widget.View;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.dialog.AboutUs;
import com.makina.collect.android.dialog.Help;
import com.makina.collect.android.listeners.DiskSyncListener;
import com.makina.collect.android.preferences.PreferencesActivity;
import com.makina.collect.android.provider.FormsProviderAPI.FormsColumns;
import com.makina.collect.android.swipelistview.adapters.PackageAdapter;
import com.makina.collect.android.swipelistview.adapters.FormItem;
import com.makina.collect.android.swipelistview.utils.SettingsManager;
import com.makina.collect.android.tasks.DiskSyncTask;
import com.makina.collect.android.utilities.Finish;
import com.makina.collect.android.utilities.VersionHidingCursorAdapter;

/**
 * Responsible for displaying all the valid forms in the forms directory. Stores the path to
 * selected form for use by {@link ActivityMainMenu}.
 * 
 * @author Yaw Anokwa (yanokwa@gmail.com)
 * @author Carl Hartung (carlhartung@gmail.com)
 */
@SuppressLint("NewApi")
public class ActivityEditForm extends SherlockActivity implements DiskSyncListener, SearchView.OnQueryTextListener {

    private static final String t = "FormChooserList";
    private static final boolean EXIT = true;
    private static final String syncMsgKey = "syncmsgkey";

    private DiskSyncTask mDiskSyncTask;

    private AlertDialog mAlertDialog;
    
    private String statusText;
    private SearchView mSearchView;
    private PackageAdapter adapter;
    private List<FormItem> data;

    private SwipeListView swipeListView;
    private ImageView empty_list;
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_form);
        
        setTitle(getString(R.string.enter_data));
        empty_list=(ImageView)findViewById(R.id.empty_list);
        
        
        data = new ArrayList<FormItem>();

        

        swipeListView = (SwipeListView) findViewById(R.id.listView);
        if (Build.VERSION.SDK_INT >= 11) {
            swipeListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            swipeListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position,
                                                      long id, boolean checked) {
                    mode.setTitle("Selected (" + swipeListView.getCountSelected() + ")");
                }
             

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    swipeListView.unselectedChoiceStates();
                }

				@Override
				public boolean onActionItemClicked(ActionMode arg0,
						android.view.MenuItem arg1) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public boolean onCreateActionMode(ActionMode mode,
						android.view.Menu menu) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public boolean onPrepareActionMode(ActionMode mode,
						android.view.Menu menu) {
					// TODO Auto-generated method stub
					return false;
				}
            });
        }

        swipeListView.setSwipeListViewListener(new BaseSwipeListViewListener() {
            @Override
            public void onOpened(int position, boolean toRight) {
            }

            @Override
            public void onClosed(int position, boolean fromRight) {
            }

            @Override
            public void onListChanged() {
            }

            @Override
            public void onMove(int position, float x) {
            }

            @Override
            public void onStartOpen(int position, int action, boolean right) {
                Log.d("swipe", String.format("onStartOpen %d - action %d", position, action));
            }

            @Override
            public void onStartClose(int position, boolean right) {
                Log.d("swipe", String.format("onStartClose %d", position));
            }

            @Override
            public void onClickFrontView(int position) {
                Log.d("swipe", String.format("onClickFrontView %d", position));
            }

            @Override
            public void onClickBackView(int position) {
                Log.d("swipe", String.format("onClickBackView %d", position));
            }

            @Override
            public void onDismiss(int[] reverseSortedPositions) {
                for (int position : reverseSortedPositions) {
                    data.remove(position);
                }
                adapter.notifyDataSetChanged();
            }

        });
        
        //home button leads back to home
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        String sortOrder = FormsColumns.DISPLAY_NAME + " ASC, " + FormsColumns.JR_VERSION + " DESC";
        Cursor c = managedQuery(FormsColumns.CONTENT_URI, null, null, null, sortOrder);
        
        while (c.moveToNext())
		{
        	FormItem item = new FormItem();
            item.setName(c.getString(12));
            item.setDate("");
            item.setVersion(getString(R.string.version)+" ");
            data.add(item);
		}
        adapter = new PackageAdapter(this, data);
        
        swipeListView.setAdapter(adapter);
        
        reload();
        
        /*swipeListView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(android.view.View arg0) {
				
				long idFormsTable = ((SimpleCursorAdapter) getListAdapter()).getItemId(position);
		        Uri formUri = ContentUris.withAppendedId(FormsColumns.CONTENT_URI, idFormsTable);

				Collect.getInstance().getActivityLogger().logAction(this, "onListItemClick", formUri.toString());

		        String action = getIntent().getAction();
		        if (Intent.ACTION_PICK.equals(action)) {
		            // caller is waiting on a picked form
		            setResult(RESULT_OK, new Intent().setData(formUri));
		        } else {
		            // caller wants to view/edit a form, so launch formentryactivity
		        	Intent i = new Intent(Intent.ACTION_EDIT, formUri);
		        	i.putExtra("newForm", true);
		            startActivity(i);
		        }
				
			}
		});*/
        if (swipeListView.getCount()!=0)
        	empty_list.setVisibility(View.GONE);

        //reload();

        /*String[] data = new String[]{ FormsColumns.DISPLAY_NAME, FormsColumns.DISPLAY_SUBTEXT, FormsColumns.JR_VERSION};
        int[] view = new int[] { R.id.text1, R.id.text2, R.id.text3};

        // render total instance view
        SimpleCursorAdapter instances = new VersionHidingCursorAdapter(FormsColumns.JR_VERSION, this, R.layout.listview_item_edit_form, c, data, view);
        setListAdapter(instances);*/

        if (savedInstanceState != null && savedInstanceState.containsKey(syncMsgKey)) {
            statusText = savedInstanceState.getString(syncMsgKey);
        }

        // DiskSyncTask checks the disk for any forms not already in the content provider
        // that is, put here by dragging and dropping onto the SDCard
        //mDiskSyncTask = (DiskSyncTask) getActivity().getLastNonConfigurationInstance();
        if (mDiskSyncTask == null) {
            Log.i(t, "Starting new disk sync task");
            mDiskSyncTask = new DiskSyncTask();
            mDiskSyncTask.setDiskSyncListener(this);
            mDiskSyncTask.execute((Void[]) null);
        }
        
        
        /*getListView().setLongClickable(true);
        getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
             public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
             //Do some
                 return true;
             }
         });*/
    }
    
    private void reload() {
        SettingsManager settings = SettingsManager.getInstance();
        swipeListView.setSwipeMode(settings.getSwipeMode());
        swipeListView.setSwipeActionLeft(settings.getSwipeActionLeft());
        swipeListView.setSwipeActionRight(settings.getSwipeActionRight());
        swipeListView.setOffsetLeft(convertDpToPixel(settings.getSwipeOffsetLeft()));
        swipeListView.setOffsetRight(convertDpToPixel(settings.getSwipeOffsetRight()));
        swipeListView.setAnimationTime(settings.getSwipeAnimationTime());
        swipeListView.setSwipeOpenOnLongPress(settings.isSwipeOpenOnLongPress());
    }

    public int convertDpToPixel(float dp) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int) px;
    }

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.menu_activity_edit_form, menu);
        
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText)
    {
    	String sortOrder = FormsColumns.DISPLAY_NAME + " ASC, " + FormsColumns.JR_VERSION + " DESC";
    	String condition=FormsColumns.DISPLAY_NAME+" LIKE '%"+newText+"%'";
    	Cursor c = managedQuery(FormsColumns.CONTENT_URI, null, condition, null, sortOrder);
        
        String[] data = new String[]{ FormsColumns.DISPLAY_NAME, FormsColumns.DISPLAY_SUBTEXT, FormsColumns.JR_VERSION};
        int[] view = new int[] { R.id.text1, R.id.text2, R.id.text3};

        // render total instance view
        SimpleCursorAdapter instances = new VersionHidingCursorAdapter(FormsColumns.JR_VERSION, this, R.layout.listview_item_edit_form, c, data, view);
        //setListAdapter(instances);
        
        return false;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        // Handle action buttons for all fragments
    	switch(item.getItemId())
    	{
	        case android.R.id.home:
	        	finish();
	        return true;
	        case R.id.menu_settings:
	        	startActivity(new Intent(this, PreferencesActivity.class));
	        	return true;
	        case R.id.menu_help:
	        	Help.helpDialog(getApplicationContext(), 0);
	        	return true;
	        case R.id.menu_about_us:
	        	AboutUs.aboutUs(this);
	        	return true;
	        case R.id.menu_exit:
	        	Finish.finish();
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }

	@Override
	public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(syncMsgKey, statusText);
    }


    /**
     * Stores the path of selected form and finishes.
     */
	
	/*@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
        // get uri to form
    	long idFormsTable = ((SimpleCursorAdapter) getListAdapter()).getItemId(position);
        Uri formUri = ContentUris.withAppendedId(FormsColumns.CONTENT_URI, idFormsTable);

		Collect.getInstance().getActivityLogger().logAction(this, "onListItemClick", formUri.toString());

        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action)) {
            // caller is waiting on a picked form
            setResult(RESULT_OK, new Intent().setData(formUri));
        } else {
            // caller wants to view/edit a form, so launch formentryactivity
        	Intent i = new Intent(Intent.ACTION_EDIT, formUri);
        	i.putExtra("newForm", true);
            startActivity(i);
        }
        
        //TODO  
        //getActivity().finish();
    }*/
    
    //TODO Back to previous task
    /*public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case android.R.id.home:
	            // This is called when the Home (Up) button is pressed
	            // in the Action Bar.
	            Intent parentActivityIntent = new Intent(this, MainMenuActivity.class);
	            parentActivityIntent.addFlags(
	                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                    Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(parentActivityIntent);
	            finish();
	            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/

    @Override
	public void onResume() {
        mDiskSyncTask.setDiskSyncListener(this);
        super.onResume();

        if (mDiskSyncTask.getStatus() == AsyncTask.Status.FINISHED) {
        	SyncComplete(mDiskSyncTask.getStatusMessage());
        }
    }


    @Override
	public void onPause() {
        mDiskSyncTask.setDiskSyncListener(null);
        super.onPause();
    }


    @Override
	public void onStart() {
    	super.onStart();
		Collect.getInstance().getActivityLogger().logOnStart(this); 
    }
    
    @Override
	public void onStop() {
		Collect.getInstance().getActivityLogger().logOnStop(this); 
    	super.onStop();
    }
    
    
    /**
     * Called by DiskSyncTask when the task is finished
     */
    @Override
    public void SyncComplete(String result) {
        Log.i(t, "disk sync task complete");
        statusText = result;
    }


    /**
     * Creates a dialog with the given message. Will exit the activity when the user preses "ok" if
     * shouldExit is set to true.
     * 
     * @param errorMsg
     * @param shouldExit
     */
    private void createErrorDialog(String errorMsg, final boolean shouldExit) {

    	Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog", "show");

        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
        mAlertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON1:
                    	Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog", 
                    			shouldExit ? "exitApplication" : "OK");
                        if (shouldExit) {
                        	finish();
                        }
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), errorListener);
        mAlertDialog.show();
    }

	@Override
	public boolean onQueryTextSubmit(String query) {
		// TODO Auto-generated method stub
		return false;
	}



}