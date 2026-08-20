package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.core.network.model.PageResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PageCollectorTest {

    /** `last` 가 true 인 페이지가 나올 때까지 이어 받는다. */
    private fun pages(vararg sizes: Int): suspend (Int) -> PageResponse<Int> {
        val chunks = sizes.mapIndexed { index, size ->
            List(size) { offset -> index * 1000 + offset }
        }
        return { page ->
            PageResponse(
                content = chunks.getOrElse(page) { emptyList() },
                page = page,
                last = page >= chunks.lastIndex,
            )
        }
    }

    @Test
    fun `마지막 페이지까지 이어 받아 하나로 합친다`() = runTest {
        val requested = mutableListOf<Int>()
        val fetch = pages(2, 2, 1)

        val all = collectAllPages { page ->
            requested += page
            fetch(page)
        }

        assertEquals(listOf(0, 1, 2), requested)
        assertEquals(listOf(0, 1, 1000, 1001, 2000), all)
    }

    @Test
    fun `첫 페이지가 마지막이면 한 번만 부른다`() = runTest {
        val requested = mutableListOf<Int>()
        val fetch = pages(3)

        collectAllPages { page ->
            requested += page
            fetch(page)
        }

        assertEquals(listOf(0), requested)
    }

    @Test
    fun `빈 페이지가 오면 last 와 무관하게 멈춘다`() = runTest {
        // 서버가 last 를 잘못 내려도 무한히 돌지 않아야 한다.
        var calls = 0

        val all = collectAllPages { page ->
            calls++
            PageResponse(content = if (page == 0) listOf(1, 2) else emptyList(), last = false)
        }

        assertEquals(2, calls)
        assertEquals(listOf(1, 2), all)
    }

    @Test
    fun `페이지 상한에 걸리면 받은 데까지 돌려준다`() = runTest {
        var calls = 0

        val all = collectAllPages(maxPages = 3) { page ->
            calls++
            PageResponse(content = listOf(page), last = false)
        }

        assertEquals(3, calls)
        assertEquals(listOf(0, 1, 2), all)
    }
}
