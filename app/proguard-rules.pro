# Keep application entry point and ViewModel classes
-keep class com.nfcpay.simulator.MainActivity { *; }

# Strip all android.util.Log calls in release builds as defense-in-depth
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}
