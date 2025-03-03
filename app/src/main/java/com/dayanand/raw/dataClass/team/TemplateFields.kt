package com.dayanand.raw.dataClass.team

data class TemplateFields(
    val ACL: ACL,
    val _version: Int,
    val created_at: String,
    val created_by: String,
    val dfe_class_uid: String,
    val league_id: String,
    val locale: String,
    val logo: String,
    val publish_details: PublishDetails,
    val season_id: String,
    val ta: String,
    val tags: List<Any>,
    val tc: String,
    val tid: String,
    val title: String,
    val tn: String,
    val updated_at: String,
    val updated_by: String,
    val year: Int
)