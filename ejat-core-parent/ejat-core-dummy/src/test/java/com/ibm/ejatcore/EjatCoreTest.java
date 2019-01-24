package ejatcore;

import org.junit.*;
import static org.junit.Assert.*;

import java.beans.Transient;

public class EjatCoreTest{
    
    @Test
    public void test_method() {
        assertNotNull(EjatCore.dummyMethod());
    }

    @Test
    public void test_method_2() {
        assertTrue(true);
    }
}