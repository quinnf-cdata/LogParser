package tests;

import models.TabMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TabMetadataTest {

    @Test
    void tabMetadata() {
        TabMetadata t = new TabMetadata();
        assertNotEquals(null,t);
    }

    @Test
    void getMetadataListSize() {
        TabMetadata t = new TabMetadata();

        assertEquals(0,t.getMetadataListSize());
    }

    @Test
    void addTabMetadata() {
        TabMetadata t = new TabMetadata();

        assertEquals(0, t.getMetadataListSize());

        t.addTabMetadata(101);
        assertEquals(1, t.getMetadataListSize());
    }

    @Test
    void getTabPageStart() {
        TabMetadata t = new TabMetadata();

        t.addTabMetadata(1);
        assertEquals(1,t.getTabPageStart(1));
    }

    @Test
    void getTabPageEnd() {
        TabMetadata t = new TabMetadata();

        t.addTabMetadata(1);
        assertEquals(250,t.getTabPageEnd(1));
    }

    @Test
    void setTabCurrentPage() {
        TabMetadata t = new TabMetadata();

        t.addTabMetadata(1);
        assertEquals(1,t.getTabPageStart(1));

        t.calculateTabPagesAvailable(1,1000);
        t.setTabCurrentPage(1,5);

        t.addTabMetadata(2);
        t.calculateTabPagesAvailable(2,10000);
        t.setTabCurrentPage(2,5);


        assertEquals(1000,t.getTabPageEnd(1));
        assertEquals(1250,t.getTabPageEnd(2));
    }

    @Test
    void getTabCurrentPage() {
        TabMetadata t = new TabMetadata();

        t.addTabMetadata(1);
        assertEquals(1,t.getTabCurrentPage(1));
    }

    @Test
    void getTabPagesAvailable() {
        TabMetadata t = new TabMetadata();

        t.addTabMetadata(1);
        t.calculateTabPagesAvailable(1,1005);

        assertEquals(5,t.getTabPagesAvailable(1));
    }

    @Test
    void calculateTabPagesAvailable() {
        TabMetadata t = new TabMetadata();

        t.addTabMetadata(1);
        t.calculateTabPagesAvailable(1,156452);

        assertEquals(626,t.getTabPagesAvailable(1));
    }

    @Test
    void setCurrentPageContainingLine() {
        TabMetadata t = new TabMetadata();

        t.addTabMetadata(1);
        t.calculateTabPagesAvailable(1,156452);

        t.setCurrentPageContainingLine(1,53251);

        assertEquals(214,t.getTabCurrentPage(1));
    }

    @Test
    void getPageOfLine() {
        TabMetadata t = new TabMetadata();

        t.addTabMetadata(1);
        t.calculateTabPagesAvailable(1,156249);

        assertEquals(214,t.getPageOfLine(1,53251));
    }

    @Test
    void testClear() {
        TabMetadata t = new TabMetadata();

        t.addTabMetadata(1);
        assertEquals(1,t.getMetadataListSize());

        t.clear();
        assertEquals(0,t.getMetadataListSize());
    }
}