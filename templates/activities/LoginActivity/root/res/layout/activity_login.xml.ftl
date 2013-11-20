<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
              xmlns:tools="http://schemas.android.com/tools"
              tools:context=".${activityClass}"              
              android:layout_width="match_parent"
              android:layout_height="match_parent"
              android:paddingLeft="@dimen/activity_horizontal_margin"
              android:paddingRight="@dimen/activity_horizontal_margin"
              android:paddingTop="@dimen/activity_vertical_margin"
              android:paddingBottom="@dimen/activity_vertical_margin"
              android:orientation="vertical"
              android:gravity="center_horizontal" >

    <com.google.android.gms.common.SignInButton
        android:id="@+id/sign_in_button"
        android:layout_width="match_parent"
        android:layout_marginBottom="32dp"
        android:layout_height="wrap_content"/>

    <FrameLayout
        android:id="@+id/email_login_fragment"
        android:layout_height="wrap_content"
        android:layout_width="match_parent"/>

</LinearLayout>

