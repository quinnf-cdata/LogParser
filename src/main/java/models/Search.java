package models;

import java.util.ArrayList;
import java.util.List;

public class Search {
    static List<Integer> searchResults;
    static int cursor;

    public Search() {
        this.searchResults = new ArrayList<>();
    }

    public List<Integer> getSearchResults() {
        return searchResults;
    }

    public void addSearchResult(Integer lineNumber) {
        this.searchResults.add(lineNumber);
    }
    public Integer getNext() {
        Integer r = searchResults.get(cursor);
        cursor++;
        return r;
    }

    public void clear() {
        searchResults.clear();
    }
}
