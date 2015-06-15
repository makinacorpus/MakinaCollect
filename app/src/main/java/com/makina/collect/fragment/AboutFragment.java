package com.makina.collect.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.makina.collect.R;

/**
 * {@code Fragment} to show the "about us" page.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class AboutFragment
        extends Fragment {

    /**
     * Use this factory method to create a new instance of this {@code Fragment}.
     *
     * @return A new instance of {@link AboutFragment}.
     */
    @NonNull
    public static AboutFragment newInstance() {
        final AboutFragment aboutFragment = new AboutFragment();
        aboutFragment.setArguments(new Bundle());

        return aboutFragment;
    }

    public AboutFragment() {
        // required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about,
                                container,
                                false);
    }
}
