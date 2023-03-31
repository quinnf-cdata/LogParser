package com.example.tests;

import models.LogFile;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
public class LogFileTest {
    @Test
    void logFileNoParams() {
        LogFile l = new LogFile();

        assertEquals(0,l.getLogData().size());
        assertEquals(0,l.getFileIndex().size());

    }

    @Test
    void logFileFullParams() {
        HashMap<Integer,String> d = new HashMap<>();
        d.put(10,"testing");

        LogFile l = new LogFile("/usr/local/file.txt","Friendly Name",d);

        assertEquals("/usr/local/file.txt",l.getLocalPath());
        assertEquals("Friendly Name",l.getFriendlyName());
        assertEquals(1,l.getLogData().size());

        l = new LogFile("/home/root/file.txt","Name Friendly",null);
        assertEquals("/home/root/file.txt",l.getLocalPath());
        assertEquals("Name Friendly",l.getFriendlyName());
        assertEquals(0,l.getLogData().size());
    }

    @Test
    void setLogData() {
        LogFile l = new LogFile();

        for (int i = 0; i<100;i++) {
            l.setLogData("Potato"+(i*100));
        }
        assertEquals(100,l.getLogData().size());

        l.setLogData("Onion",15);
        assertEquals(101,l.getLogData().size()+1);
    }

    @Test
    void getLogData() {
        LogFile l = new LogFile();

        l.setLogData("Potato");
        assertEquals(1,l.getLogData().size());
    }

    @Test
    void setGetLocalPath() {
        LogFile l = new LogFile();

        l.setLocalPath("/usr/root/file.txt");
        assertEquals("/usr/root/file.txt",l.getLocalPath());
    }

    @Test
    void setGetFriendlyName() {
        LogFile l = new LogFile();

        l.setFriendlyName("my friendly name");
        assertEquals("my friendly name",l.getFriendlyName());
    }

    @Test
    void getContentsInRange() {
        LogFile l = new LogFile();

        for (int i = 1; i < 100; i++) {
            l.setLogData("Test data "+(i));
        }

        assertEquals("15\tTest data 15\n" +
                "16\tTest data 16\n",l.getContentsInRange(15,16));
    }

    @Test
    void replaceData() {
        LogFile l = new LogFile();

        for (int i = 1; i < 100; i++) {
            l.setLogData("Test data "+(i));
        }

        assertEquals("Test data 52",l.getLogData(52));

        l.replaceData(52,"Carrot");
        assertEquals("Carrot",l.getLogData(52));
    }

    @Test
    void setGetIsLoading() {
        LogFile l = new LogFile();

        l.setLoading(true);
        assertEquals(true,l.isLoading());

        l.setLoading(false);
        assertEquals(false,l.isLoading());
    }
}
