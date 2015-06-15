# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keepattributes **

-dontobfuscate
-dontoptimize

# Allow obfuscation of android.support.v7.internal.view.menu.** to avoid problem on some devices
# (like Samsung 4.2.2) with AppCompat v21
# see https://code.google.com/p/android/issues/detail?id=78377
-keep class !android.support.v7.internal.view.menu.**, android.support.** {*;}

-keep class org.xmlpull.v1.** {*;}

-dontwarn org.apache.http.entity.mime.**
-dontwarn org.apache.james.mime4j.**

-ignorewarnings