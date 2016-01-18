# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/piyush/Downloads/android-studio/sdk/tools/proguard/proguard-android.txt
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

#-dontwarn com.mixpanel.**
#-dontwarn com.payu.custombrowser.*
#-dontwarn org.apache.http.**
#-keepattributes JavascriptInterface
#-keepclassmembers class ** {
 #   public void onEvent*(**);
  #  public void success*(**);
#}

#-keep public class com.payUMoney.sdk.WebViewActivity$PayUJavaScriptInterface
#-keep public class * implements com.payUMoney.sdk.WebViewActivity$PayUJavaScriptInterface
#-keepclassmembers class com.payUMoney.sdk.WebViewActivity$PayUJavaScriptInterface {
 #   public *;
#}
#-keepclassmembers class com.payUMoney.sdk.WebViewActivityPoints$PayUJavaScriptInterface {
#    public *;
#}
-dontwarn com.mixpanel.**
-dontwarn org.apache.http.**
-dontwarn com.android.volley.toolbox.**