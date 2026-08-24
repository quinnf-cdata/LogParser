package com.example.logparser.modules;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class LogHelper {
    private static LocalDateTime timeStart;
    private static int filesToProcess = 0;
    private static int processingFile = 0;
    private static BigInteger totalMaxRecords = BigInteger.valueOf(0);
    private static BigInteger currentRecord = BigInteger.valueOf(0);
    private static String mark = "";
    private static String status = "";
    private static boolean exactWordSearch = false;
    private final static Set<String> filters = new HashSet<>();

    public static String getMark() {
        return mark;
    }

    public static void setMark(String mark) {
        LogHelper.mark = mark;
    }

    public static String getStatus() {
        return status;
    }

    public static void setStatus(String status) {
        LogHelper.status = status;
    }
    public static void addFilter(String filter) {
        filters.add(filter);
    }

    public static void removeFilter(String filter) {
        filters.remove(filter);
    }

    public static void clearFilters() {
        filters.clear();
    }

    public static List<String> getFilters() {
        return new ArrayList<>(filters);
    }

    public static BigInteger getTotalMaxRecords() {
        return totalMaxRecords;
    }

    public static void setTotalMaxRecords(BigInteger totalMaxRecords) {
        LogHelper.totalMaxRecords = totalMaxRecords;
    }

    public static void addTotalMaxRecords(BigInteger totalMaxRecords) {
        LogHelper.totalMaxRecords.add(totalMaxRecords);
    }

    public static BigInteger getCurrentRecord() {
        return currentRecord;
    }

    public static void setCurrentRecord(BigInteger currentRecord) {
        LogHelper.currentRecord = currentRecord;
    }

    public static void addCurrentRecord(BigInteger currentRecord) {
        LogHelper.currentRecord = LogHelper.currentRecord.add(currentRecord);
    }

    public static void smartAddCurrentRecord(BigInteger currentRecord) {
        BigInteger n = currentRecord.subtract(LogHelper.currentRecord);
        if (n.compareTo(BigInteger.valueOf(0)) > 0) {
            LogHelper.currentRecord = n;

        }
    }

    public static double getProgressPercentage() {
        double value = currentRecord.doubleValue() / totalMaxRecords.doubleValue();
        value = value * 100;

        return value;
    }

    public static int getFilesToProcess() {
        return filesToProcess;
    }

    public static void setFilesToProcess(int filesToProcess) {
        LogHelper.filesToProcess = filesToProcess;
    }

    public static int getProcessingFile() {
        return processingFile;
    }

    public static void setProcessingFile(int processingFile) {
        LogHelper.processingFile = processingFile;
    }

    public static void setTimeStartNow() {
        timeStart = LocalDateTime.now();
    }

    public static String getElapsedTime() {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(timeStart, now);

        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static boolean isExactWordSearch() {
        return exactWordSearch;
    }

    public static void setExactWordSearch(boolean exactWordSearch) {
        LogHelper.exactWordSearch = exactWordSearch;
    }
}
