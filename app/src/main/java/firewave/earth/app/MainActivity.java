package firewave.earth.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Objects;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity {

    private boolean isRecording = false;

    // Requesting permission to RECORD_AUDIO
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private Thread recordingThread;
    private PCMRecorder pcmRecorder;
    private SoundDetector soundDetector;
    private TextView txtResult;
    private GifImageView gifWave;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtResult = (TextView) findViewById(R.id.txtResult);
        gifWave = (GifImageView) findViewById(R.id.gifWave);
        ((GifDrawable) gifWave.getDrawable()).stop();
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        pcmRecorder = new PCMRecorder();
        soundDetector = new SoundDetector();
        recordingThread = new Thread(() -> {
            try {
                recordAndDetect();
            } catch (Exception e) {
                Log.e("MainActivity", "Recording Error!", e);
            }
        }, "PCMRecorder Thread");

        try {
            pcmRecorder.config(16000, 1, 16);
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to config the Recorder", e);
        }
    }

    public void btnRecordOnClick(View view) throws InterruptedException {
        if (!isRecording) {
            isRecording = true;
            ((GifDrawable) gifWave.getDrawable()).start();
            txtResult.setText("");
            recordingThread = new Thread(() -> {
                try {
                    recordAndDetect();
                    isRecording = false;
                } catch (Exception e) {
                    Log.e("MainActivity", "Recording Error!", e);
                } finally {
                    view.post(() -> {
                        ((Button) view).setText("Scan");
                        ((GifDrawable) gifWave.getDrawable()).stop();
                    });

                    isRecording = false;
                }
            }, "PCMRecorder Thread");
            recordingThread.start();
            new CountDownTimer(4000, 1000) {
                public void onTick(long millisUntilFinished) {
                    ((Button) view).setText(millisUntilFinished / 1000 + 1 + "s");
                }

                public void onFinish() {
                    ((Button) view).setText("Scan");
                }
            }.start();
        }
    }

    private void recordAndDetect() throws Exception {
        Log.i(getClass().getName(), "Started the recording thread");

//        while (isRecording) {
        byte[] data = pcmRecorder.record(4000);
        String detect = soundDetector.detect(data);
        printResult(detect);
//        }

        Log.i(getClass().getName(), "Stopped the recording thread");
    }

    private void printResult(String detect) {
        String res = detect == "FIRE_ALARM" ? "ALARM" : "Huh...";
        if(Objects.equals(detect, "FIRE_ALARM")){
            txtResult.post(() -> {
                txtResult.setText("ALARM");
                txtResult.setTextColor(Color.RED);
            });
        }
        else {
            txtResult.post(() -> {
                txtResult.setText("Huh...");
                txtResult.setTextColor(getResources().getColor(R.color.green_2));
            });
        }

    }
}