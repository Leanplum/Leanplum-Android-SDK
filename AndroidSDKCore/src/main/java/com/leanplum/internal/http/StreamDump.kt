package com.leanplum.internal.http

import android.Manifest
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.leanplum.Leanplum
import com.leanplum.LeanplumActivityHelper
import com.leanplum.internal.Log
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

fun requestPermission() {
  if (ContextCompat.checkSelfPermission(Leanplum.getContext()!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(LeanplumActivityHelper.getCurrentActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1234)
  }
}

fun logBytesAndReturnStream(requestData: Map<String, Object>, inputStream: InputStream): InputStream {
  requestPermission()

  val byteArray = inputStream.readBytes()
  inputStream.close()
  byteArray.map { String.format("%02X ", it) }
    .reduce { acc, current -> acc + current }
    .also { Log.d("bytes: $it") }

  writeToFile(requestData, byteArray)

  return ByteArrayInputStream(byteArray)
}

fun writeToFile(requestData: Map<String, Object>, bytes: ByteArray) {
  try {
    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val lp = File(downloads, "leanplum-logs")
    lp.mkdirs()
    fixPermissions(lp)

    val timestamp = timestamp()

    // <timestamp>-request
    val requestFile = File(lp, "$timestamp-request")
    requestFile.createNewFile()
    requestFile.writeText(JSONObject(requestData).toString(4))
    fixPermissions(requestFile)

    // <timestamp>-responseHex
    val responseHex = File(lp, "$timestamp-responseHex")
    responseHex.createNewFile()
    responseHex.writeText(bytes
      .map { String.format("%02X ", it) }
      .reduce { acc, current -> acc + current })
    fixPermissions(responseHex)

    // <timestamp>-responseBinary
    val responseBinary = File(lp, "$timestamp-responseBinary")
    responseBinary.createNewFile()
    responseBinary.writeBytes(bytes)
    fixPermissions(responseBinary)

  } catch (t: Throwable) {
    Log.e("Error while trying to write log files: ", t)
  }
}

fun fixPermissions(file: File) {
  file.setReadable(true, false)
  file.setWritable(true, false)
  file.setExecutable(true, false)
}

fun timestamp(): String {
  val date = Date()
  val formatter = SimpleDateFormat("yyyyMMdd'_'HHmmss.SSS", Locale.US)
  return formatter.format(date)
}
