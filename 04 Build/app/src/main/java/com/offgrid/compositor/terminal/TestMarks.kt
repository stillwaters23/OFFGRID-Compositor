package com.offgrid.compositor.terminal

import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Private test results (features 15 and 37, final build M4). Each platform's test is marked
 * "baked in as success" or "failed", with a note and the date. Marks stay through every reset.
 *
 * The success code is the real check: a 6 digit code goes into the video for 30 seconds,
 * a viewer on another phone reads it, and Royalty types what they saw. A match proves the
 * effects reach viewers.
 */
internal object TestMarks {
    const val NOT_TESTED = "not"
    const val SUCCESS = "success"
    const val FAILED = "failed"

    const val MARK = "test.mark."
    const val NOTE = "test.note."
    const val DATE = "test.date."

    /** The code a viewer saw, as typed. */
    const val SEEN = "test.seen."

    /** The code the video showed last, waiting to be checked. */
    const val SHOWN = "test.shown."

    /** The test the success code proves: effects show in the video viewers see. */
    const val CODE_TEST = "burned_in"

    private const val CODE_DIGITS = 6

    fun markKey(platform: String, test: String) = "$MARK$platform.$test"

    fun noteKey(platform: String, test: String) = "$NOTE$platform.$test"

    fun dateKey(platform: String, test: String) = "$DATE$platform.$test"

    /** The platform a Tests folder belongs to, or null for any other folder. */
    fun platformOf(folder: PanelFolder): String? =
        if (folder.kind == FolderKind.TESTS && folder.id.endsWith(".tests")) folder.id.removeSuffix(".tests") else null

    fun mark(store: ThemeStore, platform: String, test: String): TestMark = when (store.choice(markKey(platform, test))) {
        SUCCESS -> TestMark.SUCCESS
        FAILED -> TestMark.FAILED
        else -> TestMark.NOT_TESTED
    }

    fun date(store: ThemeStore, platform: String, test: String): String = store.text(dateKey(platform, test))

    /** Saves a mark with today's date and an optional note. */
    fun setMark(store: ThemeStore, platform: String, test: String, mark: String, note: String?, day: String = today()) {
        store.setChoice(markKey(platform, test), mark)
        store.setText(dateKey(platform, test), day)
        if (note != null) store.setText(noteKey(platform, test), note)
    }

    /** A fresh 6 digit code. It is only a test code, never a password. */
    fun newCode(random: SecureRandom = SecureRandom()): String =
        (1..CODE_DIGITS).joinToString("") { random.nextInt(10).toString() }

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    enum class Check { NO_CODE, MATCH, MISMATCH }

    /**
     * Checks the code a viewer saw against the one the video showed, and marks the test.
     * Spaces and dashes are ignored, so "123 456" matches "123456".
     */
    fun check(store: ThemeStore, platform: String, seen: String, day: String = today()): Check {
        val shown = store.text(SHOWN + platform)
        if (shown.isEmpty()) return Check.NO_CODE
        val typed = seen.filter { it.isDigit() }
        store.setText(SHOWN + platform, "")
        return if (typed == shown) {
            setMark(store, platform, CODE_TEST, SUCCESS, "A viewer saw the test code.", day)
            Check.MATCH
        } else {
            setMark(store, platform, CODE_TEST, FAILED, "A viewer saw $typed, not $shown.", day)
            Check.MISMATCH
        }
    }
}
