package com.android.tests.libdeps;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.android.tests.libdeps.R;

public class MainActivity extends ActionBarActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lib1_main);
    }

    public void test() {
        getActionBar();                    // ERROR
        getSupportActionBar();             // OK

        startActionMode(null);             // ERROR
        startSupportActionMode(null);      // OK

        requestWindowFeature(0);           // ERROR
        supportRequestWindowFeature(0);    // OK

        setProgressBarVisibility(true);    // ERROR
        setProgressBarIndeterminate(true);
        setProgressBarIndeterminateVisibility(true);

        setSupportProgressBarVisibility(true); // OK
        setSupportProgressBarIndeterminate(true);
        setSupportProgressBarIndeterminateVisibility(true);
    }
}
