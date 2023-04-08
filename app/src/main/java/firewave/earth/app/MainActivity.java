package firewave.earth.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.io.IOException;
import java.util.Objects;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity {

    private final int recDuration = 4000;
    private final int sampleRate = 44100;
    private final int bitsPerSample = 16;
    private boolean isRecording = false;

    // Requesting permission to RECORD_AUDIO
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private Thread recordingThread;
    private Thread feedbackThread;
    private PCMRecorder pcmRecorder;
    private SoundDetector soundDetector;
    private TextView txtResult;
    private GifImageView gifWave;
    private FloatingActionButton mYesButton;
    private FloatingActionButton mNoButton;
    private ConstraintLayout resultLayout;
    private UploadResponse uploadResponse = null;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecordAccepted) finish();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtResult = (TextView) findViewById(R.id.txtResult);
        gifWave = (GifImageView) findViewById(R.id.gifWave);
        mYesButton = findViewById(R.id.yes_button);
        mNoButton = findViewById(R.id.no_button);
        resultLayout = findViewById(R.id.resultLayout);

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

        feedbackThread = new Thread(() -> {

        });

        try {
            pcmRecorder.config(sampleRate, 1, bitsPerSample);
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to config the Recorder", e);
        }
    }

    public void btnRecordOnClick(View view) throws InterruptedException {
        if (!isRecording) {
            isRecording = true;
            ((GifDrawable) gifWave.getDrawable()).start();
            hideResult();
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
            new CountDownTimer(recDuration, 1000) {
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

        byte[] data = pcmRecorder.record(recDuration);
        try {
            uploadResponse = soundDetector.detect(data);
            showResult(uploadResponse.prediction, 5);
        } catch (Exception e) {
            showError(e.getMessage());
        }


        Log.i(getClass().getName(), "Stopped the recording thread");
    }

    private void showResult(Boolean prediction, final int timeout) {
        resultLayout.post(() -> {
            setResultText(prediction);
            resultLayout.setVisibility(View.VISIBLE);

            // Hide buttons after timeout seconds
            new CountDownTimer(timeout * 1000, 1000) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    hideResult();
                }
            }.start();
        });
    }

    private void hideResult() {
        resultLayout.post(() -> {
            resultLayout.setVisibility(View.INVISIBLE);
        });
    }

    private void setResultText(Boolean prediction) {
        if (prediction) {
            txtResult.post(() -> {
                txtResult.setText("ALARM");
                txtResult.setTextColor(Color.RED);
            });
        } else {
            txtResult.post(() -> {
                txtResult.setText("Huh...");
                txtResult.setTextColor(getResources().getColor(R.color.green_2));
            });
        }

    }

    public void noButtonResultClick(View view) {
        sendFeedback(!uploadResponse.prediction);
        hideResult();
    }

    public void yesButtonResultClick(View view) {
        sendFeedback(uploadResponse.prediction);
        hideResult();
    }

    private void showError(String message) {
//        Log.e(getClass().getName(), message);
    }

    public void sendFeedback(boolean correctPrediction) {
        new Thread(() -> {
            try {
                soundDetector.sendFeedback(uploadResponse.fileName, correctPrediction);
            } catch (Exception e) {
                showError(e.getMessage());
            }
        }).start();
    }
}