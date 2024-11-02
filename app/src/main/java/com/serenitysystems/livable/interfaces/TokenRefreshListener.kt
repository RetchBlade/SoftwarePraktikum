package com.serenitysystems.livable.interfaces

import com.serenitysystems.livable.ui.login.data.UserToken

interface TokenRefreshListener {
    fun refreshUserToken(userToken: UserToken)
}
