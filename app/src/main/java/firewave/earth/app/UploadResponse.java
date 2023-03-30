package firewave.earth.app;

import com.google.gson.Gson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class UploadResponse {
    String fileName;
    Boolean prediction;

    public UploadResponse(String fileName, Boolean prediction){
        this.fileName = fileName;
        this.prediction = prediction;
    }
    public static UploadResponse fromJson(String json){
        Gson gson = new Gson();
        return gson.fromJson(json, UploadResponse.class);
    }
}
