package com.covid19army.otpsenderclient

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

lateinit var  onSmsRecievedListener: onSmsRecievedListener

    public fun setOnSmsReceivedListener(onSmsRecievedListener: onSmsRecievedListener){
        this.onSmsRecievedListener = onSmsRecievedListener
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if(Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent!!.action)){

            val bundle = intent.extras
            val pdusObj = bundle!!["pdus"] as Array<Any>

            val size = pdusObj.size

            for ( i in 0 until size) {
                var currMessage: SmsMessage? = null
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
                    val format = bundle!!["format"] as String
                    currMessage = SmsMessage.createFromPdu(pdusObj[i] as ByteArray,format)
                }else{
                    currMessage = SmsMessage.createFromPdu(pdusObj[i] as ByteArray)
                }

                val msgBody = currMessage.displayMessageBody
                Log.i("SMS", msgBody)
                if (msgBody.lowercase().startsWith("otp")){
                    val otp =  msgBody.split(' ').getOrNull(1)!!.toInt();
                    val sender = currMessage.displayOriginatingAddress
                    onSmsRecievedListener.onReceived( OtpMessage(sender!!,otp))
                }
            }

        }
    }

}