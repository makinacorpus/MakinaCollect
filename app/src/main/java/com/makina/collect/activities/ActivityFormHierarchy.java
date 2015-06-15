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

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.makina.collect.R;
import com.makina.collect.activity.AbstractBaseActivity;
import com.makina.collect.activity.DashBoardActivity;
import com.makina.collect.adapters.HierarchyListAdapter;
import com.makina.collect.application.Collect;
import com.makina.collect.dialog.DialogAboutUs;
import com.makina.collect.logic.FormController;
import com.makina.collect.logic.HierarchyElement;
import com.makina.collect.preferences.ActivityPreferences;
import com.makina.collect.utilities.Finish;
import com.makina.collect.views.CustomFontTextview;
import com.makina.collect.views.CustomListViewExpanded;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.ArrayList;
import java.util.List;

public class ActivityFormHierarchy extends AbstractBaseActivity
		implements OnClickListener{

	private static final String t = "FormHierarchyActivity";

	private static final int CHILD = 1;
	private static final int EXPANDED = 2;
	private static final int COLLAPSED = 3;
	private static final int QUESTION = 4;

	private static final String mIndent = "     ";

	
	private boolean mIsSavedForm;
	private boolean mToFormChooser;

	List<HierarchyElement> formList;

	FormIndex mStartIndex;
	private CustomListViewExpanded listView_hierarchy;
	private final int RESULT_PREFERENCES=1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_form_hierarchy);

		Finish.activityFormHierarchy=this;
		
		FormController formController = Collect.getInstance().getFormController();
		Intent intent = getIntent();
		
		mIsSavedForm = (intent.hasExtra("isSavedForm")&&intent.getBooleanExtra("isSavedForm", false));
		mToFormChooser = intent.hasExtra("toFormChooser");
		// We use a static FormEntryController to make jumping faster.
		mStartIndex = formController.getFormIndex();

		getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayUseLogoEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_title_layout_edit_form, null);
        getSupportActionBar().setCustomView(v);
        
    	listView_hierarchy=(CustomListViewExpanded)findViewById(R.id.listView_hierarchy);
    	listView_hierarchy.setExpanded(true);
    	// kinda slow, but works.
		// this scrolls to the last question the user was looking at
    	listView_hierarchy.post(new Runnable() {
			@Override
			public void run() {
				int position = 0;
				for (int i = 0; i < listView_hierarchy.getAdapter().getCount(); i++) {
					HierarchyElement he = formList.get(i);
					if (mStartIndex.equals(he.getFormIndex())) {
						position = i;
						break;
					}
				}
				listView_hierarchy.setSelection(position);
			}
		});
		refreshView();
		
		listView_hierarchy.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> a, View v, int position, long id)
			{
				ActivityForm.current_page=position+1;
				HierarchyElement h = formList.get(position);
				FormIndex index = h.getFormIndex();
				if (index == null) {
					goUpLevel();
					return;
				}

				switch (h.getType()) {
				case EXPANDED:
					h.setType(COLLAPSED);
					ArrayList<HierarchyElement> children = h.getChildren();
					for (int i = 0; i < children.size(); i++) {
						formList.remove(position + 1);
					}
					h.setIcon(getResources().getDrawable(
							R.drawable.expander_ic_right));
					h.setColor(Color.BLACK);
					break;
				case COLLAPSED:
					h.setType(EXPANDED);
					ArrayList<HierarchyElement> children1 = h.getChildren();
					for (int i = 0; i < children1.size(); i++) {
						Log.i(t, "adding child: " + children1.get(i).getFormIndex());
						formList.add(position + 1 + i, children1.get(i));

					}
					h.setIcon(getResources().getDrawable(
							R.drawable.expander_ic_right));
					h.setColor(Color.BLACK);
					break;
				case QUESTION:
					Collect.getInstance().getFormController().jumpToIndex(index);
					if (Collect.getInstance().getFormController().indexIsInFieldList()) {
						Collect.getInstance().getFormController()
								.stepToPreviousScreenEvent();
					}
					setResult(RESULT_OK);
					finish();
					return;
				case CHILD:
					setResult(RESULT_OK);
					refreshView();
					return;
				}

				// Should only get here if we've expanded or collapsed a group
				HierarchyListAdapter itla = new HierarchyListAdapter(ActivityFormHierarchy.this);
				itla.setListItems(formList);
				listView_hierarchy.setAdapter(itla);
				listView_hierarchy.setSelection(position);
			}
		});
		 
		findViewById(R.id.linearLayout_footer).setOnClickListener(this);
	}

	private void goUpLevel() {
		Collect.getInstance().getFormController().stepToOuterScreenEvent();
		refreshView();
	}

	public void refreshView() {
		FormController formController = Collect.getInstance()
				.getFormController();
		// Record the current index so we can return to the same place if the
		// user hits 'back'.
		((CustomFontTextview)findViewById(R.id.textview_form_title)).setText(formController.getFormTitle());
		FormIndex currentIndex = formController.getFormIndex();

		// If we're not at the first level, we're inside a repeated group so we
		// want to only display
		// everything enclosed within that group.
		String enclosingGroupRef = "";
		formList = new ArrayList<HierarchyElement>();

		// If we're currently at a repeat node, record the name of the node and
		// step to the next
		// node to display.
		if (formController.getEvent() == FormEntryController.EVENT_REPEAT) {
			enclosingGroupRef = formController.getFormIndex().getReference()
					.toString(false);
			formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
		} else {
			FormIndex startTest = formController.stepIndexOut(currentIndex);
			// If we have a 'group' tag, we want to step back until we hit a
			// repeat or the
			// beginning.
			while (startTest != null
					&& formController.getEvent(startTest) == FormEntryController.EVENT_GROUP) {
				startTest = formController.stepIndexOut(startTest);
			}
			if (startTest == null) {
				// check to see if the question is at the first level of the
				// hierarchy. If it is,
				// display the root level from the beginning.
				formController.jumpToIndex(FormIndex
						.createBeginningOfFormIndex());
			} else {
				// otherwise we're at a repeated group
				formController.jumpToIndex(startTest);
			}

			// now test again for repeat. This should be true at this point or
			// we're at the
			// beginning
			if (formController.getEvent() == FormEntryController.EVENT_REPEAT) {
				enclosingGroupRef = formController.getFormIndex()
						.getReference().toString(false);
				formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
			}
		}

		int event = formController.getEvent();
		if (event == FormEntryController.EVENT_BEGINNING_OF_FORM) {
			// The beginning of form has no valid prompt to display.
			formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
		}

		// Refresh the current event in case we did step forward.
		event = formController.getEvent();

		// There may be repeating Groups at this level of the hierarchy, we use
		// this variable to
		// keep track of them.
		String repeatedGroupRef = "";

		event_search: while (event != FormEntryController.EVENT_END_OF_FORM) {
			switch (event) {
			case FormEntryController.EVENT_QUESTION:
				if (!repeatedGroupRef.equalsIgnoreCase("")) {
					// We're in a repeating group, so skip this question and
					// move to the next
					// index.
					event = formController
							.stepToNextEvent(FormController.STEP_INTO_GROUP);
					continue;
				}

				FormEntryPrompt fp = formController.getQuestionPrompt();
				String label = fp.getLongText();
				if (!fp.isReadOnly() || (label != null && label.length() > 0)) {
					// show the question if it is an editable field.
					// or if it is read-only and the label is not blank.
					formList.add(new HierarchyElement(fp.getLongText(), fp
							.getAnswerText(), null,
							R.drawable.linear_blue,
							QUESTION, fp.getIndex()));
				}
				break;
			case FormEntryController.EVENT_GROUP:
				// ignore group events
				break;
			case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
				if (enclosingGroupRef.compareTo(formController.getFormIndex()
						.getReference().toString(false)) == 0) {
					// We were displaying a set of questions inside of a
					// repeated group. This is
					// the end of that group.
					break event_search;
				}

				if (repeatedGroupRef.compareTo(formController.getFormIndex()
						.getReference().toString(false)) != 0) {
					// We're in a repeating group, so skip this repeat prompt
					// and move to the
					// next event.
					event = formController
							.stepToNextEvent(FormController.STEP_INTO_GROUP);
					continue;
				}

				if (repeatedGroupRef.compareTo(formController.getFormIndex()
						.getReference().toString(false)) == 0) {
					// This is the end of the current repeating group, so we
					// reset the
					// repeatedGroupName variable
					repeatedGroupRef = "";
				}
				break;
			case FormEntryController.EVENT_REPEAT:
				FormEntryCaption fc = formController.getCaptionPrompt();
				if (enclosingGroupRef.compareTo(formController.getFormIndex()
						.getReference().toString(false)) == 0) {
					// We were displaying a set of questions inside a repeated
					// group. This is
					// the end of that group.
					break event_search;
				}
				if (repeatedGroupRef.equalsIgnoreCase("")
						&& fc.getMultiplicity() == 0) {
					// This is the start of a repeating group. We only want to
					// display
					// "Group #", so we mark this as the beginning and skip all
					// of its children
					HierarchyElement group = new HierarchyElement(
							fc.getLongText(), null, getResources().getDrawable(
									R.drawable.expander_ic_right),
							Color.BLACK, COLLAPSED, fc.getIndex());
					repeatedGroupRef = formController.getFormIndex()
							.getReference().toString(false);
					formList.add(group);
				}

				if (repeatedGroupRef.compareTo(formController.getFormIndex()
						.getReference().toString(false)) == 0) {
					// Add this group name to the drop down list for this
					// repeating group.
					HierarchyElement h = formList.get(formList.size() - 1);
					h.addChild(new HierarchyElement(mIndent + fc.getLongText()
							+ " " + (fc.getMultiplicity() + 1), null, null,
							Color.BLACK, CHILD, fc.getIndex()));
				}
				break;
			}
			event = formController
					.stepToNextEvent(FormController.STEP_INTO_GROUP);
		}

		HierarchyListAdapter itla = new HierarchyListAdapter(this);
		itla.setListItems(formList);
		listView_hierarchy.setAdapter(itla);

		// set the controller back to the current index in case the user hits
		// 'back'
		formController.jumpToIndex(currentIndex);
	}

	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (mIsSavedForm){
				Intent i;
				if (mToFormChooser){
					i  = new Intent(this, DashBoardActivity.class);
					i.putExtra("drawerSelection", 0);
				}else{
					i  = new Intent(this, DashBoardActivity.class);
					i.putExtra("drawerSelection", 1);
				}
				startActivity(i);
			}else{
				Collect.getInstance().getFormController().jumpToIndex(mStartIndex);
				
			}
		}
		return super.onKeyDown(keyCode, event);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.settings,
                                  menu);

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
		    	if (mIsSavedForm){
					Log.e(getClass().getName(), "Back to List");
					Intent i;
					if (mToFormChooser){
						i  = new Intent(this, DashBoardActivity.class);
						i.putExtra("drawerSelection", 0);
					}else{
						i  = new Intent(this, DashBoardActivity.class);
						i.putExtra("drawerSelection", 1);
					}
					startActivity(i);
				}else{
					Collect.getInstance().getFormController().jumpToIndex(mStartIndex);
					finish();
				}
			return true;
	        case R.id.menu_settings:
	        	startActivityForResult((new Intent(this, ActivityPreferences.class)),RESULT_PREFERENCES);
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
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }

	@Override
	public void onClick(View v)
	{
		switch (v.getId()) {
		case R.id.linearLayout_footer:
			finish();
			break;
		}
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
    		v = inflator.inflate(R.layout.actionbar_title_layout_edit_form_land, null);
        else
        	v = inflator.inflate(R.layout.actionbar_title_layout_edit_form, null);
        getSupportActionBar().setCustomView(v);
    }
    */
}
