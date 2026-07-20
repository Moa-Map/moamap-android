package com.example.moamap.feature.collection.instagram

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** 캡션 추출 결과. UI/전송 계층에서 분기하기 쉽도록 성공/막힘/오류를 구분한다. */
sealed interface CaptionResult {
    /** 캡션 추출 성공. [description] 이 캡션(설명글) 전체 텍스트. */
    data class Success(val description: String) : CaptionResult

    /** 로그인 필요/비공개 등으로 이 방식으로는 캡션을 가져올 수 없는 경우. */
    data object Blocked : CaptionResult

    /** shortcode 파싱 실패, 네트워크 오류 등. */
    data class Error(val message: String) : CaptionResult
}

/**
 * 인스타그램 공개 게시물/릴스 URL에서 설명(캡션) 텍스트를 추출한다.
 *
 * 일반 URL + OG 태그 방식은 인스타가 캡션을 비워서 내려주기 때문에,
 * 캡션이 그대로 포함되는 임베드 엔드포인트
 *   `https://www.instagram.com/p/{shortcode}/embed/captioned/`
 * 를 크롤러 User-Agent 로 호출해서 Caption 영역만 파싱한다.
 */
class InstagramCaptionExtractor {

    /**
     * @param rawUrl 사용자가 붙여넣은 게시물/릴스 URL
     * @return 캡션 추출 결과. 성공 시 [CaptionResult.Success.description] 에 캡션 전체 텍스트가 들어있다.
     */
    suspend fun extract(rawUrl: String): CaptionResult = withContext(Dispatchers.IO) {
        val shortcode = extractShortcode(rawUrl.trim())
            ?: return@withContext CaptionResult.Error("URL에서 게시물 ID(shortcode)를 찾지 못했습니다.")

        val embedUrl = "https://www.instagram.com/p/$shortcode/embed/captioned/"

        try {
            val connection = (URL(embedUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", CRAWLER_UA)
                setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8")
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val html = stream?.bufferedReader()?.use { it.readText() } ?: ""
            connection.disconnect()

            val caption = extractCaptionBlock(html)?.let { cleanCaptionText(it) }

            when {
                !caption.isNullOrBlank() ->
                    CaptionResult.Success(caption)

                html.contains("loginForm") || html.contains("login_required") ||
                    status == 401 || status == 403 ->
                    CaptionResult.Blocked

                else ->
                    CaptionResult.Error("캡션을 가져오지 못했습니다. (HTTP $status)")
            }
        } catch (e: Exception) {
            CaptionResult.Error(e.message ?: e.toString())
        }
    }

    /**
     * 인스타 URL에서 shortcode 추출 (/p/, /reel/, /reels/, /tv/).
     *
     * 호스트가 인스타그램인지 먼저 확인하고 경로 전체를 매칭해,
     * 비-인스타 호스트나 쿼리스트링 안에 섞인 문자열이 잘못 매칭되지 않게 한다.
     */
    private fun extractShortcode(url: String): String? =
        runCatching { URI(url) }.getOrNull()?.let { uri ->
            val host = uri.host?.lowercase()
            if (
                uri.scheme?.lowercase() !in setOf("http", "https") ||
                host !in setOf("instagram.com", "www.instagram.com", "m.instagram.com")
            ) {
                return null
            }

            Regex("""^/(?:p|reel|reels|tv)/([A-Za-z0-9_-]+)/?$""")
                .find(uri.rawPath ?: return null)
                ?.groupValues
                ?.get(1)
        }

    /** `<div class="Caption"> ... </div>` 블록에서 본문 캡션 영역만 잘라낸다. */
    private fun extractCaptionBlock(html: String): String? {
        val start = html.indexOf("class=\"Caption\"")
        if (start == -1) return null
        val from = html.indexOf('>', start) + 1
        if (from <= 0) return null
        // CaptionComments 시작 전까지가 본문 캡션
        val commentsAt = html.indexOf("class=\"CaptionComments\"", from)
        val end = if (commentsAt != -1) commentsAt else html.indexOf("</div>", from)
        if (end == -1) return null
        return html.substring(from, end)
    }

    /** 사용자명 anchor 는 제거, 해시태그/멘션 anchor 는 텍스트로 보존, 나머지 태그 제거. */
    private fun cleanCaptionText(captionHtml: String): String? {
        var s = captionHtml
        s = s.replace(
            Regex("""<a class="CaptionUsername".*?</a>""", RegexOption.DOT_MATCHES_ALL),
            "",
        )
        s = s.replace(Regex("""<a[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL), "$1")
        s = s.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        s = stripTags(s)
        s = s.replace(Regex("<[^>]*$"), "")
        s = s.replace(Regex("\n{3,}"), "\n\n")
        return htmlUnescape(s).trim().ifBlank { null }
    }

    private fun stripTags(s: String): String = s.replace(Regex("<[^>]+>"), "")

    private fun htmlUnescape(s: String): String =
        s.replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")

    private companion object {
        // 인스타가 임베드/미리보기 콘텐츠를 내려주는 크롤러 UA
        const val CRAWLER_UA =
            "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"
        const val TIMEOUT_MS = 15_000
    }
}
