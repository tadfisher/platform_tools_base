<resources>
    <#if !isNewProject>
    <string name="title_${simpleName}">${escapeXmlString(activityTitle)}</string>
    </#if>

    <!-- Strings related to login -->
    <string name="prompt_email">Email</string>
    <string name="prompt_password">Password</string>
    <string name="action_sign_in">Sign in</string>
    <string name="error_signin_failed">Sign-in failed</string>
    <string name="signin_not_implemented">Server sign-in not implemented: assuming success</string>
    <string name="registration_not_implemented">Registration flow not implemented: sign-in failed</string>
    <string name="error_password_required">Password is required</string>
    <string name="error_incorrect_password">Password is incorrect</string>
    <string name="error_email_required">Email is required</string>
    <string name="error_unknown_user">Unknown user</string>
</resources>
