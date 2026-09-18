package com.moamap.app.feature.mapdetail

import androidx.annotation.DrawableRes
import com.moamap.app.R
import com.moamap.app.feature.mapdetail.domain.model.PlaceCategoryGroup

/** 마커에 넣을 카테고리 아이콘. 피그마 「카테고리 아이콘」(`1464:1544`)에서 그룹마다 하나씩 골랐다. */
@get:DrawableRes
internal val PlaceCategoryGroup.iconRes: Int
    get() = when (this) {
        PlaceCategoryGroup.Mart -> R.drawable.ic_category_mart
        PlaceCategoryGroup.ConvenienceStore -> R.drawable.ic_category_convenience_store
        PlaceCategoryGroup.Childcare -> R.drawable.ic_category_childcare
        PlaceCategoryGroup.School -> R.drawable.ic_category_school
        PlaceCategoryGroup.Academy -> R.drawable.ic_category_academy
        PlaceCategoryGroup.Parking -> R.drawable.ic_category_parking
        PlaceCategoryGroup.GasStation -> R.drawable.ic_category_gas_station
        PlaceCategoryGroup.Subway -> R.drawable.ic_category_subway
        PlaceCategoryGroup.Bank -> R.drawable.ic_category_bank
        PlaceCategoryGroup.Culture -> R.drawable.ic_category_culture
        PlaceCategoryGroup.RealEstate -> R.drawable.ic_category_real_estate
        PlaceCategoryGroup.PublicOffice -> R.drawable.ic_category_public_office
        PlaceCategoryGroup.Attraction -> R.drawable.ic_category_attraction
        PlaceCategoryGroup.Lodging -> R.drawable.ic_category_lodging
        PlaceCategoryGroup.Restaurant -> R.drawable.ic_category_restaurant
        PlaceCategoryGroup.Cafe -> R.drawable.ic_category_cafe
        PlaceCategoryGroup.Hospital -> R.drawable.ic_category_hospital
        PlaceCategoryGroup.Pharmacy -> R.drawable.ic_category_pharmacy
    }
