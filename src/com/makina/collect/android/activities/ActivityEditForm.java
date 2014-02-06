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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.WazaBe.HoloEverywhere.app.AlertDialog;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.makina.collect.android.R;
import com.makina.collect.android.adapters.FormsListAdapter;
import com.makina.collect.android.dialog.DialogAboutUs;
import com.makina.collect.android.dialog.DialogExit;
import com.makina.collect.android.dialog.DialogHelpWithConfirmation;
import com.makina.collect.android.listeners.DeleteInstancesListener;
import com.makina.collect.android.listeners.DiskSyncListener;
import com.makina.collect.android.model.Form;
import com.makina.collect.android.preferences.ActivityPreferences;
import com.makina.collect.android.provider.FormsProvider;
import com.makina.collect.android.provider.FormsProviderAPI.FormsColumns;
import com.makina.collect.android.provider.InstanceProvider;
import com.makina.collect.android.tasks.DiskSyncTask;
import com.makina.collect.android.theme.Theme;
import com.makina.collect.android.utilities.Finish;
import com.makina.collect.android.views.CroutonView;
import com.makina.collect.android.views.CustomFontTextview;

import de.keyboardsurfer.mobile.app.android.widget.crouton.Style;
import de.timroes.swipetodismiss.SwipeDismissList;
import de.timroes.swipetodismiss.SwipeDismissList.UndoMode;
import de.timroes.swipetodismiss.SwipeDismissList.Undoable;

/**
 * Responsible for displaying all the valid forms in the forms directory. Stores the path to
 * selected form for use by {@link ActivityMainMenu}.
 * 
 * @author Yaw Anokwa (yanokwa@gmail.com)
 * @author Carl Hartung (carlhartung@gmail.com)
 */
@SuppressLint("NewApi")
public class ActivityEditForm extends SherlockListActivity implements DiskSyncListener, SearchView.OnQueryTextListener, DeleteInstancesListener {

    private static final String t = "FormChooserList";
    private static final String syncMsgKey = "syncmsgkey";

    private DiskSyncTask mDiskSyncTask;

    private AlertDialog mAlertDialog;
    
    private String statusText;
    private ArrayList<Long> mSelected;
    private FormsListAdapter instances;
    private Menu menu;
    private final int RESULT_PREFERENCES=1;
    private SearchView mSearchView;
    private List<Form> forms;
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
    	Theme.changeTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_form);
        
        Finish.activityEditForm=this;
        
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_title_layout_edit_form, null);
        getSupportActionBar().setCustomView(v);
        
        if (!getSharedPreferences("session", MODE_PRIVATE).getBoolean("help_edit", false))
                DialogHelpWithConfirmation.helpDialog(this, getString(R.string.help_title2), getString(R.string.help_edit));
            
        loadListView();
        
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
        new SwipeDismissList(getListView(), callback, mode);
        
    }
    
   
    private void createDialogDelete(final int position)
    {
    	final Form formDeleted=forms.get(position);
		forms.remove(position);
    	instances.notifyDataSetChanged();
        AlertDialog.Builder adb = new AlertDialog.Builder(ActivityEditForm.this);
        adb.setTitle(getString(R.string.delete));
        adb.setMessage(getString(R.string.delete_confirmation, formDeleted.getName()));
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
            	int countInstances=InstanceProvider.getCountInstancesByFormId(formDeleted.getForm_id());
            	if (countInstances>1)
                	CroutonView.showBuiltInCrouton(ActivityEditForm.this, countInstances+" "+getString(R.string.instances_exist), Style.ALERT);
                else if (countInstances==1)
                	CroutonView.showBuiltInCrouton(ActivityEditForm.this, getString(R.string.instance_exist), Style.ALERT);
        		else
        		{
        			FormsProvider.deleteFileOrDir(formDeleted.getFile_path());
        			FormsProvider.deleteFileOrDir(formDeleted.getDirectory_path());
        			FormsProvider.deleteForm(formDeleted.getForm_id());
        		}
            	
            	if (countInstances>0)
            	{
            		forms.add(position, formDeleted);
                	instances.notifyDataSetChanged();
            	}
            }
        });
        adb.show();
    }
    
    private void loadListView()
    {
        String sortOrder = FormsColumns.DISPLAY_NAME + " ASC, " + FormsColumns.JR_VERSION + " DESC";
        Cursor c = managedQuery(FormsColumns.CONTENT_URI, null, null, null, sortOrder);
        if(c.getCount()> 0)
		{
			forms=new ArrayList<Form>();
			while (c.moveToNext())
				forms.add(new Form(c.getInt(c.getColumnIndex(BaseColumns._ID)),c.getString(c.getColumnIndex(FormsColumns.JR_FORM_ID)),c.getString(c.getColumnIndex(FormsColumns.DISPLAY_NAME)), c.getString(c.getColumnIndex(FormsColumns.DISPLAY_SUBTEXT)),c.getString(c.getColumnIndex(FormsColumns.FORM_FILE_PATH)),c.getString(c.getColumnIndex(FormsColumns.FORM_MEDIA_PATH))));
			instances=new FormsListAdapter(this, forms);
	        setListAdapter(instances);
		}
        
    }
    
    public int convertDpToPixel(float dp) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int) px;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id)
    {
            ActivityForm.current_page=1;
           // get uri to form
               long idFormsTable = forms.get(position).getId();
         Uri formUri = ContentUris.withAppendedId(FormsColumns.CONTENT_URI, idFormsTable);

           String action = getIntent().getAction();
           if (Intent.ACTION_PICK.equals(action))
           {
               // caller is waiting on a picked form
               setResult(RESULT_OK, new Intent().setData(formUri));
           }
           else
           {
               // caller wants to view/edit a form, so launch formentryactivity
                   Intent i = new Intent(Intent.ACTION_EDIT, formUri);
                   i.putExtra("newForm", true);
            startActivity(i);
           }
           
           //TODO  
           //getActivity().finish();
       }
       
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.menu_activity_edit_form, menu);
        this.menu=menu;
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        mSearchView = (SearchView) searchItem.getActionView();
        int searchImgId = getResources().getIdentifier("android:id/search_button", null, null);
        ImageView searchButton = (ImageView) mSearchView.findViewById(searchImgId);
        if (searchButton!=null)
                searchButton.setImageResource(R.drawable.actionbar_search); 
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
    public boolean onQueryTextChange(String newText)
    {
         String sortOrder = FormsColumns.DISPLAY_NAME + " ASC, " + FormsColumns.JR_VERSION + " DESC";
         String condition=FormsColumns.DISPLAY_NAME+" LIKE '%"+newText+"%'";
         Cursor c = managedQuery(FormsColumns.CONTENT_URI, null, condition, null, sortOrder);
         if(c.getCount()> 0)
 		 {
        	forms=new ArrayList<Form>();
 			while (c.moveToNext())
 				forms.add(new Form(c.getInt(c.getColumnIndex(BaseColumns._ID)),c.getString(c.getColumnIndex(FormsColumns.JR_FORM_ID)),c.getString(c.getColumnIndex(FormsColumns.DISPLAY_NAME)), c.getString(c.getColumnIndex(FormsColumns.DISPLAY_SUBTEXT)),c.getString(c.getColumnIndex(FormsColumns.FORM_FILE_PATH)),c.getString(c.getColumnIndex(FormsColumns.FORM_MEDIA_PATH))));
 			instances=new FormsListAdapter(this, forms);
 	        setListAdapter(instances);
 		 }
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
                	Finish.finishHome();
                return true;
                case R.id.menu_settings:
                	startActivityForResult((new Intent(getApplicationContext(), ActivityPreferences.class)),RESULT_PREFERENCES);
                	return true;
                case R.id.menu_help:
                	Intent mIntent=new Intent(this, ActivityHelp.class);
                	Bundle mBundle=new Bundle();
                	mBundle.putInt("position", 1);
                	mIntent.putExtras(mBundle);
                	startActivity(mIntent);
                	return true;
                case R.id.menu_about_us:
                        DialogAboutUs.aboutUs(this);
                        return true;
                case R.id.menu_exit:
                		DialogExit.show(this);
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
        public boolean onQueryTextSubmit(String query) {
        	InputMethodManager imm = (InputMethodManager)getSystemService( Context.INPUT_METHOD_SERVICE);
        	imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            return false;
        }


        @Override
        public void deleteComplete(int deletedInstances) {
                // TODO Auto-generated method stub
                if (deletedInstances == mSelected.size()) {
                        // all deletes were successful
                        Toast.makeText(getApplicationContext(),getString(R.string.file_deleted_ok, deletedInstances),Toast.LENGTH_SHORT).show();
                } else {
                        // had some failures
                        Toast.makeText(getApplicationContext(),getString(R.string.file_deleted_error, mSelected.size()- deletedInstances, mSelected.size()),Toast.LENGTH_LONG).show();
                }
        }

        public boolean onKeyUp(int keyCode, KeyEvent event) {
	        if (keyCode == KeyEvent.KEYCODE_MENU) {
	        	menu.performIdentifierAction(R.id.menu_other, 0);
	         }
	        super.onKeyUp(keyCode, event);
	        return true;
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
        
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
        	// TODO Auto-generated method stub
        	super.onConfigurationChanged(newConfig);
        	Theme.changeTheme(this);
        	LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	View v;
        	if(newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE)
        		v = inflator.inflate(R.layout.actionbar_title_layout_edit_form_land, null);
            else
            	v = inflator.inflate(R.layout.actionbar_title_layout_edit_form, null);
            getSupportActionBar().setCustomView(v);
        }
}