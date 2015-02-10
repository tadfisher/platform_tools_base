package com.android.tests;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Ignore;
import org.junit.Test;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.os.Debug;
import android.util.ArrayMap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class UnitTest {
    @Test
    public void referenceProductionCode() {
        // Reference production code:
        Foo foo = new Foo();
        assertEquals("production code", foo.foo());
    }

    @Test
    public void mockFinalMethod() {
        Activity activity = mock(Activity.class);
        Application app = mock(Application.class);
        when(activity.getApplication()).thenReturn(app);

        assertSame(app, activity.getApplication());

        verify(activity).getApplication();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void mockFinalClass() {
        BluetoothAdapter adapter = mock(BluetoothAdapter.class);
        when(adapter.isEnabled()).thenReturn(true);

        assertTrue(adapter.isEnabled());

        verify(adapter).isEnabled();
        verifyNoMoreInteractions(adapter);
    }

    @Test
    public void exceptions() {
        try {
            ArrayMap map = new ArrayMap();
            map.isEmpty();
            fail();
        } catch (RuntimeException e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertTrue(e.getMessage().contains("isEmpty"));
            assertTrue(e.getMessage().contains("not mocked"));
            assertTrue(e.getMessage().contains("returnDefaultValues"));
        }

        try {
            Debug.getThreadAllocCount();
            fail();
        } catch (RuntimeException e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertTrue(e.getMessage().contains("getThreadAllocCount"));
            assertTrue(e.getMessage().contains("not mocked"));
            assertTrue(e.getMessage().contains("returnDefaultValues"));
        }

    }

    @Test
    public void resourcesOnClasspath() throws IOException {
        URL url = UnitTest.class.getClassLoader().getResource("resource_file.txt");
        assertNotNull("expected resource_file.txt to be in the ClassLoader's resources", url);

        InputStream stream = UnitTest.class.getClassLoader().getResourceAsStream("resource_file.txt");
        assertNotNull("expected resource_file.txt to be opened as a stream", stream);
        byte[] line = new byte[1024];
        assertTrue("Expected >0 bytes read from input stream", stream.read(line) > 0);
        String s = new String(line, "UTF-8").trim();
        assertEquals("Expected success from resource file", "success", s);
    }

    @Test
    @Ignore
    public void thisIsIgnored() {
        // Just excercise more JUnit features.
    }
}
