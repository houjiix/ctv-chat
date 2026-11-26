# Keep Room-generated implementations and Media3 metadata in release builds.
-keep class com.cinematv.app.data.**Database_Impl { *; }
-keep class androidx.media3.** { *; }
-dontwarn org.checkerframework.**
