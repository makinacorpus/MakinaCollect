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

import com.WazaBe.HoloEverywhere.app.AlertDialog;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter; 
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.dialog.AboutUs;
import com.makina.collect.android.dialog.Help;
import com.makina.collect.android.preferences.PreferencesActivity;
import com.makina.collect.android.provider.InstanceProviderAPI;
import com.makina.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import com.makina.collect.android.utilities.Finish;
/**
 * Responsible for displaying all the valid instances in the instance directory.
 * 
 * @author Yaw Anokwa (yanokwa@gmail.com)
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class ActivitySaveForm extends SherlockListActivity implements SearchView.OnQueryTextListener{

    private static final boolean EXIT = true;
    private static final boolean DO_NOT_EXIT = false;
    private AlertDialog mAlertDialog;
    private Cursor c;
    private  SearchView mSearchView;
    
    @SuppressLint("NewApi")
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
    
    @SuppressLint("ResourceAsColor")
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    	setContentView(R.layout.activity_save_form);
        
    	Typeface typeFace = Typeface.createFromAsset(getAssets(),"fonts/avenir.ttc"); 
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    	getSupportActionBar().setTitle(getString(R.string.my_forms));
    	int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
    	TextView actionbarTitle = (TextView)findViewById(titleId);
    	actionbarTitle.setTextColor(getResources().getColor(R.color.actionbarTitleColorGris));
    	actionbarTitle.setTypeface(typeFace);
    	titleId = Resources.getSystem().getIdentifier("action_bar_subtitle", "id", "android");
    	TextView actionbarSubTitle = (TextView)findViewById(titleId);
    	actionbarSubTitle.setTextColor(getResources().getColor(R.color.actionbarTitleColorBlueSave));
    	actionbarSubTitle.setTypeface(typeFace);
    	getSupportActionBar().setSubtitle(getString(R.string.saved).toUpperCase());
    	
        
        /*TextView tv = (TextView) findViewById(R.id.status_text);
        tv.setVisibility(View.GONE);*/
        
        String selection = InstanceColumns.STATUS + " != ?";
        String[] selectionArgs = {InstanceProviderAPI.STATUS_SUBMITTED};
        String sortOrder = InstanceColumns.STATUS + " DESC, " + InstanceColumns.DISPLAY_NAME + " ASC";
        Cursor c = managedQuery(InstanceColumns.CONTENT_URI, null, selection, selectionArgs, sortOrder);

        String[] data = new String[] {InstanceColumns.DISPLAY_NAME, InstanceColumns.DISPLAY_SUBTEXT};
        int[] view = new int[] { R.id.text1, R.id.text2 };

        // render total instance view
        SimpleCursorAdapter instances =new SimpleCursorAdapter(this, R.layout.listview_item_save_form, c, data, view);
        setListAdapter(instances);
    }
    

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
    }


    /**
     * Stores the path of selected instance in the parent class and finishes.
     */
    @Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
        c = (Cursor) getListAdapter().getItem(position);
        startManagingCursor(c);
        Uri instanceUri =
            ContentUris.withAppendedId(InstanceColumns.CONTENT_URI,
                c.getLong(c.getColumnIndex(BaseColumns._ID)));

        Collect.getInstance().getActivityLogger().logAction(this, "onListItemClick", instanceUri.toString());

        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action))
        {
            // caller is waiting on a picked form
            setResult(RESULT_OK, new Intent().setData(instanceUri));
        }
        else
        {
            // the form can be edited if it is incomplete or if, when it was
            // marked as complete, it was determined that it could be edited
            // later.
            String status = c.getString(c.getColumnIndex(InstanceColumns.STATUS));
            String strCanEditWhenComplete = c.getString(c.getColumnIndex(InstanceColumns.CAN_EDIT_WHEN_COMPLETE));

            boolean canEdit = status.equals(InstanceProviderAPI.STATUS_INCOMPLETE) || Boolean.parseBoolean(strCanEditWhenComplete);
            if (!canEdit)
            {
            	createErrorDialog(getString(R.string.cannot_edit_completed_form), DO_NOT_EXIT);
            	return;
            }
            else
            {
            // caller wants to view/edit a form, so launch formentryactivity
            	Intent intent=new Intent(Intent.ACTION_EDIT, instanceUri);
            	Bundle bundle=new Bundle();
            	bundle.putLong("id", c.getLong(c.getColumnIndex(BaseColumns._ID)));
            	intent.putExtras(bundle);
            	startActivity(intent);
            }
        }
        //TODO 
        //getActivity().finish();
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
	public boolean onQueryTextChange(String newText) {
		// TODO Auto-generated method stub
		String selection = InstanceColumns.DISPLAY_NAME+" LIKE '%"+newText+"%' AND "+InstanceColumns.STATUS + " != ?";
        String[] selectionArgs = {InstanceProviderAPI.STATUS_SUBMITTED};
        String sortOrder = InstanceColumns.STATUS + " DESC, " + InstanceColumns.DISPLAY_NAME + " ASC";
        Cursor c = managedQuery(InstanceColumns.CONTENT_URI, null, selection, selectionArgs, sortOrder);

        String[] data = new String[] {InstanceColumns.DISPLAY_NAME, InstanceColumns.DISPLAY_SUBTEXT};
        int[] view = new int[] { R.id.text1, R.id.text2 };

        // render total instance view
        SimpleCursorAdapter instances =new SimpleCursorAdapter(this, R.layout.listview_item_save_form, c, data, view);
        setListAdapter(instances);
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		// TODO Auto-generated method stub
		return false;
	}  


}
