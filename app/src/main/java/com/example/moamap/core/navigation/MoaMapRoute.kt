package com.example.moamap.core.navigation

/**
 * 앱 전역 내비게이션 경로 정의.
 * 각 feature 화면이 추가되면 여기에 route 를 확장한다.
 */
sealed interface MoaMapRoute {
    val route: String

    data object Onboarding : MoaMapRoute {
        override val route = "onboarding"
    }

    data object Explore : MoaMapRoute {
        override val route = "explore"
    }

    data object Collection : MoaMapRoute {
        override val route = "collection"
    }

    data object OfficialMap : MoaMapRoute {
        override val route = "official_map"
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
