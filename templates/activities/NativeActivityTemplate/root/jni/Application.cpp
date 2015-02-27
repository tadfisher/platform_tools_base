#include "Application.h"

Application::Application()
{
  sensor_manager_       = 0;
  accelerometer_sensor_ = 0;
  sensor_event_queue_   = 0;

  display_  = 0;
  surface_  = 0;
  context_  = 0;

  is_visible_ = false;
}

Application::~Application()
{

}

/**
 * Initialize the application
 */
void Application::Init( struct android_app* state )
{
  app_ = state;

  state->userData = (void*)this;
  state->onAppCmd = Application::HandleAndroidCommand;
  state->onInputEvent = Application::HandleInput;

  // Check for saved state
  if ( state->savedState != NULL )
  {
      // We are starting with a previous saved state; restore from it.
      saved_state_ = *(struct saved_state*)state->savedState;
  }

  // Initialize sensors
  InitAndroidSensors( state );
}

/**
 * Initialize Android sensors
 */
void Application::InitAndroidSensors( struct android_app* state )
{
  // Prepare to monitor accelerometer
  sensor_manager_ = ASensorManager_getInstance();
  accelerometer_sensor_ = ASensorManager_getDefaultSensor( sensor_manager_, ASENSOR_TYPE_ACCELEROMETER );
  sensor_event_queue_ = ASensorManager_createEventQueue( sensor_manager_, state->looper, LOOPER_ID_USER, NULL, NULL );
}

/**
 * Initialize an EGL context for the current display.
 */
bool Application::InitOpenGL( struct android_app* state )
{
  /*
   * Here specify the attributes of the desired configuration.
   * Below, we select an EGLConfig with at least 8 bits per color
   * component compatible with on-screen windows
   */
  const EGLint attribs[] =
  {
          EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
          EGL_BLUE_SIZE, 8,
          EGL_GREEN_SIZE, 8,
          EGL_RED_SIZE, 8,
          EGL_NONE
  };

  EGLint format;
  EGLint num_configs;
  EGLConfig config;

  display_ = eglGetDisplay( EGL_DEFAULT_DISPLAY );

  eglInitialize( display_, 0, 0 );

  /* Here, the application chooses the configuration it desires. In this
   * sample, we have a very simplified selection process, where we pick
   * the first EGLConfig that matches our criteria */
  eglChooseConfig( display_, attribs, &config, 1, &num_configs );

  /* EGL_NATIVE_VISUAL_ID is an attribute of the EGLConfig that is
   * guaranteed to be accepted by ANativeWindow_setBuffersGeometry().
   * As soon as we picked a EGLConfig, we can safely reconfigure the
   * ANativeWindow buffers to match, using EGL_NATIVE_VISUAL_ID. */
  eglGetConfigAttrib( display_, config, EGL_NATIVE_VISUAL_ID, &format );

  ANativeWindow_setBuffersGeometry( state->window, 0, 0, format );

  surface_ = eglCreateWindowSurface( display_, config, state->window, NULL );

  const EGLint context_attribs[] =
  {
    EGL_CONTEXT_CLIENT_VERSION, 2,
    EGL_NONE
  };

  context_ = eglCreateContext( display_, config, NULL, context_attribs );

  if ( eglMakeCurrent( display_, surface_, surface_, context_ ) == EGL_FALSE )
  {
      LOGE( "Unable to eglMakeCurrent" );
      return -1;
  }

  eglQuerySurface( display_, surface_, EGL_WIDTH, &screen_width_ );
  eglQuerySurface( display_, surface_, EGL_HEIGHT, &screen_height_ );

  LOGI( "GL Version: %s", glGetString(GL_VERSION) );
  LOGI( "GLSL Version: %s", glGetString(GL_SHADING_LANGUAGE_VERSION) );
  LOGI( "Screen Size: %i, %i", screen_width_, screen_height_ );

  // Initialize GL state.
  glClearColor( 0, 1.0f, 0, 1 );

  return 0;
}

/**
 * Shutdown OpenGL
 */
void Application::ShutdownOpenGL()
{
  if ( display_ != EGL_NO_DISPLAY )
  {
      eglMakeCurrent( display_, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT );

      if ( context_ != EGL_NO_CONTEXT )
      {
          eglDestroyContext( display_, context_ );
      }

      if ( surface_ != EGL_NO_SURFACE )
      {
          eglDestroySurface( display_, surface_ );
      }

      eglTerminate( display_ );
  }

  display_ = EGL_NO_DISPLAY;
  context_ = EGL_NO_CONTEXT;
  surface_ = EGL_NO_SURFACE;
}

/**
 * Run the application
 */
void Application::Run()
{
  // Elapsed time variables
  timespec time_previous;
  timespec time_now;
  clock_gettime( CLOCK_MONOTONIC, &time_previous );
  float elapsed_time = 0.0f;

  // Main loop
  while ( true )
  {
      // Read all pending events.
      int ident;
      int events;
      struct android_poll_source* source;

      // If not animating, we will block forever waiting for events.
      // If animating, we loop until all events are read, then continue
      // to draw the next frame of animation.
      const int timeout_millis = is_visible_ ? 0 : -1;
      while ( ( ident = ALooper_pollAll( timeout_millis, NULL, &events, (void**)&source) ) >= 0 )
      {
          // Process this event.
          if ( source != NULL )
          {
              source->process( app_, source );
          }

          // If a sensor has data, process it now.
          if ( ident == LOOPER_ID_USER )
          {
              if ( sensor_manager_ != NULL )
              {
                  ASensorEvent event;
                  while ( ASensorEventQueue_getEvents( sensor_event_queue_, &event, 1 ) > 0 )
                  {
                    OnAccelerometerEvent( event.acceleration.x, event.acceleration.y, event.acceleration.z );
                  }
              }
          }

          // Check if we are exiting.
          if ( app_->destroyRequested != 0 )
          {
            ShutdownOpenGL();
            return;
          }
      }

      if ( is_visible_ )
      {
          // Done with events; draw next animation frame.

          // Drawing is throttled to the screen update rate, so there
          // is no need to do timing here.
          OnDraw();
      }

      clock_gettime( CLOCK_MONOTONIC, &time_now );
      elapsed_time = (float)( ( time_now.tv_nsec - time_previous.tv_nsec ) * 0.000000001 );
      time_previous = time_now;

      OnUpdate( elapsed_time );
  }
}

/**
 * Draw to the screen
 */
void Application::OnDraw()
{
  if ( display_ == NULL )
  {
    // No display
    return;
  }

  // Clear the backbuffer
  glClear( GL_COLOR_BUFFER_BIT );

  //
  // Add Draw code here
  //

  // ..

  // Swap buffers
  eglSwapBuffers( display_, surface_ );
}

/**
 * Update the aplication
 */
void Application::OnUpdate( const float delta_seconds )
{

}

/**
 * Process the next main command.
 */
void Application::HandleAndroidCommand( struct android_app* state, int32_t cmd )
{
    Application* app = (Application*)state->userData;

    switch ( cmd )
    {
        case APP_CMD_SAVE_STATE:
            //
            // The system has asked us to save our current state.
            //

            //  Allocate the buffer for saving the state
            state->savedState = malloc( sizeof( struct saved_state ) );

            // Copy the buffer
            *( (struct saved_state*) state->savedState ) = app->saved_state_;

            // Input the size of the state saved.
            state->savedStateSize = sizeof( struct saved_state );

            break;

        case APP_CMD_INIT_WINDOW:
            //
            // The window is being shown, get it ready.
            //
            if ( state->window != NULL )
            {
                app->InitOpenGL( state );
                app->OnDraw();
            }

            break;

        case APP_CMD_TERM_WINDOW:
            // The window is being hidden or closed, clean it up.
            app->ShutdownOpenGL();

            app->is_visible_ = false;

            break;

        case APP_CMD_GAINED_FOCUS:
            //
            // When our app gains focus, we start monitoring the accelerometer.
            //
            if ( app->accelerometer_sensor_ != NULL )
            {
                ASensorEventQueue_enableSensor( app->sensor_event_queue_, app->accelerometer_sensor_ );

                // We'd like to get 60 events per second (in us).
                ASensorEventQueue_setEventRate( app->sensor_event_queue_, app->accelerometer_sensor_, (1000L/60)*1000 );
            }

            app->is_visible_ = true;

            break;

        case APP_CMD_LOST_FOCUS:
            //
            // When our app loses focus, we stop monitoring the accelerometer.
            // This is to avoid consuming battery while not being used.
            //
            if ( app->accelerometer_sensor_ != NULL )
            {
                ASensorEventQueue_disableSensor( app->sensor_event_queue_, app->accelerometer_sensor_ );
            }

            app->is_visible_ = false;

            break;
    }
}

/**
 * Process the next input event.
 */
int32_t Application::HandleInput( struct android_app* state, AInputEvent* event )
{
  Application* app = (Application*)state->userData;

    if ( AInputEvent_getType(event) == AINPUT_EVENT_TYPE_MOTION )
    {
        const float x = AMotionEvent_getX( event, 0 );
        const float y = AMotionEvent_getY( event, 0 );

        return 1;
    }

    return 0;
}

/**
 * Process accelerometer event.
 */
void Application::OnAccelerometerEvent( const float x, const float y, const float z )
{
  
}
