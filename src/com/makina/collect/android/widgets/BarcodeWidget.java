package com.makina.collect.android.widgets;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Toast;

import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.listeners.WidgetAnsweredListener;
import com.makina.collect.android.utilities.StaticMethods;
import com.makina.collect.android.views.CustomFontTextview;

/**
 * Widget that allows user to scan barcodes and add them to the form.
 * 
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class BarcodeWidget extends QuestionWidget implements IBinaryWidget {
	private Button mGetBarcodeButton;
	private CustomFontTextview mStringAnswer;

	public BarcodeWidget(Activity activity, WidgetAnsweredListener widgetAnsweredListener, FormEntryPrompt prompt) {
		super(activity, widgetAnsweredListener, prompt);
		setOrientation(LinearLayout.VERTICAL);

		TableLayout.LayoutParams params = new TableLayout.LayoutParams();
		params.setMargins(7, 5, 7, 5);

		// set button formatting
		mGetBarcodeButton = (Button)activity.getLayoutInflater().inflate(R.layout.widget_button, null);
		mGetBarcodeButton.setId(QuestionWidget.newUniqueId());
		mGetBarcodeButton.setText(getContext().getString(R.string.get_barcode));
		mGetBarcodeButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
				mAnswerFontsize);
		mGetBarcodeButton.setPadding(20, 20, 20, 20);
		mGetBarcodeButton.setEnabled(!prompt.isReadOnly());
		mGetBarcodeButton.setLayoutParams(params);
		mGetBarcodeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_barcode, 0, 0, 0);

		// launch barcode capture intent on click
		mGetBarcodeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent("com.google.zxing.client.android.SCAN");
				try {
					Collect.getInstance().getFormController()
							.setIndexWaitingForData(mPrompt.getIndex());
					((Activity) getContext()).startActivityForResult(i,
							StaticMethods.BARCODE_CAPTURE);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(
							getContext(),
							getContext().getString(
									R.string.barcode_scanner_error),
							Toast.LENGTH_SHORT).show();
					Collect.getInstance().getFormController()
							.setIndexWaitingForData(null);
				}
			}
		});

		// set text formatting
		mStringAnswer = new CustomFontTextview(getContext());
		mStringAnswer.setId(QuestionWidget.newUniqueId());
		mStringAnswer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
		mStringAnswer.setGravity(Gravity.CENTER);

		String s = prompt.getAnswerText();
		if (s != null) {
			mGetBarcodeButton.setText(getContext().getString(
					R.string.replace_barcode));
			mStringAnswer.setText(s);
		}
		// finish complex layout
		addView(mGetBarcodeButton);
		addView(mStringAnswer);
	}

	@Override
	public void clearAnswer() {
		mStringAnswer.setText(null);
		mGetBarcodeButton.setText(getContext().getString(R.string.get_barcode));
	}

	@Override
	public IAnswerData getAnswer() {
		String s = mStringAnswer.getText().toString();
		if (s == null || s.equals("")) {
			return null;
		} else {
			return new StringData(s);
		}
	}

	/**
	 * Allows answer to be set externally in {@Link FormEntryActivity}.
	 */
	@Override
	public void setBinaryData(Object answer) {
		mStringAnswer.setText((String) answer);
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
		mStringAnswer.setOnLongClickListener(l);
		mGetBarcodeButton.setOnLongClickListener(l);
	}

	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		mGetBarcodeButton.cancelLongPress();
		mStringAnswer.cancelLongPress();
	}

}
