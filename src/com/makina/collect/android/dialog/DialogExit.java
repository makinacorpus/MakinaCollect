package com.makina.collect.android.dialog;


import android.content.Context;
import android.content.DialogInterface;

import com.WazaBe.HoloEverywhere.app.AlertDialog;
import com.makina.collect.android.utilities.Finish;

public class DialogExit {

	public static void show(Context context)
    {
            AlertDialog.Builder adb = new AlertDialog.Builder(context);
            adb.setTitle("Quitter");
            adb.setMessage("Vous êtes sur le point de quitter l'application");
            adb.setIcon(android.R.drawable.ic_dialog_alert);
            adb.setNegativeButton(context.getString(android.R.string.cancel),null);
            adb.setPositiveButton(context.getString(android.R.string.yes), new AlertDialog.OnClickListener()
            {
                    public void onClick(DialogInterface dialog,int which)
                    {
                    	Finish.finish();
                    }
            });
            adb.show();
    }
}
