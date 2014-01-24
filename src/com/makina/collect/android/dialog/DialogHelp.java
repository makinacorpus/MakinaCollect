package com.makina.collect.android.dialog;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.astuetz.PagerSlidingTabStrip.IconTabProvider;
import com.makina.collect.android.R;
import com.makina.collect.android.utilities.Finish;

@SuppressLint("NewApi")
public class DialogHelp extends DialogFragment{

	private PagerSlidingTabStrip tabs;
	private ViewPager pager;
	private ContactPagerAdapter adapter;
	public static int position=0;

	public DialogHelp(int pos)
	{
		super();
		position=pos;
	}
	public DialogHelp()
	{
		super();
	}
	public static DialogHelp newInstance() {
		DialogHelp f = new DialogHelp();
		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (getDialog() != null) {
			getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
			getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		}

		View root = inflater.inflate(R.layout.dialog_help, container, false);

		tabs = (PagerSlidingTabStrip) root.findViewById(R.id.tabs);
		pager = (ViewPager) root.findViewById(R.id.pager);
		adapter = new ContactPagerAdapter();

		pager.setAdapter(adapter);
		pager.setCurrentItem(position);
		
		tabs.setViewPager(pager);

		return root;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onStart() {
		super.onStart();

		// change dialog width
		if (getDialog() != null) {

			int fullWidth = getDialog().getWindow().getAttributes().width;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
				Display display = getActivity().getWindowManager().getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				fullWidth = size.x;
			} else {
				Display display = getActivity().getWindowManager().getDefaultDisplay();
				fullWidth = display.getWidth();
			}

			final int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources()
					.getDisplayMetrics());

			int w = fullWidth - padding;
			int h = getDialog().getWindow().getAttributes().height;

			getDialog().getWindow().setLayout(w, h);
		}
	}

	public class ContactPagerAdapter extends PagerAdapter implements IconTabProvider {

		private final int[] ICONS = { R.drawable.help_download, R.drawable.help_edit,
				R.drawable.help_save, R.drawable.help_send };

		public ContactPagerAdapter() {
			super();
		}

		@Override
		public int getCount() {
			return ICONS.length;
		}

		@Override
		public int getPageIconResId(int position) {
			return ICONS[position];
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			// looks a little bit messy here
			View view= View.inflate(getActivity(), R.layout.fragment_help, null);
			TextView textview_help=(TextView) view.findViewById(R.id.textview_help);
			if (position==0)
				textview_help.setText(getString(R.string.help_download));
			else if (position==1)
				textview_help.setText(getString(R.string.help_edit));
			else if (position==2)
				textview_help.setText(getString(R.string.help_save));
			else if (position==3)
				textview_help.setText(getString(R.string.help_send));
			container.addView(view, 0);
			return view;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object view) {
			container.removeView((View) view);
		}

		@Override
		public boolean isViewFromObject(View v, Object o) {
			return v == ((View) o);
		}

	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		// TODO Auto-generated method stub
		if (getActivity()!=null)
			Finish.activityHelp.finish();
	}
	
}
