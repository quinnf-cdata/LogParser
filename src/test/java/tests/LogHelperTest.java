package tests;

import com.example.logparser.modules.LogHelper;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogHelperTest {
    @Test
    void getProgress() {
        LogHelper.setTotalMaxRecords(BigInteger.valueOf(Long.parseLong("40958066688")));
        LogHelper.setCurrentRecord(BigInteger.valueOf(Long.parseLong("4158508")));
        assertEquals(0.010153086647564764,LogHelper.getProgressPercentage());
    }
}
