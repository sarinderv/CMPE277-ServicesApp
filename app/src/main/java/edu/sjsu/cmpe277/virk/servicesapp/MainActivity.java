package edu.sjsu.cmpe277.virk.servicesapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * Sarinder Virk - sarinder.virk@sjsu.edu
 * CMPE277 - Assignment #2 - 3/12/2022
 *
 * The purpose of the App is to use Android Services (background) to download files.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickDownload(View v) {
        Toast.makeText(this, "Download button clicked", Toast.LENGTH_SHORT).show();

        int[] fieldIds = new int[] {R.id.textAddr1, R.id.textAddr2, R.id.textAddr3, R.id.textAddr4, R.id.textAddr5};
        String[] urls = new String[fieldIds.length];
        for (int i = 0; i < fieldIds.length; i++) {
            EditText textField = findViewById(fieldIds[i]);
            urls[i] = textField.getText().toString();
        }

        // Following is similar to professor's code in lecture, send intent to a service
        Intent intent = new Intent(getBaseContext(), DownloadService.class);
        intent.putExtra(DownloadService.INTENT_ARG, urls);
        intent.putExtra(DownloadService.RECEIVER, new DownloadReceiver(new Handler()));
        startService(intent);
    }

    private class DownloadReceiver extends ResultReceiver {

        public DownloadReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if (resultCode == DownloadService.UPDATE_PROGRESS) {
                int progress = resultData.getInt(DownloadService.RECEIVER); //get the progress
                ProgressBar progressBar = findViewById(R.id.progressBar);
                progressBar.setProgress(progress);
            }
        }
    }
}