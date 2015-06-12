/*
 * Copyright (C) 2011 University of Washington
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

package com.makina.collect.android.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

import com.makina.collect.android.R;
import com.makina.collect.android.activities.ActivityHelp;
import com.makina.collect.android.activities.ActivitySendForm;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.fragment.DashboardFragment;
import com.makina.collect.android.preferences.ActivityPreferences;
import com.makina.collect.android.provider.FormsProvider;
import com.makina.collect.android.provider.InstanceProvider;
import com.makina.collect.android.util.IntentUtils;
import com.makina.collect.android.util.SpannableUtils;
import com.makina.collect.android.utilities.Finish;

import java.io.File;

public class DashBoardActivity
        extends AbstractBaseActivity
        implements DashboardFragment.OnDashboardFragmentListener {

    private final int RESULT_PREFERENCES = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            final String appName = getString(R.string.app_name);
            final String substring = appName.split(" ").length == 2 ? appName.split(" ")[1] : null;
            actionBar.setTitle(SpannableUtils.makeTwoTextAppearanceSpannable(this,
                                                                             R.style.TextAppearance_ActionBar_Title_Green,
                                                                             R.style.TextAppearance_ActionBar_Title_Blue,
                                                                             appName,
                                                                             substring));
        }

        setContentView(R.layout.activity_single);

        Finish.dashBoardActivity = this;

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                                       .replace(android.R.id.content,
                                                DashboardFragment.newInstance())
                                       .commit();
        }

        if ((getIntent().getExtras() != null) && (!getIntent().getExtras()
                                                              .getBoolean(IntentUtils.FORMS_PATH_EMPTY,
                                                                          true))) {
            final File f = new File(Collect.FORMS_PATH);
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle(getString(R.string.delete));
            adb.setMessage(getString(R.string.delete_old_forms));
            adb.setIcon(android.R.attr.alertDialogIcon);
            adb.setNegativeButton(getString(android.R.string.cancel),
                                  new AlertDialog.OnClickListener() {
                                      public void onClick(DialogInterface dialog,
                                                          int which) {
                                          try {
                                              Collect.createODKDirs();
                                          }
                                          catch (RuntimeException e) {
                                              return;
                                          }
                                      }
                                  });
            adb.setPositiveButton(getString(android.R.string.yes),
                                  new AlertDialog.OnClickListener() {
                                      public void onClick(DialogInterface dialog,
                                                          int which) {
                                          InstanceProvider.deleteAllInstances();
                                          FormsProvider.deleteAllForms();
                                          for (File file : f.listFiles()) {
                                              file.delete();
                                          }

                                          try {
                                              Collect.createODKDirs();
                                          }
                                          catch (RuntimeException e) {
                                              return;
                                          }
                                      }
                                  });
            adb.setCancelable(false);
            adb.show();
            getIntent().removeExtra(IntentUtils.FORMS_PATH_EMPTY);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.settings,
                                  menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        // Handle action buttons for all fragments
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivityForResult((new Intent(this,
                                                   ActivityPreferences.class)),
                                       RESULT_PREFERENCES);
                return true;
            case R.id.menu_help:
                Intent intent = new Intent(this,
                                            ActivityHelp.class);
                Bundle bundle = new Bundle();
                bundle.putInt("position",
                               0);
                intent.putExtras(bundle);
                startActivity(intent);
                return true;
            case R.id.menu_about_us:
                startActivity(new Intent(this,
                                         AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        if (requestCode == RESULT_PREFERENCES) {
            Intent i = getIntent();
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    }

    @Override
    public void onFormShowDownload() {
        startActivity(new Intent(this,
                                 FormDetailsActivity.class));
    }

    @Override
    public void onFormShowEdit() {
        startActivity(new Intent(this,
                                 EditFormActivity.class));
    }

    @Override
    public void onFormShowFinish() {
        startActivity(new Intent(this,
                                 SaveFormActivity.class));
    }

    @Override
    public void onFormShowSend() {
        startActivity(new Intent(this,
                                 ActivitySendForm.class));
    }
}