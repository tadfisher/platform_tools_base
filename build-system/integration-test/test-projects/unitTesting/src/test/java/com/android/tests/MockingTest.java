/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tests;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.Ignore;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;

import com.android.tests.MainActivity;

public class MockingTest {
    @Test
    public void mockFinalClass() {
        // BluetoothAdapter is final in android.jar, but our new mockable jar strips all "final"
        // modifiers.
        BluetoothAdapter adapter = mock(BluetoothAdapter.class);
        when(adapter.cancelDiscovery()).thenReturn(true);

        adapter.cancelDiscovery();

        verify(adapter).cancelDiscovery();
        verifyNoMoreInteractions(adapter);
    }

    @Test
    public void mockFinalMethod() {
        Application app = mock(Application.class);
        when(app.getString(anyInt())).thenReturn(null);

        assertNull(app.getString(42));

        verify(app.getString(42));
        verifyNoMoreInteractions(app);
    }

    @Test
    public void mockActivity() {
        Activity activity = mock(Activity.class);
    }
}
