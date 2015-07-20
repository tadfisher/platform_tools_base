package com.android.build.gradle.internal.tasks;

import static com.google.common.base.Preconditions.checkState;

import com.android.builder.model.SigningConfig;
import com.android.ide.common.signing.KeystoreHelper;
import com.android.ide.common.signing.KeytoolException;
import com.android.prefs.AndroidLocation;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.StopExecutionException;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

/**
 * A validate task that creates the debug keystore if it's missing. It only creates it if it's in
 * the default debug keystore location.
 *
 * It's linked to a given SigningConfig
 */
public class ValidateSigningTask extends BaseTask {
    private SigningConfig signingConfig;

    /**
     * Annotated getter for task input.
     *
     * This is an Input and not an InputFile because the file might not exist. This is not actually
     * used by the task, this is only for Gradle to check inputs.
     *
     * @return the path of the keystore.
     */
    @Input
    @Optional
    public String getStoreLocation() {
        File f = signingConfig.getStoreFile();
        if (f != null) {
            return f.getAbsolutePath();
        }

        return null;
    }

    @TaskAction
    public void validate() throws AndroidLocation.AndroidLocationException, KeytoolException {
        File storeFile = signingConfig.getStoreFile();
        if (storeFile == null) {
            throw new StopExecutionException(
                    "Keystore file not set for signing config " + signingConfig.getName());
        }
        if (!storeFile.exists()) {
            if (KeystoreHelper.defaultDebugKeystoreLocation().equals(storeFile.getAbsolutePath())) {
                checkState(signingConfig.isSigningReady(), "Debug signing config not ready.");

                getLogger().info(
                        "Creating default debug keystore at {}",
                        storeFile.getAbsolutePath());

                //noinspection ConstantConditions - isSigningReady() called above
                if (!KeystoreHelper.createDebugStore(signingConfig.getStoreType(),
                        signingConfig.getStoreFile(), signingConfig.getStorePassword(),
                        signingConfig.getKeyPassword(), signingConfig.getKeyAlias(),
                        getILogger())) {
                    throw new RuntimeException("Unable to recreate missing debug keystore.");
                }
            } else {
                throw new IllegalArgumentException(
                        String.format(
                                "Keystore file %s not found for signing config '%s'.",
                                storeFile.getAbsolutePath(),
                                signingConfig.getName()));
            }
        }
    }

    public SigningConfig getSigningConfig() {
        return signingConfig;
    }

    public void setSigningConfig(SigningConfig signingConfig) {
        this.signingConfig = signingConfig;
    }
}
