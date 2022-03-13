package edu.sjsu.cmpe277.virk.servicesapp;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.ResultReceiver;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * See https://developer.android.com/guide/components/services#ExtendingService
 */
public class DownloadService extends Service {
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private ResultReceiver receiver;
    public static final String INTENT_ARG = "URLs";
    public static final String RECEIVER = "receiver";
    public static final int UPDATE_PROGRESS = 8344; // magic number?

    @Override
    public void onCreate() {
        Toast.makeText(this, "service creating", Toast.LENGTH_SHORT).show();

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting: "+ startId, Toast.LENGTH_SHORT).show();

        receiver = intent.getParcelableExtra(RECEIVER);

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent.getStringArrayExtra(INTENT_ARG);
        serviceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            String[] urls = (String[])msg.obj;
            for (String url : urls) {
                try {
                    downloadFile(url);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }

        // see https://stackoverflow.com/a/3028660
        private void downloadFile(String urlToDownload) throws IOException {
            // extract filename from url
            String filename = urlToDownload.substring(urlToDownload.lastIndexOf("/"));
            Toast.makeText(getBaseContext(), "Downloading: "+ filename, Toast.LENGTH_SHORT).show();

            // create url and connect
            URL url = new URL(urlToDownload);
            URLConnection connection = url.openConnection();
            connection.connect();

            // this will be useful so that you can show a typical 0-100% progress bar
            int fileLength = connection.getContentLength();

            try (
                InputStream input = new BufferedInputStream(connection.getInputStream());
                OutputStream output = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + filename);
            ) {
                byte data[] = new byte[1024];
                long total = 0;
                int count;
                // download the file
                while ((count = input.read(data)) != -1) {
                    total += count;

                    // publishing the progress....
                    Bundle resultData = new Bundle();
                    int progress = (int) (total * 100 / fileLength);
                    resultData.putInt(RECEIVER , progress);
                    receiver.send(UPDATE_PROGRESS, resultData);
                    output.write(data, 0, count);
                }

                output.flush();
            }
        }
    }
}