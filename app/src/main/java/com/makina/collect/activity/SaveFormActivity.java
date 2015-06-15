package com.makina.collect.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.makina.collect.R;
import com.makina.collect.fragment.SaveFormRecyclerViewFragment;
import com.makina.collect.util.ThemeUtils;

/**
 * Simple {@code Activity} about available forms to save.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 * @see SaveFormRecyclerViewFragment
 */
public class SaveFormActivity
        extends AbstractBaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setTitle(R.string.edit);
            actionBar.setSubtitle(R.string.form);
        }

        setContentView(R.layout.activity_single);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                                       .replace(android.R.id.content,
                                                SaveFormRecyclerViewFragment.newInstance())
                                       .commit();
        }
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

    @Nullable
    @Override
    protected ThemeUtils.AppThemeVariant applyTheme() {
        return ThemeUtils.AppThemeVariant.BLUE;
    }
}
