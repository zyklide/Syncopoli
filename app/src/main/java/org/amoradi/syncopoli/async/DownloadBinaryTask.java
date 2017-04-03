package org.amoradi.syncopoli.async;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.widget.Toast;

import org.amoradi.syncopoli.utils.TLSSocketFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class DownloadBinaryTask extends AsyncTask<String, Void, String> {

  private Context context;
  private PowerManager.WakeLock mWakeLock;
  private String filePath;

  public DownloadBinaryTask(Context context) {
    this.context = context;
  }

  @Override
  protected String doInBackground(String... filenames) {
    InputStream input = null;
    OutputStream output = null;
    HttpsURLConnection connection = null;

    try {
      URL url = new URL("https://amoradi.org/public/android/arm/" + filenames[0]);
      filePath = context.getFileStreamPath(filenames[0]).getAbsolutePath();
      connection = (HttpsURLConnection) url.openConnection();
      connection.setSSLSocketFactory(new TLSSocketFactory());
      connection.connect();

      // handle http response and break if there is an issue
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        return "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
      }

      // download the file
      input = connection.getInputStream();
      output = context.openFileOutput(filenames[0], Context.MODE_PRIVATE);

      byte data[] = new byte[4096];
      int count;
      while ((count = input.read(data)) != -1) {

        // back button will cancel the download
        if (isCancelled()) {
          input.close();
          File f = new File(filePath);
          f.delete();
          return null;
        }
        output.write(data, 0, count);
      }
    } catch (Exception e) {
      return e.toString();
    } finally {
      try {
        if (output != null)
          output.close();
        if (input != null)
          input.close();
      } catch (IOException ignored) {
      }

      if (connection != null) {
        connection.disconnect();
      }
    }
    return null;
  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();

    // enable wakelock for duration of download
    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
    mWakeLock.acquire();
  }

  @Override
  protected void onPostExecute(String result) {
    mWakeLock.release();
    if (result != null) {
      Toast.makeText(context, "Download Error: " + result, Toast.LENGTH_LONG).show();
      File f = new File(filePath);
      f.delete();
    } else {
      File f = new File(filePath);
      f.setExecutable(true);
    }
  }
}