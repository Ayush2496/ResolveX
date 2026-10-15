package com.example.resolvex_prototype.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    /** Photos are shrunk to this size (longest side) before being shown, so big camera photos cannot run out of memory. */
    private static final int MAX_DISPLAY_SIDE = 1024;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File createImageFile(Context context) {
        try {
            File imagesDir = new File(context.getFilesDir(), "issue_images");
            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
            }
            return new File(imagesDir, "img_" + System.currentTimeMillis() + ".jpg");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getBitmapFromPath(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        File file = new File(path);
        if (!file.exists()) return null;

        // Step 1: read only the image size (no pixels yet)
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);

        // Step 2: decode a smaller copy (every halving of the size uses 4x less memory)
        int longestSide = Math.max(bounds.outWidth, bounds.outHeight);
        int sampleSize = 1;
        while (longestSide / sampleSize > MAX_DISPLAY_SIDE) {
            sampleSize *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (bitmap == null) return null;

        return rotateAccordingToExif(file.getAbsolutePath(), bitmap);
    }

    /** Phone cameras store the photo sideways and note the rotation in the file; apply it. */
    private static Bitmap rotateAccordingToExif(String path, Bitmap bitmap) {
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            int degrees = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                degrees = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                degrees = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                degrees = 270;
            }
            if (degrees == 0) return bitmap;

            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) {
                bitmap.recycle();
            }
            return rotated;
        } catch (IOException e) {
            return bitmap;
        }
    }

    /** Copies a picture chosen from the gallery into the app's private storage. */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static String saveImageToInternalStorage(Context context, Uri uri) {
        if (uri == null) return "";

        File imagesDir = new File(context.getFilesDir(), "issue_images");
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }
        File file = new File(imagesDir, "img_" + System.currentTimeMillis() + ".jpg");

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return "";

            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.flush();
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void loadImageToView(String path, ImageView imageView) {
        Bitmap bitmap = getBitmapFromPath(path);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.GONE);
        }
    }
}
