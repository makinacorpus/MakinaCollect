package com.makina.collect.widgets;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Toast;

import com.makina.collect.R;
import com.makina.collect.activities.ActivityDraw;
import com.makina.collect.application.Collect;
import com.makina.collect.listeners.WidgetAnsweredListener;
import com.makina.collect.utilities.FileUtils;
import com.makina.collect.utilities.MediaUtils;
import com.makina.collect.utilities.StaticMethods;
import com.makina.collect.views.CustomFontButton;
import com.makina.collect.views.CustomFontTextview;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;

import java.io.File;

/**
 * Free drawing widget.
 * 
 * @author BehrAtherton@gmail.com
 * 
 */
public class DrawWidget extends QuestionWidget implements IBinaryWidget {
	private final static String t = "DrawWidget";

	private CustomFontButton mDrawButton;
	private String mBinaryName;
	private String mInstanceFolder;
	private ImageView mImageView;
	private CustomFontTextview mErrorTextView;

	public DrawWidget(Activity activity, WidgetAnsweredListener widgetAnsweredListener, FormEntryPrompt prompt) {
		super(activity, widgetAnsweredListener, prompt);

		mErrorTextView = new CustomFontTextview(activity);
		mErrorTextView.setId(QuestionWidget.newUniqueId());
		mErrorTextView.setText("Selected file is not a valid image");

		mInstanceFolder = Collect.getInstance().getFormController()
				.getInstancePath().getParent();

		setOrientation(LinearLayout.VERTICAL);
		TableLayout.LayoutParams params = new TableLayout.LayoutParams();
		params.setMargins(7, 5, 7, 5);
		// setup Blank Image Button
		mDrawButton = (CustomFontButton)activity.getLayoutInflater().inflate(R.layout.widget_button, null);
		mDrawButton.setId(QuestionWidget.newUniqueId());
		mDrawButton.setText(getContext().getString(R.string.draw_image));
		mDrawButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
		mDrawButton.setPadding(20, 20, 20, 20);
		mDrawButton.setEnabled(!prompt.isReadOnly());
		mDrawButton.setLayoutParams(params);
		mDrawButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_edit, 0, 0, 0);
		
		// launch capture intent on click
		mDrawButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchDrawActivity();
			}
		});

		// finish complex layout
		addView(mDrawButton);
		addView(mErrorTextView);

		if (mPrompt.isReadOnly()) {
			mDrawButton.setVisibility(View.GONE);
		}
		mErrorTextView.setVisibility(View.GONE);

		// retrieve answer from data model and update ui
		mBinaryName = prompt.getAnswerText();

		// Only add the imageView if the user has signed
		if (mBinaryName != null) {
			mImageView = new ImageView(getContext());
			mImageView.setId(QuestionWidget.newUniqueId());
			Display display = ((WindowManager) getContext().getSystemService(
					Context.WINDOW_SERVICE)).getDefaultDisplay();
			int screenWidth = display.getWidth();
			int screenHeight = display.getHeight();

			File f = new File(mInstanceFolder + File.separator + mBinaryName);

			if (f.exists()) {
				Bitmap bmp = FileUtils.getBitmapScaledToDisplay(f,
						screenHeight, screenWidth);
				if (bmp == null) {
					mErrorTextView.setVisibility(View.VISIBLE);
				}
				mImageView.setImageBitmap(bmp);
			} else {
				mImageView.setImageBitmap(null);
			}

			mImageView.setPadding(10, 10, 10, 10);
			mImageView.setAdjustViewBounds(true);
			mImageView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					launchDrawActivity();
				}
			});

			addView(mImageView);
		}

	}

	private void launchDrawActivity() {
		mErrorTextView.setVisibility(View.GONE);
		Intent i = new Intent(getContext(), ActivityDraw.class);
		i.putExtra(ActivityDraw.OPTION, ActivityDraw.OPTION_DRAW);
		// copy...
		if (mBinaryName != null) {
			File f = new File(mInstanceFolder + File.separator + mBinaryName);
			i.putExtra(ActivityDraw.REF_IMAGE, Uri.fromFile(f));
		}
		i.putExtra(ActivityDraw.EXTRA_OUTPUT,
				Uri.fromFile(new File(Collect.TMPFILE_PATH)));

		try {
			Collect.getInstance().getFormController()
					.setIndexWaitingForData(mPrompt.getIndex());
			((Activity) getContext()).startActivityForResult(i,
					StaticMethods.DRAW_IMAGE);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(
					getContext(),
					getContext().getString(R.string.activity_not_found,
							"draw image"), Toast.LENGTH_SHORT).show();
			Collect.getInstance().getFormController()
					.setIndexWaitingForData(null);
		}
	}

	private void deleteMedia() {
		// get the file path and delete the file
		String name = mBinaryName;
		// clean up variables
		mBinaryName = null;
		// delete from media provider
		int del = MediaUtils.deleteImageFileFromMediaProvider(mInstanceFolder
				+ File.separator + name);
		Log.i(t, "Deleted " + del + " rows from media content provider");
	}

	@Override
	public void clearAnswer() {
		// remove the file
		deleteMedia();
		mImageView.setImageBitmap(null);
		mErrorTextView.setVisibility(View.GONE);

		// reset buttons
		mDrawButton.setText(getContext().getString(R.string.draw_image));
	}

	@Override
	public IAnswerData getAnswer() {
		if (mBinaryName != null) {
			return new StringData(mBinaryName.toString());
		} else {
			return null;
		}
	}

	@Override
	public void setBinaryData(Object answer) {
		// you are replacing an answer. delete the previous image using the
		// content provider.
		if (mBinaryName != null) {
			deleteMedia();
		}

		File newImage = (File) answer;
		if (newImage.exists()) {
			// Add the new image to the Media content provider so that the
			// viewing is fast in Android 2.0+
			ContentValues values = new ContentValues(6);
			values.put(MediaColumns.TITLE, newImage.getName());
			values.put(MediaColumns.DISPLAY_NAME, newImage.getName());
			values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
			values.put(MediaColumns.MIME_TYPE, "image/jpeg");
			values.put(MediaColumns.DATA, newImage.getAbsolutePath());

			Uri imageURI = getContext().getContentResolver().insert(
					Images.Media.EXTERNAL_CONTENT_URI, values);
			Log.i(t, "Inserting image returned uri = " + imageURI.toString());

			mBinaryName = newImage.getName();
			Log.i(t, "Setting current answer to " + newImage.getName());
		} else {
			Log.e(t, "NO IMAGE EXISTS at: " + newImage.getAbsolutePath());
		}

		Collect.getInstance().getFormController().setIndexWaitingForData(null);
	}

	@Override
	public void setFocus(Context context) {
		// Hide the soft keyboard if it's showing.
		InputMethodManager inputManager = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
	}

	@Override
	public boolean isWaitingForBinaryData() {
		return mPrompt.getIndex().equals(
				Collect.getInstance().getFormController()
						.getIndexWaitingForData());
	}

	@Override
	public void cancelWaitingForBinaryData() {
		Collect.getInstance().getFormController().setIndexWaitingForData(null);
	}

	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		mDrawButton.setOnLongClickListener(l);
		if (mImageView != null) {
			mImageView.setOnLongClickListener(l);
		}
	}

	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		mDrawButton.cancelLongPress();
		if (mImageView != null) {
			mImageView.cancelLongPress();
		}
	}

}
