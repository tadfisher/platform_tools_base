package ${packageName};

import ${superClassFqcn};
import android.os.Bundle;

import android.opengl.GLSurfaceView;
import android.opengl.GLES20;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ${activityClass} extends ${superClass} {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load C++ library
        System.loadLibrary( "${libraryName}" );

        GLSurfaceView glView = new GLSurfaceView(this);
        glView.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);	    
            }

            @Override
            public void onDrawFrame(GL10 gl) {
		nativeDrawFrame();
            }
        });
        setContentView(glView);	
    }

    protected native void nativeDrawFrame();
}
