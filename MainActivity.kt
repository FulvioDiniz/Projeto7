package com.example.projeto7_1806

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Autenticação com e-mail e senha
        auth.signInWithEmailAndPassword("fulviodiniz0@gmail.com", "zavi5213")
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Autenticação bem-sucedida, atualize a UI com as informações do usuário conectado
                    val user = auth.currentUser
                    setContent {
                        RelayControlScreen(database, user)
                    }
                } else {
                    // Falha na autenticação, exiba uma mensagem ao usuário
                    Toast.makeText(baseContext, "Autenticação falhou.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

@Composable
fun RelayControlScreen(database: DatabaseReference, user: FirebaseUser?) {
    var isRelayOn by remember { mutableStateOf(false) }
    var temperature by remember { mutableStateOf("Carregando...") }
    var relayStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Atualiza o relógio digital e o temporizador
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    LaunchedEffect(Unit) {
        if (user != null) {
            // Obter o estado do relé
            database.child("relay").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isRelayOn = snapshot.getValue(Boolean::class.java) ?: false
                    if (isRelayOn) {
                        relayStartTime = System.currentTimeMillis()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Tratar o erro aqui
                }
            })

            // Obter a temperatura
            database.child("temperature").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    temperature = snapshot.getValue(Double::class.java)?.toString() ?: "N/A"
                }

                override fun onCancelled(error: DatabaseError) {
                    // Tratar o erro aqui
                }
            })
        }
    }

    // Função para formatar o tempo em milissegundos em horas, minutos e segundos
    fun formatTime(timeInMillis: Long): String {
        val seconds = (timeInMillis / 1000) % 60
        val minutes = (timeInMillis / (1000 * 60)) % 60
        val hours = (timeInMillis / (1000 * 60 * 60)) % 24
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // Função para obter o tempo atual formatado
    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(currentTime))
    }

    // Tema de cores
    val backgroundColor = Color(0xFFF5F5F5)
    val cardColor = Color(0xFFFFFFFF)
    val relayOnColor = Color(0xFF4CAF50)
    val relayOffColor = Color(0xFFF44336)

    // Container principal
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Relógio digital
        Text(
            text = "Hora Atual: ${getCurrentTime()}",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .padding(16.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Caixa de texto estilizada para a temperatura
        Surface(
            color = cardColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shadowElevation = 8.dp
        ) {
            Text(
                text = "Temperatura: $temperature°C",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF000000),
                modifier = Modifier
                    .padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Caixa de texto estilizada para o estado do relé
        Surface(
            color = if (isRelayOn) relayOnColor else relayOffColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shadowElevation = 8.dp
        ) {
            Text(
                text = if (isRelayOn) "Relé está ON" else "Relé está OFF",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Temporizador
        if (isRelayOn) {
            val elapsedTime = currentTime - relayStartTime
            Text(
                text = "Tempo de relé ligado: ${formatTime(elapsedTime)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray,
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Botão estilizado para alternar o estado do relé
        Button(
            onClick = {
                if (user != null) {
                    database.child("relay").setValue(!isRelayOn)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRelayOn) relayOffColor else relayOnColor,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (isRelayOn) "Desligar Relé" else "Ligar Relé",
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
