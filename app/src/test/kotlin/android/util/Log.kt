package android.util

/**
 * Stub for android.util.Log so that SmsPatternEngine can run in pure JVM
 * unit tests (./gradlew test) without needing an Android device or emulator.
 *
 * All log calls are printed to stdout so test output still shows them.
 */
object Log {
    @JvmStatic fun w(tag: String, msg: String): Int { println("W/$tag: $msg"); return 0 }
    @JvmStatic fun e(tag: String, msg: String): Int { println("E/$tag: $msg"); return 0 }
    @JvmStatic fun e(tag: String, msg: String, tr: Throwable): Int { println("E/$tag: $msg $tr"); return 0 }
    @JvmStatic fun d(tag: String, msg: String): Int { println("D/$tag: $msg"); return 0 }
    @JvmStatic fun i(tag: String, msg: String): Int { println("I/$tag: $msg"); return 0 }
    @JvmStatic fun v(tag: String, msg: String): Int { println("V/$tag: $msg"); return 0 }
}