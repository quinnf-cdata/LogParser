package com.example.logparser.modules;

import org.json.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class JSONtoLogExtractor {
    private static final String home_path = System.getProperty("user.home");
    private static File output_file = null;
    private static ArrayList<String> output_data = new ArrayList<>();
    private static Path filePath;
    public static void process(String inputPath) {
        JSONtoLogExtractor.filePath = Paths.get(inputPath);
        try {
            // JSON
            String jsonString = new String(Files.readAllBytes(filePath));;

            // Parse the JSON string into a JSONArray
            JSONArray jsonArray = new JSONArray(jsonString);
            // Loop through each JSONObject in the JSONArray

            for (int i = 0; i < jsonArray.length(); i++) {
                // Get the current JSONObject
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                try {
                    // Extract the textPayload
                    String textPayload = jsonObject.getString("textPayload");

                    // Output the textPayload
                    System.out.println(textPayload);
                    output_data.add(textPayload);
                } catch (JSONException e) {
                    continue;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            writeToFile();
        }
    }

    private static void writeToFile() {
        String fileName = String.valueOf(filePath.getFileName());
        output_file = new File(home_path+"/Downloads/Extracted-" + fileName + ".log");
        System.out.println(output_file);
        try (FileWriter fileWriter = new FileWriter(output_file)) {
            for (String s : output_data) {
                fileWriter.write(s + System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
