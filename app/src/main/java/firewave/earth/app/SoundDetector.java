package firewave.earth.app;


import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SoundDetector {
//    private final String BASE_URL = "http://10.0.2.2:5000";
    private final String BASE_URL = "http://3.66.152.172:5000";
    private UploadResponse uploadResponse = null;

    public UploadResponse detect(byte[] data) throws IOException {
        Log.i(getClass().getName(), "Start Detecting. data.length: " + data.length);

        String url = BASE_URL + "/upload";

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file_data", "file.txt",
                        RequestBody.create(MediaType.parse("application/octet-stream"), data))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        Log.i(getClass().getName(), "Received response. response code: " + response.code());
        if (response.code() == 200) {
            uploadResponse = UploadResponse.fromJson(response.body().string());
            Log.i(getClass().getName(), "Received response: " + uploadResponse.prediction);
            return uploadResponse;
        } else {
            throw new RuntimeException("Error!!!");
        }
    }

    public void sendFeedback(String fileName, boolean correctPrediction) throws JSONException, IOException {
        Log.i(getClass().getName(), "Sending feedback for file name: " + fileName + " isSignal: " + correctPrediction);

        String url = BASE_URL + "/feedback";

        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");
        JSONObject json = new JSONObject();
        json.put("fileName", fileName);
        json.put("correctPrediction", correctPrediction);
        RequestBody body = RequestBody.create(mediaType, json.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();

        Log.i(getClass().getName(), "Received response: " + response.code());
    }
}
