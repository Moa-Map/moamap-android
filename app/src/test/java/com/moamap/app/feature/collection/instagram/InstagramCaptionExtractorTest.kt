package com.moamap.app.feature.collection.instagram

import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test

/**
 * 실제 인스타 임베드 엔드포인트를 호출하는 네트워크 통합 테스트 (수동 확인용).
 *
 * 직접 확인하려면 아래 애너테이션을 잠깐 지우고 실행:
 *   ./gradlew :app:testDebugUnitTest --tests "*InstagramCaptionExtractorTest*"
 */
class InstagramCaptionExtractorTest {

    @Ignore("실제 네트워크 호출 — 필요할 때 수동으로만 실행")
    @Test
    fun `공개 릴스 URL 캡션 추출 결과 출력`() = runBlocking {
        val url = "https://www.instagram.com/reel/DasMNhUz7Pb/?igsh=YnVyYmU4enJpb3c4"

        when (val result = InstagramCaptionExtractor().extract(url)) {
            is CaptionResult.Success -> {
                println("=== ✅ SUCCESS ===")
                println(result.description)
            }
            CaptionResult.Blocked -> println("=== 🔒 BLOCKED (로그인 필요/비공개) ===")
            is CaptionResult.NetworkError -> println("=== 📡 NETWORK: ${result.message} ===")
            is CaptionResult.Error -> println("=== ❌ ERROR: ${result.message} ===")
        }
    }
}
