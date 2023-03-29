package models;

import java.util.ArrayList;
import java.util.List;

public class TabMetadata {
    private static List<TMetadata> metadataList;

    public TabMetadata() {
        metadataList = new ArrayList<>();
    }

    public List<TMetadata> getMetadataList() {
        return metadataList;
    }

    public void setMetadataList(List<TMetadata> metadataList) {
        TabMetadata.metadataList = metadataList;
    }

    public void addTabMetadata(int id) {
        metadataList.add(new TMetadata(id));
    }

    public boolean tabMetadataExists(int id) {
        return metadataList.get(id) != null;
    }

    public int getTabPageStart(int id) {
        return metadataList.get(id).getPageStart();
    }

    public int getTabPageEnd(int id) {
        return metadataList.get(id).getPageEnd();
    }

    public void setTabPageStart(int id, int pageStart) {
        metadataList.get(id).setPageStart(pageStart);
    }

    public void setTabPageEnd(int id, int pageEnd) {
        metadataList.get(id).setPageEnd(pageEnd);
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

    public void setTabPagesAvailable(int id, int availablePages) {
        metadataList.get(id).setAvailablePages(availablePages);
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

    public void setPageStart(int pageStart) {
        this.pageStart = pageStart;
    }

    public int getPageEnd() {
        return pageEnd;
    }

    public void setPageEnd(int pageEnd) {
        this.pageEnd = pageEnd;
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

    public void setAvailablePages(int availablePages) {
        this.availablePages = availablePages;
    }

    public void calculatePagesAvailable(int recordSize) {
        if (recordSize > 0) {
            this.availablePages = (int) Math.ceil(recordSize / (double) PAGE_INCREMENT);
        }
    }
}
