package com.debduttapanda.natschat

import io.nats.client.*
import io.nats.client.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class NatsManager() {
    var nc : Connection? = null
    private set
    var connect = false
    private set
    var userId: String = ""
    var connectCallback: ((Boolean)->Unit)? = null
    var messageCallback: ((com.debduttapanda.natschat.Message)->Unit)? = null

    fun connect(
        userId: String,
        connectCallback: (Boolean)->Unit,
        messageCallback: (com.debduttapanda.natschat.Message)->Unit
    ) {
        this.userId = userId
        this.connectCallback = connectCallback
        this.messageCallback = messageCallback
        CoroutineScope(Dispatchers.IO).launch {
            runNats()
        }
    }

    private fun runNats() {
        val options: Options = Options
            .Builder()
            .server(url)
            .connectionListener { conn, type ->
                when(type){
                    ConnectionListener.Events.CONNECTED -> connectCallback?.invoke(true)
                    ConnectionListener.Events.CLOSED -> connectCallback?.invoke(false)
                    ConnectionListener.Events.DISCONNECTED -> connectCallback?.invoke(false)
                    ConnectionListener.Events.RECONNECTED -> connectCallback?.invoke(true)
                    ConnectionListener.Events.RESUBSCRIBED -> connectCallback?.invoke(true)
                    ConnectionListener.Events.DISCOVERED_SERVERS -> {}
                    ConnectionListener.Events.LAME_DUCK -> {}
                }
            }
            .build()

        try {
            nc = Nats.connect(options)
            connect = true
        } catch (exp: Exception) {
            connect = false
        }
    }

    fun subscribe(topic: String){
        val d = nc?.createDispatcher { msg: Message? -> }
        val s = d?.subscribe(userId) { msg ->
            val response = String(msg.data, StandardCharsets.UTF_8)
            messageCallback?.invoke(
                Message(
                    sender = msg.replyTo,
                    topic = userId,
                    content = response
                )
            )
        }
    }

    fun pub(topic: String, msg: String){
        nc?.publish(topic, userId, msg.toByteArray(StandardCharsets.UTF_8))
    }

    fun close(){
        nc?.close()
    }
}