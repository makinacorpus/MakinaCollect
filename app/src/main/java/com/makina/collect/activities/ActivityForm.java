/*
 * Copyright (C) 2009 University of Washington
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

package com.makina.collect.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.makina.collect.R;
import com.makina.collect.activity.AbstractBaseActivity;
import com.makina.collect.activity.DashBoardActivity;
import com.makina.collect.adapters.HierarchyListAdapter;
import com.makina.collect.application.Collect;
import com.makina.collect.dialog.DialogAboutUs;
import com.makina.collect.listeners.AdvanceToNextListener;
import com.makina.collect.listeners.DeleteInstancesListener;
import com.makina.collect.listeners.FormLoaderListener;
import com.makina.collect.listeners.FormSavedListener;
import com.makina.collect.listeners.InstanceUploaderListener;
import com.makina.collect.listeners.WidgetAnsweredListener;
import com.makina.collect.logic.FormController;
import com.makina.collect.logic.FormController.FailedConstraint;
import com.makina.collect.logic.HierarchyElement;
import com.makina.collect.logic.PropertyManager;
import com.makina.collect.preferences.ActivityPreferences;
import com.makina.collect.preferences.AdminPreferencesActivity;
import com.makina.collect.provider.FormsProviderAPI.FormsColumns;
import com.makina.collect.provider.InstanceProvider;
import com.makina.collect.provider.InstanceProviderAPI;
import com.makina.collect.provider.InstanceProviderAPI.InstanceColumns;
import com.makina.collect.tasks.DeleteInstancesTask;
import com.makina.collect.tasks.FormLoaderTask;
import com.makina.collect.tasks.InstanceUploaderTask;
import com.makina.collect.tasks.SaveToDiskTask;
import com.makina.collect.utilities.FileUtils;
import com.makina.collect.utilities.Finish;
import com.makina.collect.utilities.MediaUtils;
import com.makina.collect.utilities.StaticMethods;
import com.makina.collect.views.CroutonView;
import com.makina.collect.views.CustomFontButton;
import com.makina.collect.views.CustomFontEditText;
import com.makina.collect.views.CustomFontTextview;
import com.makina.collect.views.CustomListViewExpanded;
import com.makina.collect.views.ODKView;
import com.makina.collect.widgets.QuestionWidget;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.model.xform.XFormsModule;
import org.javarosa.xpath.XPathTypeMismatchException;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * FormEntryActivity is responsible for displaying questions, animating
 * transitions between questions, and allowing the user to enter data.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 */

@SuppressLint("NewApi")
public class ActivityForm extends AbstractBaseActivity
        implements
        AnimationListener, FormLoaderListener, FormSavedListener,
        AdvanceToNextListener, OnGestureListener, WidgetAnsweredListener,
        InstanceUploaderListener, DeleteInstancesListener {
    private final int SAVEPOINT_INTERVAL = 1;

    public static final String LOCATION_RESULT = "1986";

    // Defines for FormEntryActivity
    private final boolean EXIT = true;
    private final boolean DO_NOT_EXIT = false;
    private final boolean EVALUATE_CONSTRAINTS = true;
    private final boolean DO_NOT_EVALUATE_CONSTRAINTS = false;

    // Extra returned from gp activity
    private final String KEY_ERROR = "error";

    // Identifies the gp of the form used to launch form entry
    private final String KEY_FORMPATH = "formpath";

    // Identifies whether this is a new form, or reloading a form after a screen
    // rotation (or similar)
    private final String NEWFORM = "newform";
    // these are only processed if we shut down and are restoring after an
    // external intent fires

    private final String KEY_INSTANCEPATH = "instancepath";
    private final String KEY_XPATH = "xpath";
    private final String KEY_XPATH_WAITING_FOR_DATA = "xpathwaiting";

    private final int PROGRESS_DIALOG = 1;
    private final int SAVING_DIALOG = 2;

    // Random ID
    private final int DELETE_REPEAT = 654321;

    private String mFormPath;
    private GestureDetector mGestureDetector;

    private Animation mInAnimation;
    private Animation mOutAnimation;
    private ScrollView mStaleView = null;

    private LinearLayout mQuestionHolder;
    private ScrollView mCurrentView;

    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    private String mErrorMessage;
    private int mY;

    private int viewCount = 0;

    private FormLoaderTask mFormLoaderTask;
    private SaveToDiskTask mSaveToDiskTask;

    private CustomFontButton mNextButton, mBackButton;
    private CustomFontTextview textView_quiz_question_number,
            textView_quiz_name;

    private boolean mAnswersChanged;

    enum AnimationType {
        LEFT, RIGHT, FADE, NONE
    }

    private SharedPreferences mAdminPreferences;

    public static int current_page, size;
    private CheckBox checkBox1, checkBox2, checkBox3;
    private RelativeLayout relativelayout_checkbox1, relativelayout_checkbox2,
            relativelayout_checkbox3;
    private int event;
    private boolean send = false, restart = false;
    private HashMap<String, String> mUploadedInstances;
    private Long[] mInstancesToSend;
    private String mAlertMsg;
    private Uri uri;
    private final int AUTH_DIALOG = 2;
    private CustomFontEditText saveAs;
    private List<HierarchyElement> formList;
    private CustomListViewExpanded hierarchyList;
    private final int CHILD = 1;
    private final int COLLAPSED = 3;
    private final int QUESTION = 4;

    private final String mIndent = "     ";
    private boolean exit_to_home = false;
    private final int RESULT_PREFERENCES = 1;
    private String form_name;
    private boolean fail = false;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Finish.activityForm = this;
        current_page = 1;

        // must be at the beginning of any activity that can be called from an
        // external intent
        try {
            Collect.createODKDirs();
        } catch (RuntimeException e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        setContentView(R.layout.activity_form_entry);
        textView_quiz_question_number = (CustomFontTextview) findViewById(R.id.textView_quiz_question_number);
        textView_quiz_name = ((CustomFontTextview) findViewById(R.id.textView_quiz_name));

        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_title_layout_edit_form,
                null);
        getSupportActionBar().setCustomView(v);

        Intent intent = getIntent();

        mAlertDialog = null;
        mCurrentView = null;
        mInAnimation = null;
        mOutAnimation = null;
        mGestureDetector = new GestureDetector(this);
        mQuestionHolder = (LinearLayout) findViewById(R.id.questionholder);

        // get admin preference settings
        mAdminPreferences = getSharedPreferences(
                AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

        mNextButton = (CustomFontButton) findViewById(R.id.next_button);
        mNextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                if (mNextButton.getText().equals(getString(R.string.submit)))
                    saveForm(saveAs.getText().toString());
                else
                    showNextView();
            }
        });

        mBackButton = (CustomFontButton) findViewById(R.id.prev_button);
        mBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (current_page != 1)
                    showPreviousView();
            }
        });

        // Load JavaRosa modules. needed to restore forms.
        new XFormsModule().registerModule();

        // needed to override rms property manager
        org.javarosa.core.services.PropertyManager
                .setPropertyManager(new PropertyManager(getApplicationContext()));

        String startingXPath = null;
        String waitingXPath = null;
        String instancePath = null;
        Boolean newForm = true;
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_FORMPATH)) {
                mFormPath = savedInstanceState.getString(KEY_FORMPATH);
            }
            if (savedInstanceState.containsKey(KEY_INSTANCEPATH)) {
                instancePath = savedInstanceState.getString(KEY_INSTANCEPATH);
            }
            if (savedInstanceState.containsKey(KEY_XPATH)) {
                startingXPath = savedInstanceState.getString(KEY_XPATH);
            }
            if (savedInstanceState.containsKey(KEY_XPATH_WAITING_FOR_DATA)) {
                waitingXPath = savedInstanceState
                        .getString(KEY_XPATH_WAITING_FOR_DATA);
            }
            if (savedInstanceState.containsKey(NEWFORM)) {
                newForm = savedInstanceState.getBoolean(NEWFORM, true);
            }
            if (savedInstanceState.containsKey(KEY_ERROR)) {
                mErrorMessage = savedInstanceState.getString(KEY_ERROR);
            }

        }

        // If a parse error message is showing then nothing else is loaded
        // Dialogs mid form just disappear on rotation.
        if (mErrorMessage != null) {
            createErrorDialog(mErrorMessage, EXIT);
            return;
        }

        // Check to see if this is a screen flip or a new form load.
        Object data = getLastNonConfigurationInstance();
        if (data instanceof FormLoaderTask) {
            mFormLoaderTask = (FormLoaderTask) data;
        } else if (data instanceof SaveToDiskTask) {
            mSaveToDiskTask = (SaveToDiskTask) data;
        } else if (data == null) {
            if (!newForm) {
                if (Collect.getInstance().getFormController() != null) {
                    refreshCurrentView();
                } else {
                    // we need to launch the form loader to load the form
                    // controller...
                    mFormLoaderTask = new FormLoaderTask(instancePath,
                            startingXPath, waitingXPath);
                    // TODO: this doesn' work (dialog does not get removed):
                    // showDialog(PROGRESS_DIALOG);
                    // show dialog before we execute...
                    mFormLoaderTask.execute(mFormPath);
                }
                return;
            }

            // Not a restart from a screen orientation change (or other).
            Collect.getInstance().setFormController(null);

            if (intent != null) {
                uri = intent.getData();

                if (getContentResolver().getType(uri) == InstanceColumns.CONTENT_ITEM_TYPE) {
                    // get the formId and version for this instance...
                    String jrFormId = null;
                    String jrVersion = null;
                    {
                        Cursor instanceCursor = null;
                        try {
                            instanceCursor = getContentResolver().query(uri,
                                    null, null, null, null);
                            if (instanceCursor.getCount() != 1) {
                                this.createErrorDialog("Bad URI: " + uri, EXIT);
                                return;
                            } else {
                                instanceCursor.moveToFirst();
                                instancePath = instanceCursor
                                        .getString(instanceCursor
                                                .getColumnIndex(InstanceColumns.INSTANCE_FILE_PATH));

                                jrFormId = instanceCursor
                                        .getString(instanceCursor
                                                .getColumnIndex(InstanceColumns.JR_FORM_ID));
                                int idxJrVersion = instanceCursor
                                        .getColumnIndex(InstanceColumns.JR_VERSION);

                                jrVersion = instanceCursor.isNull(idxJrVersion) ? null
                                        : instanceCursor
                                        .getString(idxJrVersion);
                            }
                        } finally {
                            if (instanceCursor != null) {
                                instanceCursor.close();
                            }
                        }
                    }

                    String[] selectionArgs;
                    String selection;

                    if (jrVersion == null) {
                        selectionArgs = new String[]{jrFormId};
                        selection = FormsColumns.JR_FORM_ID + "=? AND "
                                + FormsColumns.JR_VERSION + " IS NULL";
                    } else {
                        selectionArgs = new String[]{jrFormId, jrVersion};
                        selection = FormsColumns.JR_FORM_ID + "=? AND "
                                + FormsColumns.JR_VERSION + "=?";
                    }

                    {
                        Cursor formCursor = null;
                        try {
                            formCursor = getContentResolver().query(
                                    FormsColumns.CONTENT_URI, null, selection,
                                    selectionArgs, null);
                            if (formCursor.getCount() == 1) {
                                formCursor.moveToFirst();
                                mFormPath = formCursor
                                        .getString(formCursor
                                                .getColumnIndex(FormsColumns.FORM_FILE_PATH));
                            } else if (formCursor.getCount() < 1) {
                                this.createErrorDialog(
                                        getString(
                                                R.string.parent_form_not_present,
                                                jrFormId)
                                                + ((jrVersion == null) ? ""
                                                : "\n"
                                                + getString(R.string.version)
                                                + " "
                                                + jrVersion),
                                        EXIT);
                                return;
                            } else if (formCursor.getCount() > 1) {
                                // still take the first entry, but warn that
                                // there are multiple rows.
                                // user will need to hand-edit the SQLite
                                // database to fix it.
                                formCursor.moveToFirst();
                                mFormPath = formCursor
                                        .getString(formCursor
                                                .getColumnIndex(FormsColumns.FORM_FILE_PATH));
                                this.createErrorDialog(
                                        "Multiple matching form definitions exist",
                                        DO_NOT_EXIT);
                            }
                        } finally {
                            if (formCursor != null) {
                                formCursor.close();
                            }
                        }
                    }
                } else if (getContentResolver().getType(uri) == FormsColumns.CONTENT_ITEM_TYPE) {
                    Cursor c = null;
                    try {
                        c = getContentResolver().query(uri, null, null, null,
                                null);
                        if (c.getCount() != 1) {
                            this.createErrorDialog("Bad URI: " + uri, EXIT);
                            return;
                        } else {
                            c.moveToFirst();
                            mFormPath = c
                                    .getString(c
                                            .getColumnIndex(FormsColumns.FORM_FILE_PATH));
                            // This is the fill-blank-form code path.
                            // See if there is a savepoint for this form that
                            // has never been
                            // explicitly saved
                            // by the user. If there is, open this savepoint
                            // (resume this filled-in
                            // form).
                            // Savepoints for forms that were explicitly saved
                            // will be recovered
                            // when that
                            // explicitly saved instance is edited via
                            // edit-saved-form.
                            final String filePrefix = mFormPath.substring(
                                    mFormPath.lastIndexOf('/') + 1,
                                    mFormPath.lastIndexOf('.'))
                                    + "_";
                            final String fileSuffix = ".xml.save";
                            File cacheDir = new File(Collect.CACHE_PATH);
                            File[] files = cacheDir.listFiles(new FileFilter() {
                                @Override
                                public boolean accept(File pathname) {
                                    String name = pathname.getName();
                                    return name.startsWith(filePrefix)
                                            && name.endsWith(fileSuffix);
                                }
                            });
                            // see if any of these savepoints are for a
                            // filled-in form that has never been
                            // explicitly saved by the user...
                            for (int i = 0; i < files.length; ++i) {
                                File candidate = files[i];
                                String instanceDirName = candidate.getName()
                                        .substring(
                                                0,
                                                candidate.getName().length()
                                                        - fileSuffix.length());
                                File instanceDir = new File(
                                        Collect.INSTANCES_PATH + File.separator
                                                + instanceDirName);
                                File instanceFile = new File(instanceDir,
                                        instanceDirName + ".xml");
                                if (instanceDir.exists()
                                        && instanceDir.isDirectory()
                                        && !instanceFile.exists()) {
                                    // yes! -- use this savepoint file
                                    instancePath = instanceFile
                                            .getAbsolutePath();
                                    break;
                                }
                            }
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }
                } else {
                    this.createErrorDialog("unrecognized URI: " + uri, EXIT);
                    return;
                }

                mFormLoaderTask = new FormLoaderTask(instancePath, null, null);
                showDialog(PROGRESS_DIALOG);
                // show dialog before we execute...
                mFormLoaderTask.execute(mFormPath);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_FORMPATH, mFormPath);
        FormController formController = Collect.getInstance()
                .getFormController();
        if (formController != null) {
            outState.putString(KEY_INSTANCEPATH, formController
                    .getInstancePath().getAbsolutePath());
            outState.putString(KEY_XPATH,
                    formController.getXPath(formController.getFormIndex()));
            FormIndex waiting = formController.getIndexWaitingForData();
            if (waiting != null) {
                outState.putString(KEY_XPATH_WAITING_FOR_DATA,
                        formController.getXPath(waiting));
            }
            // save the instance to a temp path...
            SaveToDiskTask.blockingExportTempData();
        }
        outState.putBoolean(NEWFORM, false);
        outState.putString(KEY_ERROR, mErrorMessage);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if (requestCode == RESULT_PREFERENCES) {
            Intent i = getIntent();
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
            FormController formController = Collect.getInstance()
                    .getFormController();
            if (formController == null) {
                // we must be in the midst of a reload of the FormController.
                // try to save this callback data to the FormLoaderTask
                if (mFormLoaderTask != null
                        && mFormLoaderTask.getStatus() != AsyncTask.Status.FINISHED) {
                    mFormLoaderTask.setActivityResult(requestCode, resultCode,
                            intent);
                }
                return;
            }

            if (resultCode == RESULT_CANCELED) {
                // request was canceled...
                if (requestCode != StaticMethods.HIERARCHY_ACTIVITY) {
                    ((ODKView) mCurrentView).cancelWaitingForBinaryData();
                }
                return;
            }

            switch (requestCode) {
                case StaticMethods.BARCODE_CAPTURE:
                    String sb = intent.getStringExtra("SCAN_RESULT");
                    ((ODKView) mCurrentView).setBinaryData(sb);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    break;
                case StaticMethods.EX_STRING_CAPTURE:
                    String sv = intent.getStringExtra("value");
                    ((ODKView) mCurrentView).setBinaryData(sv);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    break;
                case StaticMethods.EX_INT_CAPTURE:
                    Integer iv = intent.getIntExtra("value", 0);
                    ((ODKView) mCurrentView).setBinaryData(iv);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    break;
                case StaticMethods.EX_DECIMAL_CAPTURE:
                    Double dv = intent.getDoubleExtra("value", 0.0);
                    ((ODKView) mCurrentView).setBinaryData(dv);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    break;
                case StaticMethods.DRAW_IMAGE:
                case StaticMethods.ANNOTATE_IMAGE:
                case StaticMethods.SIGNATURE_CAPTURE:
                case StaticMethods.IMAGE_CAPTURE:
                /*
				 * We saved the image to the tempfile_path, but we really want
				 * it to be in: /sdcard/odk/instances/[current
				 * instnace]/something.jpg so we move it there before inserting
				 * it into the content provider. Once the android image capture
				 * bug gets fixed, (read, we move on from Android 1.6) we want
				 * to handle images the audio and video
				 */
                    // The intent is empty, but we know we saved the image to the
                    // temp
                    // file
                    File fi = new File(Collect.TMPFILE_PATH);
                    String mInstanceFolder = formController.getInstancePath()
                            .getParent();
                    String s = mInstanceFolder + File.separator
                            + System.currentTimeMillis() + ".jpg";

                    File nf = new File(s);
                    fi.renameTo(nf);

                    ((ODKView) mCurrentView).setBinaryData(nf);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    break;
                case StaticMethods.IMAGE_CHOOSER:
				/*
				 * We have a saved image somewhere, but we really want it to be
				 * in: /sdcard/odk/instances/[current instnace]/something.jpg so
				 * we move it there before inserting it into the content
				 * provider. Once the android image capture bug gets fixed,
				 * (read, we move on from Android 1.6) we want to handle images
				 * the audio and video
				 */

                    // get gp of chosen file
                    String sourceImagePath = null;
                    Uri selectedImage = intent.getData();
                    if (selectedImage.toString().startsWith("file")) {
                        sourceImagePath = selectedImage.toString().substring(6);
                    } else {
                        String[] projection = {MediaColumns.DATA};
                        Cursor cursor = null;
                        try {
                            cursor = getContentResolver().query(selectedImage,
                                    projection, null, null, null);
                            int column_index = cursor
                                    .getColumnIndexOrThrow(MediaColumns.DATA);
                            cursor.moveToFirst();
                            sourceImagePath = cursor.getString(column_index);
                        } finally {
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    }

                    // Copy file to sdcard
                    String mInstanceFolder1 = formController.getInstancePath()
                            .getParent();
                    String destImagePath = mInstanceFolder1 + File.separator
                            + System.currentTimeMillis() + ".jpg";

                    File source = new File(sourceImagePath);
                    File newImage = new File(destImagePath);
                    FileUtils.copyFile(source, newImage);

                    ((ODKView) mCurrentView).setBinaryData(newImage);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    if (formController.indexIsInFieldList()) {
                        updateView();
                    }
                    break;
                case StaticMethods.AUDIO_CAPTURE:
                case StaticMethods.VIDEO_CAPTURE:
                case StaticMethods.AUDIO_CHOOSER:
                case StaticMethods.VIDEO_CHOOSER:
                    // For audio/video capture/chooser, we get the URI from the
                    // content
                    // provider
                    // then the widget copies the file and makes a new entry in the
                    // content provider.
                    Uri media = intent.getData();
                    ((ODKView) mCurrentView).setBinaryData(media);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    if (formController.indexIsInFieldList()) {
                        updateView();
                    }
                    break;
                case StaticMethods.LOCATION_CAPTURE:
                    String sl = intent
                            .getStringExtra(StaticMethods.LOCATION_RESULT);
                    ((ODKView) mCurrentView).setBinaryData(sl);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    break;
                case StaticMethods.HIERARCHY_ACTIVITY:
                    // We may have jumped to a new index in hierarchy activity, so
                    // refresh
                    break;

            }
            refreshCurrentView();
        }
    }

    /**
     * Refreshes the current view. the controller and the displayed view can get
     * out of sync due to dialogs and restarts caused by screen orientation
     * changes, so they're resynchronized here.
     */
    public void refreshCurrentView() {
        FormController formController = Collect.getInstance()
                .getFormController();

        if (formController != null) {
            int event = formController.getEvent();

            // When we refresh, repeat dialog state isn't maintained, so step
            // back
            // to the previous
            // question.
            // Also, if we're within a group labeled 'field list', step back to
            // the
            // beginning of that
            // group.
            // That is, skip backwards over repeat prompts, groups that are not
            // field-lists,
            // repeat events, and indexes in field-lists that is not the
            // containing
            // group.
            if (event == FormEntryController.EVENT_PROMPT_NEW_REPEAT) {
                createRepeatDialog();
            } else {
                ScrollView current = createView(event, false);
                showView(current, AnimationType.FADE);
            }
        } else {
            Bundle extra = getIntent().getExtras();
            if (extra != null) {
                // InstanceProvider.supprimerZgaw(extra.getLong("id"));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.form_edit,
                             menu);
        menuInflater.inflate(R.menu.settings,
                             menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        FormController formController = Collect.getInstance()
                .getFormController();
        switch (item.getItemId()) {
            case 0:
                if (mCurrentView != null) {
                    updateView();
                }
                return true;

            case R.id.menu_save:
                // don't exit
                dialogSaveName(false, false);
                return true;
            case R.id.menu_hierachy:
                if (formController.currentPromptIsQuestion()) {
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                }
                Intent i = new Intent(this, ActivityFormHierarchy.class);
                startActivityForResult(i, StaticMethods.HIERARCHY_ACTIVITY);
                return true;
            case R.id.menu_raz:
                razConfirmation();
                return true;
            case R.id.menu_settings:
                startActivityForResult(
                        (new Intent(this, ActivityPreferences.class)),
                        RESULT_PREFERENCES);
                return true;
            case R.id.menu_help:
                Intent mIntent = new Intent(this, ActivityHelp.class);
                Bundle mBundle = new Bundle();
                mBundle.putInt("position", 1);
                mIntent.putExtras(mBundle);
                startActivity(mIntent);
                return true;
            case R.id.menu_about_us:
                DialogAboutUs.aboutUs(this);
                return true;
            case android.R.id.home:
                createQuitDialog(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void razConfirmation() {

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(getString(R.string.raz));
        adb.setMessage(getString(R.string.raz_confirmation));
        adb.setPositiveButton(getString(android.R.string.yes),
                new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        removeTempInstance();
                        finishReturnInstance(false);
                        Intent myIntent = new Intent(Intent.ACTION_EDIT, uri);
                        myIntent.putExtra("newForm", true);
                        startActivity(myIntent);
                    }
                });
        adb.setNegativeButton(getString(android.R.string.no), null);
        adb.show();

    }

    /**
     * Attempt to save the answer(s) in the current screen to into the data
     * model.
     *
     * @param evaluateConstraints
     * @return false if any error occurs while saving (constraint violated,
     * etc...), true otherwise.
     */
    private boolean saveAnswersForCurrentScreen(boolean evaluateConstraints) {
        FormController formController = Collect.getInstance()
                .getFormController();
        // only try to save if the current event is a question or a field-list
        // group
        if (formController.currentPromptIsQuestion()) {
            LinkedHashMap<FormIndex, IAnswerData> answers = ((ODKView) mCurrentView)
                    .getAnswers();
            FailedConstraint constraint = formController.saveAllScreenAnswers(
                    answers, evaluateConstraints);
            if (constraint != null) {
                createConstraintToast(constraint.index, constraint.status);
                return false;
            }
        }
        return true;
    }

    /**
     * Clears the answer on the screen.
     */
    private void clearAnswer(QuestionWidget qw) {
        if (qw.getAnswer() != null) {
            qw.clearAnswer();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        FormController formController = Collect.getInstance()
                .getFormController();

        menu.add(0, v.getId(), 0, getString(R.string.clear_answer));
        if (formController.indexContainsRepeatableGroup()) {
            menu.add(0, DELETE_REPEAT, 0, getString(R.string.delete_repeat));
        }
        menu.setHeaderTitle(getString(R.string.edit_prompt));
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
		/*
		 * We don't have the right view here, so we store the View's ID as the
		 * item ID and loop through the possible views to find the one the user
		 * clicked on.
		 */
        for (QuestionWidget qw : ((ODKView) mCurrentView).getWidgets()) {
            if (item.getItemId() == qw.getId()) {
                createClearDialog(qw);
            }
        }
        if (item.getItemId() == DELETE_REPEAT) {
            createDeleteRepeatConfirmDialog();
        }

        return super.onContextItemSelected(item);
    }

    /**
     * If we're loading, then we pass the loading thread to our next instance.
     */
    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        FormController formController = Collect.getInstance()
                .getFormController();
        // if a form is loading, pass the loader task
        if (mFormLoaderTask != null
                && mFormLoaderTask.getStatus() != AsyncTask.Status.FINISHED)
            return mFormLoaderTask;

        // if a form is writing to disk, pass the save to disk task
        if (mSaveToDiskTask != null
                && mSaveToDiskTask.getStatus() != AsyncTask.Status.FINISHED)
            return mSaveToDiskTask;

        // mFormEntryController is static so we don't need to pass it.
        if (formController != null && formController.currentPromptIsQuestion()) {
            saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
        }
        return null;
    }

    /**
     * Creates a view given the View type and an event
     *
     * @param event
     * @param advancingPage -- true if this results from advancing through the form
     * @return newly created View
     */
    private ScrollView createView(int event, boolean advancingPage) {

        final FormController formController = Collect.getInstance()
                .getFormController();
        textView_quiz_question_number.setText(current_page + "/" + size);
        textView_quiz_name.setText(formController.getFormTitle());

        findViewById(R.id.relativeLayout_informations).setVisibility(
                View.VISIBLE);
        findViewById(R.id.buttonholder).setVisibility(View.VISIBLE);

        switch (event) {

            case FormEntryController.EVENT_END_OF_FORM:
                findViewById(R.id.relativeLayout_informations).setVisibility(
                        View.GONE);
                ScrollView endView;
                changeButtonNext(
                        getResources().getDrawable(R.drawable.finish_background),
                        R.style.ButtonFinishSave, getString(R.string.submit));

                endView = (ScrollView) View.inflate(this,
                        R.layout.activity_form_entry_end, null);

                // edittext to change the displayed name of the instance
                saveAs = (CustomFontEditText) endView.findViewById(R.id.save_name);

                // disallow carriage returns in the name
                InputFilter returnFilter = new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start,
                                               int end, Spanned dest, int dstart, int dend) {
                        for (int i = start; i < end; i++) {
                            if (Character.getType((source.charAt(i))) == Character.CONTROL) {
                                return "";
                            }
                        }
                        return null;
                    }
                };
                saveAs.setFilters(new InputFilter[]{returnFilter});

                String saveName = formController.getSubmissionMetadata().instanceName;

                if (saveName == null) {
                    // TODO Default saveAs text should be previous save name
                    // no meta/instanceName field in the form -- see if we have a
                    // name for this instance from a previous save attempt...
                    if (getContentResolver().getType(getIntent().getData()) == InstanceColumns.CONTENT_ITEM_TYPE) {
                        Uri instanceUri = getIntent().getData();
                        Cursor instance = null;
                        try {
                            instance = getContentResolver().query(instanceUri,
                                    null, null, null, null);
                            if (instance.getCount() == 1) {
                                instance.moveToFirst();
                                saveName = instance
                                        .getString(instance
                                                .getColumnIndex(InstanceColumns.DISPLAY_NAME));
                            }
                        } finally {
                            if (instance != null) {
                                instance.close();
                            }
                        }
                    }
                    // present the prompt to allow user to name the form
                    // TODO if savename != null don"t need to initialize it
                    if (saveName == null || saveName.length() == 0) {
                        saveName = formController.getFormTitle();
                    }
                    saveAs.setText(saveName);
                    saveAs.setEnabled(true);
                    saveAs.setVisibility(View.VISIBLE);
                } else {
                    // if instanceName is defined in form, this is the name -- no
                    // revisions
                    // display only the name, not the prompt, and disable edits

                    saveAs.setText(saveName);
                    saveAs.setEnabled(false);
                    saveAs.setBackgroundColor(Color.WHITE);
                    saveAs.setVisibility(View.VISIBLE);
                }

                // override the visibility settings based upon admin preferences
                if (!mAdminPreferences.getBoolean(
                        AdminPreferencesActivity.KEY_SAVE_AS, true)) {
                    saveAs.setVisibility(View.GONE);
                }

                checkBox1 = (CheckBox) endView.findViewById(R.id.checkbox1);
                checkBox2 = (CheckBox) endView.findViewById(R.id.checkbox2);
                checkBox3 = (CheckBox) endView.findViewById(R.id.checkbox3);
                relativelayout_checkbox1 = (RelativeLayout) endView
                        .findViewById(R.id.relativelayout_checkbox1);
                relativelayout_checkbox2 = (RelativeLayout) endView
                        .findViewById(R.id.relativelayout_checkbox2);
                relativelayout_checkbox3 = (RelativeLayout) endView
                        .findViewById(R.id.relativelayout_checkbox3);

                relativelayout_checkbox1
                        .setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View arg0) {
                                // TODO Auto-generated method stub
                                if (checkBox1.isChecked())
                                    checkBox1.setChecked(false);
                                else {
                                    checkBox1.setChecked(true);
                                    checkBox2.setChecked(false);
                                    checkBox3.setChecked(false);
                                }
                            }
                        });

                relativelayout_checkbox2
                        .setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View arg0) {
                                // TODO Auto-generated method stub
                                if (checkBox2.isChecked())
                                    checkBox2.setChecked(false);
                                else {
                                    checkBox2.setChecked(true);
                                    checkBox1.setChecked(false);
                                    checkBox3.setChecked(false);
                                }
                            }
                        });

                relativelayout_checkbox3
                        .setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View arg0) {
                                // TODO Auto-generated method stub
                                if (checkBox3.isChecked())
                                    checkBox3.setChecked(false);
                                else {
                                    checkBox3.setChecked(true);
                                    checkBox1.setChecked(false);
                                    checkBox2.setChecked(false);
                                }
                            }
                        });
                checkBox1
                        .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton arg0,
                                                         boolean status) {
                                // TODO Auto-generated method stub
                                if (status) {
                                    checkBox2.setChecked(false);
                                    checkBox3.setChecked(false);
                                }
                            }
                        });
                checkBox2
                        .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton arg0,
                                                         boolean status) {
                                // TODO Auto-generated method stub
                                if (status) {
                                    checkBox1.setChecked(false);
                                    checkBox3.setChecked(false);
                                }
                            }
                        });
                checkBox3
                        .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton arg0,
                                                         boolean status) {
                                // TODO Auto-generated method stub
                                if (status) {
                                    checkBox1.setChecked(false);
                                    checkBox2.setChecked(false);
                                }
                            }
                        });

                return endView;
            case FormEntryController.EVENT_QUESTION:
            case FormEntryController.EVENT_GROUP:
            case FormEntryController.EVENT_REPEAT:
                ODKView odkv = null;
                // should only be a group here if the event_group is a field-list
                try {
                    FormEntryPrompt[] prompts = formController.getQuestionPrompts();
                    FormEntryCaption[] groups = formController
                            .getGroupsForCurrentIndex();
                    odkv = new ODKView(this, this,
                            formController.getQuestionPrompts(), groups,
                            advancingPage);
                } catch (RuntimeException e) {
                    createErrorDialog(e.getMessage(), EXIT);
                    e.printStackTrace();
                    // this is badness to avoid a crash.
                    event = formController.stepToNextScreenEvent();
                    return createView(event, advancingPage);
                }

                // Makes a "clear answer" menu pop up on long-click
                for (QuestionWidget qw : odkv.getWidgets()) {
                    if (!qw.getPrompt().isReadOnly()) {
                        registerForContextMenu(qw);
                    }
                }

                if (mBackButton.isShown() && mNextButton.isShown()) {
                    mBackButton.setEnabled(true);
                    mNextButton.setEnabled(true);
                }
                return odkv;
            default:
                // this is badness to avoid a crash.
                event = formController.stepToNextScreenEvent();
                return createView(event, advancingPage);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent mv) {
        boolean handled = mGestureDetector.onTouchEvent(mv);
        if (!handled) {
            return super.dispatchTouchEvent(mv);
        }

        return handled; // this is always true
    }

    /**
     * Determines what should be displayed on the screen. Possible options are:
     * a question, an ask repeat dialog, or the submit screen. Also saves
     * answers to the data model after checking constraints.
     */
    private void showNextView() {
        FormController formController = Collect.getInstance()
                .getFormController();
        if (formController.currentPromptIsQuestion()) {
            if (!saveAnswersForCurrentScreen(EVALUATE_CONSTRAINTS)) {
                // A constraint was violated so a dialog should be showing.
                return;
            }
        }

        ScrollView next;
        event = formController.stepToNextScreenEvent();
        current_page++;
        mBackButton.setVisibility(View.VISIBLE);
        switch (event) {
            case FormEntryController.EVENT_QUESTION:
            case FormEntryController.EVENT_GROUP:
                // create a savepoint
                if ((++viewCount) % SAVEPOINT_INTERVAL == 0) {
                    SaveToDiskTask.blockingExportTempData();
                }
                next = createView(event, true);
                showView(next, AnimationType.RIGHT);
                break;
            case FormEntryController.EVENT_END_OF_FORM:
            case FormEntryController.EVENT_REPEAT:
                next = createView(event, true);
                showView(next, AnimationType.RIGHT);
                break;
            case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                createRepeatDialog();
                break;
            case FormEntryController.EVENT_REPEAT_JUNCTURE:
                // skip repeat junctures until we implement them
                break;
            default:
                break;
        }
    }

    @Override
    public void updateView() {
        FormController formController = Collect.getInstance()
                .getFormController();
        if (formController.indexIsInFieldList() && mAnswersChanged) {
            if (formController.currentPromptIsQuestion()) {
                if (!saveAnswersForCurrentScreen(EVALUATE_CONSTRAINTS)) {
                    // A constraint was violated so a dialog should be showing.
                    return;
                }
            }
            ScrollView view = mCurrentView;
            mY = view.getScrollY();
            ScrollView newView;
            int event = formController.getEvent();
            switch (event) {
                case FormEntryController.EVENT_QUESTION:
                case FormEntryController.EVENT_GROUP:
                    // create a savepoint
                    if ((++viewCount) % SAVEPOINT_INTERVAL == 0) {
                        SaveToDiskTask.blockingExportTempData();
                    }
                    newView = createView(event, true);
                    showView(newView, AnimationType.NONE);
                    break;
                case FormEntryController.EVENT_END_OF_FORM:
                case FormEntryController.EVENT_REPEAT:
                    newView = createView(event, true);
                    showView(newView, AnimationType.NONE);
                    break;
                case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                    createRepeatDialog();
                    break;
                case FormEntryController.EVENT_REPEAT_JUNCTURE:
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Determines what should be displayed between a question, or the start
     * screen and displays the appropriate view. Also saves answers to the data
     * model without checking constraints.
     */
    private void showPreviousView() {
        changeButtonNext(
                getResources().getDrawable(
                        R.drawable.selectable_item_background),
                R.style.ButtonNext, getString(R.string.next));

        current_page--;
        mBackButton.setVisibility(current_page <= 1 ? View.INVISIBLE
                : View.VISIBLE);
        FormController formController = Collect.getInstance()
                .getFormController();
        textView_quiz_question_number.setText(current_page + "/" + size);
        // The answer is saved on a back swipe, but question constraints are
        // ignored.
        if (formController.currentPromptIsQuestion()) {
            saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
        }

        if (formController.getEvent() != FormEntryController.EVENT_BEGINNING_OF_FORM) {

            if (mNextButton.getText().toString()
                    .equals(getString(R.string.next)))
                event = formController.stepToPreviousScreenEvent();

            if (event == FormEntryController.EVENT_BEGINNING_OF_FORM
                    || event == FormEntryController.EVENT_GROUP
                    || event == FormEntryController.EVENT_QUESTION) {
                // create savepoint
                if ((++viewCount) % SAVEPOINT_INTERVAL == 0) {
                    SaveToDiskTask.blockingExportTempData();
                }
            }
            ScrollView next = createView(event, false);
            showView(next, AnimationType.LEFT);
        }
    }

    private void showPreviousViewFromHierarchy(int position, FormIndex index) {
        changeButtonNext(
                getResources().getDrawable(
                        R.drawable.selectable_item_background),
                R.style.ButtonNext, getString(R.string.next));

        current_page = position + 1;
        mBackButton.setVisibility(current_page <= 1 ? View.INVISIBLE
                : View.VISIBLE);
        FormController formController = Collect.getInstance()
                .getFormController();
        formController.jumpToIndex(index);
        textView_quiz_question_number.setText(current_page + "/" + size);
        // The answer is saved on a back swipe, but question constraints are
        // ignored.
        event = formController.getEvent();
        if (event == FormEntryController.EVENT_BEGINNING_OF_FORM
                || event == FormEntryController.EVENT_GROUP
                || event == FormEntryController.EVENT_QUESTION) {
            // create savepoint
            if ((++viewCount) % SAVEPOINT_INTERVAL == 0) {
                SaveToDiskTask.blockingExportTempData();
            }
        }
        ScrollView next = createView(event, false);
        showView(next, AnimationType.LEFT);
    }

    /**
     * Displays the View specified by the parameter 'next', animating both the
     * current view and next appropriately given the AnimationType. Also updates
     * the progress bar.
     */
    public void showView(ScrollView next, AnimationType from) {
        if (from != AnimationType.NONE) {
            // disable notifications...
            if (mInAnimation != null) {
                mInAnimation.setAnimationListener(null);
            }
            if (mOutAnimation != null) {
                mOutAnimation.setAnimationListener(null);
            }

            // logging of the view being shown is already done, as this was
            // handled
            // by createView()
            switch (from) {
                case RIGHT:
                    mInAnimation = AnimationUtils.loadAnimation(this,
                            R.anim.push_left_in);
                    mOutAnimation = AnimationUtils.loadAnimation(this,
                            R.anim.push_left_out);
                    break;
                case LEFT:
                    mInAnimation = AnimationUtils.loadAnimation(this,
                            R.anim.push_right_in);
                    mOutAnimation = AnimationUtils.loadAnimation(this,
                            R.anim.push_right_out);
                    break;
                case FADE:
                    mInAnimation = AnimationUtils.loadAnimation(this,
                            android.R.anim.fade_in);
                    mOutAnimation = AnimationUtils.loadAnimation(this,
                            android.R.anim.fade_out);
                    break;
            }

            // complete setup for animations...
            mInAnimation.setAnimationListener(this);
            mOutAnimation.setAnimationListener(this);
        } else {
            mInAnimation = null;
            mOutAnimation = null;
        }

        // drop keyboard before transition...
        if (mCurrentView != null) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(mCurrentView.getWindowToken(),
                    0);
        }

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

        // adjust which view is in the layout container...
        mStaleView = mCurrentView;
        mCurrentView = next;
        mQuestionHolder.addView(mCurrentView, lp);
        mAnimationCompletionSet = 0;

        if (mStaleView != null) {
            if (from != AnimationType.NONE) {
                // start OutAnimation for transition...
                mStaleView.startAnimation(mOutAnimation);
            }
            // and remove the old view (MUST occur after start of animation!!!)
            mQuestionHolder.removeView(mStaleView);
        } else {
            mAnimationCompletionSet = 2;
        }
        // start InAnimation for transition...
        if (from != AnimationType.NONE) {
            mCurrentView.startAnimation(mInAnimation);
        }

        String logString = "";
        switch (from) {
            case RIGHT:
                logString = getString(R.string.next);
                break;
            case LEFT:
                logString = "previous";
                break;
            case FADE:
                logString = "refresh";
                break;
            case NONE:
                logString = "update";
                break;
        }
        mCurrentView.post(new Runnable() {
            public void run() {
                mCurrentView.scrollTo(0, mY);
            }
        });
    }

    // Hopefully someday we can use managed dialogs when the bugs are fixed
	/*
	 * Ideally, we'd like to use Android to manage dialogs with onCreateDialog()
	 * and onPrepareDialog(), but dialogs with dynamic content are broken in 1.5
	 * (cupcake). We do use managed dialogs for our static loading
	 * ProgressDialog. The main issue we noticed and are waiting to see fixed
	 * is: onPrepareDialog() is not called after a screen orientation change.
	 * http://code.google.com/p/android/issues/detail?id=1639
	 */

    //

    /**
     * Creates and displays a dialog displaying the violated constraint.
     */
    private void createConstraintToast(FormIndex index, int saveStatus) {
        FormController formController = Collect.getInstance()
                .getFormController();
        String constraintText = formController.getQuestionPrompt(index)
                .getConstraintText();
        switch (saveStatus) {
            case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
                if (constraintText == null) {
                    constraintText = formController.getQuestionPrompt(index)
                            .getSpecialFormQuestionText("constraintMsg");
                    if (constraintText == null) {
                        constraintText = getString(R.string.invalid_answer_error);
                    }
                }
                break;
            case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
                constraintText = formController.getQuestionPrompt(index)
                        .getSpecialFormQuestionText("requiredMsg");
                if (constraintText == null) {
                    constraintText = getString(R.string.required_answer_error);
                }
                break;
        }

        CroutonView.showBuiltInCrouton(ActivityForm.this, constraintText, Style.ALERT);
    }

    /**
     * Creates a toast with the specified message.
     *
     * @param message
     */
	/*
	 * private void showCustomToast(String message, int duration) {
	 * LayoutInflater inflater = (LayoutInflater)
	 * getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	 * 
	 * View view = inflater.inflate(R.layout.widget_toast, null);
	 * 
	 * // set the text in the view TextView tv = (TextView)
	 * view.findViewById(R.id.message); tv.setText(message);
	 * 
	 * Toast t = new Toast(this); t.setView(view); t.setDuration(duration);
	 * t.setGravity(Gravity.CENTER, 0, 0); t.show(); }
	 */

    /**
     * Creates and displays a dialog asking the user if they'd like to create a
     * repeat of the current group.
     */
    private void createRepeatDialog() {
        FormController formController = Collect.getInstance()
                .getFormController();
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIconAttribute(R.attr.dialog_icon_info);
        DialogInterface.OnClickListener repeatListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                FormController formController = Collect.getInstance()
                        .getFormController();
                switch (i) {
                    case DialogInterface.BUTTON1: // yes, repeat
                        try {
                            formController.newRepeat();
                        } catch (XPathTypeMismatchException e) {
                            ActivityForm.this.createErrorDialog(e.getMessage(),
                                    EXIT);
                            return;
                        }
                        if (!formController.indexIsInFieldList()) {
                            // we are at a REPEAT event that does not have a
                            // field-list appearance
                            // step to the next visible field...
                            // which could be the start of a new repeat group...
                            showNextView();
                        } else {
                            // we are at a REPEAT event that has a field-list
                            // appearance
                            // just display this REPEAT event's group.
                            refreshCurrentView();
                        }
                        break;
                    case DialogInterface.BUTTON2: // no, no repeat
                        showNextView();
                        break;
                }
            }
        };
        if (formController.getLastRepeatCount() > 0) {
            mAlertDialog.setTitle(getString(R.string.leaving_repeat_ask));
            mAlertDialog.setMessage(getString(R.string.add_another_repeat,
                    formController.getLastGroupText()));
            mAlertDialog.setButton(getString(R.string.add_another),
                    repeatListener);
            mAlertDialog.setButton2(getString(R.string.leave_repeat_yes),
                    repeatListener);

        } else {
            mAlertDialog.setTitle(getString(R.string.entering_repeat_ask));
            mAlertDialog.setMessage(getString(R.string.add_repeat,
                    formController.getLastGroupText()));
            mAlertDialog.setButton(getString(R.string.entering_repeat),
                    repeatListener);
            mAlertDialog.setButton2(getString(R.string.add_repeat_no),
                    repeatListener);
        }
        mAlertDialog.setCancelable(false);
        mAlertDialog.show();
    }

    /**
     * Creates and displays dialog with the given errorMsg.
     */
    private void createErrorDialog(String errorMsg, final boolean shouldExit) {
        mErrorMessage = errorMsg;
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIconAttribute(R.attr.dialog_icon_info);
        mAlertDialog.setTitle(getString(R.string.error_occured));
        mAlertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON1:
                        if (shouldExit) {
                            finish();
                        }
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), errorListener);
        mAlertDialog.show();
    }

    /**
     * Creates a confirm/cancel dialog for deleting repeats.
     */
    private void createDeleteRepeatConfirmDialog() {
        FormController formController = Collect.getInstance()
                .getFormController();
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIconAttribute(R.attr.dialog_icon_info);
        String name = formController.getLastRepeatedGroupName();
        int repeatcount = formController.getLastRepeatedGroupRepeatCount();
        if (repeatcount != -1) {
            name += " (" + (repeatcount + 1) + ")";
        }
        mAlertDialog.setTitle(getString(R.string.delete_repeat_ask));
        mAlertDialog
                .setMessage(getString(R.string.delete_repeat_confirm, name));
        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                FormController formController = Collect.getInstance()
                        .getFormController();
                switch (i) {
                    case DialogInterface.BUTTON1: // yes
                        formController.deleteRepeat();
                        showPreviousView();
                        break;
                    case DialogInterface.BUTTON2: // no
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.discard_group), quitListener);
        mAlertDialog.setButton2(getString(R.string.delete_repeat_no),
                quitListener);
        mAlertDialog.show();
    }

    /**
     * Saves data and writes it to disk. If exit is set, program will exit after
     * save completes. Complete indicates whether the user has marked the
     * instances as complete. If updatedSaveName is non-null, the instances
     * content provider is updated with the new name
     */
    private boolean saveDataToDisk(boolean exit, boolean complete,
                                   String updatedSaveName) {
        // save current answer
        if (!saveAnswersForCurrentScreen(complete)) {
            CroutonView.showBuiltInCrouton(ActivityForm.this,
                    getString(R.string.data_saved_error), Style.ALERT);
            return false;
        }

        mSaveToDiskTask = new SaveToDiskTask(getIntent().getData(), exit,
                complete, updatedSaveName);
        mSaveToDiskTask.setFormSavedListener(this);
        showDialog(SAVING_DIALOG);
        // show dialog before we execute...
        mSaveToDiskTask.execute();

        return true;
    }

    /**
     * Create a dialog with options to save and exit, save, or quit without
     * saving
     */
    private void createQuitDialog(final boolean test) {
        FormController formController = Collect.getInstance()
                .getFormController();
        String[] items;
        if (mAdminPreferences.getBoolean(AdminPreferencesActivity.KEY_SAVE_MID,
                true)) {
            String[] two = {getString(R.string.keep_changes),
                    getString(R.string.do_not_save)};
            items = two;
        } else {
            String[] one = {getString(R.string.do_not_save)};
            items = one;
        }

        mAlertDialog = new AlertDialog.Builder(this)
                .setTitle(
                        getString(R.string.quit_application,
                                formController.getFormTitle()))
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setNeutralButton(getString(R.string.do_not_exit),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {

                                dialog.cancel();

                            }
                        })
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {

                            case 0: // save and exit
                                // this is slightly complicated because if the
                                // option is disabled in
                                // the admin menu, then case 0 actually becomes
                                // 'discard and exit'
                                // whereas if it's enabled it's 'save and exit'
                                if (mAdminPreferences
                                        .getBoolean(
                                                AdminPreferencesActivity.KEY_SAVE_MID,
                                                true)) {
                                    dialogSaveName(test, true);

                                } else {
                                    removeTempInstance();
                                    finishReturnInstance(test);
                                }
                                break;

                            case 1: // discard changes and exit
                                removeTempInstance();
                                finishReturnInstance(test);
                                break;

                            case 2:// do nothing
                                break;
                        }
                    }
                }).create();
        mAlertDialog.show();
    }

    /**
     * this method cleans up unneeded files when the user selects 'discard and
     * exit'
     */
    private void removeTempInstance() {
        FormController formController = Collect.getInstance()
                .getFormController();

        // attempt to remove any scratch file
        File temp = SaveToDiskTask.savepointFile(formController
                .getInstancePath());
        if (temp.exists()) {
            temp.delete();
        }

        String selection = InstanceColumns.INSTANCE_FILE_PATH + "=?";
        String[] selectionArgs = {formController.getInstancePath()
                .getAbsolutePath()};

        boolean erase = false;
        {
            Cursor c = null;
            try {
                c = getContentResolver().query(InstanceColumns.CONTENT_URI,
                        null, selection, selectionArgs, null);
                erase = (c.getCount() < 1);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        // if it's not already saved, erase everything
        if (erase) {
            // delete media first
            String instanceFolder = formController.getInstancePath()
                    .getParent();
            int images = MediaUtils
                    .deleteImagesInFolderFromMediaProvider(formController
                            .getInstancePath().getParentFile());
            int audio = MediaUtils
                    .deleteAudioInFolderFromMediaProvider(formController
                            .getInstancePath().getParentFile());
            int video = MediaUtils
                    .deleteVideoInFolderFromMediaProvider(formController
                            .getInstancePath().getParentFile());

            File f = new File(instanceFolder);
            if (f.exists() && f.isDirectory()) {
                for (File del : f.listFiles()) {
                    del.delete();
                }
                f.delete();
            }
        }
    }

    /**
     * Confirm clear answer dialog
     */
    private void createClearDialog(final QuestionWidget qw) {
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIconAttribute(R.attr.dialog_icon_info);

        mAlertDialog.setTitle(getString(R.string.clear_answer_ask));

        String question = qw.getPrompt().getLongText();
        if (question == null) {
            question = "";
        }
        if (question.length() > 50) {
            question = question.substring(0, 50) + "...";
        }

        mAlertDialog.setMessage(getString(R.string.clearanswer_confirm,
                question));

        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON1: // yes
                        clearAnswer(qw);
                        saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                        break;
                    case DialogInterface.BUTTON2: // no
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog
                .setButton(getString(R.string.discard_answer), quitListener);
        mAlertDialog.setButton2(getString(R.string.clear_answer_no),
                quitListener);
        mAlertDialog.show();
    }

    /**
     * Creates and displays a dialog allowing the user to set the language for
     * the form.
     */
    private void createLanguageDialog() {
        FormController formController = Collect.getInstance()
                .getFormController();
        final String[] languages = formController.getLanguages();
        int selected = -1;
        if (languages != null) {
            String language = formController.getLanguage();
            for (int i = 0; i < languages.length; i++) {
                if (language.equals(languages[i])) {
                    selected = i;
                }
            }
        }
        mAlertDialog = new AlertDialog.Builder(this)
                .setSingleChoiceItems(languages, selected,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                FormController formController = Collect
                                        .getInstance().getFormController();
                                // Update the language in the content provider
                                // when selecting a new
                                // language
                                ContentValues values = new ContentValues();
                                values.put(FormsColumns.LANGUAGE,
                                        languages[whichButton]);
                                String selection = FormsColumns.FORM_FILE_PATH
                                        + "=?";
                                String selectArgs[] = {mFormPath};
                                int updated = getContentResolver().update(
                                        FormsColumns.CONTENT_URI, values,
                                        selection, selectArgs);

                                formController
                                        .setLanguage(languages[whichButton]);
                                dialog.dismiss();
                                if (formController.currentPromptIsQuestion()) {
                                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                                }
                                refreshCurrentView();
                            }
                        })
                .setTitle(getString(R.string.change_language))
                .setNegativeButton(getString(R.string.do_not_change),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                            }
                        }).create();
        mAlertDialog.show();
    }

    /**
     * We use Android's dialog management for loading/saving progress dialogs
     */
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case PROGRESS_DIALOG:
                mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mFormLoaderTask.setFormLoaderListener(null);
                        FormLoaderTask t = mFormLoaderTask;
                        mFormLoaderTask = null;
                        t.cancel(true);
                        t.destroy();
                        finish();
                    }
                };
                mProgressDialog.setIconAttribute(R.attr.dialog_icon_info);
                mProgressDialog.setTitle(getString(R.string.loading_form));
                mProgressDialog.setMessage(getString(R.string.please_wait));
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setButton(getString(R.string.cancel_loading_form),
                        loadingButtonListener);
                return mProgressDialog;
            case SAVING_DIALOG:
                mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener savingButtonListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mSaveToDiskTask.setFormSavedListener(null);
                        SaveToDiskTask t = mSaveToDiskTask;
                        mSaveToDiskTask = null;
                        t.cancel(true);
                    }
                };
                mProgressDialog.setIconAttribute(R.attr.dialog_icon_info);
                mProgressDialog.setTitle(getString(R.string.saving_form));
                mProgressDialog.setMessage(getString(R.string.please_wait));
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setButton(getString(R.string.cancel),
                        savingButtonListener);
                mProgressDialog.setButton(getString(R.string.cancel_saving_form),
                        savingButtonListener);
                return mProgressDialog;
        }
        return null;
    }

    /**
     * Dismiss any showing dialogs that we manually manage.
     */
    private void dismissDialogs() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
    }

    @Override
    protected void onPause() {
        FormController formController = Collect.getInstance()
                .getFormController();
        dismissDialogs();
        // make sure we're not already saving to disk. if we are, currentPrompt
        // is getting constantly updated
        if (mSaveToDiskTask == null
                || mSaveToDiskTask.getStatus() == AsyncTask.Status.FINISHED) {
            if (mCurrentView != null && formController != null
                    && formController.currentPromptIsQuestion()) {
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
            }
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FormController formController = Collect.getInstance()
                .getFormController();

        if (mFormLoaderTask != null) {
            mFormLoaderTask.setFormLoaderListener(this);
            if (formController == null
                    && mFormLoaderTask.getStatus() == AsyncTask.Status.FINISHED) {
                FormController fec = mFormLoaderTask.getFormController();
                if (fec != null) {
                    loadingComplete(mFormLoaderTask);
                } else {
                    dismissDialog(PROGRESS_DIALOG);
                    FormLoaderTask t = mFormLoaderTask;
                    mFormLoaderTask = null;
                    t.cancel(true);
                    t.destroy();
                    // there is no formController -- fire MainMenu activity?
                    startActivity(new Intent(this, DashBoardActivity.class));
                }
            }
        } else {
            refreshCurrentView();
        }

        if (mSaveToDiskTask != null) {
            mSaveToDiskTask.setFormSavedListener(this);
        }
        if (mErrorMessage != null
                && (mAlertDialog != null && !mAlertDialog.isShowing())) {
            createErrorDialog(mErrorMessage, EXIT);
            return;
        }
        if (formController != null)
            event = formController.getEvent();
        if (event != FormEntryController.EVENT_END_OF_FORM)
            changeButtonNext(
                    getResources().getDrawable(
                            R.drawable.selectable_item_background),
                    R.style.ButtonNext, getString(R.string.next));
        if (current_page == 1)
            mBackButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                createQuitDialog(false);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (event.isAltPressed()) {
                    showNextView();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (event.isAltPressed()) {
                    showPreviousView();
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (mFormLoaderTask != null) {
            mFormLoaderTask.setFormLoaderListener(null);
            // We have to call cancel to terminate the thread, otherwise it
            // lives on and retains the FEC in memory.
            // but only if it's done, otherwise the thread never returns
            if (mFormLoaderTask.getStatus() == AsyncTask.Status.FINISHED) {
                FormLoaderTask t = mFormLoaderTask;
                mFormLoaderTask = null;
                t.cancel(true);
                t.destroy();
            }
        }
        if (mSaveToDiskTask != null) {
            mSaveToDiskTask.setFormSavedListener(null);
            // We have to call cancel to terminate the thread, otherwise it
            // lives on and retains the FEC in memory.
            if (mSaveToDiskTask.getStatus() == AsyncTask.Status.FINISHED) {
                mSaveToDiskTask.cancel(true);
                mSaveToDiskTask = null;
            }
        }

        super.onDestroy();

    }

    private int mAnimationCompletionSet = 0;

    private void afterAllAnimations() {
        if (mStaleView != null) {
            if (mStaleView instanceof ODKView) {
                // http://code.google.com/p/android/issues/detail?id=8488
                ((ODKView) mStaleView).recycleDrawables();
            }
            mStaleView = null;
        }

        if (mCurrentView instanceof ODKView) {
            ((ODKView) mCurrentView).setFocus(this);
        }
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (mInAnimation == animation) {
            mAnimationCompletionSet |= 1;
        } else if (mOutAnimation == animation) {
            mAnimationCompletionSet |= 2;
        }

        if (mAnimationCompletionSet == 3) {
            this.afterAllAnimations();
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    /**
     * loadingComplete() is called by FormLoaderTask once it has finished
     * loading a form.
     */
    @Override
    public void loadingComplete(FormLoaderTask task) {
        dismissDialog(PROGRESS_DIALOG);

        FormController formController = task.getFormController();
        boolean pendingActivityResult = task.hasPendingActivityResult();
        int requestCode = task.getRequestCode(); // these are bogus if
        // pendingActivityResult is
        // false
        int resultCode = task.getResultCode();
        Intent intent = task.getIntent();

        mFormLoaderTask.setFormLoaderListener(null);
        FormLoaderTask t = mFormLoaderTask;
        mFormLoaderTask = null;
        t.cancel(true);
        t.destroy();
        Collect.getInstance().setFormController(formController);
        // updateMenu

        // Set the language if one has already been set in the past
        String[] languageTest = formController.getLanguages();
        if (languageTest != null) {
            String defaultLanguage = formController.getLanguage();
            String newLanguage = "";
            String selection = FormsColumns.FORM_FILE_PATH + "=?";
            String selectArgs[] = {mFormPath};
            Cursor c = null;
            try {
                c = getContentResolver().query(FormsColumns.CONTENT_URI, null,
                        selection, selectArgs, null);
                if (c.getCount() == 1) {
                    c.moveToFirst();
                    newLanguage = c.getString(c
                            .getColumnIndex(FormsColumns.LANGUAGE));
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            // if somehow we end up with a bad language, set it to the default
            try {
                formController.setLanguage(newLanguage);
            } catch (Exception e) {
                formController.setLanguage(defaultLanguage);
            }
        }

        if (pendingActivityResult) {
            // set the current view to whatever group we were at...
            refreshCurrentView();
            // process the pending activity request...
            onActivityResult(requestCode, resultCode, intent);
            return;
        }

        // it can be a normal flow for a pending activity result to restore from
        // a savepoint
        // (the call flow handled by the above if statement). For all other use
        // cases, the
        // user should be notified, as it means they wandered off doing other
        // things then
        // returned to ODK Collect and chose Edit Saved Form, but that the
        // savepoint for that
        // form is newer than the last saved version of their form data.

        // Set saved answer path
        if (formController.getInstancePath() == null) {

            // Create new answer folder.
            String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",
                    Locale.ENGLISH).format(Calendar.getInstance().getTime());
            String file = mFormPath.substring(mFormPath.lastIndexOf('/') + 1,
                    mFormPath.lastIndexOf('.'));
            String path = Collect.INSTANCES_PATH + File.separator + file + "_"
                    + time;
            if (FileUtils.createFolder(path)) {
                formController.setInstancePath(new File(path + File.separator
                        + file + "_" + time + ".xml"));
            }
        }
        refreshCurrentView();
    }

    /**
     * called by the FormLoaderTask if something goes wrong.
     */
    @Override
    public void loadingError(String errorMsg) {
        dismissDialog(PROGRESS_DIALOG);
        if (errorMsg != null) {
            createErrorDialog(errorMsg, EXIT);
        } else {
            createErrorDialog(getString(R.string.parse_error), EXIT);
        }
    }

    /**
     * Called by SavetoDiskTask if everything saves correctly.
     */
    @Override
    public void savingComplete(int saveStatus) {
        if (fail) {
            fail = false;
            ContentValues cv = new ContentValues();
            Uri toUpdate;
            toUpdate = Uri.withAppendedPath(InstanceColumns.CONTENT_URI, ""
                    + InstanceProvider.getLastIdInstance());
            cv.put(InstanceColumns.STATUS,
                    InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
            Collect.getInstance().getContentResolver()
                    .update(toUpdate, cv, null, null);
        }
        dismissDialog(SAVING_DIALOG);
        switch (saveStatus) {
            case SaveToDiskTask.SAVED:
                sendSavedBroadcast();
                break;
            case SaveToDiskTask.SAVED_AND_EXIT:
                sendSavedBroadcast();
                finishReturnInstance(exit_to_home);
                break;
            case SaveToDiskTask.SAVE_ERROR:
                CroutonView.showBuiltInCrouton(ActivityForm.this,
                        getString(R.string.data_saved_error), Style.ALERT);
                break;
            case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
            case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
                refreshCurrentView();
                // an answer constraint was violated, so do a 'swipe' to the next
                // question to display the proper toast(s)
                showNextView();
                break;
        }
        if (send)
            uploadFile();
    }

    private void uploadFile() {
        // send list of _IDs.

        mInstancesToSend = new Long[1];
        mInstancesToSend[0] = InstanceProvider.getLastIdInstance();

        InstanceUploaderTask mInstanceUploaderTask = (InstanceUploaderTask) getLastNonConfigurationInstance();
        if (mInstanceUploaderTask == null) {
            // setup dialog and upload task
            showDialog(PROGRESS_DIALOG);
            mInstanceUploaderTask = new InstanceUploaderTask();
            // register this activity with the new uploader task
            mInstanceUploaderTask.setUploaderListener(ActivityForm.this);
            mInstanceUploaderTask.execute(mInstancesToSend);
        }

        if (mInstanceUploaderTask != null) {
            mInstanceUploaderTask.setUploaderListener(this);
        }

    }

    /**
     * Attempts to save an answer to the specified index.
     *
     * @param answer
     * @param index
     * @param evaluateConstraints
     * @return status as determined in FormEntryController
     */
    public int saveAnswer(IAnswerData answer, FormIndex index,
                          boolean evaluateConstraints) {
        FormController formController = Collect.getInstance()
                .getFormController();
        if (evaluateConstraints) {
            return formController.answerQuestion(index, answer);
        } else {
            formController.saveAnswer(index, answer);
            return FormEntryController.ANSWER_OK;
        }
    }

    /**
     * Checks the database to determine if the current instance being edited has
     * already been 'marked completed'. A form can be 'unmarked' complete and
     * then resaved.
     *
     * @return true if form has been marked completed, false otherwise.
     */
    private boolean isInstanceComplete(boolean end) {
        FormController formController = Collect.getInstance()
                .getFormController();
        // default to false if we're mid form
        boolean complete = false;

        // if we're at the end of the form, then check the preferences
		/*
		 * if (end) { // First get the value from the preferences
		 * SharedPreferences sharedPreferences = PreferenceManager
		 * .getDefaultSharedPreferences(this); complete =
		 * sharedPreferences.getBoolean(
		 * ActivityPreferences.KEY_COMPLETED_DEFAULT, true); }
		 */

        // Then see if we've already marked this form as complete before
        String selection = InstanceColumns.INSTANCE_FILE_PATH + "=?";
        String[] selectionArgs = {formController.getInstancePath()
                .getAbsolutePath()};
        Cursor c = null;
        try {
            c = getContentResolver().query(InstanceColumns.CONTENT_URI, null,
                    selection, selectionArgs, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                String status = c.getString(c
                        .getColumnIndex(InstanceColumns.STATUS));
                if (InstanceProviderAPI.STATUS_COMPLETE.compareTo(status) == 0) {
                    complete = true;
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return complete;
    }

    /**
     * Returns the instance that was just filled out to the calling activity, if
     * requested.
     */
    private void finishReturnInstance(boolean test) {
        FormController formController = Collect.getInstance()
                .getFormController();
        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action)
                || Intent.ACTION_EDIT.equals(action)) {
            // caller is waiting on a picked form
            String selection = InstanceColumns.INSTANCE_FILE_PATH + "=?";
            String[] selectionArgs = {formController.getInstancePath()
                    .getAbsolutePath()};
            Cursor c = null;
            try {
                c = getContentResolver().query(InstanceColumns.CONTENT_URI,
                        null, selection, selectionArgs, null);
                if (c.getCount() > 0) {
                    // should only be one...
                    c.moveToFirst();
                    String id = c.getString(c.getColumnIndex(BaseColumns._ID));
                    Uri instance = Uri.withAppendedPath(
                            InstanceColumns.CONTENT_URI, id);
                    setResult(RESULT_OK, new Intent().setData(instance));
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        if (test)
            Finish.finishHome();
        else
            finish();
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        // only check the swipe if it's enabled in preferences
        // Looks for user swipes. If the user has swiped, move to the
        // appropriate screen.

        // for all screens a swipe is left/right of at least
        // .25" and up/down of less than .25"
        // OR left/right of > .5"
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int xPixelLimit = (int) (dm.xdpi * .25);
        int yPixelLimit = (int) (dm.ydpi * .25);

        if (mCurrentView instanceof ODKView) {
            if (((ODKView) mCurrentView).suppressFlingGesture(e1, e2,
                    velocityX, velocityY)) {
                return false;
            }
        }

        if ((Math.abs(e1.getX() - e2.getX()) > xPixelLimit && Math.abs(e1
                .getY() - e2.getY()) < yPixelLimit)
                || Math.abs(e1.getX() - e2.getX()) > xPixelLimit * 2) {
            if (velocityX > 0) {
                if (e1.getX() > e2.getX()) {
                    if (!mNextButton.getText().equals(
                            getString(R.string.submit)))
                        showNextView();

                } else if (current_page > 1) {
                    showPreviousView();
                }
            } else {
                if (e1.getX() < e2.getX()) {
                    if (current_page > 1) {
                        showPreviousView();
                    }
                } else {
                    if (!mNextButton.getText().equals(
                            getString(R.string.submit)))
                        showNextView();
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        // The onFling() captures the 'up' event so our view thinks it gets long
        // pressed.
        // We don't wnat that, so cancel it.
        mCurrentView.cancelLongPress();
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public void advance() {
        showNextView();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void sendSavedBroadcast() {
        Intent i = new Intent();
        i.setAction("com.makina.collect.FormSaved");
        this.sendBroadcast(i);
    }

    @Override
    public void setAnswerChange(boolean hasChanged) {
        mAnswersChanged = hasChanged;
    }

    @Override
    public void deleteComplete(int deletedInstances) {
        // TODO Auto-generated method stub

    }

    @Override
    public void uploadingComplete(HashMap<String, String> result) {
        // TODO Auto-generated method stub
        try {
            dismissDialog(PROGRESS_DIALOG);
        } catch (Exception e) {
            // tried to close a dialog not open. don't care.
        }

        StringBuilder selection = new StringBuilder();
        Set<String> keys = result.keySet();
        Iterator<String> it = keys.iterator();

        String[] selectionArgs = new String[keys.size()];
        int i = 0;
        while (it.hasNext()) {
            String id = it.next();
            selection.append(BaseColumns._ID + "=?");
            selectionArgs[i++] = id;
            if (i != keys.size()) {
                selection.append(" or ");
            }
        }

        boolean success = false;
        StringBuilder message = new StringBuilder();
        {
            Cursor results = null;
            try {
                results = getContentResolver().query(
                        InstanceColumns.CONTENT_URI, null,
                        selection.toString(), selectionArgs, null);
                if (results.getCount() > 0) {
                    results.moveToPosition(-1);
                    while (results.moveToNext()) {
                        String name = results.getString(results
                                .getColumnIndex(InstanceColumns.DISPLAY_NAME));
                        String id = results.getString(results
                                .getColumnIndex(BaseColumns._ID));
                        if (!result.get(id).equals(getString(R.string.success))) {
                            ContentValues cv = new ContentValues();
                            Uri toUpdate;
                            toUpdate = Uri.withAppendedPath(
                                    InstanceColumns.CONTENT_URI,
                                    "" + InstanceProvider.getLastIdInstance());
                            cv.put(InstanceColumns.STATUS,
                                    InstanceProviderAPI.STATUS_SUBMISSION_FAILED);
                            Collect.getInstance().getContentResolver()
                                    .update(toUpdate, cv, null, null);
                            message.append(name + " - "
                                    + getString(R.string.fail) + "\n\n");
                        } else {
                            message.append(name + " - " + result.get(id)
                                    + "\n\n");
                            success = true;
                        }
                    }
                } else {
                    message.append(getString(R.string.no_forms_uploaded));
                }
            } finally {
                if (results != null) {
                    results.close();
                }
            }
        }
        if (success)
            deleteSelectedInstances();
        createAlertDialog(message.toString().trim());

    }

    private void createAlertDialog(String message) {
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setTitle(getString(R.string.upload_results));
        mAlertDialog.setMessage(message);
        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON1: // ok
                        // always exit this activity since it has no interface
                        finish();
                        if (restart) {
                            Intent myIntent = new Intent(Intent.ACTION_EDIT, uri);
                            myIntent.putExtra("newForm", true);
                            startActivity(myIntent);
                        }
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), quitListener);
        mAlertDialog.setIconAttribute(R.attr.dialog_icon_info);
        mAlertMsg = message;
        mAlertDialog.show();
    }

    private void deleteSelectedInstances() {
        ArrayList<Long> mSelected = new ArrayList<Long>();
        mSelected.add(mInstancesToSend[0]);

        DeleteInstancesTask mDeleteInstancesTask = new DeleteInstancesTask();
        mDeleteInstancesTask.setContentResolver(getContentResolver());
        mDeleteInstancesTask.setDeleteListener(this);
        mDeleteInstancesTask.execute(mSelected.toArray(new Long[mSelected
                .size()]));
    }

    @Override
    public void progressUpdate(int progress, int total) {
        // TODO Auto-generated method stub
        mAlertMsg = getString(R.string.sending_items, progress, total);
        mProgressDialog.setMessage(mAlertMsg);

    }

    @Override
    public void authRequest(Uri url, HashMap<String, String> doneSoFar) {
        // TODO Auto-generated method stub
        if (mProgressDialog.isShowing()) {
            // should always be showing here
            mProgressDialog.dismiss();
        }

        // add our list of completed uploads to "completed"
        // and remove them from our toSend list.
        ArrayList<Long> workingSet = new ArrayList<Long>();
        Collections.addAll(workingSet, mInstancesToSend);
        if (doneSoFar != null) {
            Set<String> uploadedInstances = doneSoFar.keySet();
            Iterator<String> itr = uploadedInstances.iterator();

            while (itr.hasNext()) {
                Long removeMe = Long.valueOf(itr.next());
                workingSet.remove(removeMe);
            }
            mUploadedInstances.putAll(doneSoFar);
        }

        // and reconstruct the pending set of instances to send
        Long[] updatedToSend = new Long[workingSet.size()];
        for (int i = 0; i < workingSet.size(); ++i) {
            updatedToSend[i] = workingSet.get(i);
        }
        mInstancesToSend = updatedToSend;

        showDialog(AUTH_DIALOG);
    }

    private void saveForm(String save_as) {
        if (save_as.length() < 1)
            CroutonView.showBuiltInCrouton(ActivityForm.this,
                    getString(R.string.save_as_error), Style.ALERT);
        else {
            Boolean instanceComplete = true;
            if (checkBox1.isChecked()) {
                saveDataToDisk(EXIT, instanceComplete, save_as.toString());
            } else if (checkBox2.isChecked()) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
                if (ni == null || !ni.isConnected()) {
                    // no network connection
                    fail = true;
                    CroutonView.showBuiltInCrouton(ActivityForm.this,
                            getString(R.string.no_connexion), Style.ALERT);
                    saveDataToDisk(false, true, save_as.toString());
                } else {
                    saveDataToDisk(false, true, save_as.toString());
                    send = true;
                }
            } else if (checkBox3.isChecked()) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
                if (ni == null || !ni.isConnected()) {
                    fail = true;
                    CroutonView.showBuiltInCrouton(ActivityForm.this,
                            getString(R.string.no_connexion), Style.ALERT);
                    saveDataToDisk(false, true, save_as.toString());
                } else {
                    saveDataToDisk(false, true, save_as.toString());
                    send = true;
                    restart = true;
                }
            } else
                CroutonView.showBuiltInCrouton(ActivityForm.this,
                        getString(R.string.checkbox_error), Style.ALERT);
        }
    }

    public void refreshViewHierarchy() {
        FormController formController = Collect.getInstance()
                .getFormController();
        // Record the current index so we can return to the same place if the
        // user hits 'back'.
        FormIndex currentIndex = formController.getFormIndex();

        // If we're not at the first level, we're inside a repeated group so we
        // want to only display
        // everything enclosed within that group.
        String enclosingGroupRef = "";
        formList = new ArrayList<HierarchyElement>();

        // If we're currently at a repeat node, record the name of the node and
        // step to the next
        // node to display.
        if (formController.getEvent() == FormEntryController.EVENT_REPEAT) {
            enclosingGroupRef = formController.getFormIndex().getReference()
                    .toString(false);
            formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
        } else {
            FormIndex startTest = formController.stepIndexOut(currentIndex);
            // If we have a 'group' tag, we want to step back until we hit a
            // repeat or the
            // beginning.
            while (startTest != null
                    && formController.getEvent(startTest) == FormEntryController.EVENT_GROUP) {
                startTest = formController.stepIndexOut(startTest);
            }
            if (startTest == null) {
                // check to see if the question is at the first level of the
                // hierarchy. If it is,
                // display the root level from the beginning.
                formController.jumpToIndex(FormIndex
                        .createBeginningOfFormIndex());
            } else {
                // otherwise we're at a repeated group
                formController.jumpToIndex(startTest);
            }

            // now test again for repeat. This should be true at this point or
            // we're at the
            // beginning
            if (formController.getEvent() == FormEntryController.EVENT_REPEAT) {
                enclosingGroupRef = formController.getFormIndex()
                        .getReference().toString(false);
                formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
            }
        }

        int event = formController.getEvent();
        if (event == FormEntryController.EVENT_BEGINNING_OF_FORM) {
            // The beginning of form has no valid prompt to display.
            formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
        }

        // Refresh the current event in case we did step forward.
        event = formController.getEvent();

        // There may be repeating Groups at this level of the hierarchy, we use
        // this variable to
        // keep track of them.
        String repeatedGroupRef = "";

        event_search:
        while (event != FormEntryController.EVENT_END_OF_FORM) {
            switch (event) {
                case FormEntryController.EVENT_QUESTION:
                    if (!repeatedGroupRef.equalsIgnoreCase("")) {
                        // We're in a repeating group, so skip this question and
                        // move to the next
                        // index.
                        event = formController
                                .stepToNextEvent(FormController.STEP_INTO_GROUP);
                        continue;
                    }

                    FormEntryPrompt fp = formController.getQuestionPrompt();
                    String label = fp.getLongText();
                    if (!fp.isReadOnly() || (label != null && label.length() > 0)) {
                        // show the question if it is an editable field.
                        // or if it is read-only and the label is not blank.
                        formList.add(new HierarchyElement(fp.getLongText(), fp
                                .getAnswerText(), null,
                                R.drawable.linear_blue,
                                QUESTION, fp.getIndex()));
                    }
                    break;
                case FormEntryController.EVENT_GROUP:
                    // ignore group events
                    break;
                case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                    if (enclosingGroupRef.compareTo(formController.getFormIndex()
                            .getReference().toString(false)) == 0) {
                        // We were displaying a set of questions inside of a
                        // repeated group. This is
                        // the end of that group.
                        break event_search;
                    }

                    if (repeatedGroupRef.compareTo(formController.getFormIndex()
                            .getReference().toString(false)) != 0) {
                        // We're in a repeating group, so skip this repeat prompt
                        // and move to the
                        // next event.
                        event = formController
                                .stepToNextEvent(FormController.STEP_INTO_GROUP);
                        continue;
                    }

                    if (repeatedGroupRef.compareTo(formController.getFormIndex()
                            .getReference().toString(false)) == 0) {
                        // This is the end of the current repeating group, so we
                        // reset the
                        // repeatedGroupName variable
                        repeatedGroupRef = "";
                    }
                    break;
                case FormEntryController.EVENT_REPEAT:
                    FormEntryCaption fc = formController.getCaptionPrompt();
                    if (enclosingGroupRef.compareTo(formController.getFormIndex()
                            .getReference().toString(false)) == 0) {
                        // We were displaying a set of questions inside a repeated
                        // group. This is
                        // the end of that group.
                        break event_search;
                    }
                    if (repeatedGroupRef.equalsIgnoreCase("")
                            && fc.getMultiplicity() == 0) {
                        // This is the start of a repeating group. We only want to
                        // display
                        // "Group #", so we mark this as the beginning and skip all
                        // of its children
                        HierarchyElement group = new HierarchyElement(
                                fc.getLongText(), null, getResources().getDrawable(
                                R.drawable.expander_ic_right), Color.BLACK,
                                COLLAPSED, fc.getIndex());
                        repeatedGroupRef = formController.getFormIndex()
                                .getReference().toString(false);
                        formList.add(group);
                    }

                    if (repeatedGroupRef.compareTo(formController.getFormIndex()
                            .getReference().toString(false)) == 0) {
                        // Add this group name to the drop down list for this
                        // repeating group.
                        HierarchyElement h = formList.get(formList.size() - 1);
                        h.addChild(new HierarchyElement(mIndent + fc.getLongText()
                                + " " + (fc.getMultiplicity() + 1), null, null,
                                Color.BLACK, CHILD, fc.getIndex()));
                    }
                    break;
            }
            event = formController
                    .stepToNextEvent(FormController.STEP_INTO_GROUP);
        }

        HierarchyListAdapter itla = new HierarchyListAdapter(this);
        itla.setListItems(formList);
        hierarchyList.setAdapter(itla);

        // set the controller back to the current index in case the user hits
        // 'back'
        formController.jumpToIndex(currentIndex);
    }

    private void changeButtonNext(Drawable background, int id_style,
                                  String value) {
        mNextButton.setBackgroundDrawable(background);
        mNextButton.setTextAppearance(this, id_style);
        mNextButton.setText(value);
    }

    private void dialogSaveName(final boolean _exit_to_home, final boolean exit) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(getString(R.string.form_name));
        alert.setMessage(getString(R.string.form_name_description));

        // Set an EditText view to get user input
        final CustomFontEditText input = new CustomFontEditText(this);
        if (form_name == null)
            form_name = textView_quiz_name.getText().toString();
        input.setText(form_name);
        alert.setView(input);
        alert.setIconAttribute(R.attr.dialog_icon_save);
        alert.setNegativeButton(getString(R.string.cancel), null);
        alert.setPositiveButton(getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        form_name = input.getText().toString();
                        exit_to_home = _exit_to_home;
                        saveDataToDisk(exit, isInstanceComplete(false),
                                form_name);
                        dialog.dismiss();
                    }
                });

        alert.show();

    }

    /*
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
        Theme.changeTheme(this);
        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v;
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
            v = inflator.inflate(
                    R.layout.actionbar_title_layout_edit_form_land, null);
        else
            v = inflator.inflate(R.layout.actionbar_title_layout_edit_form,
                    null);
        getSupportActionBar().setCustomView(v);
    }
    */
}
