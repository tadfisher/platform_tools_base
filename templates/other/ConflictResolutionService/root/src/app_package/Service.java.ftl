package ${packageName};

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.cloudsave.BaseEventService;
import com.google.android.gms.cloudsave.Entity;

public class ${className} extends BaseEventService {
    static final boolean LOCAL_LOG = false;
    static final String TAG = ${className}.class.getSimpleName();

    public ${className}() {
      super("${className}");
      if (LOCAL_LOG) Log.i(TAG, "Constructor");
    }

    /**
     * This method is called when there is a conflict between the currently accepted entity and the
     * new entity being saved. Following are the two scenerios when a conflict can happen and
     * which view of entity is considered as accepted depends on it:
     * <ol>
     *     <li>During a sync locally saved changes to an entity are being written to the server by
     *         cloudsave but the server's view of the entity is different than what was originally
     *         read. Let's call this a remote conflict. The server's view of the entity is passed to
     *         this method as current accepted entity.
     *     <li>During a save called by a client app the view of the entity in the local database can
     *         be different than what was read by the app. This can happen if the entity was
     *         updated by another thread or was updated due to a sync by cloudsave. Lets call this a
     *         local conflict. In this case the database's view of the entity is passed to this
     *         method as current accepted entity.
     * </ol>
     * <p/>
     * Regardless of whether this method is called due to remote conflict or local conflict, the
     * acceptedEntity will always represent the view of the entity that is currently seen as the
     * accepted view and the newEntity always represents the change that is being proposed to the
     * accepted view. The least common ancestor provided along with the afore-mentioned views allows
     * the application to do a three way merge for the given conflict.
     * <p/>
     * Developers are strongly encouraged to use the methods provided in
     * {@link com.google.android.gms.cloudsave.Merge} to assist with conflict resolution.
     *
     * @param acceptedEntity Represents the previously accepted version. In case of a conflict
     *        during a sync with server this entity represents the server's version of entity. In
     *        case of a conflict during a local save, this entity represents the current version
     *        of entity stored in the local database.
     * @param newEntity Represents the change that is being saved either locally or remotely. In
     *        case of a conflict during sync, it represents the updated entity present in the local
     *        db which needs to be written to the server. In case of a conflict during save, it
     *        represents the entity being saved by the client app.
     * @param ancestorEntity Represents the least common ancestor of the given acceptedEntity and
     *        the newEntity.
     * @return a {@link Entity} Representing a resolution to the conflict consistent with the app's
     *      semantics.
     */
    @Override
    protected Entity onConflict(Entity acceptedEntity, Entity newEntity, Entity ancestorEntity) {
        if (LOCAL_LOG) {
            Log.i(TAG, "onConflict " + acceptedEntity.describeContents());
            Log.i(TAG, "           " + newEntity.describeContents());
            Log.i(TAG, "           " + ancestorEntity.describeContents());
        }

        return acceptedEntity;
    }
}

