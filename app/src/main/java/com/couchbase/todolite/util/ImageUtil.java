package com.couchbase.todolite.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;

import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtil {
    /**
     * Create a thumbnail bitmap from an input stream. This method is not
     * currently work with a FileInputStream as the FileInputStream doesn't
     * support mark and reset.
     *
     * @param is The input stream to read from
     * @param width width
     * @param height height
     * @return a result thumbnail bitmap
     */
    public static Bitmap thumbmailFromInputStream(InputStream is, int width, int height) {
        Bitmap bitmap = decodeBitmapFromInputStream(is, width, height);
        return ThumbnailUtils.extractThumbnail(bitmap, width, height);
    }

    /**
     * Create a thumbnail bitmap from a filename
     * @param filename The full path of the file
     * @param width width
     * @param height height
     * @return a result thumbnail bitmap
     */
    public static Bitmap thumbnailFromFile(String filename, int width, int height) {
        Bitmap bitmap = decodeBitmapFromFile(filename, width, height);
        return ThumbnailUtils.extractThumbnail(bitmap, width, height);
    }

    /**
     * Create a thumbnail bitmap from a descriptor
     * @param descriptor The file descriptor to read from
     * @param width width
     * @param height height
     * @return a result thumbnail bitmap
     */
    public static Bitmap thumbnailFromDescriptor(FileDescriptor descriptor, int width, int height) {
        Bitmap bitmap = decodeBitmapFromDescriptor(descriptor, width, height);
        return ThumbnailUtils.extractThumbnail(bitmap, width, height);
    }

    /**
     * Decode and sample down a bitmap from an input stream to the requested width and height.
     *
     * @param is The input stream to read from
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static Bitmap decodeBitmapFromInputStream(InputStream is, int reqWidth, int reqHeight) {
        // Bitmap m = BitmapFactory.decodeStream(is);
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        Bitmap mm = BitmapFactory.decodeStream(is, null, options);

        return mm;
    }

    /**
     * Decode and sample down a bitmap from a file to the requested width and height.
     *
     * @param filename The full path of the file to decode
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static Bitmap decodeBitmapFromFile(String filename, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filename, options);
    }

    /**
     * Decode and sample down a bitmap from a file input stream to the requested width and height.
     *
     * @param fileDescriptor The file descriptor to read from
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static Bitmap decodeBitmapFromDescriptor(
            FileDescriptor fileDescriptor, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
    }

    /**
     *
     * Calculate an inSampleSize for use in a {@link android.graphics.BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link android.graphics.BitmapFactory}. This implementation calculates
     * the closest inSampleSize that is a power of 2 and will result in the final decoded bitmap
     * having a width and height equal to or larger than the requested width and height.
     *
     * @param options An options object with out* params already populated (run through a decode*
     *            method with inJustDecodeBounds==true
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        /// Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
