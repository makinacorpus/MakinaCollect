/*
 * Copyright (C) 2013 47 Degrees, LLC
 *  http://47deg.com
 *  hello@47deg.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.makina.collect.android.swipelistview.adapters;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.makina.collect.android.R;
import com.fortysevendeg.swipelistview.SwipeListView;

import java.util.List;

public class PackageAdapter extends BaseAdapter {

    private List<FormItem> data;
    private Context context;

    public PackageAdapter(Context context, List<FormItem> data) {
        this.context = context;
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public FormItem getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

//    @Override
//    public boolean isEnabled(int position) {
//        if (position == 2) {
//            return false;
//        } else {
//            return true;
//        }
//    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final FormItem item = getItem(position);
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.listview_item_edit_form, parent, false);
            holder = new ViewHolder();
            holder.textView_name = (TextView) convertView.findViewById(R.id.text1);
            holder.textView_version = (TextView) convertView.findViewById(R.id.text2);
            holder.textView_date = (TextView) convertView.findViewById(R.id.text3);
            holder.imageView_delete = (ImageView) convertView.findViewById(R.id.imageView_delete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ((SwipeListView)parent).recycle(convertView, position);

        holder.textView_name.setText(item.getName());
        holder.textView_version.setText(item.getVersion());
        holder.textView_date.setText(item.getDate());


        holder.imageView_delete.setOnClickListener(new View.OnClickListener()
        {
            @Override
            @TargetApi(14)
            public void onClick(View v)
            {
               
            }
        });


        return convertView;
    }

    static class ViewHolder {
        TextView textView_name;
        TextView textView_version;
        TextView textView_date;
        ImageView imageView_delete;
    }

    private boolean isPlayStoreInstalled() {
        Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=dummy"));
        PackageManager manager = context.getPackageManager();
        List<ResolveInfo> list = manager.queryIntentActivities(market, 0);

        return list.size() > 0;
    }

}
