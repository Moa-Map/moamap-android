package com.example.moamap.core.navigation

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

    data object OfficialMap : MoaMapRoute {
        override val route = "official_map"
    }

    data object DensityMapDetail : MoaMapRoute {
        override val route = "density_map_detail"
    }

    data object MapDetail : MoaMapRoute {
        override val route = "map_detail"
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
