package tests;

import com.example.logparser.modules.Constants;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class ConstantsTest {
    @Test
    void testEnumsToArrayLite() {
        String[] consts = Constants.LOG_CATEGORIES.getStringValuesLite();
        System.out.print(Arrays.toString(consts));
    }

    @Test
    void testToString() {
        String s = Constants.LOG_CATEGORIES.QID.toString();
        System.out.print(s);
    }

    @Test
    void testToName() {
        String s = Constants.LOG_CATEGORIES.QID.name();
        System.out.print(s);
    }
}
