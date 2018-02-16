package com.leanplum;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * An immutable reference to a file, allowing to {@link #stream(Context) open an InputStream from it}.
 * <p>
 * It can only reference either:
 * <ul>
 * <li>{@link ResourceReference an Android resource}</li>
 * <li>{@link AssetReference an Android asset}</li>
 * <li>{@link FileReference a file (not resource/asset)}</li>
 * <li>{@link MemoryReference in-memory data}</li>
 * </ul>
 *
 * @author Eugenio Marletti
 */
public abstract class StreamReference {
  private StreamReference() {
  }

  /**
   * Opens an {@link InputStream} from this reference by using the provided {@link Context}.
   */
  @Nullable
  public abstract InputStream stream(@NonNull Context context) throws IOException;

  /**
   * {@link StreamReference} that can {@link #stream} without a {@link Context}.
   */
  private static abstract class StreamReferenceSansContext extends StreamReference {
    private StreamReferenceSansContext() {
    }

    /**
     * Opens an {@link InputStream} from this reference.
     */
    @Nullable
    public abstract InputStream stream() throws IOException;
  }

  /**
   * An immutable reference to an Android resource file.
   *
   * @see #getResourceId()
   */
  public static final class ResourceReference extends StreamReference {
    @IdRes
    private final int resourceId;

    public ResourceReference(@IdRes int resourceId) {
      this.resourceId = resourceId;
    }

    @IdRes
    public int getResourceId() {
      return resourceId;
    }

    @Nullable
    @Override
    public InputStream stream(@NonNull Context context) throws FileNotFoundException {
      Resources res = context.getResources();
      // Based on resource Id, we can extract package it belongs, directory where it is stored
      // and name of the file.
      Uri resourceUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
          "://" + res.getResourcePackageName(resourceId)
          + '/' + res.getResourceTypeName(resourceId)
          + '/' + res.getResourceEntryName(resourceId));
      return context.getContentResolver().openInputStream(resourceUri);
    }

    // auto-generated
    @Override
    public int hashCode() {
      return resourceId;
    }

    // auto-generated
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ResourceReference that = (ResourceReference) o;

      return resourceId == that.resourceId;
    }
  }

  /**
   * An immutable reference to an Android asset file.
   *
   * @see #getFileName()
   */
  public static final class AssetReference extends StreamReference {
    @NonNull
    private final String fileName;

    public AssetReference(@NonNull String fileName) {
      this.fileName = fileName;
    }

    @NonNull
    public String getFileName() {
      return fileName;
    }

    @NonNull
    @Override
    public InputStream stream(@NonNull Context context) throws IOException {
      return context.getAssets().open(fileName);
    }

    // auto-generated
    @Override
    public int hashCode() {
      return fileName.hashCode();
    }

    // auto-generated
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      AssetReference that = (AssetReference) o;

      return fileName.equals(that.fileName);
    }
  }

  /**
   * An immutable reference to a file that is not an {@link ResourceReference Android resource} nor an
   * {@link AssetReference Android asset}.
   *
   * @see #getFile()
   */
  public static final class FileReference extends StreamReferenceSansContext {
    @NonNull
    private final File file;

    public FileReference(@NonNull File file) {
      this.file = file;
    }

    @NonNull
    public File getFile() {
      return file;
    }

    @NonNull
    @Override
    public FileInputStream stream() throws FileNotFoundException {
      return new FileInputStream(file);
    }

    /**
     * @deprecated Since a {@link Context} is not needed to open an {@link InputStream} for this file
     * reference, using {@link #stream()} is recommended.
     */
    @NonNull
    @Override
    public FileInputStream stream(@Nullable Context context) throws FileNotFoundException {
      return stream();
    }

    // auto-generated
    @Override
    public int hashCode() {
      return file.hashCode();
    }

    // auto-generated
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      FileReference that = (FileReference) o;

      return file.equals(that.file);
    }
  }

  /**
   * An immutable reference to an in-memory file.
   *
   * @see #getData()
   */
  public static final class MemoryReference extends StreamReferenceSansContext {
    @NonNull
    private final byte[] data;

    public MemoryReference(@NonNull byte[] data) {
      this.data = data;
    }

    /**
     * Returns a copy of the data.
     */
    @NonNull
    public byte[] getData() {
      return Arrays.copyOf(data, data.length);
    }

    @NonNull
    @Override
    public ByteArrayInputStream stream() {
      return new ByteArrayInputStream(data);
    }

    /**
     * @deprecated Since a {@link Context} is not needed to open an {@link InputStream} for this file
     * reference, using {@link #stream()} is recommended.
     */
    @NonNull
    @Override
    public ByteArrayInputStream stream(@Nullable Context context) {
      return stream();
    }

    // auto-generated
    @Override
    public int hashCode() {
      return Arrays.hashCode(data);
    }

    // auto-generated
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      MemoryReference that = (MemoryReference) o;

      return Arrays.equals(data, that.data);
    }
  }
}
