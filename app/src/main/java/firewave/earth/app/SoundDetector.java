package firewave.earth.app;


import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SoundDetector {
    private final String SOUND_DETECTOR_URL = "http://3.66.152.172:5000/upload";
    ;

    public String detect(byte[] data) throws IOException {
        Log.i(getClass().getName(), "Start Detecting. data.length: " + data.length);

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file_data", "file.txt",
                        RequestBody.create(MediaType.parse("application/octet-stream"), data))
                .build();

        Request request = new Request.Builder()
                .url(SOUND_DETECTOR_URL)
                .post(requestBody)
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        Log.i(getClass().getName(), "Received response. response code: " + response.code());
        if (response.code() == 200) {
            String res = response.body().string();
            Log.i(getClass().getName(), "Received response: " + res);
            return res;
        } else {
            return "ERROR";
        }
    }
}
