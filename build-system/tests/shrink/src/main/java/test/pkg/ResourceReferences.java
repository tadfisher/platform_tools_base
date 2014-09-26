package test.pkg;

import android.content.Context;
import android.content.res.Resources;

public class ResourceReferences {
    public static void referenceResources(Context context) {
        Resources resources = context.getResources();
        int dynamicId1 = resources.getIdentifier("used3", "layout", context.getPackageName());
        System.out.println(dynamicId1);

        int dynamicId2 = resources.getIdentifier("test.pkg:layout/used4", null, null);
        System.out.println(dynamicId2);

        int dynamicId3 = resources.getIdentifier("test.pkg:" + getType() + "/" + getLayoutUrl(),
                null, null);
        System.out.println(dynamicId3);

        int dynamicId4 = resources.getIdentifier("test.pkg:string/unused2", null, null);
        System.out.println(dynamicId4);
    }

    public static String getType() {
        return "string";
    }

    public static String getLayoutUrl() {
        // Prevent inlining
        if (System.currentTimeMillis() % 2 == 0) {
            return "used5";
        } else {
            return "used6";
        }
    }
}
