package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabMetadata {

    private static Map<Integer,TMetadata> metadataList;

    public TabMetadata() {
        metadataList = new HashMap<>();
    }

    public int getMetadataListSize() {
        return metadataList.size();
    }

    public void addTabMetadata(int id) {
        metadataList.put(id,new TMetadata(id));
    }

    public int getTabPageStart(int id) {
        return metadataList.get(id).getPageStart();
    }

    public int getTabPageEnd(int id) {
        return metadataList.get(id).getPageEnd();
    }

    public void setTabCurrentPage(int id, int currentPage) {
        metadataList.get(id).setCurrentPage(currentPage);
    }

    public int getTabCurrentPage(int id) {
        return metadataList.get(id).getCurrentPage();
    }

    public int getTabPagesAvailable(int id) {
        return metadataList.get(id).getAvailablePages();
    }

    public void calculateTabPagesAvailable(int id, int recordAmount) {
        metadataList.get(id).calculatePagesAvailable(recordAmount);
    }
    public void setCurrentPageContainingLine(int id, Integer lineNumber) {
        metadataList.get(id).setCurrentPageContainingLine(lineNumber);
    }

    public int getPageOfLine(int id, Integer lineNumber) { return metadataList.get(id).getPageOfLine(lineNumber); }

    public void clear() {
        metadataList.clear();
    }
}

class TMetadata {
    private final int PAGE_INCREMENT = 250;
    private int id;
    private int pageStart;
    private int pageEnd;
    private int currentPage;
    private int availablePages;

    public TMetadata(int id) {
        this.id = id;
        this.availablePages = 1;
        setCurrentPage(1);
    }

    public int getId() {
        return id;
    }

    public int getPageStart() {
        return pageStart;
    }

    public int getPageEnd() {
        return pageEnd;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        if (currentPage > availablePages) {
            currentPage = availablePages;
        }

        if (currentPage > 0) {
            this.pageStart = (currentPage-1) * PAGE_INCREMENT+1;
            this.pageEnd = pageStart - 1 + PAGE_INCREMENT;
            this.currentPage = currentPage;
        }
    }

    public void setCurrentPageContainingLine(Integer lineNumber) {
        currentPage = (int) Math.ceil(Double.parseDouble(lineNumber.toString()) / PAGE_INCREMENT);
    }

    public int getPageOfLine(Integer lineNumber) { return (int) Math.ceil(Double.parseDouble(lineNumber.toString()) / PAGE_INCREMENT); }

    public int getAvailablePages() {
        return availablePages;
    }

    public void calculatePagesAvailable(int recordSize) {
        if (recordSize > 0) {
            this.availablePages = (int) Math.ceil(recordSize / (double) PAGE_INCREMENT);
        }
    }
}
