package poliv.jr.com.syncapplication.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import poliv.jr.com.syncapplication.utility.Utility;

import static org.junit.jupiter.api.Assertions.*;

class FileManagerTest {

    FileManager fileManager;

    @BeforeEach
    void setUp() {
        fileManager = FileManager.getInstance(Utility.getFolderPath());
    }

    @Test
    void getItemsList() {
        int expected = 7;
        int actual = fileManager.getItemsList().size();

        assertEquals(expected, actual);
    }
}