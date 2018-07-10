/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package didikee.video.rangeseekbar;

import android.util.Log;

public class FileLog {


    public static void e(final String message, final Throwable exception) {
        Log.e("tmessages", message, exception);
    }

    public static void e(final String message) {
        Log.e("tmessages", message);
    }

    public static void e(final Throwable e) {
    }

    public static void d(final String message) {
        Log.d("tmessages", message);

    }

    public static void w(final String message) {
        Log.w("tmessages", message);
    }

}
