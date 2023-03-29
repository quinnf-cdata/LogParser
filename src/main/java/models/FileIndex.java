package models;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileIndex {
    private HashMap<Integer,String> fileIndex;
    private List<Integer> keys;
    private HashMap<Integer,Classification> classificationIndex;

    public FileIndex() {
        fileIndex = new HashMap<>();
        classificationIndex = new HashMap<>();
        keys = new ArrayList<>();
    }

    public HashMap<Integer,String> getFileIndex() {
        return fileIndex;
    }

    public List<String[]> searchClassificationIndex(Classification criteria) {
        List<String[]> found = new ArrayList<>();

        for (Map.Entry m:classificationIndex.entrySet()) {
            if (m.getValue().toString().equalsIgnoreCase(criteria.toString())) {
                found.add(new String[] {m.getKey().toString(),m.getValue().toString()});
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

    public List<String[]> searchIndexByClassification(Classification classification) {
        List<String[]> found = new ArrayList<>();

        for (Map.Entry m:classificationIndex.entrySet()) {
            if (m.getValue().equals(classification)) {
                String d = fileIndex.get(m.getKey());
                if (!(d == null)) {
                    found.add(new String[] {m.getKey().toString(),d});
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

        for (Map.Entry m:fileIndex.entrySet()) {
            if (m.getValue().toString().toUpperCase().contains(criteria)) {
                found.add(new String[] {m.getKey().toString(),m.getValue().toString()});
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


    private Classification validateKeyword(String input) {
        input = input.toUpperCase();

        for (Classification c : Classification.values()) {
            if (input.matches(regexWildcard(c.regexString))) {
                return c;
            }
        }

        return null;
    }

    private void recordWordAndLine(String input, int lineNumber) {
        for (Classification c : Classification.values()) {
            Pattern p = Pattern.compile(c.regexString);
            Matcher m = p.matcher(input.toUpperCase());
            if (m.find()) {
                String match = m.group();
                if (!existsInIndex(match) || c.allowDuplicates) {
                    fileIndex.put(Integer.valueOf(lineNumber),match);
                }
            }
        }
    }

    private boolean existsInIndex(String word) {
        for (Map.Entry m:fileIndex.entrySet()) {
            if (m.getValue().toString().equalsIgnoreCase(word)) {
                return true;
            }
        }

        return false;
    }

    public List<Integer> getKeys() { return keys; }
    public Integer getKey(int index) { return keys.get(index); }

    public void addToIndex(String line, int lineNumber) {
        Classification classification = validateKeyword(line);

        if (classification != null) {
            classificationIndex.put(Integer.valueOf(lineNumber),classification);
            recordWordAndLine(line,lineNumber);
        }
    }

    private String regexWildcard(String input) {
        return ".*"+input+".*";
    }

    public String toString(Classification c) {

        if (c.groupOnly) {
            return toString(c.group);
        }

        List<String> sortableList = new ArrayList<>();

        for (Map.Entry m:classificationIndex.entrySet()) {
            Classification cc = (Classification) m.getValue();

            if (cc.equals(c)) {
                String f = fileIndex.get(m.getKey());
                if (!(f == null)) {
                    sortableList.add(m.getKey() + "\t" + f +"\n");
                }
            }
        }

        return sortAndStructureIndexString(sortableList);
    }

    public String toString(int group) {

        List<String> sortableList = new ArrayList<>();

        for (Map.Entry m:classificationIndex.entrySet()) {
            Classification cc = (Classification) m.getValue();

            if (cc.group == group) {
                String f = fileIndex.get(m.getKey());
                if (!(f == null)) {
                    sortableList.add(m.getKey() + "\t" + f +"\n");
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

    public HashMap<Integer,Classification> getClassificationIndex() { return classificationIndex; }




    public enum Classification {
        CONNECTION("\\[CONNECTION:\\s\\d\\]\\b",false,3,false),
        HTTP_REQUEST("\\[HTTP\\|REQ: \\d+\\]",false,2,false),
        HTTP_RESPONSE("\\[HTTP\\|RES: \\d+\\]",false,2,false),
        CONNECTION_CLOSED_MESSAGE("\\[INFO\\|CONNEC\\] CLOSED [a-zA-Z]* CONNECTION",true,1,true),
        CONNECTION_OPEN_MESSAGE("\\[INFO\\|CONNEC\\] OPENED [a-zA-Z]* CONNECTION",true,1,true);

        public final String regexString;
        public final boolean groupOnly;
        public final int group;
        public final boolean allowDuplicates;
        Classification(String regexString, boolean groupOnly, int group, boolean allowDuplicates) {
            this.regexString = regexString;
            this.groupOnly = groupOnly;
            this.group = group;
            this.allowDuplicates = allowDuplicates;
        }
    }

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

    public Classification toClassification(Object o) {
        for (Classification c : Classification.values()) {
            if (o.toString() == c.toString()) {
                return c;
            }
        }
        return null;
    }

    public void clear() {
        fileIndex.clear();
        classificationIndex.clear();
    }

}
