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
import com.WazaBe.HoloEverywhere.app.AlertDialog;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.dialog.AboutUs;
import com.makina.collect.android.dialog.Help;
import com.makina.collect.android.listeners.DeleteInstancesListener;
import com.makina.collect.android.preferences.PreferencesActivity;
import com.makina.collect.android.provider.InstanceProviderAPI;
import com.makina.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import com.makina.collect.android.receivers.NetworkReceiver;
import com.makina.collect.android.tasks.DeleteInstancesTask;
import com.makina.collect.android.utilities.Finish;

/**
 * Responsible for displaying all the valid forms in the forms directory. Stores
 * the path to selected form for use by {@link ActivityMainMenu}.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */

@SuppressLint("NewApi")
public class ActivitySendForm extends SherlockListActivity implements DeleteInstancesListener,SearchView.OnQueryTextListener  {

	private static final String t = "InstanceUploaderList";
	
	private static final String BUNDLE_SELECTED_ITEMS_KEY = "selected_items";
	private static final String BUNDLE_TOGGLED_KEY = "toggled";

	private static final int MENU_PREFERENCES = Menu.FIRST;
	private static final int INSTANCE_UPLOADER = 0;

	//private boolean mShowUnsent = true;
	private SimpleCursorAdapter mInstances;
	private ArrayList<Long> mSelected = new ArrayList<Long>();
	private boolean mRestored = false;
	private boolean mToggled = false;
	private AlertDialog mAlertDialog;
	DeleteInstancesTask mDeleteInstancesTask = null;
	private  SearchView mSearchView;
	private TextView textView_pannier;

	public Cursor getAllCursor(String condition_search) {
		// get all complete or failed submission instances
		String selection = InstanceColumns.DISPLAY_NAME+" LIKE '%"+condition_search+"%' AND ("+InstanceColumns.STATUS + "=? or "+ InstanceColumns.STATUS + "=? or " + InstanceColumns.STATUS+ "=? )";
		String selectionArgs[] = { InstanceProviderAPI.STATUS_COMPLETE,
				InstanceProviderAPI.STATUS_SUBMISSION_FAILED,
				InstanceProviderAPI.STATUS_SUBMITTED };
		String sortOrder = InstanceColumns.DISPLAY_NAME + " ASC";
		Cursor c = managedQuery(InstanceColumns.CONTENT_URI, null, selection,selectionArgs, sortOrder);
		return c;
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.menu_activity_send_form, menu);
        
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        return true;
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
	        case R.id.menu_send:
	        	uploadInstancesOption();
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
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_form);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.send_data));
        textView_pannier=(TextView)findViewById(R.id.textView_pannier);
        RelativeLayout relativeLayout_check_all=(RelativeLayout) findViewById(R.id.check_all);
        relativeLayout_check_all.setOnClickListener(new View.OnClickListener()
        {
			@Override
			public void onClick(View arg0)
			{
				if (mToggled)
				{
					selectAllOption();
					ImageView imageView_check_all=(ImageView)findViewById(R.id.imageView_check_all);
					imageView_check_all.setImageResource(R.drawable.case_off);
					TextView textView_check_all=(TextView)findViewById(R.id.textView_check_all);
					textView_check_all.setText("TOUT SÉLECTIONNER");
					
				}
				else
				{
					selectAllOption();
					ImageView imageView_check_all=(ImageView)findViewById(R.id.imageView_check_all);
					imageView_check_all.setImageResource(R.drawable.case_on);
					TextView textView_check_all=(TextView)findViewById(R.id.textView_check_all);
					textView_check_all.setText("TOUT DÉSÉLECTIONNER");
					
				}
			}
		});
       

		
	}
	
	

	@Override
	public void onResume() {
		Cursor c = getAllCursor("");

		String[] data = new String[] { InstanceColumns.DISPLAY_NAME,InstanceColumns.DISPLAY_SUBTEXT };
		int[] view = new int[] { R.id.text1, R.id.text2 };

		// render total instance view
		mInstances = new SimpleCursorAdapter(this,R.layout.listview_item_send_form, c, data, view);
		
		setListAdapter(mInstances);
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		getListView().setItemsCanFocus(false);

		

		// if current activity is being reinitialized due to changing
		// orientation restore all check
		// marks for ones selected
		if (mRestored)
		{
			ListView ls = getListView();
			
			for (long id : mSelected)
			{
				for (int pos = 0; pos < ls.getCount(); pos++)
				{
					if (id == ls.getItemIdAtPosition(pos)) {
						ls.setItemChecked(pos, true);
						break;
					}
				}

			}
			mRestored = false;
		}
		 
		LinearLayout linearLayout_footer=(LinearLayout) findViewById(R.id.linearLayout_footer);
        if ( (getListView()==null) || (getListView().getCount()==0) )
        	linearLayout_footer.setVisibility(View.GONE);
        else
        	linearLayout_footer.setVisibility(View.VISIBLE);
		super.onResume();
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

	private void uploadSelectedFiles() {
		// send list of _IDs.
		long[] instanceIDs = new long[mSelected.size()];
		for (int i = 0; i < mSelected.size(); i++) {
			instanceIDs[i] = mSelected.get(i);
		}

		Intent i = new Intent(this, InstanceUploaderActivity.class);
		i.putExtra(ActivityForm.KEY_INSTANCES, instanceIDs);
		startActivityForResult(i, INSTANCE_UPLOADER);
	}
	
	protected void selectAllOption (){
		// toggle selections of items to all or none
		ListView ls = getListView();
		mToggled = !mToggled;

		Collect.getInstance()
				.getActivityLogger()
				.logAction(this, "toggleButton",
		Boolean.toString(mToggled));
		// remove all items from selected list
		mSelected.clear();
		for (int pos = 0; pos < ls.getCount(); pos++) {
			ls.setItemChecked(pos, mToggled);
			// add all items if mToggled sets to select all
			if (mToggled) mSelected.add(ls.getItemIdAtPosition(pos));
		}
		textView_pannier.setText(mSelected.size()+" formulaire(s) sélectionné(s)");
	}
	
	protected void uploadInstancesOption (){
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

		if (NetworkReceiver.running == true) {
			//another upload is already running
			Toast.makeText(this,"Background send running, please try again shortly",Toast.LENGTH_SHORT).show();
		} else if (ni == null || !ni.isConnected()) {
			//no network connection
			Collect.getInstance().getActivityLogger()
					.logAction(this, "uploadButton", "noConnection");

			Toast.makeText(this,R.string.no_connection, Toast.LENGTH_SHORT).show();
		} else {
			Collect.getInstance()
					.getActivityLogger()
					.logAction(this, "uploadButton",
							Integer.toString(mSelected.size()));

			if (mSelected.size() > 0) {
				// items selected
				uploadSelectedFiles();
				mToggled = false;
				mSelected.clear();
				ActivitySendForm.this.getListView().clearChoices();
			} else {
				// no items selected
				Toast.makeText(getApplicationContext(),
						getString(R.string.noselect_error),
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void createPreferencesMenu() {
		Intent i = new Intent(this, PreferencesActivity.class);
		startActivity(i);
	}
	
	protected void delete () {
		Collect.getInstance().getActivityLogger().logAction(this, "deleteButton", Integer.toString(mSelected.size()));
		if (mSelected.size() > 0) {
			createDeleteInstancesDialog();
		} else {
			Toast.makeText(getApplicationContext(),
					R.string.noselect_error, Toast.LENGTH_SHORT).show();
		}
	}
	
	private void createDeleteInstancesDialog() {
        Collect.getInstance().getActivityLogger().logAction(this, "createDeleteInstancesDialog", "show");

		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setTitle(getString(R.string.delete_file));
		mAlertDialog.setMessage(getString(R.string.delete_confirm,
				mSelected.size()));
		DialogInterface.OnClickListener dialogYesNoListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int i) {
				switch (i) {
				case DialogInterface.BUTTON1: // delete
			    	Collect.getInstance().getActivityLogger().logAction(this, "createDeleteInstancesDialog", "delete");
					deleteSelectedInstances();
					break;
				case DialogInterface.BUTTON2: // do nothing
			    	Collect.getInstance().getActivityLogger().logAction(this, "createDeleteInstancesDialog", "cancel");
					break;
				}
			}
		};
		mAlertDialog.setCancelable(false);
		mAlertDialog.setButton(getString(R.string.delete_yes),
				dialogYesNoListener);
		mAlertDialog.setButton2(getString(R.string.delete_no),
				dialogYesNoListener);
		mAlertDialog.show();
	}
	
	private void deleteSelectedInstances() {
		if (mDeleteInstancesTask == null) {
			mDeleteInstancesTask = new DeleteInstancesTask();
			mDeleteInstancesTask.setContentResolver(getContentResolver());
			mDeleteInstancesTask.setDeleteListener(this);
			mDeleteInstancesTask.execute(mSelected.toArray(new Long[mSelected
					.size()]));
		} else {
			Toast.makeText(this, getString(R.string.file_delete_in_progress),
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		// get row id from db
		Cursor c = (Cursor) getListAdapter().getItem(position);
		long k = c.getLong(c.getColumnIndex(BaseColumns._ID));

		Collect.getInstance().getActivityLogger().logAction(this, "onListItemClick", Long.toString(k));

		// add/remove from selected list
		if (mSelected.contains(k))
			mSelected.remove(k);
		else
			mSelected.add(k);
		
		textView_pannier.setText(mSelected.size()+" formulaire(s) sélectionné(s)");
		
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		long[] selectedArray = new long[mSelected.size()];
		for (int i = 0; i < mSelected.size(); i++)
			selectedArray[i] = mSelected.get(i);
		outState.putLongArray(BUNDLE_SELECTED_ITEMS_KEY, selectedArray);
		outState.putBoolean(BUNDLE_TOGGLED_KEY, mToggled);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (resultCode == RESULT_CANCELED) {
			return;
		}
		switch (requestCode) {
		// returns with a form path, start entry
		case INSTANCE_UPLOADER:
			if (intent.getBooleanExtra(ActivityForm.KEY_SUCCESS, false)) {
				mSelected.clear();
				getListView().clearChoices();
				/*if (mInstances.isEmpty()) {
					getActivity().finish();
				}*/
			}
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	public void deleteComplete(int deletedInstances) {
		Log.i(t, "Delete instances complete");
        Collect.getInstance().getActivityLogger().logAction(this, "deleteComplete", Integer.toString(deletedInstances));
		if (deletedInstances == mSelected.size()) {
			// all deletes were successful
			Toast.makeText(this,getString(R.string.file_deleted_ok, deletedInstances),
					Toast.LENGTH_SHORT).show();
		} else {
			// had some failures
			Log.e(t, "Failed to delete "
					+ (mSelected.size() - deletedInstances) + " instances");
			Toast.makeText(this,getString(R.string.file_deleted_error, mSelected.size()
							- deletedInstances, mSelected.size()),
					Toast.LENGTH_LONG).show();
		}
		mDeleteInstancesTask = null;
		mSelected.clear();
		getListView().clearChoices(); // doesn't unset the checkboxes
		for ( int i = 0 ; i < getListView().getCount() ; ++i ) {
			getListView().setItemChecked(i, false);
		}
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		// TODO Auto-generated method stub
		Cursor c = getAllCursor(newText);

		String[] data = new String[] { InstanceColumns.DISPLAY_NAME,InstanceColumns.DISPLAY_SUBTEXT };
		int[] view = new int[] { R.id.text1, R.id.text2 };

		// render total instance view
		mInstances = new SimpleCursorAdapter(this,R.layout.listview_item_send_form, c, data, view);
		
		setListAdapter(mInstances);
         
        ListView ls = getListView();
        for (int i=0;i<ls.getCount();i++)
        {
        	if (mSelected.contains(ls.getItemIdAtPosition(i)))
        		getListView().setItemChecked(i, true);
        }
    	
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		// TODO Auto-generated method stub
		return false;
	}

}
