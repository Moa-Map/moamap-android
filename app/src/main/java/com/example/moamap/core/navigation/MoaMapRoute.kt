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

    /** 인스타그램 URL 로 장소를 가져오는 4단계 흐름을 감싸는 중첩 그래프. */
    data object PlaceImport : MoaMapRoute {
        override val route = "place_import"
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
     * 지도 상세.
     *
     * 상세 내용은 아직 목데이터라 [ARG_MAP_TITLE] 만 화면에 쓰인다. [ARG_MAP_ID] 는
     * `GET /api/v1/maps/{mapId}` 를 붙일 때 그대로 쓰려고 함께 넘긴다.
     */
    data object MapDetail : MoaMapRoute {
        const val ARG_MAP_ID = "mapId"
        const val ARG_MAP_TITLE = "mapTitle"

        override val route = "map_detail/{$ARG_MAP_ID}/{$ARG_MAP_TITLE}"

        /** 지도 이름에 `/` 가 들어가면 경로가 끊기므로 인코딩해서 넘긴다. */
        fun createRoute(mapId: Long, mapTitle: String): String =
            "map_detail/$mapId/${Uri.encode(mapTitle)}"
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
