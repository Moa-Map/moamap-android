package com.example.moamap.feature.explore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreMapDetailNavigationTest {
    @Test
    fun `only the first sample community map opens the preview detail`() {
        assertTrue(shouldOpenMapDetail(mapId = 1L))
        assertFalse(shouldOpenMapDetail(mapId = 2L))
        assertFalse(shouldOpenMapDetail(mapId = 3L))
        assertFalse(shouldOpenMapDetail(mapId = 4L))
    }
}
