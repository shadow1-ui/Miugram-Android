/* Miugram additions are licensed under GPL-2.0-or-later. */
package org.telegram.messenger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AtomicFile;

import java.io.File;
import java.io.FileOutputStream;

/** Private, per-account, non-backed-up profile header image. Never uploaded. */
public final class MiugramProfileBackground {
    private MiugramProfileBackground() {}

    private static File file(long accountId) {
        return new File(ApplicationLoader.applicationContext.getNoBackupFilesDir(),
                "miugram-profile-" + accountId + ".jpg");
    }

    public static boolean save(long accountId, Bitmap image) {
        if (accountId <= 0 || image == null || image.isRecycled()) return false;
        AtomicFile atomic = new AtomicFile(file(accountId));
        FileOutputStream out = null;
        try {
            out = atomic.startWrite();
            if (!image.compress(Bitmap.CompressFormat.JPEG, 88, out)) throw new java.io.IOException("image encoding failed");
            atomic.finishWrite(out);
            return true;
        } catch (Throwable e) {
            if (out != null) atomic.failWrite(out);
            FileLog.e(e);
            return false;
        }
    }

    public static Bitmap load(long accountId) {
        if (accountId <= 0) return null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            AtomicFile atomic = new AtomicFile(file(accountId));
            try (java.io.FileInputStream input = atomic.openRead()) {
                BitmapFactory.decodeStream(input, null, options);
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) return null;
            options.inJustDecodeBounds = false;
            options.inSampleSize = 1;
            while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 1600) options.inSampleSize *= 2;
            try (java.io.FileInputStream input = atomic.openRead()) {
                return BitmapFactory.decodeStream(input, null, options);
            }
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    public static boolean remove(long accountId) {
        if (accountId <= 0) return false;
        File image = file(accountId);
        new AtomicFile(image).delete();
        return !image.exists() && !new File(image.getPath() + ".bak").exists()
                && !new File(image.getPath() + ".new").exists();
    }
}
