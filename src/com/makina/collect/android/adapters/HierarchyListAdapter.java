package com.makina.collect.android.adapters;

import java.util.List;

import com.makina.collect.android.R;
import com.makina.collect.android.logic.HierarchyElement;
import com.makina.collect.android.views.CustomFontTextview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;


public class HierarchyListAdapter extends BaseAdapter {
    
    private Context context;
    private static LayoutInflater inflater=null;
    private List<HierarchyElement> hierarchies;
    
    public HierarchyListAdapter(Context context)
    {
    	this.context = context;
        inflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }
    
    public View getView(final int position, final View convertView, ViewGroup parent) 
    {
        View vi=convertView;
         if(convertView==null)
            vi = inflater.inflate(R.layout.listview_item_hierarchy, null);

         CustomFontTextview textview_hierarchy_question=(CustomFontTextview)vi.findViewById(R.id.textview_hierarchy_question);
         CustomFontTextview textview_hierarchy_response=(CustomFontTextview)vi.findViewById(R.id.textview_hierarchy_response);
         
         textview_hierarchy_question.setText(hierarchies.get(position).getPrimaryText());
         textview_hierarchy_response.setText(hierarchies.get(position).getSecondaryText());
         return vi;
    }


	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return hierarchies.size();
	}
	public void setListItems(List<HierarchyElement> it) {
		hierarchies = it;
    }
}