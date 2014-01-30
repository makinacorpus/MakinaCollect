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
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.AttributeSet;
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
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.WazaBe.HoloEverywhere.app.AlertDialog;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.makina.collect.android.R;
import com.makina.collect.android.adapters.FormsListAdapter;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.dialog.DialogAboutUs;
import com.makina.collect.android.dialog.DialogExit;
import com.makina.collect.android.dialog.DialogHelpWithConfirmation;
import com.makina.collect.android.model.Form;
import com.makina.collect.android.preferences.ActivityPreferences;
import com.makina.collect.android.provider.InstanceProvider;
import com.makina.collect.android.provider.InstanceProviderAPI;
import com.makina.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import com.makina.collect.android.theme.Theme;
import com.makina.collect.android.utilities.Finish;

import de.timroes.swipetodismiss.SwipeDismissList;
import de.timroes.swipetodismiss.SwipeDismissList.UndoMode;
import de.timroes.swipetodismiss.SwipeDismissList.Undoable;
/**
 * Responsible for displaying all the valid instances in the instance directory.
 * 
 * @author Yaw Anokwa (yanokwa@gmail.com)
 * @author Carl Hartung (carlhartung@gmail.com)
 */
@SuppressLint("NewApi")
public class ActivitySaveForm extends SherlockListActivity implements SearchView.OnQueryTextListener{

	private AlertDialog mAlertDialog;
    private FormsListAdapter instances;
    private Menu menu;
    private final int RESULT_PREFERENCES=1;
    private SearchView mSearchView;
    private List<Form> forms;
    @SuppressLint("NewApi")
	@Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.menu_activity_save_form, menu);
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
	        	startActivityForResult((new Intent(this, ActivityPreferences.class)),RESULT_PREFERENCES);
	        	return true;
	        case R.id.menu_help:
	        	Intent mIntent=new Intent(this, ActivityHelp.class);
	        	Bundle mBundle=new Bundle();
	        	mBundle.putInt("position", 2);
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
    
    @SuppressLint("ResourceAsColor")
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Theme.changeTheme(this);
    	setContentView(R.layout.activity_save_form);
        
    	Finish.activitySaveForm=this;
    	
    	if (!getSharedPreferences("session", MODE_PRIVATE).getBoolean("help_saved", false))
    		DialogHelpWithConfirmation.helpDialog(this, getString(R.string.help_title3),getString(R.string.help_save));
    	
    	getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_title_layout_save_form, null);
        getSupportActionBar().setCustomView(v);
    	
        loadListView();
        
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
    	//final Cursor c=instances.getCursor();
    	final Form formDeleted=forms.get(position);
		forms.remove(position);
    	instances.notifyDataSetChanged();
		AlertDialog.Builder adb = new AlertDialog.Builder(ActivitySaveForm.this);
		adb.setTitle(getString(R.string.delete));
		adb.setMessage(getString(R.string.delete_confirmation,formDeleted.getName()));
		adb.setIconAttribute(android.R.attr.alertDialogIcon);
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
	        setListAdapter(instances);
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
     * Stores the path of selected instance in the parent class and finishes.
     */
    @Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
        
        Uri instanceUri =ContentUris.withAppendedId(InstanceColumns.CONTENT_URI,forms.get(position).getId());

        Collect.getInstance().getActivityLogger().logAction(this, "onListItemClick", instanceUri.toString());

        Intent intent=new Intent(Intent.ACTION_EDIT, instanceUri);
    	Bundle bundle=new Bundle();
    	bundle.putLong("id", forms.get(position).getId());
    	intent.putExtras(bundle);
    	startActivity(intent);
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
    	InputMethodManager imm = (InputMethodManager)getSystemService( Context.INPUT_METHOD_SERVICE);
    	imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
        return false;
    }
	public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
        	menu.performIdentifierAction(R.id.menu_other, 0);
         }
        super.onKeyUp(keyCode, event);
        return true;
     }
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
    	// TODO Auto-generated method stub
    	super.onConfigurationChanged(newConfig);
    	LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View v;
    	if(newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE)
    		v = inflator.inflate(R.layout.actionbar_title_layout_save_form_land, null);
        else
        	v = inflator.inflate(R.layout.actionbar_title_layout_save_form, null);
        getSupportActionBar().setCustomView(v);
    }

}
