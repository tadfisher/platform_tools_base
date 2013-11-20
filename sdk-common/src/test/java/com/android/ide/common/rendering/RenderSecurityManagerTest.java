/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.ide.common.rendering;

import com.google.common.io.Files;

import junit.framework.TestCase;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class RenderSecurityManagerTest extends TestCase {

    public void testExec() throws Exception {
        assertNull(RenderSecurityManager.getCurrent());

        RenderSecurityManager manager = new RenderSecurityManager(null,
                null);
        try {
            assertNull(RenderSecurityManager.getCurrent());
            manager.setActive(true);
            assertSame(manager, RenderSecurityManager.getCurrent());
            if (new File("/bin/ls").exists()) {
                Runtime.getRuntime().exec("/bin/ls");
            } else {
                manager.checkExec("/bin/ls");
            }
            fail("Should have thrown security exception");
        } catch (SecurityException exception) {
            assertEquals("com.android.ide.common.rendering.RenderSecurityException: "
                    + "Read access not allowed during rendering (/bin/ls)", exception.toString());
            // pass
        } finally {
            manager.dispose();
            assertNull(RenderSecurityManager.getCurrent());
        }
    }

    public void testSetSecurityManager() throws Exception {
        RenderSecurityManager manager = new RenderSecurityManager(null,
                null);
        try {
            manager.setActive(true);
            System.setSecurityManager(null);
            fail("Should have thrown security exception");
        } catch (SecurityException exception) {
            // pass
        } finally {
            manager.dispose();
        }
    }

    public void testInvalidRead() throws Exception {
        RenderSecurityManager manager = new RenderSecurityManager(null,
                null);
        try {
            manager.setActive(true);

            File file = new File(System.getProperty("user.home"));
            //noinspection ResultOfMethodCallIgnored
            file.lastModified();

            fail("Should have thrown security exception");
        } catch (SecurityException exception) {
            // pass
        } finally {
            manager.dispose();
        }
    }

    public void testReadOk() throws Exception {
        RenderSecurityManager manager = new RenderSecurityManager(null,
                null);
        try {
            manager.setActive(true);

            File jdkHome = new File(System.getProperty("java.home"));
            assertTrue(jdkHome.exists());
            //noinspection ResultOfMethodCallIgnored
            File[] files = jdkHome.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        Files.toByteArray(file);
                    }
                }
            }
        } finally {
            manager.dispose();
        }
    }

    public void testProperties() throws Exception {
        RenderSecurityManager manager = new RenderSecurityManager(null,
                null);
        try {
            manager.setActive(true);

            System.getProperties();

            fail("Should have thrown security exception");
        } catch (SecurityException exception) {
            // pass
        } finally {
            manager.dispose();
        }
    }

    public void testExit() throws Exception {
        RenderSecurityManager manager = new RenderSecurityManager(null,
                null);
        try {
            manager.setActive(true);

            System.exit(-1);

            fail("Should have thrown security exception");
        } catch (SecurityException exception) {
            // pass
        } finally {
            manager.dispose();
        }
    }

    public void testThread() throws Exception {
        final AtomicBoolean failedUnexpectedly = new AtomicBoolean(false);
        Thread otherThread = new Thread("other") {
            @Override
            public void run() {
                try {
                    System.getProperties();
                } catch (SecurityException e) {
                    failedUnexpectedly.set(true);
                }
            }
        };
        RenderSecurityManager manager = new RenderSecurityManager(null,
                null);
        try {
            manager.setActive(true);

            // Threads cloned from this one should inherit the same security constraints
            final AtomicBoolean failedAsExpected = new AtomicBoolean(false);
            final Thread renderThread = new Thread("render") {
                @Override
                public void run() {
                    try {
                        System.getProperties();
                    } catch (SecurityException e) {
                        failedAsExpected.set(true);
                    }
                }
            };
            renderThread.start();
            renderThread.join();
            assertTrue(failedAsExpected.get());
            otherThread.start();
            otherThread.join();
            assertFalse(failedUnexpectedly.get());
        } finally {
            manager.dispose();
        }
    }

    public void testActive() throws Exception {
        RenderSecurityManager manager = new RenderSecurityManager(null,
                null);
        try {
            manager.setActive(true);

            try {
                System.getProperties();
                fail("Should have thrown security exception");
            } catch (SecurityException exception) {
                // pass
            }

            manager.setActive(false);

            try {
                System.getProperties();
            } catch (SecurityException exception) {
                fail(exception.toString());
            }

            manager.setActive(true);

            try {
                System.getProperties();
                fail("Should have thrown security exception");
            } catch (SecurityException exception) {
                // pass
            }
        } finally {
            manager.dispose();
        }
    }
}
