package com.example.logparser.javafx.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import com.example.logparser.modules.Utilities;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class TabMetadata {

    private static final Map<Integer,TMetadata> metadataList = new HashMap<>();

    public TabMetadata() {}

    public int getMetadataListSize() {
        return metadataList.size();
    }

    public int addTabMetadata() {
        int id = metadataList.size();

        metadataList.put(id,new TMetadata(id));

        return id;
    }

    public BigInteger getTabPageStart(int id) {
        return metadataList.get(id).getPageStart();
    }

    public BigInteger getTabPageEnd(int id) { return metadataList.get(id).getPageEnd(); }

    public void setTabPageStart(int id, BigInteger start) {
        metadataList.get(id).setPageStart(start);
    }

    public void setTabPageEnd(int id, BigInteger end) {
        metadataList.get(id).setPageEnd(end);
    }
    public boolean setTabCurrentPage(int id, int currentPage) {
        return metadataList.get(id).setCurrentPage(currentPage);
    }

    public IntegerProperty getTabCurrentPage(int id) {
        TMetadata m = metadataList.get(id);

        if (!Utilities.isNullOrEmpty(m)) {
            return m.getCurrentPage();
        }

        return new SimpleIntegerProperty(0);
    }

    public int getTabPagesAvailable(int id) {
        return metadataList.get(id).getAvailablePages();
    }

    public void calculateTabPagesAvailable(int id, BigInteger recordAmount) {
        metadataList.get(id).calculatePagesAvailable(recordAmount);
    }
    public void setCurrentPageContainingLine(int id, Integer lineNumber) {
        metadataList.get(id).setCurrentPageContainingLine(lineNumber);
    }

    public boolean tabMetadataExists (int id) {
        return metadataList.containsKey(id);
    }

    public int getPageOfLine(int id, Long lineNumber) { return metadataList.get(id).getPageOfLine(lineNumber); }

    public void clear() {
        metadataList.clear();
    }
}

class TMetadata {
    private final int PAGE_INCREMENT = 250;
    private final int id;
    private BigInteger pageStart;
    private BigInteger pageEnd;
    private final IntegerProperty currentPage = new SimpleIntegerProperty();
    private int availablePages;

    public TMetadata(int id) {
        this.id = id;
        this.availablePages = 1;
        setCurrentPage(1);
    }

    public int getId() {
        return id;
    }

    public BigInteger getPageStart() {
        return pageStart;
    }

    public BigInteger getPageEnd() {
        return pageEnd;
    }

    public void setPageStart(BigInteger pageStart) {
        this.pageStart = pageStart;
    }

    public void setPageEnd(BigInteger pageEnd) {
        this.pageEnd = pageEnd;
    }

    public IntegerProperty getCurrentPage() {
        return currentPage;
    }

    public boolean setCurrentPage(int newPage) {
        if (newPage <= 0 || newPage == this.currentPage.getValue()) {
            return false;
        }

        newPage = Math.min(newPage, availablePages);

        /*if ((currentPage.get() - newPage == 1 || currentPage.get() - newPage == -1) && pageStart != 0) {
            pageStart = pageEnd;
        } else*/ {
            pageStart = BigInteger.valueOf((long) (newPage - 1) * PAGE_INCREMENT + 1);
        }
        pageEnd = pageStart.add(BigInteger.valueOf(PAGE_INCREMENT));

        currentPage.set(newPage);

        return true;
    }

    public void setCurrentPageContainingLine(Integer lineNumber) {
        setCurrentPage((int) Math.ceil(Double.parseDouble(lineNumber.toString()) / PAGE_INCREMENT));
    }

    public int getPageOfLine(Long lineNumber) { return (int) Math.ceil(Double.parseDouble(lineNumber.toString()) / PAGE_INCREMENT); }

    public int getAvailablePages() {
        return availablePages;
    }

    public void calculatePagesAvailable(BigInteger recordCount) {
        if (recordCount.compareTo(BigInteger.ZERO) > 0) {
            BigDecimal r = new BigDecimal(recordCount);

            double d = r.divide(BigDecimal.valueOf(PAGE_INCREMENT)).doubleValue();
            this.availablePages = (int) Math.ceil(d);
        }

        if (availablePages < currentPage.getValue()) {
            currentPage.set(availablePages);
        }
    }
}
