package com.privateconverter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaConverterTest {
    @Test fun heicAndHeifExtensionsAreRecognized() {
        assertTrue("photo.heic".endsWith(".heic", ignoreCase = true))
        assertTrue("photo.HEIF".endsWith(".heif", ignoreCase = true))
    }

    @Test fun movExtensionIsRecognized() {
        assertTrue("clip.mov".endsWith(".mov", ignoreCase = true))
    }

    @Test fun unrelatedExtensionsAreNotAccepted() {
        assertFalse("photo.png".endsWith(".heic", ignoreCase = true))
        assertFalse("clip.avi".endsWith(".mov", ignoreCase = true))
    }
}
