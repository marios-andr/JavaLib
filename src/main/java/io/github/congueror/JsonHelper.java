package io.github.congueror;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JsonHelper {
    private static final Gson GSON = new Gson();

    public static JsonObject httpResponseToJson(HttpResponse<String> response) {
        JsonElement a = JsonParser.parseString(response.body());
        if (a.isJsonObject()) {
            a.getAsJsonObject().addProperty("status", response.statusCode());
            return a.getAsJsonObject();
        }
        else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("status", response.statusCode());
            jsonObject.addProperty("message", response.body());
            return jsonObject;
        }
    }

    public static void saveToFile(JsonObject jsonObject, Path path) {
        try (FileWriter fileWriter = new FileWriter(path.toFile())) {
            JsonWriter jsonWriter = GSON.newJsonWriter(Streams.writerForAppendable(fileWriter));
            jsonWriter.setIndent("   ");
            GSON.toJson(jsonObject, jsonWriter);
        } catch (Exception e) {
            System.out.println("Something went wrong while saving JSON file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static JsonObject readFromFile(Path path) {
        JsonObject jsonObject = new JsonObject();

        if (!Files.exists(path)) {
            saveToFile(jsonObject, path);
            return jsonObject;
        }

        try (FileReader fileReader = new FileReader(path.toFile())) {
            JsonReader jsonReader = GSON.newJsonReader(fileReader);
            jsonObject = GSON.fromJson(jsonReader, JsonObject.class);
        } catch (Exception e) {
            System.out.println("Something went wrong while reading JSON file: " + e.getMessage());
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static boolean hasAll(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (!obj.has(key)) {
                return false;
            }
        }
        return true;
    }

    public static Map<String, Object> parseJson(String json) {
        Map<String,Object> map = new HashMap<>();
        map = (Map<String,Object>) GSON.fromJson(json, map.getClass());
        return map;
    }


}
