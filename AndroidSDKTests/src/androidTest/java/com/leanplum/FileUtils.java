package com.leanplum;

import androidx.annotation.NonNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

  /**
   * Uploads test resource to Android device.
   */
  public static void uploadResource(
      @NonNull String resourceName,
      @NonNull String destinationFile) throws IOException {

    InputStream in = FileUtils.class.getResourceAsStream(resourceName);
    File destination = new File(destinationFile);
    destination.createNewFile();
    FileOutputStream out = new FileOutputStream(destination);
    copyStream(in, out);
    in.close();
    out.close();
  }

  private static void copyStream(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[1024];
    int read;
    while ((read = in.read(buffer)) != -1) {
      out.write(buffer, 0, read);
    }
  }
}
