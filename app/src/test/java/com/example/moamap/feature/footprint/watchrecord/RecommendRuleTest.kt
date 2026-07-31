package com.example.moamap.feature.footprint.watchrecord

import com.example.moamap.core.walksession.WalkSample
import com.example.moamap.core.walksession.WalkSessionPayload
import com.example.moamap.core.walksession.computeStats
import com.example.moamap.feature.footprint.domain.model.ReceivedWalkSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 추천 장소를 몇 곳 띄울지는 기록 시간이 정한다.
 *
 * 전에는 버튼의 왼쪽·오른쪽 절반이 갈랐다. 두 결과를 눈으로 견주려던 임시 장치라
 * 규칙이라 할 게 없었다.
 */
class RecommendRuleTest {

    private fun session(durationMillis: Long): ReceivedWalkSession {
        val startedAt = 1_700_000_000_000
        val payload = WalkSessionPayload(
            clientSessionId = "s1",
            startedAtEpochMillis = startedAt,
            endedAtEpochMillis = startedAt + durationMillis,
            samples = listOf(WalkSample(tsEpochMillis = startedAt, lat = 37.5, lng = 127.0)),
        )

        return ReceivedWalkSession(
            payload = payload,
            stats = payload.computeStats(),
            receivedAtEpochMillis = startedAt + durationMillis,
            fileName = "walk-session-$startedAt-s1.json",
        )
    }

    @Test
    fun `10분을 넘게 걸었으면 여러 곳을 띄운다`() {
        assertTrue(session(11 * 60 * 1_000L).recommendsMultiplePlaces())
        assertTrue(session(60 * 60 * 1_000L).recommendsMultiplePlaces())
    }

    @Test
    fun `10분을 못 채웠으면 한 곳만 띄운다`() {
        assertFalse(session(9 * 60 * 1_000L).recommendsMultiplePlaces())
        assertFalse(session(0L).recommendsMultiplePlaces())
    }

    @Test
    fun `딱 10분은 여러 곳 쪽이다`() {
        // "10분 이상" 이 기준이다. 경계를 어느 쪽에 붙일지는 코드만 봐서는 알 수 없다.
        assertTrue(session(RECOMMEND_MULTI_THRESHOLD_MILLIS).recommendsMultiplePlaces())
        assertFalse(session(RECOMMEND_MULTI_THRESHOLD_MILLIS - 1).recommendsMultiplePlaces())
    }
}
