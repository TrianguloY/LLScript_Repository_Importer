# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/pierrot/Devel/Android/SDKs/android-studio/sdk/tools/proguard/proguard-android.txt
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

-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault,*Annotation*,InnerClasses,SourceFile,LineNumberTable

-keep public class * extends android.content.Context
-keep class com.google.gson.** {*;}
-keep interface com.google.gson.** {*;}
-keep class dw.xmlrpc.** {*;}
-keep interface dw.xmlrpc.** {*;}
-keeppackagenames org.jsoup.nodes
-keep class com.trianguloy.llscript.repository.internal.PageCacheManager$Page {*;}
#-dontwarn **
