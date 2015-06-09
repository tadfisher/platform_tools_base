android {
    defaultConfig {
        ndk {
            moduleName "${libraryName}"
            cFlags "-DNULL=0"
            ldLibs "android", "GLESv2"
            stl "c++_static"
        }
    }
}
