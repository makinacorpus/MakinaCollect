package com.makina.collect.android.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.makina.collect.android.R;
import com.makina.collect.android.fragment.AboutFragment;

/**
 * Simple {@code Activity} showing the "about us" page.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 * @see com.makina.collect.android.fragment.AboutFragment
 */
public class AboutActivity
        extends AbstractBaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setTitle(R.string.about_us);
        }

        setContentView(R.layout.activity_single);

        overridePendingTransition(android.R.anim.fade_in,
                                  android.R.anim.fade_out);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                                       .replace(android.R.id.content,
                                                AboutFragment.newInstance())
                                       .commit();
        }
    }

    @Override
    public void finish() {
        super.finish();

        overridePendingTransition(android.R.anim.fade_in,
                                  android.R.anim.fade_out);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
