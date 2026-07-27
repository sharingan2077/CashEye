package com.yandex.school.casheye.core.designsystem.component.editor

data class EditorOption(
    val id: Int,
    val label: String,
    val emoji: String = "",
    val currencyCode: String? = null,
    val subtitle: String? = null,
)
