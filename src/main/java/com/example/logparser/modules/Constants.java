package com.example.logparser.modules;

public class Constants {
    public static final String LUCENE_SPECIAL_CHARACTERS = "[\\\\+\\-!\\(\\)\\{\\}\\[\\]^~*?&|:]"; // removed \*"
    public static final String UTILITY_CHECKBOX_NAME = "Select/Deselect all";
    public static final Integer MAX_LINES_DEFAULT = 100;
    public static final String[] INVALID_EXTENSIONS = {"jpg", "jpeg", "png", "bmp", "gif", "exe", "zip", "xlsx", "xls", "doc", "docx", "pdf"};

    public enum LOG_CATEGORIES {
        QID ("Q-Id"),
        HTTP ("HTTP"),
        CONTENT (""),
        CONNECTION (""),
        META (""),
        LEVEL (""),
        TIMESTAMP (""),
        LINE ("");

        private final String searchName;
        LOG_CATEGORIES(String searchName) { this.searchName = searchName; }

        public static String[] getStringValuesLite() {
            return new String[] {HTTP.toString(),QID.toString(),CONNECTION.toString(),CONTENT.toString()};
        }

        public String getSearchName() { return searchName; }

    }

    public enum DOUBLE_CLICK_IGNORE {
        LIFETIME ("Duration"),
        HTTP ("HTTP"),
        DRIVER_INFO ("Driver Info"),
        ERRORS ("Errors"),
        QUERY_INFO ("Query Info"),
        CONNECTION_STRING ("Connection String"),
        RELEASE_DATE("Version Date"),
        JVM_HEAP("JVM Heap");

        private final String name;

        DOUBLE_CLICK_IGNORE(String name) {
            this.name = name;
        }
        public String getName() { return name; }
    }

    public enum ACTION_TYPES {
        SPLIT,
        SORT,
        NO_FILE,
        IMAGE_PROHIBITED,
        ERROR,
        DBKCLK;

        ACTION_TYPES() {}
    }
}
