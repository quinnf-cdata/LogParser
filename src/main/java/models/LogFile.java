package models;

import java.util.*;

public class LogFile extends FileIndex {
    private  int id;
    private HashMap<Integer,String> data;
    private  String localPath;
    private  String friendlyName;
    private FileIndex fileIndex;
    private boolean isLoading = false;
    private String firstEntryDateTime;
    private String lastEntryDateTime;

    public LogFile() {}
    public LogFile(  String localPath) {
        this.localPath = localPath;
        this.data = new HashMap<>();
        fileIndex = new FileIndex();
    }
    public LogFile( String localPath, String friendlyName) {
        this.localPath = localPath;
        this.friendlyName = friendlyName;
        this.data = new HashMap<>();
        fileIndex = new FileIndex();
    }

    public LogFile( String localPath, String friendlyName, HashMap<Integer,String> data) {
        this.localPath = localPath;
        this.friendlyName = friendlyName;
        if (data == null) {
            this.data = new HashMap<>();
        } else {
            this.data = data;
        }
        fileIndex = new FileIndex();
    }


    public void setId(int id) {
        this.id = id;
    }

    public int getId() { return id; }
    public void setLogData(String data) { setLogData(data,data.length()+1);}
    public void setLogData(String data, int lineNumber) { this.data.put(Integer.valueOf(lineNumber),data); }
    public HashMap<Integer,String>  getLogData() { return data; }
    public String getLogData(int index) { return data.get(Integer.valueOf(index)); }

    public void setLocalPath(String path) { localPath = path; }
    public String getLocalPath() { return localPath; }

    public void setFriendlyName(String friendlyName) { this.friendlyName = friendlyName; }

    public String getFriendlyName() { return friendlyName; }
    public String getContentsInRange(int start, int end) {
        StringBuilder output = new StringBuilder();
        if (end > data.size()) { end = data.size(); }
        for (int i = start; i <= end; i++) {
            output.append(i + "\t" + data.get(Integer.valueOf(i)) + "\n");
        }

        return output.toString();
    }

    public void replaceData(Integer key,String replacement) {
        data.replace(key,replacement);
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }

    private void setFirstEntryDateTime() {
        for (int i = 0; i < data.size(); i++) {
        }
    }
}
