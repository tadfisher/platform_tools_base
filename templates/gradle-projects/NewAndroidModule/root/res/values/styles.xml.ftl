<resources>

    <!-- Base application theme. -->
<#if hasAppBar?? && hasAppBar && appCompat>
    <style name="AppTheme" parent="Theme.AppCompat.Light.NoActionBar">
       <!-- Customize your theme here. -->
        <item name="colorPrimary">#607D8B</item>
        <item name="colorPrimaryDark">#455A64</item>
        <item name="colorAccent">#FFAB40</item>
    </style>
<#else>
    <style name="AppTheme" parent="<#if
            appCompat>Theme.AppCompat<#else
            >android:Theme.Holo</#if><#if
            !baseTheme?contains("dark")>.Light<#if
            baseTheme?contains("darkactionbar")>.DarkActionBar</#if></#if>">
        <!-- Customize your theme here. -->
    </style>
</#if>
</resources>
