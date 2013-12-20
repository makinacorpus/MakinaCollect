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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.WazaBe.HoloEverywhere.app.AlertDialog;
import com.WazaBe.HoloEverywhere.app.Dialog;
import com.WazaBe.HoloEverywhere.app.ProgressDialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.dialog.AboutUs;
import com.makina.collect.android.dialog.Help;
import com.makina.collect.android.dialog.HelpWithConfirmation;
import com.makina.collect.android.listeners.DeleteInstancesListener;
import com.makina.collect.android.listeners.InstanceUploaderListener;
import com.makina.collect.android.preferences.ActivityPreferences;
import com.makina.collect.android.provider.InstanceProvider;
import com.makina.collect.android.provider.InstanceProviderAPI;
import com.makina.collect.android.provider.FormsProviderAPI.FormsColumns;
import com.makina.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import com.makina.collect.android.receivers.NetworkReceiver;
import com.makina.collect.android.tasks.DeleteInstancesTask;
import com.makina.collect.android.tasks.InstanceUploaderTask;
import com.makina.collect.android.utilities.Finish;
import com.makina.collect.android.utilities.WebUtils;
import com.makina.collect.android.views.CustomFontTextview;

import de.timroes.swipetodismiss.SwipeDismissList;
import de.timroes.swipetodismiss.SwipeDismissList.UndoMode;
import de.timroes.swipetodismiss.SwipeDismissList.Undoable;

/**
 * Responsible for displaying all the valid forms in the forms directory. Stores
 * the path to selected form for use by {@link ActivityMainMenu}.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */

@SuppressLint("NewApi")
public class ActivitySendForm extends SherlockListActivity implements DeleteInstancesListener,SearchView.OnQueryTextListener, InstanceUploaderListener  {

	private static final String t = "InstanceUploaderList";
	
	private static final String BUNDLE_SELECTED_ITEMS_KEY = "selected_items";
	private static final String BUNDLE_TOGGLED_KEY = "toggled";
	private static final int INSTANCE_UPLOADER = 0;
	
	private ProgressDialog mProgressDialog;

	private String mAlertMsg;

	//private boolean mShowUnsent = true;
	private SimpleCursorAdapter mInstances;
	private ArrayList<Long> mSelected = new ArrayList<Long>();
	private boolean mRestored = false;
	private boolean mToggled = false;
	private AlertDialog mAlertDialog;
	DeleteInstancesTask mDeleteInstancesTask = null;
	private  SearchView mSearchView;
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
        
        mUploadedInstances = new HashMap<String, String>();
        
        Finish.activitySendForm=this;
        
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        
        getLayoutInflater().setFactory(new LayoutInflater.Factory()
        {
            public View onCreateView(String name, Context context, AttributeSet attrs)
            {
            	if (name.equalsIgnoreCase("com.android.internal.view.menu.IconMenuItemView")|| name.equalsIgnoreCase("TextView"))
                {
                    try
                    {
                        LayoutInflater li = LayoutInflater.from(context);
                        final View view = li.createView(name, null, attrs);
                        new Handler().post(new Runnable()
                        {
                            public void run()
                            {
                            	((TextView)view).setTextColor(getResources().getColor(R.color.actionbarTitleColorGris));
                                ((TextView)view).setTypeface(Typeface.createFromAsset(getAssets(),"fonts/avenir.ttc"));
                            }
                        });
                        return view;
                    }
                    catch (InflateException e){}
                    catch (ClassNotFoundException e)
                    {}
                }
                return null;
            }
        });
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
	        	startActivity(new Intent(this, ActivityPreferences.class));
	        	return true;
	        case R.id.menu_help:
	        	Help.helpDialog(this, getString(R.string.help_send));
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
        Typeface typeFace = Typeface.createFromAsset(getAssets(),"fonts/avenir.ttc"); 
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    	getSupportActionBar().setTitle(getString(R.string.box));
    	int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
    	TextView actionbarTitle = (TextView)findViewById(titleId);
    	if (actionbarTitle!=null)
    	{
    	actionbarTitle.setTextColor(getResources().getColor(R.color.actionbarTitleColorGris));
    	actionbarTitle.setTypeface(typeFace);
    	}
    	titleId = Resources.getSystem().getIdentifier("action_bar_subtitle", "id", "android");
    	TextView actionbarSubTitle = (TextView)findViewById(titleId);
    	if (actionbarSubTitle!=null)
    	{
	    	actionbarSubTitle.setTextColor(getResources().getColor(R.color.actionbarTitleColorBlueSend));
	    	actionbarSubTitle.setTypeface(typeFace);
    	}
    	getSupportActionBar().setSubtitle(getString(R.string.send));
        
    	mProgressDialog = new ProgressDialog(this);
    	
    	if (!getSharedPreferences("session", MODE_PRIVATE).getBoolean("help_send", false))
    		HelpWithConfirmation.helpDialog(this, getString(R.string.help_send));
    	
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
					textView_check_all.setText("TOUT S�LECTIONNER");
					
				}
				else
				{
					selectAllOption();
					ImageView imageView_check_all=(ImageView)findViewById(R.id.imageView_check_all);
					imageView_check_all.setImageResource(R.drawable.case_on);
					TextView textView_check_all=(TextView)findViewById(R.id.textView_check_all);
					textView_check_all.setText("TOUT D�S�LECTIONNER");
					
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
        
        CustomFontTextview textview_download_form=(CustomFontTextview) findViewById(R.id.textview_download_form);
        textview_download_form.setOnClickListener(new View.OnClickListener()
        {
			@Override
			public void onClick(View arg0)
			{
				startActivity(new Intent(getApplicationContext(), ActivityDownloadForm.class));
			}
		});
		
        getListView().setOnItemLongClickListener(new OnItemLongClickListener()
        {
        	@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,int position, long arg3)
			{
				// TODO Auto-generated method stub
        		createDialogDelete(position);
        		return false;
			}
		});
        
        SwipeDismissList.OnDismissCallback callback = new SwipeDismissList.OnDismissCallback()
        {
            @Override
			public Undoable onDismiss(AbsListView listView, int position)
			{
				// TODO Auto-generated method stub
				createDialogDelete(position);
				return null;
			}
        };
        UndoMode mode = SwipeDismissList.UndoMode.SINGLE_UNDO;
        SwipeDismissList swipeList = new SwipeDismissList(getListView(), callback, mode);
	}
	
	private void createDialogDelete(int position)
    {
    	final Cursor c=mInstances.getCursor();
		c.moveToPosition(position);
		AlertDialog.Builder adb = new AlertDialog.Builder(ActivitySendForm.this);
		adb.setTitle("Suppression");
		adb.setMessage("Voulez-vous vraiment supprimer "+c.getString(c.getColumnIndex(FormsColumns.DISPLAY_NAME))+" ?");
		adb.setNegativeButton(getString(android.R.string.cancel),null);

		adb.setPositiveButton(getString(android.R.string.yes), new AlertDialog.OnClickListener()
		{
			public void onClick(DialogInterface dialog,int which)
			{
				InstanceProvider.deleteInstance(c.getLong(c.getColumnIndex(BaseColumns._ID)));
				loadListView();
			}
		});
		adb.show();
    }

	private void loadListView()
	{
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
	}
	@Override
	public void onResume() {
		loadListView();
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
		instanceIDs = new long[mSelected.size()];
		for (int i = 0; i < mSelected.size(); i++)
		{
			instanceIDs[i] = mSelected.get(i);
		}

		/*Intent i = new Intent(this, InstanceUploaderActivity.class);
		i.putExtra(ActivityForm.KEY_INSTANCES, instanceIDs);
		startActivityForResult(i, INSTANCE_UPLOADER);*/
		
        
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
		textView_pannier.setText(mSelected.size()+" formulaire(s) s�lectionn�(s)");
	}
	
	protected void uploadInstancesOption (){
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

		if (NetworkReceiver.running == true) 
		{
			//another upload is already running
			Toast.makeText(this,"Background send running, please try again shortly",Toast.LENGTH_SHORT).show();
		} 
		else if (ni == null || !ni.isConnected()) 
		{
			//no network connection
			Collect.getInstance().getActivityLogger().logAction(this, "uploadButton", "noConnection");
			Toast.makeText(this,R.string.no_connection, Toast.LENGTH_SHORT).show();
		} 
		else
		{
			Collect.getInstance().getActivityLogger().logAction(this, "uploadButton",Integer.toString(mSelected.size()));

			if (mSelected.size() > 0) 
			{
				// items selected
				uploadSelectedFiles();
			}
			else 
			{
				// no items selected
				Toast.makeText(getApplicationContext(),getString(R.string.noselect_error),Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void createPreferencesMenu() {
		Intent i = new Intent(this, ActivityPreferences.class);
		startActivity(i);
	}
	
	
	private void deleteSelectedInstances(ArrayList<Long> mSelected) {
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
		
		textView_pannier.setText(mSelected.size()+" formulaire(s) s�lectionn�(s)");
		
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
		
		mDeleteInstancesTask = null;
		mSelected.clear();
		getListView().clearChoices(); // doesn't unset the checkboxes
		for ( int i = 0 ; i < getListView().getCount() ; ++i ) {
			getListView().setItemChecked(i, false);
		}
	}

	@Override
	public boolean onQueryTextChange(String newText)
	{
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
                    while (results.moveToNext()) {
                        String name =
                            results.getString(results.getColumnIndex(InstanceColumns.DISPLAY_NAME));
                        String id = results.getString(results.getColumnIndex(BaseColumns._ID));
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

        createAlertDialog(message.toString().trim());
        deleteSelectedInstances(mSelected);
        
        mToggled = false;
		mSelected.clear();
		ActivitySendForm.this.getListView().clearChoices();
	}

	@Override
	public void progressUpdate(int progress, int total) {
		// TODO Auto-generated method stub
		mAlertMsg = getString(R.string.sending_items, progress, total);
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
        Collections.addAll(workingSet, mInstancesToSend);
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
    	Collect.getInstance().getActivityLogger().logAction(this, "createAlertDialog", "show");

        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setTitle(getString(R.string.upload_results));
        mAlertDialog.setMessage(message);
        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON1: // ok
                    	Collect.getInstance().getActivityLogger().logAction(this, "createAlertDialog", "OK");
                        // always exit this activity since it has no interface
                        mAlertShowing = false;
                        textView_pannier.setText("0 formulaire(s) s�lectionn�s");
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), quitListener);
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
        mAlertShowing = true;
        mAlertMsg = message;
        mAlertDialog.show();
    }
	
	@Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
            	Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.PROGRESS_DIALOG", "show");

                mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        	Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.PROGRESS_DIALOG", "cancel");
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
                mProgressDialog.setButton(getString(R.string.cancel), loadingButtonListener);
                return mProgressDialog;
            case AUTH_DIALOG:
                Log.i(t, "onCreateDialog(AUTH_DIALOG): for upload of " + mInstancesToSend.length + " instances!");
            	Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.AUTH_DIALOG", "show");
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
                    	Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.AUTH_DIALOG", "OK");
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
                    	Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.AUTH_DIALOG", "cancel");
                        finish();
                    }
                });

                b.setCancelable(false);
                return b.create();
        }
        return null;
    }


}
