package com.example.moamap.core.navigation

import android.net.Uri

/**
 * 앱 전역 내비게이션 경로 정의.
 * 각 feature 화면이 추가되면 여기에 route 를 확장한다.
 */
sealed interface MoaMapRoute {
    val route: String

    /** 앱 진입점. 저장된 세션을 확인해 [Login] 또는 [Explore] 로 보낸다. */
    data object Splash : MoaMapRoute {
        override val route = "splash"
    }

    data object Login : MoaMapRoute {
        override val route = "login"
    }

    data object Explore : MoaMapRoute {
        override val route = "explore"
    }

    data object Collection : MoaMapRoute {
        override val route = "collection"
    }

    /**
     * 링크로 장소를 가져오는 흐름을 감싸는 중첩 그래프.
     *
     * 인스타그램과 외부 지도가 같은 화면을 쓰고 부르는 API 와 문구만 달라서, 어느 쪽으로
     * 들어왔는지를 [ARG_SOURCE] 로 받는다. 값은 `PlaceImportSource` 의 이름이다.
     *
     * [ARG_URL] 은 다른 앱에서 공유로 들어왔을 때 URL 입력 화면을 미리 채우는 값이다.
     * 모음 탭에서 들어오면 비어 있다.
     */
    data object PlaceImport : MoaMapRoute {
        const val ARG_SOURCE = "source"
        const val ARG_URL = "url"

        override val route = "place_import/{$ARG_SOURCE}?$ARG_URL={$ARG_URL}"

        /**
         * URL 은 선택값이라 경로 조각이 아니라 질의 문자열에 싣는다. 모음 탭처럼 넘길
         * 것이 없는 경로에서는 경로 조각이 비어 route 와 맞지 않고, 그러면 이동 자체가
         * 실패한다.
         *
         * URL 에는 `?` 나 `&` 가 들어 있어 그대로 붙이면 주소가 끊긴다. 인코딩해서 넘긴다.
         */
        fun createRoute(source: String, url: String = ""): String =
            "place_import/$source?$ARG_URL=${Uri.encode(url)}"
    }

    /** 모음 탭의 `새 지도` 로 들어가는 지도 생성 화면. */
    data object CreateMap : MoaMapRoute {
        override val route = "create_map"
    }

    data object OfficialMap : MoaMapRoute {
        override val route = "official_map"
    }

    data object DensityMapDetail : MoaMapRoute {
        override val route = "density_map_detail"
    }

    /**
     * 지도 설명.
     *
     * 아직 참여하지 않은 공개 지도를 탐색 탭에서 눌렀을 때 거치는 소개 화면이다.
     * 참여 중인 지도는 이 화면을 건너뛰고 [MapDetail] 로 바로 간다.
     */
    data object MapIntro : MoaMapRoute {
        const val ARG_MAP_ID = "mapId"

        override val route = "map_intro/{$ARG_MAP_ID}"

        fun createRoute(mapId: Long): String = "map_intro/$mapId"
    }

    /**
     * 지도 상세.
     *
     * [ARG_MAP_TITLE] 은 `GET /api/v1/maps/{mapId}` 응답이 오기 전까지 상단바를 채우는
     * 초기값이다. 응답이 도착하면 서버 이름으로 덮어쓴다.
     */
    data object MapDetail : MoaMapRoute {
        const val ARG_MAP_ID = "mapId"
        const val ARG_MAP_TITLE = "mapTitle"

        override val route = "map_detail/{$ARG_MAP_ID}?$ARG_MAP_TITLE={$ARG_MAP_TITLE}"

        /**
         * 이름은 선택값이라 경로 조각이 아니라 질의 문자열에 싣는다. 설명 화면처럼 넘길
         * 이름이 없는 경로에서는 경로 조각이 비어 route 와 맞지 않고, 그러면 이동 자체가
         * 실패한다.
         *
         * 이름에 `/` 나 `&` 가 들어가면 주소가 끊기므로 인코딩해서 넘긴다.
         */
        fun createRoute(mapId: Long, mapTitle: String = ""): String =
            "map_detail/$mapId?$ARG_MAP_TITLE=${Uri.encode(mapTitle)}"
    }

    data object Notification : MoaMapRoute {
        override val route = "notification"
    }

    data object MyPage : MoaMapRoute {
        override val route = "mypage"
    }

    data object ProfileEdit : MoaMapRoute {
        override val route = "profile_edit"
    }

    data object Settings : MoaMapRoute {
        override val route = "settings"
    }
}
