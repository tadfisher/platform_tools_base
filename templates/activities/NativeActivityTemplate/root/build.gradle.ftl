android {
    defaultConfig {
        ndk {
            moduleName "${activityClass}"
            cFlags "-DNULL=0"
            ldLibs "android", "EGL", "GLESv1_CM", "dl", "log"
            stl "stlport_shared"
        }
    }
}
