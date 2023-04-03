package tests;

import controllers.SearchController;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class SearchControllerTest {
    @Test
    void searchController() {
        SearchController s = new SearchController();
        assertNotEquals(null,s);
    }

    @Test
    void getKeyword() {
        SearchController s = new SearchController();

        s.setKeyword("Carrot");
        assertEquals("Carrot",s.getKeyword());
    }

    @Test
    void setKeyword() {
        SearchController s = new SearchController();

        s.setKeyword("Carrot");
        assertEquals("Carrot",s.getKeyword());
    }

    @Test
    void execSearch() {
        SearchController s = new SearchController();

        assertEquals(0,s.getResultList().size());

        HashMap<Integer,String> d = new HashMap<>();
        d.put(1,"Hello world");
        d.put(2,"This is a line of text");
        d.put(3,"This is to test the search features");

        s.execSearch(d,"line");
        assertEquals(1,s.getResultList().size());

        s.setKeyword("this is");
        s.execSearch(d);
        assertEquals(2,s.getResultList().size());
    }

    @Test
    void getResultList() {
        SearchController s = new SearchController();

        assertEquals(0,s.getResultList().size());

        HashMap<Integer,String> d = new HashMap<>();
        d.put(1,"Hello world");
        d.put(2,"This is a line of text");
        d.put(3,"This is to test the search features");

        s.execSearch(d,"hello");
        assertEquals(1,s.getResultList().get(0));

        s.setKeyword("this is");
        s.execSearch(d);
        assertEquals(2,s.getResultList().get(0));
        assertEquals(3,s.getResultList().get(1));
    }

    @Test
    void getNextResult() {
        SearchController s = new SearchController();

        HashMap<Integer,String> d = new HashMap<>();
        d.put(1,"Hello world");
        d.put(2,"This is a line of text");
        d.put(3,"This is to test the search features");

        s.execSearch(d,"this is");
        assertEquals(2,s.getNextResult());
        assertEquals(3,s.getNextResult());
    }
}