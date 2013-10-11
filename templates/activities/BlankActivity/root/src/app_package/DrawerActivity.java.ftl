package ${packageName};

import <#if appCompat?has_content>android.support.v7.app.ActionBarActivity<#else>android.app.Activity</#if>;
import android.<#if appCompat?has_content>support.v7.</#if>app.ActionBar;
import android.<#if appCompat?has_content>support.v4.</#if>app.Fragment;
import android.<#if appCompat?has_content>support.v4.</#if>app.FragmentManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ${activityClass} extends ${(appCompat?has_content)?string('ActionBar','')}Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #onRestoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.${layoutName});

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                get${Support}FragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        DummyFragment fragment = DummyFragment.newInstance(position + 1);
        mTitle = getTitleForSection(position);
        onRestoreActionBar();

        FragmentManager fragmentManager = get${Support}FragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    public String getTitleForSection(int position) {
        switch (position) {
            case 0:
                return getString(R.string.title_section1);
            case 1:
                return getString(R.string.title_section2);
            case 2:
                return getString(R.string.title_section3);
        }
        return null;
    }

    @Override
    public void onRestoreActionBar() {
        ActionBar actionBar = get${Support}ActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    <#include "include_options_menu.java.ftl">

    <#include "include_fragment.java.ftl">

}
