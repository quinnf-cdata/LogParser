package com.example.logparser.modules;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import com.example.logparser.LogParserApplication;
import com.example.logparser.models.JSONConstant;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtils {
    public enum JSON_LISTS {
        FILTERS,
        ACTIONTYPES;

        JSON_LISTS() {}
    }
    private static final Path EXTERNAL_JSON_PATH = Paths.get(System.getenv("APPDATA")+ "/CData Internal/Log Analyzer/custom_filters.json");
    private static final Set<JSONConstant> jsonFilters = new HashSet<>();
    private static final Set<JSONConstant> jsonActionTypes = new HashSet<>();
    private static boolean jsonError = false;
    private static String jsonErrorMessage = null;

    public static void readExternalJSON() {
        try {
            fileExistsElseCreate();

            String jsonString = new String(Files.readAllBytes(EXTERNAL_JSON_PATH));
            JSONArray jsonArray = new JSONArray(jsonString);
            JSONObject filters = jsonArray.getJSONObject(0);

            if (Utilities.isNullOrEmpty(filters)) {
                return;
            }

            readFromJSON(filters,JSON_LISTS.FILTERS.name().toLowerCase(),jsonFilters);
            readFromJSON(filters,JSON_LISTS.ACTIONTYPES.name().toLowerCase(),jsonActionTypes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            jsonError = true;
            jsonErrorMessage = e.getMessage();
        }
    }

    private static void readFromJSON(JSONObject jsonObject, String parentCategory, Set<JSONConstant> outputSet) {
        JSONArray jsonArray = (JSONArray) jsonObject.get(parentCategory);
        for (Object o : jsonArray) {
            JSONObject jo = (JSONObject) o;
            String displayName = (String) jo.get("displayname");
            String pattern = (String) jo.get("pattern");

            outputSet.add(new JSONConstant(displayName,pattern));
        }
    }

    private static void fileExistsElseCreate() {
        File file = EXTERNAL_JSON_PATH.toFile();
        String directoryPath = EXTERNAL_JSON_PATH.toString().replaceAll(EXTERNAL_JSON_PATH.getFileName().toString(), "");

        try {
            Files.createDirectories(Paths.get(directoryPath));

            if (file.createNewFile()) {
                StringBuilder sb = new StringBuilder();

                try (InputStream is = LogParserApplication.class.getResourceAsStream("custom_filters_template.json")) {
                    if (is == null) {
                        throw new IOException("Resource not found");
                    }
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

                    // Read each line in file
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static Set<JSONConstant> getJSONList(JSON_LISTS list) {
        switch (list) {
            case ACTIONTYPES -> {
                return new HashSet<>(jsonActionTypes);
            }
            case FILTERS -> {
                return new HashSet<>(jsonFilters);
            }
        }
        return null;
    }

    public static boolean isJsonError() { return jsonError; }
    public static String getJsonErrorMessage() { return jsonErrorMessage; }
}
