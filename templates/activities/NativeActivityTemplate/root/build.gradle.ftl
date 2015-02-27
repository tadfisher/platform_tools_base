android {
    defaultConfig {
        ndk {
            moduleName "${libraryName}"
            cFlags "-DNULL=0"
            ldLibs "android", "EGL", "GLESv2", "dl", "log"
            stl "c++_static"
        }
    }
}
