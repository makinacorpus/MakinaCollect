package com.makina.collect.android.widgets;

import java.io.File;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Toast;

import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.listeners.WidgetAnsweredListener;
import com.makina.collect.android.utilities.FileUtils;
import com.makina.collect.android.utilities.MediaUtils;
import com.makina.collect.android.utilities.StaticMethods;
import com.makina.collect.android.views.CustomFontButton;

/**
 * Widget that allows user to take pictures, sounds or video and add them to the
 * form.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class VideoWidget extends QuestionWidget implements IBinaryWidget {
	private final static String t = "MediaWidget";

	private CustomFontButton mCaptureButton;
	private CustomFontButton mPlayButton;
	private CustomFontButton mChooseButton;

	private String mBinaryName;

	private String mInstanceFolder;
	

	private WidgetAnsweredListener mWidgetAnsweredListener;

	public VideoWidget(Activity activity, WidgetAnsweredListener widgetAnsweredListener, FormEntryPrompt prompt) {
		super(activity, widgetAnsweredListener, prompt);

		mWidgetAnsweredListener = widgetAnsweredListener;
		mWidgetAnsweredListener.setAnswerChange(false);
		
		mInstanceFolder = Collect.getInstance().getFormController()
				.getInstancePath().getParent();

		setOrientation(LinearLayout.VERTICAL);

		TableLayout.LayoutParams params = new TableLayout.LayoutParams();
		params.setMargins(7, 5, 7, 5);
		// setup capture button
		mCaptureButton = (CustomFontButton)activity.getLayoutInflater().inflate(R.layout.widget_button, null);
		mCaptureButton.setId(QuestionWidget.newUniqueId());
		mCaptureButton.setText(getContext().getString(R.string.capture_video));
		mCaptureButton
				.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
		mCaptureButton.setPadding(20, 20, 20, 20);
		mCaptureButton.setEnabled(!prompt.isReadOnly());
		mCaptureButton.setLayoutParams(params);
		mCaptureButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_camera, 0, 0, 0);

		// launch capture intent on click
		mCaptureButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(
						android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
				i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
						Video.Media.EXTERNAL_CONTENT_URI.toString());
				try {
					Collect.getInstance().getFormController()
							.setIndexWaitingForData(mPrompt.getIndex());
					mWidgetAnsweredListener.setAnswerChange(true);
					((Activity) getContext()).startActivityForResult(i,
							StaticMethods.VIDEO_CAPTURE);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(
							getContext(),
							getContext().getString(R.string.activity_not_found,
									"capture video"), Toast.LENGTH_SHORT)
							.show();
					Collect.getInstance().getFormController()
							.setIndexWaitingForData(null);
				}

			}
		});

		// setup capture button
		mChooseButton = (CustomFontButton)activity.getLayoutInflater().inflate(R.layout.widget_button, null);
		mChooseButton.setId(QuestionWidget.newUniqueId());
		mChooseButton.setText(getContext().getString(R.string.choose_video));
		mChooseButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
		mChooseButton.setPadding(20, 20, 20, 20);
		mChooseButton.setEnabled(!prompt.isReadOnly());
		mChooseButton.setLayoutParams(params);
		mChooseButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_gallery, 0, 0, 0);

		// launch capture intent on click
		mChooseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_GET_CONTENT);
				i.setType("video/*");
				// Intent i =
				// new Intent(Intent.ACTION_PICK,
				// android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
				try {
					Collect.getInstance().getFormController()
							.setIndexWaitingForData(mPrompt.getIndex());
					mWidgetAnsweredListener.setAnswerChange(true);
					((Activity) getContext()).startActivityForResult(i,
							StaticMethods.VIDEO_CHOOSER);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(
							getContext(),
							getContext().getString(R.string.activity_not_found,
									"choose video "), Toast.LENGTH_SHORT)
							.show();
					Collect.getInstance().getFormController()
							.setIndexWaitingForData(null);
				}

			}
		});

		// setup play button
		mPlayButton = (CustomFontButton)activity.getLayoutInflater().inflate(R.layout.widget_button, null);
		mPlayButton.setId(QuestionWidget.newUniqueId());
		mPlayButton.setText(getContext().getString(R.string.play_video));
		mPlayButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
		mPlayButton.setPadding(20, 20, 20, 20);
		mPlayButton.setLayoutParams(params);
		mPlayButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_play_clip, 0, 0, 0);
		
		// on play, launch the appropriate viewer
		mPlayButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent("android.intent.action.VIEW");
				File f = new File(mInstanceFolder + File.separator
						+ mBinaryName);
				i.setDataAndType(Uri.fromFile(f), "video/*");
				try {
					((Activity) getContext()).startActivity(i);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(
							getContext(),
							getContext().getString(R.string.activity_not_found,
									"video video"), Toast.LENGTH_SHORT).show();
				}
			}
		});

		// retrieve answer from data model and update ui
		mBinaryName = prompt.getAnswerText();
		if (mBinaryName != null) {
			mPlayButton.setEnabled(true);
		} else {
			mPlayButton.setEnabled(false);
		}

		// finish complex layout
		addView(mCaptureButton);
		addView(mChooseButton);
		addView(mPlayButton);

		// and hide the capture and choose button if read-only
		if (mPrompt.isReadOnly()) {
			mCaptureButton.setVisibility(View.GONE);
			mChooseButton.setVisibility(View.GONE);
		}

	}

    private void deleteMedia() {
        // get the file path and delete the file
    	String name = mBinaryName;
        // clean up variables
    	mBinaryName = null;
    	// delete from media provider
        int del = MediaUtils.deleteVideoFileFromMediaProvider(mInstanceFolder + File.separator + name);
        Log.i(t, "Deleted " + del + " rows from media content provider");
    }

	@Override
	public void clearAnswer() {
		// remove the file
		deleteMedia();

		// reset buttons
		mPlayButton.setEnabled(false);
	}

	@Override
	public IAnswerData getAnswer() {
		if (mBinaryName != null) {
			return new StringData(mBinaryName.toString());
		} else {
			return null;
		}
	}

	private String getPathFromUri(Uri uri) {
		if (uri.toString().startsWith("file")) {
			return uri.toString().substring(6);
		} else {
			String[] videoProjection = { MediaColumns.DATA };
			Cursor c = null;
			try {
				c = getContext().getContentResolver().query(uri,
						videoProjection, null, null, null);
				int column_index = c.getColumnIndexOrThrow(MediaColumns.DATA);
				String videoPath = null;
				if (c.getCount() > 0) {
					c.moveToFirst();
					videoPath = c.getString(column_index);
				}
				return videoPath;
			} finally {
				if (c != null) {
					c.close();
				}
			}
		}
	}

	@Override
	public void setBinaryData(Object binaryuri) {
		// you are replacing an answer. remove the media.
		if (mBinaryName != null) {
			deleteMedia();
		}

		// get the file path and create a copy in the instance folder
		String binaryPath = getPathFromUri((Uri) binaryuri);
		String extension = binaryPath.substring(binaryPath.lastIndexOf("."));
		String destVideoPath = mInstanceFolder + File.separator
				+ System.currentTimeMillis() + extension;

		File source = new File(binaryPath);
		File newVideo = new File(destVideoPath);
		FileUtils.copyFile(source, newVideo);

		if (newVideo.exists()) {
			// Add the copy to the content provier
			ContentValues values = new ContentValues(6);
			values.put(MediaColumns.TITLE, newVideo.getName());
			values.put(MediaColumns.DISPLAY_NAME, newVideo.getName());
			values.put(MediaColumns.DATE_ADDED, System.currentTimeMillis());
			values.put(MediaColumns.DATA, newVideo.getAbsolutePath());

			Uri VideoURI = getContext().getContentResolver().insert(
					Video.Media.EXTERNAL_CONTENT_URI, values);
			Log.i(t, "Inserting VIDEO returned uri = " + VideoURI.toString());
		} else {
			Log.e(t, "Inserting Video file FAILED");
		}

		mBinaryName = newVideo.getName();
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
		mCaptureButton.setOnLongClickListener(l);
		mChooseButton.setOnLongClickListener(l);
		mPlayButton.setOnLongClickListener(l);
	}

	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		mCaptureButton.cancelLongPress();
		mChooseButton.cancelLongPress();
		mPlayButton.cancelLongPress();
	}

}
