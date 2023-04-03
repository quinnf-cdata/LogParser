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
        this.keyword = "";
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
        if (!this.keyword.equalsIgnoreCase(keyword)) {
            search.clear();
            this.keyword = keyword;
        }
    }

    public void execSearch(HashMap<Integer,String> data, String keyword) {
        setKeyword(keyword);
        execSearch(data);
    }

    public void execSearch(HashMap<Integer,String> data) {
        keyword = keyword.toUpperCase();
        for (Map.Entry<Integer,String> m:data.entrySet()) {
            if (m.getValue().toUpperCase().contains(keyword)) {
                search.addSearchResult(m.getKey());
            }
        }
    }
    public List<Integer> getResultList() { return search.getSearchResults(); }


    public Integer getNextResult() { return search.getNext(); }
}
