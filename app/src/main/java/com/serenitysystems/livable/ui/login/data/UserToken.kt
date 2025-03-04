package com.serenitysystems.livable.ui.login.data

data class UserToken(
    val email: String = "",
    val nickname: String = "",
    val password: String = "",
    val birthdate: String = "",
    val gender: String = "",
    val wgId: String = "",
    val wgRole: String = "",
    var profileImageUrl: String = ""
) {
    // Parameterloser Konstruktor wird von der Data Class automatisch bereitgestellt
    constructor() : this("", "", "", "", "", "", "", "")
}
