package ${packageName};

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.cloudsave.BaseEventService;
import com.google.android.gms.cloudsave.Entity;

/*
    TODO 1: Before you run your application, you need to obtain a client ID for your Android app in Google Developers Console.

    To get a client ID, follow this link, follow the directions and press "Create" at the end:
https://console.developers.google.com/flows/enableapi?apiid=datastoremobile&keyType=CLIENT_SIDE_ANDROID&r=${debugKeystoreSha1}%3B${packageName}

    You can also add your credentials to an existing key, using this line:
    ${debugKeystoreSha1};${packageName}


    TODO 2: Once you have registered the client ID, you need to create a consent screen in Google Developers Console.

    To create a consent screen, select "APIs & auth > Consent Screen" in Google Developers Console.
    In the dialog, supply your email address (the other fields in the dialog are optional) and click "Save".
*/
public class ${cloudSaveService} extends BaseEventService {
 static final boolean LOCAL_LOG = false;
    static final String TAG = SyncDisambiguationService.class.getSimpleName();

    public SyncDisambiguationService() {
        super("SyncDisambiguationService");
        if (LOCAL_LOG) Log.i(TAG, "Constructor");
    }

    /*
     * Called when there is a conflict between the local copy and the remote. In
     * this case, we always choose the remote copy, your implementation might be
     * different and can choose which fields to update based on data or some
     * other logic.
     */
    @Override
    protected Entity onConflict(Entity local, Entity base, Entity remote) {
        if (LOCAL_LOG) {
            Log.i(TAG, "onConflict " + local.describeContents());
            Log.i(TAG, "           " + base.describeContents());
            Log.i(TAG, "           " + remote.describeContents());
        }

        return remote;
    }

}
