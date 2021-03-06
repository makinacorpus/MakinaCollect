package com.makina.collect.widgets;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Toast;

import com.makina.collect.R;
import com.makina.collect.application.Collect;
import com.makina.collect.listeners.WidgetAnsweredListener;
import com.makina.collect.utilities.MediaUtils;
import com.makina.collect.utilities.StaticMethods;
import com.makina.collect.views.CustomFontButton;
import com.makina.collect.views.CustomFontTextview;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;

import java.io.File;
import java.util.Date;

/**
 * Widget that allows user to take pictures, sounds or video and add them to the
 * form.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class ImageWebViewWidget extends QuestionWidget implements IBinaryWidget {
	private final static String t = "MediaWidget";

	private CustomFontButton mCaptureButton;
	private CustomFontButton mChooseButton;
	private WebView mImageDisplay;

	private String mBinaryName;

	private String mInstanceFolder;

	private CustomFontTextview mErrorTextView;

	private String constructImageElement() {
		File f = new File(mInstanceFolder + File.separator + mBinaryName);

		Display display = ((WindowManager) getContext().getSystemService(
				Context.WINDOW_SERVICE)).getDefaultDisplay();
		int screenWidth = display.getWidth();
		// int screenHeight = display.getHeight();

		String imgElement = f.exists() ? ("<img align=\"middle\" src=\"file:///"
				+ f.getAbsolutePath()
				+
				// Appending the time stamp to the filename is a hack to prevent
				// caching.
				"?"
				+ new Date().getTime()
				+ "\" width=\""
				+ Integer.toString(screenWidth - 10) + "\" >")
				: "";

		return imgElement;
	}

	@Override
	public boolean suppressFlingGesture(MotionEvent e1, MotionEvent e2,
			float velocityX, float velocityY) {
		if (mImageDisplay == null
				|| mImageDisplay.getVisibility() != View.VISIBLE) {
			return false;
		}

		Rect rect = new Rect();
		mImageDisplay.getHitRect(rect);

		// Log.i(t, "hitRect: " + rect.left + "," + rect.top + " : " +
		// rect.right + "," + rect.bottom );
		// Log.i(t, "e1 Raw, Clean: " + e1.getRawX() + "," + e1.getRawY() +
		// " : " + e1.getX() + "," + e1.getY());
		// Log.i(t, "e2 Raw, Clean: " + e2.getRawX() + "," + e2.getRawY() +
		// " : " + e2.getX() + "," + e2.getY());

		// starts in WebView
		if (rect.contains((int) e1.getRawX(), (int) e1.getRawY())) {
			return true;
		}

		// ends in WebView
		if (rect.contains((int) e2.getRawX(), (int) e2.getRawY())) {
			return true;
		}

		// transits WebView
		if (rect.contains((int) ((e1.getRawX() + e2.getRawX()) / 2.0),
				(int) ((e1.getRawY() + e2.getRawY()) / 2.0))) {
			return true;
		}
		// Log.i(t, "NOT SUPPRESSED");
		return false;
	}

	public ImageWebViewWidget(Activity activity, WidgetAnsweredListener widgetAnsweredListener, FormEntryPrompt prompt) {
		super(activity, widgetAnsweredListener, prompt);

		mInstanceFolder = Collect.getInstance().getFormController()
				.getInstancePath().getParent();

		setOrientation(LinearLayout.VERTICAL);

		TableLayout.LayoutParams params = new TableLayout.LayoutParams();
		params.setMargins(7, 5, 7, 5);

		mErrorTextView = new CustomFontTextview(activity);
		mErrorTextView.setId(QuestionWidget.newUniqueId());
		mErrorTextView.setText("Selected file is not a valid image");

		// setup capture button
		mCaptureButton = (CustomFontButton)activity.getLayoutInflater().inflate(R.layout.widget_button, null);
		mCaptureButton.setId(QuestionWidget.newUniqueId());
		mCaptureButton.setText(getContext().getString(R.string.capture_image));
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
				mErrorTextView.setVisibility(View.GONE);
				Intent i = new Intent(
						android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
				// We give the camera an absolute filename/path where to put the
				// picture because of bug:
				// http://code.google.com/p/android/issues/detail?id=1480
				// The bug appears to be fixed in Android 2.0+, but as of feb 2,
				// 2010, G1 phones only run 1.6. Without specifying the path the
				// images returned by the camera in 1.6 (and earlier) are ~1/4
				// the size. boo.

				// if this gets modified, the onActivityResult in
				// FormEntyActivity will also need to be updated.
				i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
						Uri.fromFile(new File(Collect.TMPFILE_PATH)));
				try {
					Collect.getInstance().getFormController()
							.setIndexWaitingForData(mPrompt.getIndex());
					((Activity) getContext()).startActivityForResult(i,
							StaticMethods.IMAGE_CAPTURE);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(
							getContext(),
							getContext().getString(R.string.activity_not_found,
									"image capture"), Toast.LENGTH_SHORT)
							.show();
					Collect.getInstance().getFormController()
							.setIndexWaitingForData(null);
				}

			}
		});

		// setup chooser button
		mChooseButton = (CustomFontButton)activity.getLayoutInflater().inflate(R.layout.widget_button, null);
		mChooseButton.setId(QuestionWidget.newUniqueId());
		mChooseButton.setText(getContext().getString(R.string.choose_image));
		mChooseButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
		mChooseButton.setPadding(20, 20, 20, 20);
		mChooseButton.setEnabled(!prompt.isReadOnly());
		mChooseButton.setLayoutParams(params);
        mChooseButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_gallery, 0, 0, 0);

		// launch capture intent on click
		mChooseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mErrorTextView.setVisibility(View.GONE);
				Intent i = new Intent(Intent.ACTION_GET_CONTENT);
				i.setType("image/*");

				try {
					Collect.getInstance().getFormController()
							.setIndexWaitingForData(mPrompt.getIndex());
					((Activity) getContext()).startActivityForResult(i,
							StaticMethods.IMAGE_CHOOSER);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(
							getContext(),
							getContext().getString(R.string.activity_not_found,
									"choose image"), Toast.LENGTH_SHORT).show();
					Collect.getInstance().getFormController()
							.setIndexWaitingForData(null);
				}

			}
		});

		// finish complex layout
		addView(mCaptureButton);
		addView(mChooseButton);
		addView(mErrorTextView);

		// and hide the capture and choose button if read-only
		if (prompt.isReadOnly()) {
			mCaptureButton.setVisibility(View.GONE);
			mChooseButton.setVisibility(View.GONE);
		}
		mErrorTextView.setVisibility(View.GONE);

		// retrieve answer from data model and update ui
		mBinaryName = prompt.getAnswerText();

		// Only add the imageView if the user has taken a picture
		if (mBinaryName != null) {
			mImageDisplay = new WebView(getContext());
			mImageDisplay.setId(QuestionWidget.newUniqueId());
			mImageDisplay.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
			mImageDisplay.getSettings().setBuiltInZoomControls(true);
			mImageDisplay.getSettings().setDefaultZoom(
					WebSettings.ZoomDensity.FAR);
			mImageDisplay.setVisibility(View.VISIBLE);
			mImageDisplay.setLayoutParams(params);

			// HTML is used to display the image.
			String html = "<body>" + constructImageElement() + "</body>";

			mImageDisplay.loadDataWithBaseURL("file:///" + mInstanceFolder
					+ File.separator, html, "text/html", "utf-8", "");
			addView(mImageDisplay);
		}
	}

    private void deleteMedia() {
        // get the file path and delete the file
    	String name = mBinaryName;
        // clean up variables
    	mBinaryName = null;
    	// delete from media provider
        int del = MediaUtils.deleteImageFileFromMediaProvider(mInstanceFolder + File.separator + name);
        Log.i(t, "Deleted " + del + " rows from media content provider");
    }

	@Override
	public void clearAnswer() {
		// remove the file
		deleteMedia();

		if (mImageDisplay != null) {
			// update HTML to not hold image file reference.
			String html = "<body></body>";
			mImageDisplay.loadDataWithBaseURL("file:///" + mInstanceFolder
					+ File.separator, html, "text/html", "utf-8", "");

			mImageDisplay.setVisibility(View.INVISIBLE);
		}

		mErrorTextView.setVisibility(View.GONE);

		// reset buttons
		mCaptureButton.setText(getContext().getString(R.string.capture_image));
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
    public void setBinaryData(Object newImageObj) {
        // you are replacing an answer. delete the previous image using the
        // content provider.
        if (mBinaryName != null) {
            deleteMedia();
        }

        File newImage = (File) newImageObj;
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
		mCaptureButton.setOnLongClickListener(l);
		mChooseButton.setOnLongClickListener(l);
	}

	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		mCaptureButton.cancelLongPress();
		mChooseButton.cancelLongPress();
	}

}
