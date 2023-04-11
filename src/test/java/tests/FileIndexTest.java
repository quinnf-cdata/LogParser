package tests;

import models.Category;
import models.FileIndex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileIndexTest {

    @Test
    void addToIndex() {
        FileIndex f = new FileIndex();
        f.addToIndex(1, "2023-02-24T14:53:12.488-0500\t2\t[Connection: 2]Connected (0 - OK)");
        f.addToIndex(123, "2023-02-24T14:53:12.488-0500\t2\t[Connection: 3]Connected (0 - OK)");
        f.addToIndex(3, "2023-02-28T15:19:35.960+0000\t5\t[ |Q-Id]\t[META|Schema] Engine Invalid object name 'sys_disconnect'");
        f.addToIndex(15, "2023-02-28T15:19:35.938+0000\t2\t[2|Q-Id]\t[HTTP|Res: 4997] Request completed in 502 ms.");

        assertEquals(2,f.searchIndexByCategory(FileIndex.Category.CONNECTION).size());
        assertEquals(1,f.searchIndexByCategory(FileIndex.Category.HTTP_RESPONSE).size());
        assertEquals(0,f.searchIndexByCategory(FileIndex.Category.HTTP_REQUEST).size());
    }

    @Test
    void addToClassificationIndex() {
        FileIndex f = new FileIndex();
        f.addToClassificationIndex(0, FileIndex.Category.CONNECTION);
        f.addToClassificationIndex(3, FileIndex.Category.CONNECTION);
        f.addToClassificationIndex(0, FileIndex.Category.HTTP_REQUEST);

        assertEquals(2,f.getCategoryByLineNumber(0).size());
        assertEquals(1,f.getCategoryByLineNumber(3).size());
    }

    @Test
    void searchClassificationIndex() {
        FileIndex f = new FileIndex();
        f.addToClassificationIndex(0, FileIndex.Category.CONNECTION);
        f.addToClassificationIndex(3, FileIndex.Category.CONNECTION);
        f.addToClassificationIndex(0, FileIndex.Category.HTTP_REQUEST);

        assertEquals(2, f.searchClassificationIndex(FileIndex.Category.CONNECTION).size());
        assertEquals(1, f.searchClassificationIndex(FileIndex.Category.HTTP_REQUEST).size());
        assertEquals(0, f.searchClassificationIndex(FileIndex.Category.HTTP_RESPONSE).size());
    }

    @Test
    void searchIndexByClassification() {
        FileIndex f = new FileIndex();
        f.addToIndex(1, "2023-02-24T14:53:12.488-0500\t2\t[Connection: 2]Connected (0 - OK)");
        f.addToIndex(123, "2023-02-24T14:53:12.488-0500\t2\t[Connection: 3]Connected (0 - OK)");
        f.addToIndex(3, "2023-02-28T15:19:35.960+0000\t5\t[ |Q-Id]\t[META|Schema] Engine Invalid object name 'sys_disconnect'");
        f.addToIndex(15, "2023-02-28T15:19:35.938+0000\t2\t[2|Q-Id]\t[HTTP|Res: 4997] Request completed in 502 ms.");
        f.addToIndex(200, "2023-03-29T10:43:12.535-0400\t1\t[2|Q-Id]\t[INFO|Connec] Closed Xero connection");

        assertEquals(2,f.searchIndexByCategory(FileIndex.Category.CONNECTION).size());
        assertEquals(1,f.searchIndexByCategory(FileIndex.Category.HTTP_RESPONSE).size());
        assertEquals(0,f.searchIndexByCategory(FileIndex.Category.HTTP_REQUEST).size());
        assertEquals(1,f.searchIndexByCategory(FileIndex.Category.CONNECTION_MESSAGE).size());
    }

    @Test
    void searchIndex() {
        FileIndex f = new FileIndex();
        f.addToIndex(1, "2023-02-24T14:53:12.488-0500\t2\t[Connection: 2]Connected (0 - OK)");
        f.addToIndex(123, "2023-02-24T14:53:12.488-0500\t2\t[Connection: 3]Connected (0 - OK)");
        f.addToIndex(3, "2023-02-28T15:19:35.960+0000\t5\t[ |Q-Id]\t[META|Schema] Engine Invalid object name 'sys_disconnect'");
        f.addToIndex(15, "2023-02-28T15:19:35.938+0000\t2\t[2|Q-Id]\t[HTTP|Res: 4997] Request completed in 502 ms.");
        f.addToIndex(200, "2023-03-29T10:43:12.535-0400\t1\t[2|Q-Id]\t[INFO|Connec] Closed Xero connection");

        assertEquals(3,f.searchIndex("connection").size());
        assertEquals(1,f.searchIndex("RES: 4997").size());
        assertEquals(0,f.searchIndex("Request completed").size());
        assertEquals(1,f.searchIndex("Closed").size());
    }

    @Test
    void testToStringGroup() {
        FileIndex f = new FileIndex();

        f.addToIndex(1, "2023-02-24T14:53:12.488-0500\t2\t[Connection: 2]Connected (0 - OK)");
        f.addToIndex(123, "2023-02-24T14:53:12.488-0500\t2\t[Connection: 3]Connected (0 - OK)");
        f.addToIndex(3, "2023-02-28T15:19:35.960+0000\t5\t[ |Q-Id]\t[META|Schema] Engine Invalid object name 'sys_disconnect'");
        f.addToIndex(15, "2023-02-28T15:19:35.938+0000\t2\t[2|Q-Id]\t[HTTP|Res: 4997] Request completed in 502 ms.");
        f.addToIndex(200, "2023-03-29T10:43:12.535-0400\t1\t[2|Q-Id]\t[INFO|Connec] Closed Xero connection");
        f.addToIndex(100, "2023-03-29T10:43:12.535-0400\t1\t[2|Q-Id]\t[INFO|Connec] Opened Xero connection");

        assertEquals("1\t[CONNECTION: 2]\n" +
                "123\t[CONNECTION: 3]\n",f.toString(3));
        assertEquals("15\t[HTTP|RES: 4997]\n",f.toString(2));
        assertEquals("100\t[INFO|CONNEC] OPENED XERO CONNECTION\n" +
                "200\t[INFO|CONNEC] CLOSED XERO CONNECTION\n",f.toString(1));
    }

    @Test
    void testToStringClassification() {
        FileIndex f = new FileIndex();

        f.addToIndex(1, "2023-02-24T14:53:12.488-0500\t2\t[Connection: 2]Connected (0 - OK)");
        f.addToIndex(123, "2023-02-24T14:53:12.488-0500\t2\t[Connection: 3]Connected (0 - OK)");
        f.addToIndex(3, "2023-02-28T15:19:35.960+0000\t5\t[ |Q-Id]\t[META|Schema] Engine Invalid object name 'sys_disconnect'");
        f.addToIndex(15, "2023-02-28T15:19:35.938+0000\t2\t[2|Q-Id]\t[HTTP|Res: 4997] Request completed in 502 ms.");
        f.addToIndex(200, "2023-03-29T10:43:12.535-0400\t1\t[2|Q-Id]\t[INFO|Connec] Closed Xero connection");
        f.addToIndex(100, "2023-03-29T10:43:12.535-0400\t1\t[2|Q-Id]\t[INFO|Connec] Opened Xero connection");

        assertEquals("1\t[CONNECTION: 2]\n" +
                "123\t[CONNECTION: 3]\n",f.toString(FileIndex.Category.CONNECTION));
        assertEquals("15\t[HTTP|RES: 4997]\n",f.toString("HTTP_RESPONSE"));
        assertEquals("100\t[INFO|CONNEC] OPENED XERO CONNECTION\n" +
                "200\t[INFO|CONNEC] CLOSED XERO CONNECTION\n",f.toString("CONNECTION_MESSAGE"));
    }

    @Test
    void getClassificationIndex() {
        FileIndex f = new FileIndex();

        assertEquals(0,f.getCategoryIndex().size());
    }

    @Test
    void testClear() {
        FileIndex f = new FileIndex();

        f.addToIndex(123, "2023-02-24T14:53:12.488-0500\t2\t[Connection: 3]Connected (0 - OK)");

        assertEquals(1, f.getCategoryIndex().size());
        assertEquals(1, f.getFileIndex().size());

        f.clear();

        assertEquals(0, f.getCategoryIndex().size());
        assertEquals(0, f.getFileIndex().size());
    }

    @Test
    void toClassification() {
        FileIndex f = new FileIndex();

        assertEquals("CONNECTION",f.toClassification("Connection"));
        assertEquals(null,f.toClassification("Potato"));
    }
}