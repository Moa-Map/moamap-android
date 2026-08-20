package com.moamap.app.feature.officialmap.domain.model

enum class AgeGroup(val label: String) {
    UNDER_10("10대 미만"),
    TEENS("10대"),
    TWENTIES("20대"),
    THIRTIES("30대"),
    FORTIES("40대"),
    FIFTIES("50대"),
    SIXTIES("60대"),
    SEVENTIES_UP("70대 이상"),
}

enum class Gender(val label: String) {
    MALE("남성"),
    FEMALE("여성"),
}

data class PopulationShare(
    val label: String,
    val rate: Double,
)
