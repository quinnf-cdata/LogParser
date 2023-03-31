package models;

import java.util.*;

public class LogFile extends FileIndex {
    private  int id;
    private HashMap<Integer,String> data;
    private  String localPath;
    private  String friendlyName;
    private boolean isLoading = false;

    public LogFile() {
        data = new HashMap<>();
    }

    public LogFile( String localPath, String friendlyName, HashMap<Integer,String> data) {
        this.localPath = localPath;
        this.friendlyName = friendlyName;
        if (data == null) {
            this.data = new HashMap<>();
        } else {
            this.data = data;
        }
    }
    public void setLogData(String data) { setLogData(data,this.data.size()+1);}
    public void setLogData(String data, int lineNumber) { this.data.put(lineNumber,data); }
    public HashMap<Integer,String>  getLogData() { return data; }
    public String getLogData(int key) { return data.get(key); }

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
}
