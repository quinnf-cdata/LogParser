package controllers;

import models.FileIndex;
import models.Search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchController {
    private String keyword;
    private Search search;
    private int currentResultLocation;

    public SearchController() {
        search = new Search();
    }

    public SearchController(String keyword) {
        this.keyword = keyword;
        search = new Search();
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public void execSearch(HashMap<Integer,String> data, String keyword) {
        setKeyword(keyword);
        execSearch(data);
    }

    public void execSearch(HashMap<Integer,String> data) {
        keyword = keyword.toUpperCase();
        for (Map.Entry m:data.entrySet()) {
            if (m.getValue().toString().toUpperCase().contains(keyword)) {
                search.addSearchResult((Integer) m.getKey());
                continue;
            }
        }
    }
    public List<Integer> getResultList() { return search.getSearchResults(); }

    public Integer getResultIndex(int index) {
        return search.getSearchResults().get(index);
    }

    public Integer getNextResult() { return search.getNext(); }
}
