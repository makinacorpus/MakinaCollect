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

package com.makina.collect.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.makina.collect.R;
import com.makina.collect.activity.AbstractBaseActivity;
import com.makina.collect.adapters.FormsListAdapter;
import com.makina.collect.dialog.DialogAboutUs;
import com.makina.collect.dialog.DialogHelpWithConfirmation;
import com.makina.collect.model.Form;
import com.makina.collect.preferences.ActivityPreferences;
import com.makina.collect.provider.InstanceProvider;
import com.makina.collect.provider.InstanceProviderAPI;
import com.makina.collect.provider.InstanceProviderAPI.InstanceColumns;
import com.makina.collect.utilities.Finish;

import java.util.ArrayList;
import java.util.List;
/**
 * Responsible for displaying all the valid instances in the instance directory.
 * 
 * @author Yaw Anokwa (yanokwa@gmail.com)
 * @author Carl Hartung (carlhartung@gmail.com)
 */
@Deprecated
@SuppressLint("NewApi")
public class ActivitySaveForm
        extends AbstractBaseActivity
        implements SearchView.OnQueryTextListener{

    protected ListView mList;

    private AlertDialog mAlertDialog;
    private FormsListAdapter instances;
    private final int RESULT_PREFERENCES = 1;
    private SearchView mSearchView;
    private List<Form> forms;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        final MenuInflater menuInflater = getMenuInflater();

        menuInflater.inflate(R.menu.search,
                             menu);
        menuInflater.inflate(R.menu.settings,
                             menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        // Handle action buttons for all fragments

        switch (item.getItemId()) {
            case android.R.id.home:
                Finish.finishHome();
                return true;
            case R.id.menu_settings:
                startActivityForResult((new Intent(this,
                                                   ActivityPreferences.class)),
                                       RESULT_PREFERENCES);
                return true;
            case R.id.menu_help:
                Intent mIntent = new Intent(this,
                                            ActivityHelp.class);
                Bundle mBundle = new Bundle();
                mBundle.putInt("position",
                               2);
                mIntent.putExtras(mBundle);
                startActivity(mIntent);
                return true;
            case R.id.menu_about_us:
                DialogAboutUs.aboutUs(this);
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
        
    	Finish.activitySaveForm=this;
    	
    	if (!getSharedPreferences("session", MODE_PRIVATE).getBoolean("help_saved", false))
    		DialogHelpWithConfirmation.helpDialog(this, getString(R.string.help_title3),getString(R.string.help_save));
    	
    	getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_title_layout_save_form, null);
        getSupportActionBar().setCustomView(v);

        mList = (ListView) findViewById(android.R.id.list);

        loadListView();

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent,
                                    View view,
                                    int position,
                                    long id) {
                Uri instanceUri = ContentUris.withAppendedId(InstanceColumns.CONTENT_URI,
                                                             forms.get(position)
                                                                  .getId());

                Intent intent = new Intent(Intent.ACTION_EDIT,
                                           instanceUri);
                Bundle bundle = new Bundle();
                bundle.putLong("id",
                               forms.get(position)
                                    .getId());
                intent.putExtras(bundle);
                startActivity(intent);

                //TODO
                //getActivity().finish();
            }
        });

        mList.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0,
                                           View arg1,
                                           int position,
                                           long arg3) {
                // TODO Auto-generated method stub
                createDialogDelete(position);
                return false;
            }
        });
    }
    
    private void createDialogDelete(final int position)
    {
    	//final Cursor c=instances.getCursor();
    	final Form formDeleted=forms.get(position);
		forms.remove(position);
    	instances.notifyDataSetChanged();
		AlertDialog.Builder adb = new AlertDialog.Builder(ActivitySaveForm.this);
		adb.setTitle(getString(R.string.delete));
		adb.setMessage(getString(R.string.delete_confirmation,formDeleted.getName()));
		adb.setIconAttribute(R.attr.dialog_icon_delete);
		adb.setNegativeButton(getString(android.R.string.cancel),new AlertDialog.OnClickListener()
        {
            public void onClick(DialogInterface dialog,int which)
            {
            	forms.add(position, formDeleted);
            	instances.notifyDataSetChanged();
            }
        });
		adb.setPositiveButton(getString(android.R.string.yes), new AlertDialog.OnClickListener()
		{
			public void onClick(DialogInterface dialog,int which)
			{
				InstanceProvider.deleteInstance(formDeleted.getId());
			}
		});
		adb.show();
    }
    private void loadListView()
    {
    	String selectionArgs[] = { InstanceProviderAPI.STATUS_INCOMPLETE};
        String selection = InstanceColumns.STATUS + " = ?";
        //String[] selectionArgs = {InstanceProviderAPI.STATUS_SUBMITTED};
        String sortOrder = InstanceColumns.STATUS + " DESC, " + InstanceColumns.DISPLAY_NAME + " ASC";
        Cursor c = managedQuery(InstanceColumns.CONTENT_URI, null, selection, selectionArgs, sortOrder);
        if(c.getCount()> 0)
		{
			forms=new ArrayList<Form>();
			while (c.moveToNext())
				forms.add(new Form(c.getInt(c.getColumnIndex(BaseColumns._ID)),c.getString(c.getColumnIndex(InstanceColumns.JR_FORM_ID)),c.getString(c.getColumnIndex(InstanceColumns.DISPLAY_NAME)), c.getString(c.getColumnIndex(InstanceColumns.DISPLAY_SUBTEXT)),c.getString(c.getColumnIndex(InstanceColumns.INSTANCE_FILE_PATH)),""));
			instances=new FormsListAdapter(this, forms);
            mList.setAdapter(instances);
		}
    }
    
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	if (requestCode == RESULT_PREFERENCES)
    	{
    		Intent i = getIntent();
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
    	}
    }

    /**
     * Creates a dialog with the given message. Will exit the activity when the user preses "ok" if
     * shouldExit is set to true.
     * 
     * @param errorMsg
     * @param shouldExit
     */
	private void createErrorDialog(String errorMsg, final boolean shouldExit) {
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIconAttribute(R.attr.dialog_icon_info);
        mAlertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON1:
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
        mList.setAdapter(instances);

		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
    	InputMethodManager imm = (InputMethodManager)getSystemService( Context.INPUT_METHOD_SERVICE);
    	imm.hideSoftInputFromWindow(mSearchView.getWindowToken(),
                                    0);
        return false;
    }

    /*
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
    	// TODO Auto-generated method stub
    	super.onConfigurationChanged(newConfig);
    	Theme.changeTheme(this);
    	LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View v;
    	if(newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE)
    		v = inflator.inflate(R.layout.actionbar_title_layout_save_form_land, null);
        else
        	v = inflator.inflate(R.layout.actionbar_title_layout_save_form, null);
        getSupportActionBar().setCustomView(v);
    }
    */
}
