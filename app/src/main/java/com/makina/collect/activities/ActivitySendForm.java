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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.makina.collect.R;
import com.makina.collect.activity.AbstractBaseActivity;
import com.makina.collect.adapters.SendFormsListAdapter;
import com.makina.collect.application.Collect;
import com.makina.collect.dialog.DialogAboutUs;
import com.makina.collect.dialog.DialogHelpWithConfirmation;
import com.makina.collect.listeners.DeleteInstancesListener;
import com.makina.collect.listeners.InstanceUploaderListener;
import com.makina.collect.model.Form;
import com.makina.collect.preferences.ActivityPreferences;
import com.makina.collect.provider.InstanceProvider;
import com.makina.collect.provider.InstanceProviderAPI;
import com.makina.collect.provider.InstanceProviderAPI.InstanceColumns;
import com.makina.collect.receivers.NetworkReceiver;
import com.makina.collect.tasks.DeleteInstancesTask;
import com.makina.collect.tasks.InstanceUploaderTask;
import com.makina.collect.utilities.Finish;
import com.makina.collect.utilities.WebUtils;
import com.makina.collect.views.CroutonView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Responsible for displaying all the valid forms in the forms directory. Stores
 * the path to selected form for use by ActivityMainMenu.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */

@SuppressLint("NewApi")
public class ActivitySendForm extends AbstractBaseActivity
        implements DeleteInstancesListener,SearchView.OnQueryTextListener, InstanceUploaderListener  {

	private static final String t = "InstanceUploaderList";
	
	private static final String BUNDLE_SELECTED_ITEMS_KEY = "selected_items";
	private static final String BUNDLE_TOGGLED_KEY = "toggled";

	protected ListView mList;

	private ProgressDialog mProgressDialog;

	private String mAlertMsg;

	//private boolean mShowUnsent = true;
	private SendFormsListAdapter mInstances;
	 private List<Form> forms;
	private ArrayList<Long> mSelected = new ArrayList<>();
	private boolean mRestored = false;
	private boolean mToggled = false;
	private AlertDialog mAlertDialog;
	DeleteInstancesTask mDeleteInstancesTask = null;
	private TextView textView_pannier;
	private final int PROGRESS_DIALOG=1;
	
	private HashMap<String, String> mUploadedInstances;
	private final static int AUTH_DIALOG = 2;
	private String mUrl;
	private final static String AUTH_URI = "auth";
    private static final String ALERT_MSG = "alertmsg";
    private static final String ALERT_SHOWING = "alertshowing";
    private boolean mAlertShowing;
    private long[] instanceIDs;
    private Long[] mInstancesToSend;
    private InstanceUploaderTask mInstanceUploaderTask ;
    private final int RESULT_PREFERENCES=1;
    private SearchView mSearchView;

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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.search,
                             menu);
        menuInflater.inflate(R.menu.form_send,
                             menu);
        menuInflater.inflate(R.menu.settings,
                             menu);

        mUploadedInstances = new HashMap<>();

        Finish.activitySendForm = this;

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
            case R.id.menu_send:
                uploadInstancesOption();
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
                               3);
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

    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_send_form);
        
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_title_layout_send_form,
                                  null);
        getSupportActionBar().setCustomView(v);

		mList = (ListView) findViewById(android.R.id.list);

        mProgressDialog = new ProgressDialog(this);
    	
    	if (!getSharedPreferences("session", MODE_PRIVATE).getBoolean("help_send", false))
    		DialogHelpWithConfirmation.helpDialog(this, getString(R.string.help_title4),getString(R.string.help_send));
    	
        textView_pannier=(TextView)findViewById(R.id.textView_pannier);
        RelativeLayout relativeLayout_check_all=(RelativeLayout) findViewById(R.id.check_all);
        relativeLayout_check_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mToggled) {
                    selectAllOption();
                    ImageView imageView_check_all = (ImageView) findViewById(R.id.imageView_check_all);
                    imageView_check_all.setImageResource(R.drawable.case_off);
                    TextView textView_check_all = (TextView) findViewById(R.id.textView_check_all);
                    textView_check_all.setText(getString(R.string.select_all));

                }
                else {
                    selectAllOption();
                    ImageView imageView_check_all = (ImageView) findViewById(R.id.imageView_check_all);
                    imageView_check_all.setImageResource(R.drawable.case_on);
                    TextView textView_check_all = (TextView) findViewById(R.id.textView_check_all);
                    textView_check_all.setText(getString(R.string.deselect_all));

                }
            }
        });
       

     // get any simple saved state...
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ALERT_MSG)) {
                mAlertMsg = savedInstanceState.getString(ALERT_MSG);
            }
            if (savedInstanceState.containsKey(ALERT_SHOWING)) {
                mAlertShowing = savedInstanceState.getBoolean(ALERT_SHOWING, false);
            }

            mUrl = savedInstanceState.getString(AUTH_URI);
        }

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

        mList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> a,
                                    View v,
                                    int position,
                                    long id) {
                // get row id from db
                long k = forms.get(position)
                              .getId();

                // add/remove from selected list
                if (mSelected.contains(k))
                    mSelected.remove(k);
                else
                    mSelected.add(k);

                if (mSelected.size() == 0)
                    textView_pannier.setText(getString(R.string.no_form_selected));
                else if (mSelected.size() == 1)
                    textView_pannier.setText(getString(R.string.form_selected));
                else if (mSelected.size() > 1)
                    textView_pannier.setText(mSelected.size() + " " + getString(R.string.forms_selected));

            }
        });
	}
	
	private void createDialogDelete(final int position)
    {
		final Form formDeleted=forms.get(position);
		forms.remove(position);
    	mInstances.notifyDataSetChanged();
		AlertDialog.Builder adb = new AlertDialog.Builder(ActivitySendForm.this);
		adb.setTitle(getString(R.string.delete));
		adb.setMessage(getString(R.string.delete_confirmation,
                                 formDeleted.getName()));
		adb.setIconAttribute(R.attr.dialog_icon_delete);
		adb.setNegativeButton(getString(android.R.string.cancel),new AlertDialog.OnClickListener()
        {
            public void onClick(DialogInterface dialog,int which)
            {
            	forms.add(position, formDeleted);
            	mInstances.notifyDataSetChanged();
            }
        });
		adb.setPositiveButton(getString(android.R.string.yes), new AlertDialog.OnClickListener()
		{
			public void onClick(DialogInterface dialog,int which)
			{
				InstanceProvider.deleteInstance(formDeleted.getId());
				loadListView();
			}
		});
		adb.show();
    }

	private void loadListView()
	{
		Cursor c = getAllCursor("");

		// render total instance view
		if(c.getCount()> 0)
		{
			forms=new ArrayList<Form>();
			while (c.moveToNext())
				forms.add(new Form(c.getInt(c.getColumnIndex(BaseColumns._ID)),c.getString(c.getColumnIndex(InstanceColumns.JR_FORM_ID)),c.getString(c.getColumnIndex(InstanceColumns.DISPLAY_NAME)), c.getString(c.getColumnIndex(InstanceColumns.DISPLAY_SUBTEXT)),c.getString(c.getColumnIndex(InstanceColumns.INSTANCE_FILE_PATH)),""));
			mInstances=new SendFormsListAdapter(this, forms);
            mList.setAdapter(mInstances);
		}

        mList.setAdapter(mInstances);
        mList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mList.setItemsCanFocus(false);

		// if current activity is being reinitialized due to changing
		// orientation restore all check
		// marks for ones selected
		if (mRestored)
		{
			for (long id : mSelected)
			{
				for (int pos = 0; pos < mList.getCount(); pos++)
				{
					if (id == mList.getItemIdAtPosition(pos)) {
                        mList.setItemChecked(pos, true);
						break;
					}
				}

			}
			mRestored = false;
		}

        if ((mList == null) || (mList.getCount() == 0)) {
        	findViewById(R.id.linearLayout_footer).setVisibility(View.GONE);
        	findViewById(R.id.empty).setVisibility(View.VISIBLE);
		}
        else
        {
        	findViewById(R.id.linearLayout_footer).setVisibility(View.VISIBLE);
        	findViewById(R.id.empty).setVisibility(View.GONE);
        }
	}
	@Override
	public void onResume() {
		super.onResume();
		loadListView();
	}
	
	private void uploadSelectedFiles() {
		// send list of _IDs.
		instanceIDs = new long[mSelected.size()];
		for (int i = 0; i < mSelected.size(); i++)
		{
			instanceIDs[i] = mSelected.get(i);
		}

		mInstancesToSend = new Long[(instanceIDs == null) ? 0 : instanceIDs.length];
        if ( instanceIDs != null ) {
        	for ( int i = 0 ; i < instanceIDs.length ; ++i ) {
        		mInstancesToSend[i] = instanceIDs[i];
        	}
        }

		mInstanceUploaderTask = (InstanceUploaderTask) getLastNonConfigurationInstance();
        if (mInstanceUploaderTask == null) {
            // setup dialog and upload task
            showDialog(PROGRESS_DIALOG);
            mInstanceUploaderTask = new InstanceUploaderTask();

            // register this activity with the new uploader task
            mInstanceUploaderTask.setUploaderListener(ActivitySendForm.this);

            mInstanceUploaderTask.execute(mInstancesToSend);
        }
        
        if (mInstanceUploaderTask != null) {
            mInstanceUploaderTask.setUploaderListener(this);
        }
        if (mAlertShowing)
        	createAlertDialog(mAlertMsg);
        
	}
	
	protected void selectAllOption (){
		// toggle selections of items to all or none
		mToggled = !mToggled;

		// remove all items from selected list
		mSelected.clear();
		for (int pos = 0; pos < mList.getCount(); pos++) {
            mList.setItemChecked(pos, mToggled);
			// add all items if mToggled sets to select all
			if (mToggled) mSelected.add(mList.getItemIdAtPosition(pos));
		}
		if (mSelected.size()==0)
        	textView_pannier.setText(getString(R.string.no_form_selected));
        else if (mSelected.size()==1)
        	textView_pannier.setText(getString(R.string.form_selected));
        else if (mSelected.size()>1)
        	textView_pannier.setText(mSelected.size()+" "+getString(R.string.forms_selected));
	}
	
	protected void uploadInstancesOption(){
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

		if (mSelected.size() == 0) 
		{
			CroutonView.showBuiltInCrouton(ActivitySendForm.this,
                                           getString(R.string.noselect_error),
                                           Style.ALERT);
	    	
		}
		else if (NetworkReceiver.running == true) 
		{
			//another upload is already running
			CroutonView.showBuiltInCrouton(ActivitySendForm.this, "Background send running, please try again shortly", Style.ALERT);
			ContentValues cv = new ContentValues();
			Uri toUpdate;
			for (int i=0; i<mSelected.size(); i++)
			{
				toUpdate = Uri.withAppendedPath(InstanceColumns.CONTENT_URI, ""+mSelected.get(i));
				cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
                Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
			}
		} 
		else if (ni == null || !ni.isConnected()) 
		{
			//no network connection
			CroutonView.showBuiltInCrouton(ActivitySendForm.this, getString(R.string.no_connexion), Style.ALERT);
			ContentValues cv = new ContentValues();
			Uri toUpdate;
			for (int i=0; i<mSelected.size(); i++)
			{
				toUpdate = Uri.withAppendedPath(InstanceColumns.CONTENT_URI, ""+mSelected.get(i));
				cv.put(InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
                Collect.getInstance().getContentResolver().update(toUpdate, cv, null, null);
			}
		} 
		else
		{
			uploadSelectedFiles();
		}
	}
	
	private void deleteSelectedInstances(ArrayList<Long> mSelected) {
		if (mDeleteInstancesTask == null) {
			mDeleteInstancesTask = new DeleteInstancesTask();
			mDeleteInstancesTask.setContentResolver(getContentResolver());
			mDeleteInstancesTask.setDeleteListener(this);
			mDeleteInstancesTask.execute(mSelected.toArray(new Long[mSelected
					.size()]));
		} else {
			CroutonView.showBuiltInCrouton(ActivitySendForm.this, getString(R.string.file_delete_in_progress), Style.ALERT);
	    	
		}
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		long[] selectedArray = new long[mSelected.size()];
		for (int i = 0; i < mSelected.size(); i++)
			selectedArray[i] = mSelected.get(i);
		outState.putLongArray(BUNDLE_SELECTED_ITEMS_KEY,
                              selectedArray);
		outState.putBoolean(BUNDLE_TOGGLED_KEY, mToggled);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void deleteComplete(int deletedInstances) {
		mDeleteInstancesTask = null;
		mSelected.clear();
        mList.clearChoices();
		loadListView();
		// doesn't unset the checkboxes
		for ( int i = 0 ; i < mList.getCount() ; ++i ) {
            mList.setItemChecked(i, false);
		}
		
		if ( (mList==null) || (mList.getCount()==0) )
		{
			findViewById(R.id.linearLayout_footer).setVisibility(View.GONE);
        	findViewById(R.id.empty).setVisibility(View.VISIBLE);
		}
        else
        {
        	findViewById(R.id.linearLayout_footer).setVisibility(View.VISIBLE);
        	findViewById(R.id.empty).setVisibility(View.GONE);
        }
		
		
    }

	@Override
	public boolean onQueryTextChange(String newText)
	{
		// TODO Auto-generated method stub
		Cursor c = getAllCursor(newText);

		if(c.getCount()> 0)
		{
			forms=new ArrayList<Form>();
			while (c.moveToNext())
				forms.add(new Form(c.getInt(c.getColumnIndex(BaseColumns._ID)),c.getString(c.getColumnIndex(InstanceColumns.JR_FORM_ID)),c.getString(c.getColumnIndex(InstanceColumns.DISPLAY_NAME)), c.getString(c.getColumnIndex(InstanceColumns.DISPLAY_SUBTEXT)),c.getString(c.getColumnIndex(InstanceColumns.INSTANCE_FILE_PATH)),""));
			mInstances=new SendFormsListAdapter(this, forms);
            mList.setAdapter(mInstances);
		}

        mList.setAdapter(mInstances);
        for (int i=0;i<mList.getCount();i++)
        {
        	if (mSelected.contains(mList.getItemIdAtPosition(i)))
                mList.setItemChecked(i, true);
        }
    	
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
    	InputMethodManager imm = (InputMethodManager)getSystemService( Context.INPUT_METHOD_SERVICE);
    	imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
        return false;
    }

	@Override
	public void uploadingComplete(HashMap<String, String> result)
	{
		// TODO Auto-generated method stub
		try
		{
            dismissDialog(PROGRESS_DIALOG);
        } catch (Exception e) {
            // tried to close a dialog not open. don't care.
        }

        StringBuilder selection = new StringBuilder();
        Set<String> keys = result.keySet();
        Iterator<String> it = keys.iterator();

        String[] selectionArgs = new String[keys.size()];
        int i = 0;
        while (it.hasNext()) {
            String id = it.next();
            selection.append(BaseColumns._ID + "=?");
            selectionArgs[i++] = id;
            if (i != keys.size()) {
                selection.append(" or ");
            }
        }
        
        StringBuilder message = new StringBuilder();
        {
        	Cursor results = null;
        	try {
                results = getContentResolver().query(InstanceColumns.CONTENT_URI,
                		null, selection.toString(), selectionArgs, null);
                if (results.getCount() > 0) {
                    results.moveToPosition(-1);
                    i=-1;
                    while (results.moveToNext())
                    {
                    	i++;
                        String name =
                            results.getString(results.getColumnIndex(InstanceColumns.DISPLAY_NAME));
                        String id = results.getString(results.getColumnIndex(BaseColumns._ID));
                        if (!result.get(id).equals(getString(R.string.success)))
                        {
                        	message.append(name + " - " + getString(R.string.fail) + "\n\n");
                        	mSelected.remove(i);
                        }
                        else
                        	message.append(name + " - " + result.get(id) + "\n\n");
                    }
                } else {
                    message.append(getString(R.string.no_forms_uploaded));
                }
        	} finally {
        		if ( results != null ) {
        			results.close();
        		}
        	}
        }

        deleteSelectedInstances(mSelected);
        
        mToggled = false;
		mSelected.clear();
        mList.clearChoices();
		if (mSelected.size()==0)
        	textView_pannier.setText(getString(R.string.no_form_selected));
        else if (mSelected.size()==1)
        	textView_pannier.setText(getString(R.string.form_selected));
        else if (mSelected.size()>1)
        	textView_pannier.setText(mSelected.size()+" "+getString(R.string.forms_selected));
	
		if ( (mList==null) || (mList.getCount()==0) )
		{
			findViewById(R.id.linearLayout_footer).setVisibility(View.GONE);
        	findViewById(R.id.empty).setVisibility(View.VISIBLE);
		}
        else
        {
        	findViewById(R.id.linearLayout_footer).setVisibility(View.VISIBLE);
        	findViewById(R.id.empty).setVisibility(View.GONE);
        }
		
		createAlertDialog(message.toString().trim());
	}

	@Override
	public void progressUpdate(int progress, int total) {
		// TODO Auto-generated method stub
		mAlertMsg = getString(R.string.sending_items,
                              progress,
                              total);
        mProgressDialog.setMessage(mAlertMsg);
		
	}

	@Override
	public void authRequest(Uri url, HashMap<String, String> doneSoFar) {
		// TODO Auto-generated method stub
		if (mProgressDialog.isShowing()) {
            // should always be showing here
            mProgressDialog.dismiss();
        }

        // add our list of completed uploads to "completed"
        // and remove them from our toSend list.
        ArrayList<Long> workingSet = new ArrayList<Long>();
        Collections.addAll(workingSet,
                           mInstancesToSend);
        if (doneSoFar != null) {
            Set<String> uploadedInstances = doneSoFar.keySet();
            Iterator<String> itr = uploadedInstances.iterator();

            while (itr.hasNext()) {
                Long removeMe = Long.valueOf(itr.next());
                boolean removed = workingSet.remove(removeMe);
                if (removed) {
                    Log.i(t, removeMe
                            + " was already sent, removing from queue before restarting task");
                }
            }
            mUploadedInstances.putAll(doneSoFar);
        }

        // and reconstruct the pending set of instances to send
        Long[] updatedToSend = new Long[workingSet.size()];
        for ( int i = 0 ; i < workingSet.size() ; ++i ) {
        	updatedToSend[i] = workingSet.get(i);
        }
        mInstancesToSend = updatedToSend;

        mUrl = url.toString();
        showDialog(AUTH_DIALOG);
	}
	
	private void createAlertDialog(String message) {
    	mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setTitle(getString(R.string.upload_results));
        mAlertDialog.setMessage(message);
        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON1: // ok
                    	// always exit this activity since it has no interface
                        mAlertShowing = false;
                        textView_pannier.setText(getString(R.string.no_form_selected));
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), quitListener);
        mAlertDialog.setIconAttribute(R.attr.dialog_icon_info);
        mAlertShowing = true;
        mAlertMsg = message;
        mAlertDialog.show();
    }
	
	@Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case PROGRESS_DIALOG:
            	mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        	dialog.dismiss();
                            mInstanceUploaderTask.cancel(true);
                            mInstanceUploaderTask.setUploaderListener(null);
                            finish();
                        }
                    };
                mProgressDialog.setTitle(getString(R.string.uploading_data));
                mProgressDialog.setMessage(mAlertMsg);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setIcon(android.R.drawable.ic_menu_upload);
                mProgressDialog.setButton(getString(R.string.cancel), loadingButtonListener);
                return mProgressDialog;
            case AUTH_DIALOG:
                Log.i(t, "onCreateDialog(AUTH_DIALOG): for upload of " + mInstancesToSend.length + " instances!");
            	AlertDialog.Builder b = new AlertDialog.Builder(this);

                LayoutInflater factory = LayoutInflater.from(this);
                final View dialogView = factory.inflate(R.layout.dialog_server_authentification, null);

                // Get the server, username, and password from the settings
                SharedPreferences settings =
                    PreferenceManager.getDefaultSharedPreferences(getBaseContext());

                String server = mUrl;
                if (server == null) {
                    Log.e(t, "onCreateDialog(AUTH_DIALOG): No failing mUrl specified for upload of " + mInstancesToSend.length + " instances!");
                    // if the bundle is null, we're looking for a formlist
                    String submissionUrl = getString(R.string.default_odk_submission);
                    server =
                        settings.getString(ActivityPreferences.KEY_SERVER_URL,
                            getString(R.string.default_server_url))
                                + settings.getString(ActivityPreferences.KEY_SUBMISSION_URL, submissionUrl);
                }

                final String url = server;

                Log.i(t, "Trying connecting to: " + url);

                EditText username = (EditText) dialogView.findViewById(R.id.username_edit);
                String storedUsername = settings.getString(ActivityPreferences.KEY_USERNAME, null);
                username.setText(storedUsername);

                EditText password = (EditText) dialogView.findViewById(R.id.password_edit);
                String storedPassword = settings.getString(ActivityPreferences.KEY_PASSWORD, null);
                password.setText(storedPassword);

                b.setTitle(getString(R.string.server_requires_auth));
                b.setMessage(getString(R.string.server_auth_credentials, url));
                b.setView(dialogView);
                b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    	EditText username = (EditText) dialogView.findViewById(R.id.username_edit);
                        EditText password = (EditText) dialogView.findViewById(R.id.password_edit);

                        Uri u = Uri.parse(url);
                        WebUtils.addCredentials(username.getText().toString(), password.getText()
                                .toString(), u.getHost());

                        showDialog(PROGRESS_DIALOG);
                        mInstanceUploaderTask = new InstanceUploaderTask();

                        // register this activity with the new uploader task
                        mInstanceUploaderTask.setUploaderListener(ActivitySendForm.this);

                        mInstanceUploaderTask.execute(mInstancesToSend);
                    }
                });
                b.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    	finish();
                    }
                });

                b.setCancelable(false);
                return b.create();
        }
        return null;
    }
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	if (requestCode == RESULT_PREFERENCES)
    	{
    		Intent i = getIntent();
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
    	}
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
    		v = inflator.inflate(R.layout.actionbar_title_layout_send_form_land, null);
        else
        	v = inflator.inflate(R.layout.actionbar_title_layout_send_form, null);
        getSupportActionBar().setCustomView(v);
    }
    */
}
