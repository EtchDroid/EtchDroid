package android.util

object Log {
    fun e(tag: String?, message: String?, error: Throwable) {
        System.err.println(String.format("[ERROR] %s: %s", tag, message))
        error.printStackTrace(System.err)
    }

    fun d(tag: String?, message: String?) {
        println(String.format("[DEBUG] %s: %s", tag, message))
    }

    fun d(tag: String?, message: String?, throwable: Throwable) {
        Log.d(tag, message)
        throwable.printStackTrace()
    }
}