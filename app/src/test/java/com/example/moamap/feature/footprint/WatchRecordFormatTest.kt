package com.example.moamap.feature.footprint

import com.example.moamap.feature.footprint.watchrecord.formatCoordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchRecordFormatTest {

    @Test
    fun `좌표는 소수점 다섯 자리로 보여준다`() {
        // 다섯 자리면 1m 남짓. 그보다 길게 보여줘도 읽는 사람에게 의미가 없다.
        assertEquals("37.49630, 126.95740", formatCoordinate(37.4963, 126.9574))
    }

    @Test
    fun `자리수가 넘치면 반올림한다`() {
        assertEquals("37.49635, 126.95746", formatCoordinate(37.4963456, 126.9574567))
    }

    @Test
    fun `남반구와 서반구 좌표도 부호를 잃지 않는다`() {
        assertEquals("-33.86880, -151.20930", formatCoordinate(-33.8688, -151.2093))
    }
}
