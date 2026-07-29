package com.example.moamap.feature.officialmap.presentation

/**
 * 등록 장소 수를 카드 메타에 들어갈 문자열로 만든다. (예: 5416 -> "5,416곳")
 *
 * 천 단위 구분을 넣는다. 공식지도는 공공데이터라 장소가 수천 건씩 들어 있어
 * 구분자가 없으면 자릿수를 읽기 어렵다.
 */
fun formatPlaceCount(count: Int): String = "%,d곳".format(count)
