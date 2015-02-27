#pragma once

#include <jni.h>
#include <errno.h>

#include <EGL/egl.h>
#include <GLES2/gl2.h>

#include <android/sensor.h>
#include <android/log.h>
#include <android_native_app_glue.h>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "native-activity", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "native-activity", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "native-activity", __VA_ARGS__))

/**
 * Application saved state
 */
struct saved_state
{
  char memblock[512];
};

/**
 * Application class
 */
class Application
{
public:
  Application();
  ~Application();

  void Init( struct android_app* state );
  void InitAndroidSensors( struct android_app* state );
  bool InitOpenGL( struct android_app* state );

  void ShutdownOpenGL();

  void Run();

  void OnDraw();
  void OnUpdate( const float delta_seconds );
  void OnAccelerometerEvent( const float x, const float y, const float z );

private:
  // Android App
  struct android_app* app_;

  // Phone Sensors
  ASensorManager* sensor_manager_;
  const ASensor* accelerometer_sensor_;
  ASensorEventQueue* sensor_event_queue_;

  // EGL
  EGLDisplay display_;
  EGLSurface surface_;
  EGLContext context_;

  // Screen
  int32_t screen_width_;
  int32_t screen_height_;

  // State
  bool is_visible_;
  saved_state saved_state_;

  static int32_t HandleInput( struct android_app* state, AInputEvent* event );
  static void HandleAndroidCommand( struct android_app* state, int32_t cmd );
};
