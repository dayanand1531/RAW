package com.dayanand.raw.dataClass.schedule

data class CsCustomFields(
    val family_night: String,
    val hide_tune_in: Boolean,
    val sponsor: Sponsor,
    val tune_in: List<Any>,
    val utility_menu: List<Any>
)