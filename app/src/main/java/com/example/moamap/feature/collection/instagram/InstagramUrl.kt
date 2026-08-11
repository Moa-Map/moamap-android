package com.example.moamap.feature.collection.instagram

/**
 * 인스타그램 게시물 URL 판정.
 *
 * 캡션 추출기와 공유 링크 파서가 같은 기준을 봐야 해서 한곳에 모은다. 한쪽만 경로를
 * 늘리면 공유는 받아놓고 캡션은 못 읽는 상태가 된다.
 */
internal object InstagramUrl {

    /**
     * `scheme://host/path` 에서 호스트와 경로만 떼어낸다.
     *
     * `java.net.URI` 는 인코딩되지 않은 한글이 섞인 주소에서 예외를 던진다. 공유로
     * 들어오는 주소는 그런 형태가 흔해서 정규식으로 가른다.
     */
    private val UrlParts = Regex("""^https?://([^/?#]+)([^?#]*)""", RegexOption.IGNORE_CASE)

    private val Hosts = setOf("instagram.com", "www.instagram.com", "m.instagram.com")

    private val ShortcodePath = Regex("""^/(?:p|reel|reels|tv)/([A-Za-z0-9_-]+)/?$""")

    /**
     * 게시물·릴스 URL 이면 shortcode 를, 아니면 null 을 준다.
     *
     * 호스트를 먼저 확인하고 경로 전체를 매칭한다. 그래야 다른 사이트 주소나 질의
     * 문자열에 섞인 문자열이 잘못 잡히지 않는다.
     */
    fun shortcodeOf(url: String): String? {
        val (host, path) = UrlParts.find(url.trim())?.destructured ?: return null
        // 포트가 붙어 있어도 호스트 비교가 어긋나지 않게 잘라낸다.
        if (host.lowercase().substringBefore(':') !in Hosts) return null

        return ShortcodePath.find(path)?.groupValues?.get(1)
    }
}
