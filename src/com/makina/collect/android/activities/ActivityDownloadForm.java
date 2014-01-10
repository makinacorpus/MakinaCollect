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
import java.util.HashMap;
import java.util.Set;

import com.WazaBe.HoloEverywhere.app.AlertDialog;
import com.WazaBe.HoloEverywhere.app.ProgressDialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.dialog.AboutUs;
import com.makina.collect.android.dialog.Help;
import com.makina.collect.android.dialog.HelpWithConfirmation;
import com.makina.collect.android.listeners.FormDownloaderListener;
import com.makina.collect.android.listeners.FormListDownloaderListener;
import com.makina.collect.android.logic.FormDetails;
import com.makina.collect.android.preferences.ActivityPreferences;
import com.makina.collect.android.tasks.DownloadFormListTask;
import com.makina.collect.android.tasks.DownloadFormsTask;
import com.makina.collect.android.utilities.Finish;
import com.makina.collect.android.utilities.WebUtils;
import com.makina.collect.android.views.CustomActionBar;
import com.makina.collect.android.views.CustomFontTextview;

import android.widget.SearchView;
/**
 * Responsible for displaying, adding and deleting all the valid forms in the forms directory. One
 * caveat. If the server requires authentication, a dialog will pop up asking when you request the
 * form list. If somehow you manage to wait long enough and then try to download selected forms and
 * your authorization has timed out, it won't again ask for authentication, it will just throw a 401
 * and you'll have to hit 'refresh' where it will ask for credentials again. Technically a server
 * could point at other servers requiring authentication to download the forms, but the current
 * implementation in Collect doesn't allow for that. Mostly this is just because it's a pain in the
 * butt to keep track of which forms we've downloaded and where we're needing to authenticate. I
 * think we do something similar in the instanceuploader task/activity, so should change the
 * implementation eventually.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 */
@SuppressLint("NewApi")
public class ActivityDownloadForm extends SherlockListActivity implements FormListDownloaderListener,
        FormDownloaderListener,SearchView.OnQueryTextListener {
    private static final String t = "RemoveFileManageList";

    private static final int PROGRESS_DIALOG = 1;
    private static final int AUTH_DIALOG = 2;

    private static final String BUNDLE_TOGGLED_KEY = "toggled";
    private static final String BUNDLE_SELECTED_COUNT = "selectedcount";
    private static final String BUNDLE_FORM_MAP = "formmap";
    private static final String DIALOG_TITLE = "dialogtitle";
    private static final String DIALOG_MSG = "dialogmsg";
    private static final String DIALOG_SHOWING = "dialogshowing";
    private static final String FORMLIST = "formlist";

    public static final String LIST_URL = "listurl";

    private static final String FORMNAME = "formname";
    private static final String FORMDETAIL_KEY = "formdetailkey";
    private static final String FORMID_DISPLAY = "formiddisplay";

    private String mAlertMsg;
    private boolean mAlertShowing = false;
    private String mAlertTitle;

    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;

    private DownloadFormListTask mDownloadFormListTask;
    private DownloadFormsTask mDownloadFormsTask;

    private HashMap<String, FormDetails> mFormNamesAndURLs = new HashMap<String,FormDetails>();
    private SimpleAdapter mFormListAdapter;
    private ArrayList<HashMap<String, String>> mFormList;

    private boolean mToggled = false;

    private static final boolean EXIT = true;
    private static final boolean DO_NOT_EXIT = false;
    private boolean mShouldExit;
    private static final String SHOULD_EXIT = "shouldexit";
    private CustomFontTextview textView_pannier;
    private SearchView mSearchView;
    private ArrayList<String> mSelected = new ArrayList<String>();

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	
        setContentView(R.layout.activity_download_form);
       
        createDialog(PROGRESS_DIALOG);
        textView_pannier=(CustomFontTextview)findViewById(R.id.textView_pannier);
        Finish.activityDownloadForm=this;
        
        getSupportActionBar().setTitle(getString(R.string.download_menu));
        getSupportActionBar().setSubtitle(getString(R.string.form));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
    	TextView actionbarTitle = (TextView)findViewById(titleId);
    	titleId = Resources.getSystem().getIdentifier("action_bar_subtitle", "id", "android");
    	TextView actionbarSubTitle = (TextView)findViewById(titleId);
    	CustomActionBar.showActionBar(this, actionbarTitle, actionbarSubTitle, getResources().getColor(R.color.actionbarTitleColorGreenDownload), getResources().getColor(R.color.actionbarTitleColorGris));
    	
    	if (!getSharedPreferences("session", MODE_PRIVATE).getBoolean("help_download", false))
    		HelpWithConfirmation.helpDialog(this, getString(R.string.help_download));
		
    	
    	
        // need clear background before load
        //getListView().setBackgroundResource(R.drawable.background);

        
        if (savedInstanceState != null) {
            // If the screen has rotated, the hashmap with the form ids and urls is passed here.
            if (savedInstanceState.containsKey(BUNDLE_FORM_MAP)) {
                mFormNamesAndURLs =
                    (HashMap<String, FormDetails>) savedInstanceState
                            .getSerializable(BUNDLE_FORM_MAP);
            }

            // indicating whether or not select-all is on or off.
            if (savedInstanceState.containsKey(BUNDLE_TOGGLED_KEY)) {
                mToggled = savedInstanceState.getBoolean(BUNDLE_TOGGLED_KEY);
            }

            // to restore alert dialog.
            if (savedInstanceState.containsKey(DIALOG_TITLE)) {
                mAlertTitle = savedInstanceState.getString(DIALOG_TITLE);
            }
            if (savedInstanceState.containsKey(DIALOG_MSG)) {
                mAlertMsg = savedInstanceState.getString(DIALOG_MSG);
            }
            if (savedInstanceState.containsKey(DIALOG_SHOWING)) {
                mAlertShowing = savedInstanceState.getBoolean(DIALOG_SHOWING);
            }
            if (savedInstanceState.containsKey(SHOULD_EXIT)) {
                mShouldExit = savedInstanceState.getBoolean(SHOULD_EXIT);
            }
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(FORMLIST)) {
            mFormList =
                (ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable(FORMLIST);
        } else {
            mFormList = new ArrayList<HashMap<String, String>>();
        }
        
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
					CustomFontTextview textView_check_all=(CustomFontTextview)findViewById(R.id.textView_check_all);
					textView_check_all.setText("TOUT SÉLECTIONNER");
					
				}
				else
				{
					selectAllOption();
					ImageView imageView_check_all=(ImageView)findViewById(R.id.imageView_check_all);
					imageView_check_all.setImageResource(R.drawable.case_on);
					CustomFontTextview textView_check_all=(CustomFontTextview)findViewById(R.id.textView_check_all);
					textView_check_all.setText("TOUT DÉSÉLECTIONNER");
					
				}
			}
		});
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

    private void clearChoices() {
        ActivityDownloadForm.this.getListView().clearChoices();
        
    }


    @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Object o = getListAdapter().getItem(position);
		@SuppressWarnings("unchecked")
		HashMap<String, String> item = (HashMap<String, String>) o;
        FormDetails detail = mFormNamesAndURLs.get(item.get(FORMDETAIL_KEY));
        
        if (mSelected.contains(detail.formID))
        	mSelected.remove(mSelected.indexOf(detail.formID));
        else
        	mSelected.add(detail.formID);
        
        Collect.getInstance().getActivityLogger().logAction(this, "onListItemClick", detail.downloadUrl);
        
        textView_pannier.setText(mSelected.size()+" formulaire(s) s�lectionn�(s)");
        
    }


    /**
     * Starts the download task and shows the progress dialog.
     */
    private void downloadFormList() {
        ConnectivityManager connectivityManager =(ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

        if (ni == null || !ni.isConnected()) {
            Toast.makeText(getApplicationContext(), R.string.no_connection, Toast.LENGTH_SHORT).show();
        } else {

            mFormNamesAndURLs = new HashMap<String, FormDetails>();
            if (mProgressDialog != null) {
                // This is needed because onPrepareDialog() is broken in 1.6.
                mProgressDialog.setMessage(getString(R.string.please_wait));
            }
            showDialog(PROGRESS_DIALOG);

            if (mDownloadFormListTask != null &&
            	mDownloadFormListTask.getStatus() != AsyncTask.Status.FINISHED) {
            	return; // we are already doing the download!!!
            } else if (mDownloadFormListTask != null) {
            	mDownloadFormListTask.setDownloaderListener(null);
            	mDownloadFormListTask.cancel(true);
            	mDownloadFormListTask = null;
            }

            mDownloadFormListTask = new DownloadFormListTask();
            mDownloadFormListTask.setDownloaderListener(this);
            mDownloadFormListTask.execute();
        }
    }


    @Override
	public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUNDLE_TOGGLED_KEY, mToggled);
        outState.putInt(BUNDLE_SELECTED_COUNT, selectedItemCount());
        outState.putSerializable(BUNDLE_FORM_MAP, mFormNamesAndURLs);
        outState.putString(DIALOG_TITLE, mAlertTitle);
        outState.putString(DIALOG_MSG, mAlertMsg);
        outState.putBoolean(DIALOG_SHOWING, mAlertShowing);
        outState.putBoolean(SHOULD_EXIT, mShouldExit);
        outState.putSerializable(FORMLIST, mFormList);
    }


    /**
     * returns the number of items currently selected in the list.
     *
     * @return
     */
    private int selectedItemCount() {
        int count = 0;
        SparseBooleanArray sba = getListView().getCheckedItemPositions();
        for (int i = 0; i < getListView().getCount(); i++) {
            if (sba.get(i, false)) {
                count++;
            }
        }
        return count;
    }

    protected void refreshFormsOption(){
    	Collect.getInstance().getActivityLogger().logAction(this, "refreshForms", "");
        mToggled = false;
        downloadFormList();
        ActivityDownloadForm.this.getListView().clearChoices();
        clearChoices();
    }
    
    protected void getFormsOption(){
    	downloadSelectedFiles();
        mToggled = false;
        clearChoices();
    }
    
    protected void selectAllOption(){
    	ListView ls = getListView();
        mToggled = !mToggled;
        Collect.getInstance().getActivityLogger().logAction(this, "toggleFormCheckbox", Boolean.toString(mToggled));
        mSelected.clear();
        if (mToggled)
	        for (int pos = 0; pos < ls.getCount(); pos++)
	        {
	            ls.setItemChecked(pos, mToggled);
	            mSelected.add(mFormList.get(pos).get(FORMDETAIL_KEY));
	        }
        else
        {
        	for (int pos = 0; pos < ls.getCount(); pos++)
        	{
	            ls.setItemChecked(pos, mToggled);
	        }
        }
        textView_pannier.setText(mSelected.size()+" formulaire(s) sélectionné(s)");
   }
    
    protected void clearAllOption()
    {
    	ListView ls = getListView();
        mToggled = false;
        mSelected.clear();
        
    	for (int pos = 0; pos < ls.getCount(); pos++)
    	{
            ls.setItemChecked(pos, mToggled);
        }
        
        textView_pannier.setText(mSelected.size()+" formulaire(s) sélectionné(s)");
   }

    private void dismissDialog()
    {
    	if (mProgressDialog!=null)
    		mProgressDialog.dismiss();
    }
    private void createDialog(int id)
    {
    	
        switch (id)
        {
            case PROGRESS_DIALOG:
                Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.PROGRESS_DIALOG", "show");
                mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.PROGRESS_DIALOG", "OK");
                            dialog.dismiss();
                            // we use the same progress dialog for both
                            // so whatever isn't null is running
                            if (mDownloadFormListTask != null) {
                                mDownloadFormListTask.setDownloaderListener(null);
                                mDownloadFormListTask.cancel(true);
                                mDownloadFormListTask = null;
                            }
                            if (mDownloadFormsTask != null) {
                                mDownloadFormsTask.setDownloaderListener(null);
                                mDownloadFormsTask.cancel(true);
                                mDownloadFormsTask = null;
                            }
                        }
                    };
                mProgressDialog.setTitle(getString(R.string.downloading_data));
                mProgressDialog.setMessage(getString(R.string.please_wait));
                mProgressDialog.setIcon(R.drawable.actionbar_about_us);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setButton(getString(R.string.cancel), loadingButtonListener);
                mProgressDialog.show();
                break;
            case AUTH_DIALOG:
                Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.AUTH_DIALOG", "show");
                AlertDialog.Builder b = new AlertDialog.Builder(this);

                LayoutInflater factory = LayoutInflater.from(this);
                final View dialogView = factory.inflate(R.layout.dialog_server_authentification, null);

                // Get the server, username, and password from the settings
                SharedPreferences settings =
                    PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
                String server =
                    settings.getString(ActivityPreferences.KEY_SERVER_URL,
                        getString(R.string.default_server_url));

                String formListUrl = getString(R.string.default_odk_formlist);
                final String url =
                    server + settings.getString(ActivityPreferences.KEY_FORMLIST_URL, formListUrl);
                Log.i(t, "Trying to get formList from: " + url);

                EditText username = (EditText) dialogView.findViewById(R.id.username_edit);
                String storedUsername = settings.getString(ActivityPreferences.KEY_USERNAME, null);
                username.setText(storedUsername);

                EditText password = (EditText) dialogView.findViewById(R.id.password_edit);
                String storedPassword = settings.getString(ActivityPreferences.KEY_PASSWORD, null);
                password.setText(storedPassword);

                b.setTitle(getString(R.string.server_requires_auth));
                b.setMessage(getString(R.string.server_auth_credentials, url));
                b.setView(dialogView);
                b.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.AUTH_DIALOG", "OK");

                        EditText username = (EditText) dialogView.findViewById(R.id.username_edit);
                        EditText password = (EditText) dialogView.findViewById(R.id.password_edit);

                        Uri u = Uri.parse(url);

                        WebUtils.addCredentials(username.getText().toString(), password.getText()
                                .toString(), u.getHost());
                        downloadFormList();
                    }
                });
                b.setNegativeButton(getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.AUTH_DIALOG", "Cancel");
                            //this.finish();
                        }
                    });

                b.setCancelable(false);
                mAlertShowing = false;
                mProgressDialog.show();
        }
    }


    /**
     * starts the task to download the selected forms, also shows progress dialog
     */
    @SuppressWarnings("unchecked")
    private void downloadSelectedFiles() {
        int totalCount = 0;
        ArrayList<FormDetails> filesToDownload = new ArrayList<FormDetails>();

        SparseBooleanArray sba = getListView().getCheckedItemPositions();
        for (int i = 0; i < getListView().getCount(); i++) {
            if (sba.get(i, false)) {
                HashMap<String, String> item =
                    (HashMap<String, String>) getListAdapter().getItem(i);
                filesToDownload.add(mFormNamesAndURLs.get(item.get(FORMDETAIL_KEY)));
            }
        }
        totalCount = filesToDownload.size();

        Collect.getInstance().getActivityLogger().logAction(this, "downloadSelectedFiles", Integer.toString(totalCount));

        if (totalCount > 0) {
            // show dialog box
            createDialog(PROGRESS_DIALOG);

            mDownloadFormsTask = new DownloadFormsTask();
            mDownloadFormsTask.setDownloaderListener(this);
            mDownloadFormsTask.execute(filesToDownload);
        } else {
            Toast.makeText(this.getApplicationContext(), R.string.noselect_error, Toast.LENGTH_SHORT)
                    .show();
        }
    }


    /*@Override
    public Object onRetainNonConfigurationInstance() {
        if (mDownloadFormsTask != null) {
            return mDownloadFormsTask;
        } else {
            return mDownloadFormListTask;
        }
    }*/


    @Override
	public void onDestroy() {
        if (mDownloadFormListTask != null) {
            mDownloadFormListTask.setDownloaderListener(null);
        }
        if (mDownloadFormsTask != null) {
            mDownloadFormsTask.setDownloaderListener(null);
        }
        super.onDestroy();
    }

   
    @Override
	public void onResume() {
    	
    	if (this.getLastNonConfigurationInstance() instanceof DownloadFormListTask)
    	{
            mDownloadFormListTask = (DownloadFormListTask) this.getLastNonConfigurationInstance();
            if (mDownloadFormListTask.getStatus() == AsyncTask.Status.FINISHED)
            {
                try
                {
                    this.dismissDialog(PROGRESS_DIALOG);
                }
                catch (IllegalArgumentException e)
                {
                    Log.i(t, "Attempting to close a dialog that was not previously opened");
                }
                mDownloadFormsTask = null;
            }
        }
    	else if (this.getLastNonConfigurationInstance() instanceof DownloadFormsTask)
    	{
            mDownloadFormsTask = (DownloadFormsTask) this.getLastNonConfigurationInstance();
            if (mDownloadFormsTask.getStatus() == AsyncTask.Status.FINISHED)
            {
                try
                {
                    this.dismissDialog(PROGRESS_DIALOG);
                }
                catch (IllegalArgumentException e)
                {
                    Log.i(t, "Attempting to close a dialog that was not previously opened");
                }
                mDownloadFormsTask = null;
            }
        }
    	else if (this.getLastNonConfigurationInstance() == null)
    	{
            // first time, so get the formlist
            downloadFormList();
        }
    	
    	String[] data = new String[] {FORMNAME, FORMID_DISPLAY, FORMDETAIL_KEY};
        int[] view = new int[] {R.id.text1, R.id.text2};

        mFormListAdapter =new SimpleAdapter(this, mFormList, R.layout.listview_item_download_form, data, view);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        getListView().setItemsCanFocus(false);
        setListAdapter(mFormListAdapter);
        
        if (mDownloadFormListTask != null) {
            mDownloadFormListTask.setDownloaderListener(this);
        }
        if (mDownloadFormsTask != null) {
            mDownloadFormsTask.setDownloaderListener(this);
        }
        if (mAlertShowing)
        {
            createAlertDialog(mAlertTitle, mAlertMsg, mShouldExit);
        }
        
        super.onResume();
    }


    @Override
	public void onPause() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        super.onPause();
    }


    /**
     * Called when the form list has finished downloading. results will either contain a set of
     * <formname, formdetails> tuples, or one tuple of DL.ERROR.MSG and the associated message.
     *
     * @param result
     */
    @Override
	public void formListDownloadingComplete(HashMap<String, FormDetails> result) {
    	dismissDialog();
        mDownloadFormListTask.setDownloaderListener(null);
        mDownloadFormListTask = null;

        if (result == null)
        {
            Log.e(t, "Formlist Downloading returned null.  That shouldn't happen");
            // Just displayes "error occured" to the user, but this should never happen.
            createAlertDialog(getString(R.string.load_remote_form_error),getString(R.string.error_occured), EXIT);
            return;
        }

        if (result.containsKey(DownloadFormListTask.DL_AUTH_REQUIRED))
        {
            // need authorization
            this.showDialog(AUTH_DIALOG);
        }
        else if (result.containsKey(DownloadFormListTask.DL_ERROR_MSG)) {
            // Download failed
            String dialogMessage =getString(R.string.list_failed_with_error, result.get(DownloadFormListTask.DL_ERROR_MSG).errorStr);
            String dialogTitle = getString(R.string.load_remote_form_error);
            createAlertDialog(dialogTitle, dialogMessage, DO_NOT_EXIT);
        }
        else
        {
            // Everything worked. Clear the list and add the results.
            mFormNamesAndURLs = result;

            mFormList.clear();

            ArrayList<String> ids = new ArrayList<String>(mFormNamesAndURLs.keySet());
            for (int i = 0; i < result.size(); i++)
            {
            	String formDetailsKey = ids.get(i);
            	FormDetails details = mFormNamesAndURLs.get(formDetailsKey);
                HashMap<String, String> item = new HashMap<String, String>();
                item.put(FORMNAME, details.formName);
                item.put(FORMID_DISPLAY,((details.formVersion == null) ? "" : (getString(R.string.version) + " " + details.formVersion + " ")) +"ID: " + details.formID );
                item.put(FORMDETAIL_KEY, formDetailsKey);

                // Insert the new form in alphabetical order.
                if (mFormList.size() == 0)
                {
                    mFormList.add(item);
                }
                else
                {
                    int j;
                    for (j = 0; j < mFormList.size(); j++)
                    {
                        HashMap<String, String> compareMe = mFormList.get(j);
                        String name = compareMe.get(FORMNAME);
                        if (name.compareTo(mFormNamesAndURLs.get(ids.get(i)).formName) > 0)
                        {
                            break;
                        }
                    }
                    mFormList.add(j, item);
                }
            }
            mFormListAdapter.notifyDataSetChanged();
        }
        
        LinearLayout linearLayout_footer=(LinearLayout) findViewById(R.id.linearLayout_footer);
        RelativeLayout empty=(RelativeLayout)findViewById(R.id.empty);
        if ( (mFormList==null) || (mFormList.size()==0) )
        {
        	linearLayout_footer.setVisibility(View.GONE);
        	empty.setVisibility(View.VISIBLE);
        }
        else
        {
        	linearLayout_footer.setVisibility(View.VISIBLE);
        	empty.setVisibility(View.GONE);
        }
        
        
    }


    /**
     * Creates an alert dialog with the given tite and message. If shouldExit is set to true, the
     * activity will exit when the user clicks "ok".
     *
     * @param title
     * @param message
     * @param shouldExit
     */
    private void createAlertDialog(String title, String message, final boolean shouldExit) {
        Collect.getInstance().getActivityLogger().logAction(this, "createAlertDialog", "show");
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setTitle(title);
        mAlertDialog.setMessage(message);
        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON1: // ok
                        Collect.getInstance().getActivityLogger().logAction(this, "createAlertDialog", "OK");
                        // just close the dialog
                        mAlertShowing = false;
                        // successful download, so quit
                        /*if (shouldExit) {
                            this.finish();
                        }*/
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), quitListener);
        mAlertDialog.setIcon(R.drawable.actionbar_about_us);
        mAlertMsg = message;
        mAlertTitle = title;
        mAlertShowing = true;
        mShouldExit = shouldExit;
        mAlertDialog.show();
    }


    @Override
    public void progressUpdate(String currentFile, int progress, int total) {
        mAlertMsg = getString(R.string.fetching_file, currentFile, progress, total);
        if (mProgressDialog==null)
        {
        	mProgressDialog = new ProgressDialog(this);
        	mProgressDialog.setMessage(mAlertMsg);
        }
    }


    @Override
    public void formsDownloadingComplete(HashMap<FormDetails, String> result) {
        if (mDownloadFormsTask != null) {
            mDownloadFormsTask.setDownloaderListener(null);
        }

        if (mProgressDialog.isShowing()) {
            // should always be true here
            mProgressDialog.dismiss();
        }

        Set<FormDetails> keys = result.keySet();
        StringBuilder b = new StringBuilder();
        for (FormDetails k : keys) {
            b.append(k.formName +
            	" (" +
            	((k.formVersion != null) ?
            			(this.getString(R.string.version) + ": " + k.formVersion + " ")
            			: "") +
            	"ID: " + k.formID + ") - " +
            	result.get(k));
            b.append("\n\n");
        }
       clearAllOption();
       createAlertDialog(getString(R.string.download_forms_result), b.toString().trim(), EXIT);
    }
    
    private void setupSearchView(MenuItem searchItem)
    {
    	mSearchView.setOnQueryTextListener(this);
    }
 
    public boolean onQueryTextChange(String newText){
    	ArrayList<HashMap<String, String>> mFormList=new ArrayList<HashMap<String,String>>();
    	for (int i=0; i<this.mFormList.size();i++)
    		if (this.mFormList.get(i).get(FORMNAME).toUpperCase().contains(newText.toUpperCase()))
    			mFormList.add(this.mFormList.get(i));
    	
    	String[] data = new String[] {FORMNAME, FORMID_DISPLAY, FORMDETAIL_KEY};
        int[] view = new int[] {R.id.text1, R.id.text2};
        
    	 mFormListAdapter =new SimpleAdapter(this, mFormList, R.layout.listview_item_download_form, data, view);
         setListAdapter(mFormListAdapter);
         
        /* ListView ls = new ListView(getApplicationContext());
         ls.setAdapter(mFormListAdapter);*/
         for (int i=0;i<mFormList.size();i++)
         {
        	 if (mSelected.contains(mFormList.get(i).get(FORMDETAIL_KEY)))
        		 getListView().setItemChecked(i, true);
         }
    	
        return false;
    }
 
    public boolean onQueryTextSubmit(String query) {
        return false;
    }
 
    public boolean onClose() {
        
        return false;
    }
 
    protected boolean isAlwaysExpanded() {
        return false;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.menu_activity_download_form, menu);
        
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        mSearchView = (SearchView) searchItem.getActionView();
        setupSearchView(searchItem);
        
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
	        case R.id.menu_download:
	        	getFormsOption();
	        return true;
	        case R.id.menu_settings:
	        	startActivity(new Intent(this, ActivityPreferences.class));
	        	return true;
	        case R.id.menu_help:
	        	Help.helpDialog(this, getString(R.string.help_download));
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

}
