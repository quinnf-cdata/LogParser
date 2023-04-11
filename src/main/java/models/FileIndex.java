package models;

import controllers.CategoryController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileIndex {
    private final HashMap<Integer,String> fileIndex;
    private Map<Integer,List<Category>> categoryIndex;
    private final CategoryController categoryController;


    public FileIndex() {
        fileIndex = new HashMap<>();
        categoryIndex = new HashMap<>();
        categoryController = new CategoryController();
    }

    public List<String[]> searchClassificationIndex(Category criteria) {
        List<String[]> found = new ArrayList<>();

        for (Map.Entry<Integer,List<Category>> entry : categoryIndex.entrySet()) {
            List<Category> values = entry.getValue();

            for (Category c : values) {
                if (c.toString().equalsIgnoreCase(criteria.toString())) {
                    found.add(new String[] {entry.getKey().toString(),entry.getValue().toString()});
                }
            }
        }

        found.sort(Comparator.comparing(o -> o[1]));

        return found;
    }

    public List<String[]> searchIndexByCategory(Category category) {
        List<String[]> found = new ArrayList<>();

        for (Map.Entry<Integer,List<Category>> entry : categoryIndex.entrySet()) {
            List<Category> c = entry.getValue();
            for (Category v : c) {
                if (v.equals(category)) {
                    String d = fileIndex.get(entry.getKey());
                    if (d != null) {
                        found.add(new String[] {entry.getKey().toString(),d});
                    }
                }
            }
        }

        Collections.sort(found, new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                return o1[1].compareTo(o2[1]);
            }
        });

        return found;
    }

    public List<String[]> searchIndex(String criteria) {
        List<String[]> found = new ArrayList<>();

        criteria = criteria.toUpperCase();

        for (Map.Entry<Integer,String> entry:fileIndex.entrySet()) {
            if (entry.getValue().toUpperCase().contains(criteria)) {
                found.add(new String[] {entry.getKey().toString(),entry.getValue()});
            }
        }

        Collections.sort(found, new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                return o1[1].compareTo(o2[1]);
            }
        });

        return found;
    }


    private Category validateKeyword(String input) {
        input = input.toUpperCase();

        for (Category c : categoryController.getCategories()) {
            if (input.matches(regexWildcard(c.getRegexPattern()))) {
                return c;
            }
        }

        return null;
    }

    private void recordWordAndLine(String input, int lineNumber) {
        for (Category c : categoryController.getCategories()) {
            Pattern p = Pattern.compile(c.getRegexPattern());
            Matcher m = p.matcher(input.toUpperCase());
            if (m.find()) {
                String match = m.group();
                if (!existsInIndex(match) || c.isAllowDuplicates()) {
                    fileIndex.put(lineNumber,match);
                }
            }
        }
    }

    private boolean existsInIndex(String word) {
        for (Map.Entry<Integer,String> m:fileIndex.entrySet()) {
            if (m.getValue().equalsIgnoreCase(word)) {
                return true;
            }
        }

        return false;
    }

    public void addToIndex(int lineNumber, String line) {
        Category category = validateKeyword(line);

        if (category != null) {
            addToClassificationIndex(lineNumber, category);
            recordWordAndLine(line,lineNumber);
        }
    }

    public HashMap<Integer, String> getFileIndex() {
        return fileIndex;
    }

    public void addToClassificationIndex(Integer lineNumber, Category category) {
        List<Category> v = categoryIndex.get(lineNumber);

        if (v == null) {
            v = new ArrayList<>();
        }

        v.add(category);

        this.categoryIndex.put(lineNumber,v);
    }

    private String regexWildcard(String input) {
        return ".*"+input+".*";
    }

    public String toString(Category c) {

        if (c.isGroupOnly()) {
            return toString(c.getGroup());
        }

        List<String> sortableList = new ArrayList<>();

        for (Map.Entry<Integer,List<Category>> m: categoryIndex.entrySet()) {
            for (Category cc : m.getValue()) {
                if (cc.equals(c)) {
                    String f = fileIndex.get(m.getKey());
                    if (!(f == null)) {
                        sortableList.add(m.getKey() + "\t" + f +"\n");
                    }
                }
            }
        }

        return sortAndStructureIndexString(sortableList);
    }

    public String toString(int group) {

        List<String> sortableList = new ArrayList<>();

        for (Map.Entry<Integer,List<Category>> m: categoryIndex.entrySet()) {
            for (Category cc : m.getValue()) {
                if (cc.getGroup() == group) {
                    String f = fileIndex.get(m.getKey());
                    if (f != null) {
                        sortableList.add(m.getKey() + "\t" + f +"\n");
                    }
                }
            }
        }

        return sortAndStructureIndexString(sortableList);
    }

    private String sortAndStructureIndexString(List<String> list) {
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int o1Prefix = Integer.valueOf(o1.substring(0,o1.indexOf("\t")));
                int o2Prefix = Integer.valueOf(o2.substring(0,o2.indexOf("\t")));

                return o1Prefix-o2Prefix;
            }
        });

        StringBuilder output = new StringBuilder();
        for (String s : list) {
            output.append(s);
        }

        return output.toString();
    }

    public Map<Integer,List<Category>> getCategoryIndex() { return categoryIndex; }
    public List<Category> getCategoryByLineNumber(int lineNumber) { return categoryIndex.get(lineNumber); }
    public HashSet<Category> getCategoryList() { return categoryController.getCategories(); }




    /*public enum Category {
        CONNECTION("\\[CONNECTION: \\d+\\]",false,3,false),
        HTTP_REQUEST("\\[HTTP\\|REQ: \\d+\\]",false,2,false),
        HTTP_RESPONSE("\\[HTTP\\|RES: \\d+\\]",false,2,false),
        CONNECTION_MESSAGE("\\[INFO\\|CONNEC\\] (CLOSED|OPENED) [a-zA-Z]* CONNECTION",true,1,true);

        public final String regexString;
        public final boolean groupOnly;
        public final int group;
        public final boolean allowDuplicates;
        Category(String regexString, boolean groupOnly, int group, boolean allowDuplicates) {
            this.regexString = regexString;
            this.groupOnly = groupOnly;
            this.group = group;
            this.allowDuplicates = allowDuplicates;
        }
    }*/

    public enum GroupName {
        CONNECTION_MESSAGE(1,"Connections Open/Close"),
        HTTP_MESSAGE(2,"HTTP"),
        CONNECTION_BLOCK(3,"Connection block");

        public final int groupID;
        public final String friendlyName;

        GroupName(int groupID, String friendlyName) {
            this.groupID = groupID;
            this.friendlyName = friendlyName;
        }
    }

    public Category toClassification(Object o) {
        for (Category c : categoryController.getCategories()) {
            if (o.toString().equalsIgnoreCase(c.getName())) {
                return c;
            }
        }
        return null;
    }

    public void clear() {
        fileIndex.clear();
        categoryIndex.clear();
    }

}
