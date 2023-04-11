package controllers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import models.Category;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CategoryController {
    private HashSet<Category> categories;
    private Path CATEGORY_FILE;

    public CategoryController() {
        setFilePath();
        categories = new HashSet<>();
        getCategoriesFromFile();
    }

    public HashSet<Category> getCategories() {
        return categories;
    }

    private void getCategoriesFromFile() {
        try {
            Files.createDirectories(CATEGORY_FILE.getParent());

            if (!Files.exists(CATEGORY_FILE)) {
                Files.createFile(CATEGORY_FILE);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);

            System.out.println(CATEGORY_FILE.toString());
            Category[] category = objectMapper.readValue(new File(CATEGORY_FILE.toString()),Category[].class);

            for (Category c : category) {
                categories.add(c);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setFilePath() {
        String OS = (System.getProperty("os.name")).toUpperCase();
        if (OS.contains("WIN")) {
            CATEGORY_FILE = Paths.get(System.getenv("AppData") + "\\LogFileParser\\categories.json");
        }
    }
}
