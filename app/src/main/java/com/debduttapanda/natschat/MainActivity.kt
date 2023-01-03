package com.debduttapanda.natschat

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.debduttapanda.natschat.ui.theme.NatsChatTheme
import io.nats.client.Connection
import io.nats.client.Message
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import kotlin.math.log



class MainActivity : ComponentActivity() {
    val nats = NatsManager()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NatsChatTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Box{
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val login = remember {
                                mutableStateOf(false)
                            }
                            val userId = remember {
                                mutableStateOf("")
                            }
                            val destination = remember {
                                mutableStateOf("")
                            }

                            val messages = remember{ mutableStateListOf<com.debduttapanda.natschat.Message>() }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        if(!login.value){
                                            if(userId.value.isNotEmpty()){
                                                nats.connect(
                                                    userId = userId.value,
                                                    connectCallback = {
                                                        login.value = it
                                                        nats.subscribe(userId.value)
                                                    },
                                                    messageCallback = {
                                                        messages.add(0,it)
                                                    }
                                                )
                                            }
                                        }
                                        else{
                                            nats.close()
                                        }
                                    }
                                ) {
                                    Text(if(login.value) "Logout" else "Login")
                                }
                                TextField(
                                    value = userId.value,
                                    onValueChange = {
                                        userId.value = it
                                    },
                                    placeholder = {
                                        Text("User Id")
                                    },
                                    enabled = !login.value,
                                    modifier = Modifier.weight(1f)
                                )
                                TextField(
                                    value = destination.value,
                                    onValueChange = {
                                        destination.value = it
                                    },
                                    placeholder = {
                                        Text("Destination")
                                    },
                                    enabled = !login.value,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            val input = remember {
                                mutableStateOf("")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        nats.pub(destination.value, input.value)
                                        messages.add(0,Message(userId.value,destination.value,input.value))
                                    },
                                    enabled = login.value
                                ) {
                                    Text("Send")
                                }
                                TextField(
                                    value = input.value,
                                    onValueChange = {
                                        input.value = it
                                    },
                                    placeholder = {
                                        Text("Send a message")
                                    }
                                )
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                            ){
                                items(messages){
                                    Text(
                                        "${it.sender}: ${it.topic} = ${it.content}",
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

