
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        <#if navType == 'drawer'>if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.${menuName}, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);<#else>
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.${menuName}, menu);
        return true;</#if>
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
<#if isLibraryProject?? && isLibraryProject>
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
<#else>
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
</#if>
        return super.onOptionsItemSelected(item);
    }
