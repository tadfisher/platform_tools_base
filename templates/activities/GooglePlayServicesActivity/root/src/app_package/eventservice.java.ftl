package ${packageName};

import android.content.Intent;

import com.google.android.gms.cloudsave.BaseEventService;
import com.google.android.gms.cloudsave.Entity;

public class ${cloudSaveService} extends BaseEventService {
    public ${cloudSaveService}() {
        super("${cloudSaveService}");
    }

    @Override
    protected Entity onConflict(Entity localEntity, Entity baseEntity,
            Entity remoteEntity) {
        // TODO Implement conflict resolution here.
        return remoteEntity;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // TODO
    }
}
