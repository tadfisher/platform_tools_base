apply plugin: 'com.android.model.application'
model {
    android {
        compileSdkVersion = 21
        buildToolsVersion = "22.0.0"
    }
    android.ndk {
        moduleName = "${libraryName}"
        ldLibs += ["android", "GLESv2"]
        stl = "c++_static"
    }
}
