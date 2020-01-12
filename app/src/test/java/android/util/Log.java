package android.util;

public class Log {
    public static int e(String tag, String message, Throwable error) {
        System.err.println(String.format("[ERROR] %s: %s", tag, message));
        error.printStackTrace(System.err);
        return 0;
    }

    public static int d(String tag, String message) {
        System.out.println(String.format("[DEBUG] %s: %s", tag, message));
        return 0;
    }

    public static int d(String tag, String message, Throwable throwable) {
        d(tag, message);
        throwable.printStackTrace();
        return 0;
    }
}
