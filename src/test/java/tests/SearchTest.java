package tests;

import models.Search;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchTest {
    @Test
    void search() {
        Search s = new Search();
        assertNotEquals(null,s);
    }

    @Test
    void getSearchResults() {
        Search s = new Search();
        s.addSearchResult(10);
        s.addSearchResult(120);

        assertEquals(10,s.getSearchResults().get(0));
        assertEquals(120,s.getSearchResults().get(1));
    }

    @Test
    void addSearchResult() {
        Search s = new Search();
        s.addSearchResult(10);
        s.addSearchResult(120);

        assertEquals(2,s.getSearchResults().size());
    }

    @Test
    void getNext() {
        Search s = new Search();
        s.addSearchResult(10);
        s.addSearchResult(120);

        assertEquals(10,s.getNext());
        assertEquals(120,s.getNext());
    }
}