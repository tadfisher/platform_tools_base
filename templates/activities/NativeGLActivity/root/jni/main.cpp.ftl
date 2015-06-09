#include <jni.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

extern "C"
{
  JNIEXPORT void JNICALL Java_${"${packageName}_${activityClass}"?replace(".", "_")}_nativeDrawFrame( JNIEnv* env, jobject activity );
}

JNIEXPORT void JNICALL Java_${"${packageName}_${activityClass}"?replace(".", "_")}_nativeDrawFrame( JNIEnv* env, jobject activity )
{
  glClearColor(1.0f, 0.0f, 1.0f, 1.0f);
  glClear(GL_COLOR_BUFFER_BIT);
}
