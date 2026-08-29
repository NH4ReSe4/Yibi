package com.dubiao.yibi.speech

import android.speech.RecognizerIntent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnDeviceSpeechControllerTest {
    @Test fun recognitionIntentUsesSimplifiedChineseWithoutLanguageQueryExtras() {
        val intent = speechRecognitionIntent(preferOffline = true)

        assertEquals("zh-CN", intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertTrue(intent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false))
        assertTrue(intent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
        assertEquals(3, intent.getIntExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 0))
        assertFalse(intent.hasExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE))
        assertFalse(intent.hasExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE))
    }

    @Test fun onlineFallbackAllowsTheSystemRecognizerToUseNetwork() {
        val intent = speechRecognitionIntent(preferOffline = false)

        assertEquals("zh-CN", intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertFalse(intent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true))
    }
}
