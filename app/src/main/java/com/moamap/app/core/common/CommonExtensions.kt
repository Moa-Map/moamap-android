package com.moamap.app.core.common

/** 있으면 빼고 없으면 넣는다. 여러 개를 골라 담는 화면(모음 편집·장소 가져오기·약관 동의)이 같이 쓴다. */
internal fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

/** 앞뒤 공백을 걷어내고, 남는 게 없으면 null. 서버가 빈 값을 ""·"  " 로 보내는 경우를 접는다. */
internal fun String?.trimToNull(): String? = this?.trim()?.ifEmpty { null }
