package android.util;

public class Log {
    public static int d(java.lang.String tag, java.lang.String message) {
        System.out.println(String.format("[DEBUG] %s: %s", tag, message));
        return 0;
    }

    public static int d(java.lang.String tag, java.lang.String message, java.lang.Throwable throwable) {
        d(tag, message);
        throwable.printStackTrace();
        return 0;
    }
}
