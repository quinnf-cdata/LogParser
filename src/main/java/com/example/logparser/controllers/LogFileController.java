package com.example.logparser.controllers;

import com.example.logparser.modules.Constants;
import com.example.logparser.models.LogFile;
import com.example.logparser.modules.LogHelper;
import com.example.logparser.modules.Utilities;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.ScoreDoc;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogFileController {
    private static Set<LogFile> logFiles;
    private static boolean processing = false;
    private static Map<String, StringBuilder> memoryFiles = null;


    public LogFileController() {
        logFiles = new HashSet<>();
    }


    public LogFile getLogFileByIndex(int index) {
        try {
            return new ArrayList<>(logFiles).get(index);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LogFile getLogFileByID(String id) {
        return getLogFileByID(Integer.valueOf(id));
    }
    public LogFile getLogFileByID(int id) {
        try {
            for (LogFile f : logFiles) {
                if (f.getMetadataID() == id) {
                    return f;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return null;
    }
    public int getLogFileIdx(String strPath) {
        // Replace forward slashes with back slashes, for consistency
        strPath = strPath.replaceAll("/","\\\\");
        Path keyPath = Paths.get(strPath);
        // Iterate through log file objects to find log file index by given path
        ArrayList<LogFile> localLogs = new ArrayList<>(logFiles);

        for (int x = 0; x < localLogs.size(); x++) {
            LogFile l = localLogs.get(x);
            if (l.getLocalPath().getFileName().toString().compareTo(keyPath.getFileName().toString()) == 0) {
                // Return index
                return x;
            }
        }
        // If no file found, return -1
        return -1;
    }
    public void addLogFile(Path path, boolean allowIndex) {
        addLogFile(path,allowIndex,false);
    }
    public void addLogFile(Path path, boolean allowIndex, boolean isMaster) {
        if (Utilities.isNullOrEmpty(path)) {
            return;
        }

        int existLogIdx = getLogFileIdx(path.toString());
        LogFile newLogFile = new LogFile(path,allowIndex);
        newLogFile.setMaster(isMaster);

        if (existLogIdx == -1) {
            logFiles.add(newLogFile);
            registerLogFile(newLogFile);
        } else {
            ArrayList<LogFile> a = new ArrayList<>(logFiles);
            a.set(existLogIdx,newLogFile);
            logFiles.clear();
            logFiles.addAll(a);
        }
    }

    public void removeLogFile(int index) {
        if (index >= 0) {
            ArrayList<LogFile> a = new ArrayList<>(logFiles);
            a.remove(index);
            logFiles.clear();
            logFiles.addAll(a);
        }
    }

     /*File manipulation*/

    public void splitLogsByType(String type, boolean orderByTimestamp, boolean memoryOnly, ArrayList<String> fileList) {

        ArrayList<LogFile> localLogs = new ArrayList<>();

        if (fileList != null) {
            for (LogFile l : logFiles) {
                if (fileList.stream().anyMatch(f -> f.equalsIgnoreCase(l.getFileName()))) {
                    localLogs.add(l);
                }
            }
        } else {
            localLogs.addAll(logFiles);
        }

        if (memoryFiles == null) {
            memoryFiles = new HashMap<>();
        }
        Set<String> outputFiles = new HashSet<>();
        LogHelper.setFilesToProcess(localLogs.size());
        LogHelper.setProcessingFile(0);
        for (LogFile l : localLogs) {
            LogHelper.setProcessingFile(LogHelper.getProcessingFile()+1);
            LogHelper.setTotalMaxRecords(l.getLineCount());
            LogHelper.setCurrentRecord(BigInteger.valueOf(0));
            if (l.isMaster()) { // Only process unsplit files loaded by user
                String parentDir = l.getLocalPath().getParent().toString();
                splitLogByType(type,l,parentDir,orderByTimestamp, memoryOnly,outputFiles);
            }
        }
    }

    private void splitLogByType(String type, LogFile logFile, String directory, boolean orderByTimestamp, boolean memoryOnly, Set<String> outputFiles) {

        setProcessing(true);
        String category = type.replaceAll("[^a-zA-Z0-9]", "");
        BigInteger linesCount = logFile.getLineCount();

        while (logFile.isLoading()) {
            System.out.print(".");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (Exception ignored) {}
            logFile = getLogFileByIndex(0);
            // Keep checking till complete.
        }
        System.out.println();

        logFile.setMaxLines(1); // Only handle one found line at a time
        Object[] firstItem = null;
        ScoreDoc[] firstHits = null;
        StoredFields firstStoredFields = null;
        BigInteger firstLine;

        int foundHits = logFile.getDocHitCount(type,type, false);
        System.out.println("Found " + foundHits + " hits.");
        System.out.println("Splitting into individual logs...");
        LogHelper.setMark(logFile.getFileName());

        for (int i = 0; i < foundHits; i++) {

            Object[] secondItem;
            ScoreDoc[] secondHits = new ScoreDoc[0];
            StoredFields secondStoredFields;
            BigInteger secondLine;

            String outputFileName;
            Path outputFilePath;

            try (IndexReader reader = DirectoryReader.open(logFile.getIndex())) {
                Document firstDoc;
                Document secondDoc;

                if (firstItem == null) {
                    firstItem = logFile.queryIndex(category,type, false, reader);
                    firstHits = (ScoreDoc[]) firstItem[0];
                    firstStoredFields = (StoredFields) firstItem[1];
                }

                if (firstHits.length <= 0) {return;}

                firstDoc = firstStoredFields.document(firstHits[0].doc);
                outputFileName = firstDoc.get(category).replaceAll("[^a-zA-Z0-9]", "")+".log";
                outputFilePath = Paths.get(directory + "\\" + outputFileName);

                if (i == 0) {
                    firstLine = BigInteger.valueOf(0);
                } else {
                    firstLine = BigInteger.valueOf(Integer.parseInt(firstDoc.get(Constants.LOG_CATEGORIES.LINE.name())));
                }

                String lineHeader1 = firstDoc.get(category);
//                System.out.println("Compiling data for " + lineHeader1+"...");

                boolean doesEqual = false;
                do {
                    String lineHeader2 = "END";

                    if (i < foundHits-1) {
                        if (doesEqual && secondHits != null) {
                            secondItem = logFile.queryIndex(category,type,secondHits[0],false,reader);
                        } else {
                            secondItem = logFile.queryIndex(category,type,firstHits[0], false,reader);
                        }
                        secondHits = (ScoreDoc[]) secondItem[0];
                        secondStoredFields = (StoredFields) secondItem[1];
                        secondDoc = secondStoredFields.document(secondHits[0].doc);
                        secondLine = BigInteger.valueOf(Long.parseLong(secondDoc.get(Constants.LOG_CATEGORIES.LINE.name())));
                        lineHeader2 = secondDoc.get(category);
                    } else {
                        secondItem = null;
                        secondHits = null;
                        secondStoredFields = null;
                        secondLine = linesCount.add(BigInteger.valueOf(1));
                    }

                    doesEqual = false;// Reset bool

                    if (lineHeader1.equalsIgnoreCase(lineHeader2)) {
                        doesEqual = true;
                        i++;
                    }
//                    System.out.print("\rStarting on line: "+firstLine+".\tEnding on line: "+secondLine);
                    LogHelper.setStatus("Starting on line: "+firstLine+".\tEnding on line: "+secondLine);
                    LogHelper.smartAddCurrentRecord(secondLine);
                } while (doesEqual);
//                System.out.println();

                StringBuilder sb = new StringBuilder();

                TreeMap<BigInteger, String> lines = logFile.readLines(firstLine,secondLine);

                for (BigInteger key : lines.keySet()) {
                    sb.append(lines.get(key)).append("\n");
                }

                memoryFiles.putIfAbsent(outputFilePath.toString(), new StringBuilder());
                memoryFiles.get(outputFilePath.toString()).append(sb);

                // Shift everything for the loop
                if (i < foundHits - 1) {
                    firstItem = secondItem;
                    firstHits = secondHits;
                    firstStoredFields = secondStoredFields;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (Map.Entry<String, StringBuilder> entry : memoryFiles.entrySet()) {
            if (outputFiles.add(entry.getKey())) { // Add returns false if the element was already in the set
                if (!memoryOnly) {
                    createFile(Path.of(entry.getKey()));
                }
                LogFile newLog = new LogFile(entry.getKey(),!memoryOnly);
                newLog.setTimePattern(logFile.getTimePattern());
                newLog.setParentFile(logFile.getFileName());
                newLog.setMemoryFile(true);
                registerLogFile(newLog);
            }
            if (!memoryOnly) {
                try (FileOutputStream fos = new FileOutputStream(entry.getKey())) {
                    fos.write(entry.getValue().toString().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Indexing new files...");
        List<Integer> removeOutputFile = new ArrayList<>();
        for (String s : outputFiles) {
            int idx = getLogFileIdx(s);

            if (idx == -1) {
                removeOutputFile.add(idx);
                continue;
            }
            LogFile l = getLogFileByIndex(idx);
            l.indexFile();
        }

        for (Integer i : removeOutputFile) {
            outputFiles.remove(i);
        }

        int returnCount = (logFiles.size()-1);
//        logFile.setWasSplit(true);

        System.out.println("Split complete. " + returnCount + " new files.");

        if (orderByTimestamp) { orderByTimeStamp(); }

        setProcessing(false);
    }

    public void combineAndDelete() throws IncompatibleFileError {
        setProcessing(true);
        System.out.println("Begin combining files...");

        ArrayList<LogFile> masterFiles = new ArrayList<>();

        LogFile log1 = null;
        for (LogFile l : logFiles) {
            if (l.isMaster()) {
                masterFiles.add(l);
            }
            
            if (Utilities.isNullOrEmpty(log1)) {
                log1 = l;
            }
             else if (!Utilities.isNullOrEmpty(l.getTimePattern()) && !Utilities.isNullOrEmpty(log1.getTimePattern()) && !log1.getTimePattern().equalsIgnoreCase(l.getTimePattern())) {
                setProcessing(false);
                String pname = Utilities.isNullOrEmpty(log1.getParentFile()) ? log1.getFileName() : log1.getParentFile();
                String pname2 = Utilities.isNullOrEmpty(l.getParentFile()) ? l.getFileName() : l.getParentFile();

                throw new IncompatibleFileError(pname,pname2,"Timestamp format mismatch");
            }
        }
        ArrayList<Path> toDelete = new ArrayList<>();
        ArrayList<Path> outputFiles = new ArrayList<>();
        ArrayList<LogFile> localLogFiles = new ArrayList<>(logFiles);

        for (int i = 0; i < masterFiles.size(); i++) { // This loop can probably be removed
            String masterFileName = masterFiles.get(i).getFileName().replaceAll(".log","");
            Path file = localLogFiles.get(i).getLocalPath();

            Pattern pattern = Pattern.compile("\\d+");
            localLogFiles.sort((o1, o2) -> {
                String s1 = o1.getLocalPath().getFileName().toString();
                String s2 = o2.getLocalPath().getFileName().toString();

                Matcher m1 = pattern.matcher(s1);
                Matcher m2 = pattern.matcher(s2);

                int num1 = m1.find() ? Integer.parseInt(m1.group()) : Integer.MAX_VALUE;
                int num2 = m2.find() ? Integer.parseInt(m2.group()) : Integer.MAX_VALUE;

                return Integer.compare(num1, num2);
            });



            for (LogFile l : localLogFiles) {
                if (l.isMaster()) {
                    // Don't touch the original file
                    continue;
                } else if (toDelete.contains(l.getLocalPath())) {
                    // Don't reprocess a file
                    continue;
                } else if (l.isLoading()) {
                    // Check if file is loading... It shouldn't be at this point.
                    System.out.print("\nLoading");
                    while (l.isLoading()) {
                        System.out.print(".");
                        try {
                            TimeUnit.SECONDS.sleep(3);
                        } catch (Exception ignored) {}
                        l = new ArrayList<>(logFiles).get(getLogFileIdx(l.getLocalPath().toString()));
                        // Keep checking till complete.
                    }
                }

                Path outputFilePath;
                try {
                    String outputFileName  = "Sorted_" + masterFileName + ".log";
                    String outputFileDirectory = file.getParent().toString();
                    outputFilePath = Paths.get(outputFileDirectory + "\\" + outputFileName);
                    boolean registerFile = false;

                    if (!pathExists(outputFiles,outputFilePath)) {
                        createFile(outputFilePath);
                        outputFiles.add(outputFilePath);
                        registerFile = true;
                    }

                    StringBuilder sb = memoryFiles.get(l.getLocalPath().toString());

                    if (sb == null) {
                        sb = new StringBuilder();
                        FileInputStream fis = new FileInputStream(l.getLocalPath().toString());
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        BufferedReader br = new BufferedReader(new InputStreamReader(bis));

                        // Read each line in file
                        while (bis.available() > 0) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                sb.append(line).append("\n");
                            }
                        }
                        br.close();
                        bis.close();
                        fis.close();
                    }

                    try (FileOutputStream fos = new FileOutputStream(outputFilePath.toString(), true)) {
                        fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    if (registerFile) {
                        addLogFile(outputFilePath,true,true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (!l.isMaster()) {
                        toDelete.add(l.getLocalPath());
                    }
                }
//                addLogFile(outputFilePath,false);
            }
        }

        System.out.println("Removing temporary files...");
        for (Path p : toDelete) {
            deleteFile(p);
            int idx;
            if ((idx = getLogFileIdx(p.toString())) > -1) {
                LogFile toRemove = new ArrayList<>(logFiles).get(idx);
                logFiles.remove(toRemove);
            }
        }

        System.out.println("Combine process complete");
        setProcessing(false);
    }

    public void orderByTimeStamp() {
        System.out.println("Begin timestamp sort...");

        String timePattern = logFiles.stream()
                .filter(LogFile::isMaster)
                .map(LogFile::getTimePattern)
                .filter(timePatternStr -> !Utilities.isNullOrEmpty(timePatternStr))
                .findFirst()
                .orElse("");

        if (Utilities.isNullOrEmpty(timePattern)) {
            System.out.println("No valid time pattern found.");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timePattern);

        for (LogFile l : logFiles) {
            if (l.isMaster() || l.isSorted() || l.isLoading()) {
                continue;
            }

            Path path = l.getLocalPath();
            List<LogLine> logLines = parseLogLines(path, formatter, timePattern);

            logLines.sort(Comparator.comparing(LogLine::getTimestamp));

            if (memoryFiles.containsKey(path.toString())) {
                StringBuilder sortedString = new StringBuilder();
                for (LogLine logLine : logLines) {
                    sortedString.append(logLine.getLine()).append("\n");
                }
                memoryFiles.replace(path.toString(), sortedString);
            } else {
                writeLogLinesToPath(path, logLines);
            }

            l.setSorted(true);
            System.out.println("Sorted " + path.getFileName());
        }

        System.out.println("Sorting files complete");
    }

    private ZonedDateTime timestamp = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
    private List<LogLine> parseLogLines(Path path, DateTimeFormatter formatter, String timePattern) {
        List<LogLine> logLines = new ArrayList<>();
        Consumer<String> lineConsumer = line -> {

            String foundDate = Utilities.parseDateFromString(line, timePattern);
            if (!Utilities.isNullOrEmpty(foundDate)) {
                timestamp = ZonedDateTime.parse(foundDate, formatter);
            }
            logLines.add(new LogLine(timestamp, line));
        };

        StringBuilder content = memoryFiles.get(path.toString());
        if (!Utilities.isNullOrEmpty(content)) {
            for (String line : content.toString().split("\n")) {
                lineConsumer.accept(line);
            }
        } else {
            try (BufferedReader br = Files.newBufferedReader(path)) {
                String line;
                while ((line = br.readLine()) != null) {
                    lineConsumer.accept(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return logLines;
    }

    private void writeLogLinesToPath(Path path, List<LogLine> logLines) {
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (LogLine logLine : logLines) {
                bw.write(logLine.getLine());
                bw.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private class LogLine {
        private final ZonedDateTime timestamp;
        private final String line;

        public LogLine(ZonedDateTime timestamp, String line) {
            this.timestamp = timestamp;
            this.line = line;
        }

        public ZonedDateTime getTimestamp() {
            return timestamp;
        }

        public String getLine() {
            return line;
        }
    }

    private boolean pathExists(ArrayList<Path> list, Path absolutePath) {
        for (Path p : list) {
            if (p.equals(absolutePath)) {
                return true;
            }
        }
        return false;
    }

    private void deleteFile(Path path) {
        File f = new File(path.toString());
        if(f.exists() && !f.isDirectory()) {
            f.delete();
        }
    }
    private void createFile(Path path) {
        try {
            File f = new File(path.toString());

            if (!f.createNewFile()) {
                f.delete();
                createFile(path);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<LogFile> getLogFiles() {
        return new ArrayList<>(logFiles);
    }

    public void decodeHexData(boolean v, boolean asciiOnly) {
        for (LogFile l : logFiles) {
            l.setDecodeHex(v);
            l.setFilterNonASCII(asciiOnly);
        }
    }

    public void registerLogFile(LogFile newLog) {
        ArrayList<LogFile> localLogs = new ArrayList<>(logFiles);
        boolean exists = false;
        for (int i = 0; i < localLogs.size(); i++) {
            LogFile l = localLogs.get(i);
            if (l.getLocalPath().toString().equalsIgnoreCase(newLog.getLocalPath().toString())) {
                exists = true;
                l.setSorted(false);
                localLogs.set(i,l);
                break;
            }
        }

        if (!exists) {
            localLogs.add(newLog);
        }

        logFiles.clear();
        logFiles.addAll(localLogs);

    }

    public void writeToDisk(String stringPath, LogFile source) {
        StringBuilder sb = new StringBuilder();

        sb.append(source.readLinesToString(BigInteger.valueOf(0),BigInteger.valueOf(-1), false));

        try (FileOutputStream fos = new FileOutputStream(stringPath)) {
            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isProcessing() {
        return processing;
    }

    public void setProcessing(boolean processing) {
        LogFileController.processing = processing;
    }

    public void clear() {
        // Clear the logFile object of all logs
        logFiles.clear();
        clearMemoryFiles();
    }

    public void clearMemoryFiles() {
        if (memoryFiles != null) {
            memoryFiles.clear();
        }
    }
}

