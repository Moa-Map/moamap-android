package com.example.moamap.feature.collection.share

import com.example.moamap.feature.collection.domain.model.PlaceImportSource
import com.example.moamap.feature.collection.instagram.InstagramUrl

/** 다른 앱이 공유해 온 텍스트를 장소 가져오기 흐름이 쓸 수 있는 형태로 판정한 결과. */
internal sealed interface SharedLink {

    /** 지원하는 링크. [url] 을 [source] 흐름의 URL 입력에 그대로 채운다. */
    data class Supported(val url: String, val source: PlaceImportSource) : SharedLink

    /** 링크가 없거나 지원하지 않는 곳에서 왔다. */
    data object Unsupported : SharedLink
}

/**
 * 공유 텍스트에서 장소 가져오기에 쓸 링크를 뽑아낸다.
 *
 * 인스타그램·네이버 지도·카카오맵·구글 지도 모두 전용 스킴 없이 `ACTION_SEND` 의
 * `text/plain` 하나로 들어오므로, 어느 앱에서 왔는지는 링크 호스트로만 알 수 있다.
 */
internal object SharedLinkParser {

    fun parse(sharedText: String?): SharedLink {
        val url = sharedText?.let(::firstUrlIn) ?: return SharedLink.Unsupported
        val source = sourceOf(url) ?: return SharedLink.Unsupported

        return SharedLink.Supported(url = url, source = source)
    }

    /**
     * 공유 텍스트에는 URL 만 오지 않는다.
     *
     * 네이버 지도는 `네이버 지도\nhttps://naver.me/...`, 구글 지도는 장소 이름을 앞에
     * 붙여 보낸다. 사용자가 직접 문장을 덧붙이기도 한다. 첫 URL 하나만 쓴다.
     */
    private fun firstUrlIn(text: String): String? =
        UrlInText.find(text)
            ?.value
            // 문장 끝에 붙어 온 경우 마침표나 닫는 괄호까지 URL 로 딸려 들어온다.
            ?.trimEnd { char -> char in TRAILING_PUNCTUATION }
            ?.ifBlank { null }

    private fun sourceOf(url: String): PlaceImportSource? {
        if (InstagramUrl.shortcodeOf(url) != null) return PlaceImportSource.Instagram

        val (rawHost, path) = UrlParts.find(url)?.destructured ?: return null
        val host = rawHost.lowercase().substringBefore(':')

        return when {
            MapShareDomains.any { domain -> host.isUnder(domain) } -> PlaceImportSource.MapShare

            // 구글은 도메인 하나를 온갖 서비스가 나눠 쓴다. 검색 결과 링크를 공유했을 때
            // 지도 흐름이 열리지 않도록 지도 링크인지 따로 가린다.
            GoogleDomains.any { domain -> host.isUnder(domain) } && isGoogleMapPath(host, path) ->
                PlaceImportSource.MapShare

            else -> null
        }
    }

    /**
     * 서버 `ShareLinkUrlParser.hostMatches` 와 같은 규칙.
     *
     * 정확히 일치하는 목록으로 들면 `applink.map.kakao.com` 처럼 단축링크가 풀린 뒤에야
     * 드러나는 서브도메인을 놓친다. `evil.com` 이 `map.naver.com.evil.com` 으로 흉내 내는
     * 것은 점 경계를 확인해 막는다.
     */
    private fun String.isUnder(domain: String): Boolean =
        this == domain || endsWith(".$domain")

    /** `maps.app.goo.gl/xxx` 처럼 경로에 `/maps` 가 없는 형태가 있어 호스트도 함께 본다. */
    private fun isGoogleMapPath(host: String, path: String): Boolean =
        host.startsWith("maps.") || path.startsWith("/maps")

    /** `java.net.URI` 는 인코딩되지 않은 한글에서 던진다. 공유 링크는 그런 형태가 흔하다. */
    private val UrlParts = Regex("""^https?://([^/?#]+)([^?#]*)""", RegexOption.IGNORE_CASE)

    private val UrlInText = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    private const val TRAILING_PUNCTUATION = ".,;:!?)]}>\"'"

    /**
     * 지도 서비스의 도메인. 서브도메인도 함께 인정한다.
     *
     * 서버가 실제로 받아주는 것(`naver.me`·`map.naver.com`, `kko.to`·`map.kakao.com`)보다
     * 넓게 잡는다. 앱의 판정은 인스타냐 지도냐를 고르는 용도지 링크를 걸러내는 관문이
     * 아니다. 좁게 잡으면 서버가 처리할 수 있는 링크를 앱이 먼저 막아버리고, 사용자는
     * 이유도 알 수 없는 토스트만 본다. 지도 링크인데 서버가 못 읽으면 그 사정을 담은
     * 서버 메시지가 뜨는 편이 낫다.
     */
    private val MapShareDomains = listOf(
        "naver.me",
        "map.naver.com",
        "place.naver.com",
        "kko.to",
        "kko.kakao.com",
        "map.kakao.com",
    )

    /** 구글은 도메인 하나를 온갖 서비스가 나눠 써서 경로까지 봐야 한다. */
    private val GoogleDomains = listOf(
        "goo.gl",
        "google.com",
        "google.co.kr",
    )
}
