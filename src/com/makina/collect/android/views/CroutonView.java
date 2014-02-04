package com.makina.collect.android.views;

import com.makina.collect.android.R.color;

import android.app.Activity;
import android.graphics.Color;
import android.app.Activity;
import android.view.View;
import de.keyboardsurfer.mobile.app.android.widget.crouton.Configuration;
import de.keyboardsurfer.mobile.app.android.widget.crouton.Crouton;
import de.keyboardsurfer.mobile.app.android.widget.crouton.Style;

public class CroutonView {

	private static final Style INFINITE = new Style.Builder()
			.setBackgroundColorValue(color.crouton_color).build();
	private static final Configuration CONFIGURATION_INFINITE = new Configuration.Builder()
			.setDuration(Configuration.DURATION_INFINITE).build();

	public static void showBuiltInCrouton(Activity activity,
			String croutonText, final Style croutonStyle) {
		final boolean infinite = INFINITE == croutonStyle;
		final Crouton crouton;
		crouton = Crouton.makeText(activity, croutonText, INFINITE);
		crouton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		})
				.setConfiguration(
						infinite ? CONFIGURATION_INFINITE
								: Configuration.DEFAULT).show();
	}

}
