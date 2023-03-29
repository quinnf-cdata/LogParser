package controllers;

import com.google.protobuf.ByteString;
import models.FileIndex;
import models.LogFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogFileController {
    private List<LogFile> logFiles;

    private final boolean[] loading = {false};


    public LogFileController() {
        logFiles = new ArrayList<>();
    }

    public void getFileContents(String path) {

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                /*while (loading[0] == true) {
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }*/
                loading[0] = true;
                int currentFileIndex = getLogFileIdx(path);
                if (currentFileIndex == -1) {
                    addLogFile(path,null);
                }

                LogFile l = logFiles.get(currentFileIndex);
                l.setLoading(true);

                try {
                    int lineNumber = 0;

                    FileInputStream fis = new FileInputStream(path);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    BufferedReader br = new BufferedReader(new InputStreamReader(bis));

                    while (bis.available() > 0) {
                        String line;
                        while ((line=br.readLine())!=null) {
                            lineNumber++;
                            l.addToIndex(line,lineNumber);
                            l.setLogData(line,lineNumber);
                        }


                    }

                    br.close();
                    bis.close();
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                l.setLoading(false);
                loading[0] = false;
            }
        });

        t.start();
    }

    public boolean getLoadingStatus() {
        return loading[0];
    }


    public void appendLogFileContents(String content, int lineNumber,String path) {

        LogFile f = logFiles.get(getLogFileIdx(path));

        f.setLogData(content,lineNumber);
    }

    public List<LogFile> getLogFiles() {
        return logFiles;
    }

    public LogFile getLogFile(int index) { return  logFiles.get(index); }
    public int getLogFileIdx(String path) {
        for (int x = 0; x < logFiles.size(); x++) {
            LogFile l = logFiles.get(x);
            if (l.getLocalPath().compareTo(path) == 0) {
                return x;
            }
        }
        return -1;
    }

    public int addLogFile(String path, HashMap<Integer,String> data) {
        addLogFile(path, data, null);
        return logFiles.size()-1;
    }
    public void addLogFile(String path, HashMap<Integer,String> data,String friendlyName) {
        if (getLogFileIdx(path) == -1) {
            logFiles.add(new LogFile(path,friendlyName,data));
        }
    }


    // File manipulation

    public void splitLogByType(String type) {
        // default to first log
        Path p = Paths.get(logFiles.get(0).getLocalPath());
        String parentDir = p.getParent().toString();


        splitLogByType(type,logFiles.get(0),parentDir);
    }
    public void splitLogByType(String type,LogFile logFile,String directory) {
        // Get first connection line number, then get the next one to indicate end of file. Repeat
        List<String[]> connections = logFile.searchIndexByClassification(logFile.toClassification(type));


        for (int i = 0; i < connections.size(); i++) {
            String[] firstConnection = connections.get(i);
            int connectionLineNum_1 = Integer.parseInt(firstConnection[0]);

            String[] secondConnection;
            int connectionLineNum_2;

            if (i < connections.size()-1) {
                secondConnection = connections.get(i+1);
                connectionLineNum_2 = Integer.parseInt(secondConnection[0]);
            } else {
                connectionLineNum_2 = logFile.getLogData().size();
            }

            try {
                String fn = firstConnection[1].replaceAll("[^a-zA-Z0-9]","");
                String fPath = directory + "\\" + fn + ".log";

                createFile(fPath);
                FileOutputStream fos = new FileOutputStream(fPath,true);

                for (int j = connectionLineNum_1; j < connectionLineNum_2; j++) {
                    fos.write((logFile.getLogData(j)+"\n").getBytes(StandardCharsets.UTF_8));
                }

                addLogFile(fPath,null,firstConnection[1]);

                fos.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private void createFile(String fname) {
        try {
            File f = new File(fname);

            if (!f.createNewFile()) {
                f.delete();
                createFile(fname);
            }

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void storeMemoryFilesToDisk(int index) {
        LogFile l = logFiles.get(index);
        if (l.isLoading()) {
            System.out.println("Waiting for " + l.getFriendlyName() + " to finish loading...");
            while (l.isLoading()) {
                // Keep checking till complete.
            }
        }
        Path p = Paths.get(l.getLocalPath());
        String parentDir = p.getParent().toString();
        String newFilePath = parentDir+"\\"+l.getFriendlyName()+".log";

        createFile(newFilePath);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry m:l.getLogData().entrySet()) {
                    try {
                        FileOutputStream fos = new FileOutputStream(newFilePath,true);
                        fos.write((m.getValue()+"\n").getBytes(StandardCharsets.UTF_8));
                        fos.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        });

        t.start();
    }

    public void loadFilesToMemory() {
        List<LogFile> lf =this.getLogFiles();
        int size = lf.size();

        for (int i = 0; i < size; i++) {
            LogFile l = lf.get(i);
            this.getFileContents(l.getLocalPath());
        }
    }
    // Decoding
    public void decodeHexData(int index) {
        LogFile l = getLogFile(index);

        if (l.isLoading()) {
            System.out.println("Waiting for " + l.getFriendlyName() + " to finish loading...");
            while (l.isLoading()) {
                // Keep checking till complete.
            }
        }

        for (Map.Entry m:l.getLogData().entrySet()) {
            String line = m.getValue().toString().replaceAll("[\s\n]","");

            if(isHex(line)) {
                l.replaceData((Integer) m.getKey(),decodeHex(line));
            }
        }
    }

    private String decodeHex(String hexString) {
        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < hexString.length(); i += 2) {
            String str = hexString.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
    }

    private String decodeProtobuf(String hexString) {
        byte[] bytes = ByteBuffer.wrap(hexString.getBytes()).array();
        ByteString decoded= ByteString.copyFrom(bytes);

        String decodedHex = decoded.toStringUtf8();

        return decodedHex;
    }


    private boolean isHex(String s) {
        Pattern p = Pattern.compile("[0-9a-fA-F]+");
        Matcher m = p.matcher(s);

        if (m.matches()) {
            return true;
        }

        return false;
    }

    public void clear() {
        logFiles.clear();
    }
}
