package com.android.tests.basic;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.TextView;

public class MainTest extends ActivityInstrumentationTestCase2<Main> {

    /**
     * Creates an {@link ActivityInstrumentationTestCase2} that tests the {@link Main} activity.
     */
    public MainTest() {
        super(Main.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Espresso will not launch the activity for us, we must launch it via getActivity()
        getActivity();
    }

    @MediumTest
    public void testEspresso() {
        onView(withId(R.id.text)).check(matches(withText(containsString("Basic"))));
    }
}

