package com.example.logparser.models;

import com.example.logparser.javafx.modules.Utils;
import com.example.logparser.modules.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class LogFile {
    private StandardAnalyzer analyzer;
    private Directory index;
    private Path localPath;
    private boolean loading = false;
    private int maxLines = 100;
    private boolean allowIndex = false;
    private boolean isMaster = false;
    private boolean isSorted = false;
    private boolean decodeHex = false;
    private boolean filterNonASCII = true;
    private boolean memoryFile = false;
    private int metadataID = -1;
    private int linesHidden = 0;
    private String timePattern;
    private String parentFile;

    public LogFile(String file) {
        init(Paths.get(file));
    }
    public LogFile(Path file) {
        init(file);
    }
    public LogFile(String file, Boolean allowIndex) {
        init(Paths.get(file));
        setAllowIndex(allowIndex);
    }

    public LogFile(Path file, Boolean allowIndex) {
        init(file);
        setAllowIndex(allowIndex);
    }

    public LogFile() {
        init(null);
    }

    private void init(Path file) {
        analyzer = new StandardAnalyzer();
        index = new ByteBuffersDirectory();

        if (file != null) {
            localPath = file;
        }
    }

    public void setMetadataID(int id) {
        this.metadataID = id;
    }

    public int getMetadataID() { return metadataID; }
    public String getFileName() {
        return localPath.getFileName().toString();
    }

    public void setAllowIndex(boolean allowIndex) {
        this.allowIndex = allowIndex;
        if (this.allowIndex) {
            if (Utilities.isNull(this.index)) {
                index = new ByteBuffersDirectory();
            }
            indexFile();
        } else {
            try {
                this.index.close();
            } catch (IOException e) {
                System.out.println("Index was not open.");
            }
            this.index = null;
        }
    }

    public void setMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    public boolean isMaster() {return isMaster;}

    /*public TreeMap<BigInteger, String> readLines(BigInteger start) {
        BigInteger end = start.add(BigInteger.valueOf(maxLines));
        return readLines(start,end);
    }*/

    public void setParentFile(String parentFile) { this.parentFile = parentFile; }
    public String getParentFile() { return parentFile; }
    public TreeMap<BigInteger, String> readLines(BigInteger start, BigInteger end) {
        return readLines(start,end,false);
    }
    public TreeMap<BigInteger, String> readLines(BigInteger start, BigInteger end, boolean returnMax) {
        TreeMap<BigInteger, String> output = new TreeMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(localPath.toString()))) {
            String line;
            BigInteger lineNumber = BigInteger.valueOf(0);
            linesHidden = 0;
            boolean skipUntilTimeStamp = false;

            while ((line = br.readLine()) != null) {
                lineNumber = lineNumber.add(BigInteger.valueOf(1));

                if (lineNumber.compareTo(start) < 0) {
                    continue;
                }

                if (lineNumber.subtract(BigInteger.valueOf(linesHidden)).compareTo(end) >= 0 && end.compareTo(BigInteger.valueOf(-1)) != 0) {
                    break;
                }

                if (skipUntilTimeStamp) {
                    if (Utilities.containsDateTime(line,timePattern)) {
                        skipUntilTimeStamp = false;
                    } else {
                        linesHidden++;
                        continue;
                    }
                }


                if (!checkFilter(line)) {
                    output.put(lineNumber, line);
                } else {
                    linesHidden++;
                    skipUntilTimeStamp = true;
                }
            }

            if (returnMax) {
                output.put(lineNumber,"");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return output;
    }


    private boolean checkFilter(String line) {
        ArrayList<String> patterns = new ArrayList<>();

        for (JSONConstant c : Objects.requireNonNull(JSONUtils.getJSONList(JSONUtils.JSON_LISTS.FILTERS))) {
            if (LogHelper.getFilters().stream().anyMatch(f -> f.equalsIgnoreCase(c.getDisplayName()))) {
                patterns.add(c.getPattern());
            }
        }

        for (String s : patterns) {
            if (line.toLowerCase().contains(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public BigInteger getMaxLineNumberFromRead(BigInteger end) {
        TreeMap<BigInteger, String> lines = readLines(end.subtract(BigInteger.valueOf(1)),end,true);
        if (Utilities.isNullOrEmpty(lines)) {
            return BigInteger.valueOf(0);
        }

        return lines.lastKey();
    }
    public String readLinesToString(BigInteger start, BigInteger end,boolean showLineNumbers) {
        StringBuilder sb = new StringBuilder();
        TreeMap<BigInteger, String> lines = readLines(start,end);

        for (BigInteger lineNumber : lines.keySet()) {
            String l = lines.get(lineNumber);
            if (showLineNumbers) {
                sb.append(lineNumber).append("\t");
            }

            if (decodeHex) {
                l = hexToAscii(l);
            }

            sb.append(l).append("\n");
        }

        return sb.toString();
    }

    public String hexToAscii(String input) {
        if (input == null || input.isBlank()) return input;

        final Pattern explicit = Pattern.compile(
                "(?i)\\b0x([0-9a-f]{2})\\b(?:\\s+(?=\\b0x[0-9a-f]{2}\\b))?"
        );

        // Plain bytes: NN, but NOT part of a decimal number like 65.48
        // (block if immediately preceded/followed by '.')
        final Pattern plain = Pattern.compile("(?i)(?<!\\.)\\b([0-9a-f]{2})\\b(?!\\.)");

        // 1) Always decode explicit 0xNN occurrences, preserving surrounding text
        String afterExplicit = replaceAllHexTokens(input, explicit);

        // 2) For plain NN tokens, only decode if there are at least 2 such tokens in the string
        //    (prevents "Numeric Value: 48" from becoming "Numeric Value: H")
        Matcher m = plain.matcher(afterExplicit);
        List<int[]> matches = new ArrayList<>();
        while (m.find()) {
            matches.add(new int[]{m.start(), m.end(), Integer.parseInt(m.group(1), 16)});
        }

        if (matches.size() < 2) {
            // Also remove the extra spaces only when decoding; otherwise keep string intact
            return afterExplicit;
        }

        // Build output by replacing each plain token with ASCII, and removing whitespace between
        // consecutive *decoded* tokens (so "48 65 ..." -> "He..." not "H e ...")
        StringBuilder out = new StringBuilder(afterExplicit.length());
        int last = 0;

        for (int i = 0; i < matches.size(); i++) {
            int start = matches.get(i)[0];
            int end = matches.get(i)[1];
            char ascii = (char) matches.get(i)[2];

            // append text before this token
            out.append(afterExplicit, last, start);
            // append decoded char
            out.append(ascii);

            last = end;

            // If next match starts after only whitespace, drop that whitespace (removes spaces between bytes)
            if (i + 1 < matches.size()) {
                int nextStart = matches.get(i + 1)[0];
                if (onlyWhitespace(afterExplicit, last, nextStart)) {
                    last = nextStart; // skip whitespace between tokens
                }
            }
        }

        // append remaining tail
        out.append(afterExplicit, last, afterExplicit.length());

        String output = out.toString().replaceAll("[\n\r]", "");

        if (filterNonASCII) {
            output = output.replaceAll("[^\\x20-\\x7E]", " ");
        }

        return output;
    }

    private String replaceAllHexTokens(String input, Pattern p) {
        Matcher m = p.matcher(input);
        StringBuffer sb = new StringBuffer();
        boolean found = false;

        while (m.find()) {
            found = true;
            int decimal = Integer.parseInt(m.group(1), 16);
            char ascii = (char) decimal;
            m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(ascii)));
        }
        m.appendTail(sb);

        return found ? sb.toString() : input;
    }

    private boolean onlyWhitespace(String s, int from, int to) {
        for (int i = from; i < to; i++) {
            if (!Character.isWhitespace(s.charAt(i))) return false;
        }
        return true;
    }

    public void indexFile() {
        Thread t = new Thread(() -> {
            try {
                if (!allowIndex) {
                    return;
                } else if (isLoading()) {
                    return;
                }

                if (index == null) {
                    index = new ByteBuffersDirectory();
                }

                setLoading(true);
                IndexWriterConfig config = new IndexWriterConfig(analyzer);

                IndexWriter w = new IndexWriter(index,config);

                try (BufferedReader br = new BufferedReader(new FileReader(localPath.toString()))) {
                    String line;
                    String tp= "";
                    int lineNumber = 0;

                    while ((line = br.readLine()) != null) {
                        LogEntry logEntry = new LogEntry(line);
                        lineNumber++;
                        addDoc(w,lineNumber,logEntry);
                        if (tp.isEmpty()) {
                            tp = logEntry.getTimePattern();
                        }
                    }

                    this.timePattern = tp;
                }
                w.close();
            } catch (IOException e) {
                System.out.println(localPath.toString());
                e.printStackTrace();
            } finally {
                loading = false;
            }
        });
        t.start();
    }

    public HashMap<String, Integer> queryIndexToHierarchy(String category, String queryStr) {
        HashMap<String, Integer> outputList = new HashMap<>();

        if (indexNotReady()) {return null;}

        try (IndexReader reader = DirectoryReader.open(index)) {
            Object[] queryObject = queryIndex(category, queryStr, null, true,reader);

            ScoreDoc[] hits = (ScoreDoc[]) queryObject[0];
            StoredFields storedFields = (StoredFields) queryObject[1];

            System.out.println("Found " + hits.length);

            HashMap<String, ConnectionDuration> connectionDurations = new HashMap<>();

            for (ScoreDoc hit : hits) {
                StringBuilder sb = new StringBuilder();

                Document d = storedFields.document(hit.doc);

                String parent = d.get(Constants.LOG_CATEGORIES.QID.name());
                String child = d.get(Constants.LOG_CATEGORIES.HTTP.name());

                if (parent.isEmpty()) {
                    parent = d.get(Constants.LOG_CATEGORIES.CONNECTION.name());
                    if (parent.isEmpty()) {
                        // TODO: Connection handling
                        continue;
                    }
                }

                sb.append(parent);
                String lineNumber = d.get(Constants.LOG_CATEGORIES.LINE.name());
                String content = d.get(Constants.LOG_CATEGORIES.CONTENT.name());

                if (!child.isEmpty()) {
                    // This is for HTTP specific
                    String temp;

                    sb.append("/");
                    sb.append(child);

                    if (content.contains("HTTP|Req: ")) {
                        temp = String.format("%-10s %s", lineNumber, "Request");
                        sb.append("/").append(temp);
                    } else if (content.contains("HTTP|Res: ")) {
                        temp = String.format("%-10s %s", lineNumber, "Response");
                        sb.append("/").append(temp);
                    }
//                } else if (!(child = d.get(Constants.LOG_CATEGORIES.CONNECTION.name())).equals("")) {
                    // TODO: Do something here... not sure what yet.
                } else {
                    child = parseConnectionInfo(content, lineNumber);
                    sb.append("/");
                    sb.append(child);
                }

                if (child.contains("\n")) {
                    String[] splitString = child.split("\n");
                    for (String s : splitString) {
                        outputList.put((parent + "/" + s),Integer.parseInt(lineNumber));
                    }
                } else {
                    outputList.put(sb.toString(),Integer.parseInt(lineNumber));
                }


                String timeString = d.get(Constants.LOG_CATEGORIES.TIMESTAMP.name());

                if (!Utilities.isNullOrEmpty(timeString) && !Utilities.isNullOrEmpty(this.timePattern)) {
                    ConnectionDuration duration = this.getDurationObject(connectionDurations,parent,timeString);
                    connectionDurations.put(parent, duration);

                    // Add special indexes here or else they will get duplicated
                    if (!child.isEmpty()
                            && !child.contains(Constants.DOUBLE_CLICK_IGNORE.DRIVER_INFO.getName())
                            && !child.contains(Constants.DOUBLE_CLICK_IGNORE.CONNECTION_STRING.getName())
                            && !child.contains(Constants.DOUBLE_CLICK_IGNORE.QUERY_INFO.getName())
                            && !child.contains(Constants.DOUBLE_CLICK_IGNORE.ERRORS.getName())
                            && !child.contains(Constants.DOUBLE_CLICK_IGNORE.JVM_HEAP.getName())) {
                        String key = parent+"/"+child;
                        duration = this.getDurationObject(connectionDurations,key,timeString);
                        connectionDurations.put(key, duration);
                    }
                }

            }

            for (Map.Entry<String,ConnectionDuration> m : connectionDurations.entrySet()) {
                StringBuilder sb = new StringBuilder();
                ConnectionDuration duration = m.getValue();

                sb.append(m.getKey())
                        .append("/")
                        .append(Constants.DOUBLE_CLICK_IGNORE.LIFETIME.getName())
                        .append(": ")
                        .append(duration.getDuration())
                        .append("ms");

                outputList.put(sb.toString(),0);
            }
        } catch (DateTimeParseException e) {
            System.out.println("The time pattern is set to: " + this.timePattern);
            e.printStackTrace();
            Utils.openAlertDialog(Constants.ACTION_TYPES.ERROR.name(),"The time pattern is set to: " + this.timePattern+"\n" + e);
        }
        catch (Exception e) {
            e.printStackTrace();
            Utils.openAlertDialog(Constants.ACTION_TYPES.ERROR.name(),e.toString());
        }
        return new HashMap<>(outputList);
    }

    private ConnectionDuration getDurationObject(HashMap<String, ConnectionDuration> list, String key, String timeString) {
        ConnectionDuration duration = list.get(key);

        if (duration == null) {
            duration = new ConnectionDuration(this.timePattern,timeString);
        } else {
            duration.compareThisDate(timeString);
        }

        return duration;
    }

    private String parseConnectionInfo(String input, String lineNumber) {
        StringBuilder output = new StringBuilder();
        String categoryName = "";

        String upperInput = input.toUpperCase();
        String methodStr = input.replaceAll("META\\|Schema|INFO\\|Connec|EXEC\\|Messag|]", "")
                     .replace("/", "\\");


        if (upperInput.contains("INFO|CONNEC")) {
            if (methodStr.contains("Opened")) {
                categoryName = Constants.DOUBLE_CLICK_IGNORE.DRIVER_INFO.getName();
                methodStr = methodStr.replaceFirst("^.*?(?=Opened)", "")
                         .replaceAll("Opened |\\. ", "\n" + categoryName + "/");
                methodStr += "\n" + categoryName + "/" + Constants.DOUBLE_CLICK_IGNORE.RELEASE_DATE.getName() + ": " + getReleaseDate(methodStr);
            } else if (upperInput.contains("CONNECTION STRING")) {
                categoryName = "Connection String";
                methodStr = methodStr.toLowerCase();
                int keyWordIdx = methodStr.indexOf("connection string: ");
                methodStr = methodStr.substring(keyWordIdx,methodStr.length())
                         .replaceAll("connection string: ", "")
                         .replaceAll(";", ";\n")
                         .replaceAll("\n", "\n" + categoryName + "/");
            } else if (upperInput.contains("JVM HEAP")) {
                categoryName = "JVM Heap";
                methodStr = methodStr.replaceAll("JVM Heap","")
                        .replaceAll("->","")
                        .replaceAll("  +"," ");
                String[] metrics = methodStr.split(" ");

                StringBuilder newString = new StringBuilder();
                for (String metric : metrics) {
                    metric = metric.trim();

                    if (metric.isEmpty()) {
                        continue;
                    }

                    if (Utilities.isNumeric(metric)) {
                        newString.append(" ").append(metric);
                    } else {
                        newString.append("\n").append(categoryName).append("/").append(metric);
                    }
                }

                methodStr = newString.toString();
            }
        } else if (upperInput.contains("EXEC|MESSAG") || upperInput.contains("EXEC|PARSED")) {
            categoryName = Constants.DOUBLE_CLICK_IGNORE.QUERY_INFO.getName();
            methodStr = String.format("%-10s %s", lineNumber, methodStr);
        }

        if (upperInput.contains("ERROR") || upperInput.contains("FAIL")) {
            if (!upperInput.contains("ONERROR") && !upperInput.contains("MEMORYERROR")) {
                output.append("Errors/").append(lineNumber).append("\t").append(methodStr.trim());
            }
        }

        if (!categoryName.isEmpty()) {
            methodStr = methodStr.trim();
            if (!methodStr.startsWith(categoryName)) {
                output.append(categoryName).append("/");
            }
            output.append(methodStr);
        }

        return output.toString();
    }


    private String getReleaseDate(String driverInfoString) {
        Pattern pattern = Pattern.compile("\\.(\\d{4}+)\\.");
        Matcher matcher = pattern.matcher(driverInfoString);

        int days = 0;

        if (matcher.find()) {
            String found = matcher.group(1);
            days = Utilities.isNumeric(found) ? Integer.parseInt(found) : 0;
        }

        return Utilities.convertIntToDate(days).toString();
    }

    public int getHiddenLines() {
        return linesHidden;
    }

    public Object[] queryIndex(String category, String queryStr, boolean escapeSpecial, IndexReader reader) {
        try {
            return queryIndex(category, queryStr, null, escapeSpecial, reader   );
        } catch (ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Object[] queryIndex(String category, String querystr, ScoreDoc scoreDoc, boolean escapeSpecial, IndexReader reader) throws ParseException, IOException {
        if (indexNotReady()) {
            return null;
        }

        if (escapeSpecial) {
            querystr = escapeSpecialCharacters(querystr);
        }



        Query q = executeQuery(querystr, category);

        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = (scoreDoc == null)
                ? searcher.search(q, maxLines)
                : searcher.searchAfter(scoreDoc, q, maxLines);

        ScoreDoc[] hits = docs.scoreDocs;
        StoredFields storedFields = searcher.storedFields();

        return new Object[] {hits, storedFields};
    }

    private String escapeSpecialCharacters(String input) {
        return input.replaceAll(Constants.LUCENE_SPECIAL_CHARACTERS, "\\\\$0");
    }

    public int getDocHitCount(String category, String searchPhrase, boolean escapeSpecial) {
        // Sanitize the category
        category = category.replaceAll("[^a-zA-Z\\d]", "");

        // Handle special character escaping
        searchPhrase = escapeSpecial
                ? escapeSpecialCharacters(searchPhrase)
                : searchPhrase;

        if (indexNotReady()) {
            return -1;
        }

        try {
            Query q = executeQuery(searchPhrase, category);

            try (IndexReader reader = DirectoryReader.open(index)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                return searcher.count(q);
            }
        } catch (ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<Long> getHitLines(String category, String queryString, boolean escapeSpecial) {
        ArrayList<Long> output = new ArrayList<>();

        if (indexNotReady()) {
            return output;
        }

        try (IndexReader reader = DirectoryReader.open(index)) {
            Object[] item = queryIndex(category, queryString, escapeSpecial, reader);
            if (item == null) {
                return output;
            }

            ScoreDoc[] hits = (ScoreDoc[]) item[0];
            StoredFields storedFields = (StoredFields) item[1];

            for (ScoreDoc hit : hits) {
                Document d = storedFields.document(hit.doc);
                output.add(Long.parseLong(d.get(Constants.LOG_CATEGORIES.LINE.name())));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return output;
    }

    private Query executeQuery(String queryString, String category) throws ParseException {
        QueryParser qp;

        // Simplify string empty check and utilize proper class creation.
        qp = category.isEmpty() ?
                new MultiFieldQueryParser(Constants.LOG_CATEGORIES.getStringValuesLite(), analyzer) :
                new QueryParser(category, analyzer);

        // Simplify conditions.
        if (queryString.contains("\s")) {
            qp.setDefaultOperator(QueryParser.Operator.AND);
        }

        if (queryString.isEmpty()) {
            queryString = "*";
        }

        if (queryString.startsWith("*")) {
            qp.setAllowLeadingWildcard(true);
        }
        else if (LogHelper.isExactWordSearch()) {
            // Exact word
            if (!queryString.startsWith("\"") && !queryString.endsWith("\"")) {
                queryString = "\""+queryString+"\"";
            }
        }

        return qp.parse(queryString);
    }

    public BigInteger getLineCount() {
        if (memoryFile) {
            return BigInteger.valueOf(0);
        }

        try (Stream<String> stream = Files.lines(localPath)) {
            return BigInteger.valueOf(stream.count() - linesHidden);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public Directory getIndex() { return index; }

    private void addDoc(IndexWriter w, Integer lineNumber, LogEntry logEntry) throws IOException {
        Document doc = new Document();
        doc.add(new StringField(Constants.LOG_CATEGORIES.LINE.name(), Integer.toString(lineNumber), Field.Store.YES));
        doc.add(new TextField(Constants.LOG_CATEGORIES.TIMESTAMP.name(), logEntry.getTimestamp(),Field.Store.YES));
        doc.add(new TextField(Constants.LOG_CATEGORIES.LEVEL.name(), logEntry.getLevel(),Field.Store.YES));
        doc.add(new TextField(Constants.LOG_CATEGORIES.QID.name(), logEntry.getqId(),Field.Store.YES));
        doc.add(new TextField(Constants.LOG_CATEGORIES.HTTP.name(), logEntry.getHttp(),Field.Store.YES));
        doc.add(new TextField(Constants.LOG_CATEGORIES.META.name(), logEntry.getMeta(),Field.Store.YES));
        doc.add(new TextField(Constants.LOG_CATEGORIES.CONTENT.name(),logEntry.getContent(),Field.Store.YES));
        doc.add(new TextField(Constants.LOG_CATEGORIES.CONNECTION.name(),logEntry.getConnection(),Field.Store.YES));

        w.addDocument(doc);
    }

    public Path getLocalPath() {
        return localPath;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public int getMaxLines() {
        return maxLines;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public boolean isSorted() {
        return isSorted;
    }

    public void setSorted(boolean sorted) {
        isSorted = sorted;
    }

    public void setDecodeHex(boolean decodeHex) {
        this.decodeHex = decodeHex;
    }

    public void setFilterNonASCII(boolean filterNonASCII) {
        this.filterNonASCII = filterNonASCII;
    }
    public boolean indexNotReady() {
        // This method returns with NOT ready because all the methods that use it check for NOT ready.
        try {
            boolean ready = false;
            if (index == null) {
                ready = false;
            } else if (DirectoryReader.indexExists(index)) {
                ready = true;
            }

            if (ready) {
                ready = !isLoading();
            }

            return !ready;
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    public void setMemoryFile(boolean memoryFile) {
        this.memoryFile = memoryFile;
    }
    public boolean isMemoryFile() { return memoryFile; }

    public String getTimePattern() { return timePattern; }
    public void setTimePattern(String timePattern) { this.timePattern = timePattern; }
}

class LogEntry {
    private final String line;
    private String timestamp;
    private String timePattern = "";
    private String level;
    private String qId;
    private String http;
    private String connection;
    private String meta;
    private String content;

    public LogEntry(String line) {
        this.line = line;
        this.timestamp = "";
        this.level = "";
        this.qId = "";
        this.meta = "";
        this.content = "";
        this.http = "";
        this.connection = "";
        parseLine();
    }

    public void parseLine() {
        String[] fields = splitLine(line);

        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            String smallField = field.replaceAll("\\d","");

            if (smallField.startsWith("HTTP|Req: ]") || smallField.startsWith("HTTP|Res: ]")) {
                setHttp(field);
            }

            switch (i){
                case 0:
                    if (!setTimestamp(field)) {
                        setContent(field);
                    }
                    break;
                case 1:
                    if (Utilities.isNumeric(field)) {
                        setLevel(field);
                    } else {
                        setContent(field);
                    }
                    break;
                case 2:
                    String[] newLoggerValues = extractNewLogger(line);

                    if (newLoggerValues != null) {
                        String qid = newLoggerValues[0];
                        String conn = newLoggerValues[1];

                        if (Utilities.isNullOrEmpty(qid) && oldLoggerParse(smallField)) { qid = Utilities.extractNumericFromString(field); }
                        if (Utilities.isNullOrEmpty(conn) && oldLoggerParse(smallField)) { conn = Utilities.extractNumericFromString(field); }

                        setqId("ConnectionId: " + qid);
                        setConnection("Connection: " + conn);
                    } else if (smallField.startsWith("[|Q-Id]")) {
                        setqId(field);
                    } else if (smallField.startsWith("[Connection: ]") || smallField.startsWith("|CONN]")) {
                        setConnection(field);
                    } else if (smallField.startsWith("[HTTP|Req: ]") || smallField.startsWith("[HTTP|Res: ]")) {
                        setHttp(field);
                    } else {
                        setContent(field);
                    }
                    break;
                default:
                    setContent(field);
                    break;
            }
        }
    }

    public static boolean oldLoggerParse(String s) {
        if (s.startsWith("[|Q-Id]")) {
            return true;
        } else if (s.startsWith("[Connection: ]")) {
            return true;
        } else if (s.startsWith("[HTTP|Req: ]") || s.startsWith("[HTTP|Res: ]")) {
            return true;
        }
        return false;
    }

    public static String[] extractNewLogger(String input) {
        String regex = "\\[\\s*\\d+\\|\\s*(?:\\d+|Q-Id)\\s*\\|\\s*\\d+\\]";

        // Compile the pattern
        Pattern pattern = Pattern.compile(regex);

        // Create a matcher for the input string
        Matcher matcher = pattern.matcher(input);

        // Array to store the numbers
        String[] numbers = new String[2]; // Since we know we need two numbers

        // Find and process the match
        if (matcher.find()) {
            String extracted = matcher.group(); // Get the matched substring

            // Remove brackets and split the string to extract numbers
            extracted = extracted.replace("[", "").replace("]", ""); // Remove brackets
            String[] parts = extracted.split("\\|"); // Split by "|"

            // Extract the numeric parts
            numbers[0] = parts[0].trim(); // First number
            numbers[1] = parts[2].trim(); // Second number
        }
        return numbers;
    }

    public static String[] splitLine(String line) {
        line = line.replaceAll("\\s\\s\\s+","\t");
        Pattern p = Pattern.compile("([^\\t\\[]+)|(\\[Connection: \\d+])|(\\[\\d+\\|Q-Id])");
        Matcher m = p.matcher(line);
        List<String> result = new ArrayList<>();

        while(m.find()) {
            if (m.group(1) != null) {
                result.add(m.group(1).trim());
            } else if (m.group(2) != null) {
                result.add(m.group(2).trim());
            } else if (m.group(3) != null) {
                result.add(m.group(3).trim());
            }
        }

        return result.toArray(new String[0]);
    }
    public String getTimestamp() {
        return timestamp;
    }

    public boolean setTimestamp(String timestamp) {
        if (isValidDate(timestamp)) {
            this.timestamp = timestamp;
            return true;
        }
        return false;
    }

    public String getTimePattern() { return timePattern; }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        if (level.length() == 1 && Utilities.isNumeric(level)) {
            this.level = level;
        }
    }

    public String getqId() {
        return qId;
    }

    public void setqId(String qId) {
        this.qId = qId;
    }

    public String getHttp() {
        return http;
    }

    public void setHttp(String http) {
        String s = parseRegex(http, "HTTP\\|Re[q|s]: \\d+\\]");
        if (s.isEmpty()) {
            s = parseRegex(http, "HTTP\\|Re[q|s]: \\]");
        }

        s = s.replaceAll("\\|Re[q|s]:|]","");
        this.http = s;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        // If content is being concatenated, it's likely it needs a whitespace between two strings.
        // Whitespaces need to be carefully added.

        content = content.trim();

        if (!this.content.isEmpty()) {
            this.content += " ";
        }

        this.content += content;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }


    private boolean isValidDate(String dateString) {
        String pattern = "";

        if (dateString.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4}")) {
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        } else if (dateString.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{2}:\\d{2}")) {
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
        } else if (dateString.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d*Z")) {
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSX";
        }

        if (!pattern.isEmpty()) {
            this.timePattern = pattern;
        }

        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
                ZonedDateTime.parse(dateString, dtf);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }

    }

    private String parseRegex(String input, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group();
        }

        return "";
    }
}