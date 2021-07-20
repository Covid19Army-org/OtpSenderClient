package com.covid19army.otpsenderclient

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.telephony.SmsManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.*
import com.rabbitmq.client.*


class MainActivity : AppCompatActivity() {

 var factory:ConnectionFactory = ConnectionFactory()
    lateinit var connection:Connection
    lateinit var channel:Channel
    lateinit var subscribeThread: Thread
    lateinit var tv: TextView
    lateinit var smsReceiver: SmsReceiver
    val mapper = jacksonObjectMapper()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermissions()
        tv =  findViewById<TextView>(R.id.message)
        setupConnectionFactory()
        Toast.makeText(applicationContext,"connection successful to rabbit mq",Toast.LENGTH_SHORT).show()

        val handler = MyHandler(tv, mapper, applicationContext)

        subscribe(handler)



    }



    private fun setupConnectionFactory() {
        var uri = getString(R.string.rabbitConnectioinString);
        try {
            factory.setAutomaticRecoveryEnabled(false);
            factory.setUri(uri);


        } catch ( e1:Exception) {
            e1.printStackTrace();
        }
    }

    fun subscribe(handler:Handler){
        Toast.makeText(applicationContext,"inside subscribe",Toast.LENGTH_SHORT).show()

        subscribeThread = Thread{
            //Toast.makeText(applicationContext,"inside subscribe ${counter}",Toast.LENGTH_SHORT).show()
            connection = factory.newConnection()
            channel = connection.createChannel()
            channel.basicQos(1)
            val consumer = MyConsumer(handler, channel)
            try {


                    channel.basicConsume("sendotpqueue", false, consumer)
                    //connection.close()
                   // channel.close()
                    Thread.sleep(10000)

                 }catch(e:Exception){
                connection.close()
                channel.close()
                val msg = handler.obtainMessage()
                val bundle = Bundle()
                bundle.putString("msg", e.message)
                msg.data = bundle
                handler.sendMessage(msg)
                //Toast.makeText(applicationContext,e.message,Toast.LENGTH_SHORT).show()
            }
        }
        subscribeThread.start()
        Toast.makeText(applicationContext,"started thread ${subscribeThread.id}",Toast.LENGTH_SHORT).show()
    }

    class MyHandler(private val weakTextView: TextView, val mapper: ObjectMapper,val context: Context) : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val data = msg.data.getString("msg")
            val otp:OtpMessage = mapper.readValue(data!!)

            weakTextView.text = otp.mobilenumber
            try {
                val smsManager: SmsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(otp.mobilenumber, null, otp.otp.toString(), null, null)
                Toast.makeText(
                    context, "Message Sent",
                    Toast.LENGTH_LONG
                ).show()
            } catch (ex: java.lang.Exception) {
                Toast.makeText(
                   context, ex.message.toString(),
                    Toast.LENGTH_LONG
                ).show()
                ex.printStackTrace()
            }
        }
    }

    class MyConsumer( val handler: Handler, channel: Channel) : DefaultConsumer(channel){

        override fun handleDelivery(
            consumerTag: String?,
            envelope: Envelope?,
            properties: AMQP.BasicProperties?,
            body: ByteArray?
        ) {
            super.handleDelivery(consumerTag, envelope, properties, body)


            val routingKey = envelope!!.routingKey
            val contentType = properties!!.contentType
            val deliveryTag = envelope!!.deliveryTag
            // (process the message components here ...)
            // (process the message components here ...)
            val message = String(body!!)
            val msg = handler.obtainMessage()
            val bundle = Bundle()
            bundle.putString("msg", message)
            msg.data = bundle
            handler.sendMessage(msg)
            channel.basicAck(deliveryTag, false)

        }
    }

    fun requestPermissions(){
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            ), 12
        )
    }

    fun registerSmsReceiver(smsRecievedListener: onSmsRecievedListener){

        smsReceiver = SmsReceiver()
        smsReceiver.setOnSmsReceivedListener(smsRecievedListener)
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED")
        this.registerReceiver(smsReceiver, intentFilter)
    }

    public override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        registerSmsReceiver(SmsReceivedListenerImpl(applicationContext))
    }


    public override fun onDestroy() {
        super.onDestroy()
        try {
            subscribeThread.interrupt()
            this.unregisterReceiver(smsReceiver)
        }catch (e:java.lang.Exception){

        }

    }

class SmsReceivedListenerImpl(val context: Context) : onSmsRecievedListener{
    override fun onReceived(message: OtpMessage) {
        Toast.makeText(context, "${message.mobilenumber} ${message.otp}", Toast.LENGTH_SHORT ).show()
    }

}

    /*
    ConnectionFactory factory = new ConnectionFactory();
    private fun setupConnectionFactory() {
        String uri = "CLOUDAMQP_URL";
        try {
            factory.setAutomaticRecoveryEnabled(false);
            factory.setUri(uri);
        } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e1) {
            e1.printStackTrace();
        }
    }
      */
}