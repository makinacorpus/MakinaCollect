/*
 * Copyright (C) 2012 University of Washington
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

package com.makina.collect.adapters;


import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.makina.collect.R;
import com.makina.collect.model.Form;
import com.makina.collect.theme.Theme;
import com.makina.collect.views.CustomFontTextview;

import java.util.List;

/**
 * Implementation of cursor adapter that displays the version of a form if a form has a version.
 *
 * @author mitchellsundt@gmail.com
 */
public class FormsListAdapter extends BaseAdapter {
	
	private Activity activity;
    private static LayoutInflater inflater=null;
    private List<Form> forms;

    public FormsListAdapter(Activity activity, List<Form> forms)
    {
    	Theme.changeTheme(activity);
    	this.activity = activity;
        inflater = (LayoutInflater)this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.forms=forms;
    }
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return forms.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return arg0;
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return arg0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup arg2) {
		 View vi=convertView;
         if(convertView==null)
            vi = inflater.inflate(R.layout.listview_item_save_form, null);

         CustomFontTextview text1=(CustomFontTextview)vi.findViewById(R.id.text1);
         CustomFontTextview text2=(CustomFontTextview)vi.findViewById(R.id.text2);
         
         
         text1.setText(forms.get(position).getName());
         text2.setText(forms.get(position).getDescription());
         return vi;
	}
	
}