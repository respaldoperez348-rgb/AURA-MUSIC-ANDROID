package com.auramusic.app

import com.auramusic.app.utils.formatDuration
import com.auramusic.app.utils.resize
import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeUtilsTest {

    @Test
    fun lh3GoogleusercontentIsRewrittenToRequestedSize() {
        val url = "https://lh3.googleusercontent.com/abc=w120-h120-l90-rj"
        assertEquals(
            "https://lh3.googleusercontent.com/abc=w544-h544-p-l90-rj",
            url.resize(544, 544)
        )
    }

    @Test
    fun yt3GoogleusercontentIsRewrittenToHighResolution() {
        val url = "https://yt3.googleusercontent.com/abc=w60-h60-l90-rj"
        assertEquals(
            "https://yt3.googleusercontent.com/abc=w544-h544-p-l90-rj",
            url.resize(544, 544)
        )
    }

    @Test
    fun ggphtAvatarUsesHighResSize() {
        val url = "https://yt3.ggpht.com/avatar=s88"
        assertEquals(
            "https://yt3.ggpht.com/avatar=w544-h544-p-l90-rj",
            url.resize(544, 544)
        )
    }

    @Test
    fun formatDurationProducesCorrectFormat() {
        assertEquals("3:20", formatDuration(200))
        assertEquals("3:50", formatDuration(230))
        assertEquals("0:05", formatDuration(5))
        assertEquals("6:01", formatDuration(361))
    }
}
