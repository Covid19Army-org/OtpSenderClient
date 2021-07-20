package com.covid19army.otpsenderclient

interface onSmsRecievedListener {
    fun onReceived( message:OtpMessage)
}