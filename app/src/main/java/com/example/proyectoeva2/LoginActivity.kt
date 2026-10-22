package com.example.proyectoeva2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var editTextCorreo: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var textEstadoLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        editTextCorreo = findViewById(R.id.editTextCorreo)
        editTextPassword = findViewById(R.id.editTextPassword)
        textEstadoLogin = findViewById(R.id.textEstadoLogin)

        findViewById<Button>(R.id.btnIniciarSesion).setOnClickListener{iniciarSesion()}

        findViewById<Button>(R.id.btnRegistrar).setOnClickListener {registrarUsuario()}
        // Si ya hay una sesión activa, abrir directamente el panel.
        if (auth.currentUser != null) {
            abrirPanel()
        }
    }

    private fun iniciarSesion() {
        val correo = editTextCorreo.text.toString().trim()
        val password = editTextPassword.text.toString()

        if (correo.isBlank() || password.isBlank())
        {
            mostrarEstado("Escribe tu correo y contraseña.")
            return
        }

        auth.signInWithEmailAndPassword(correo, password)
            .addOnCompleteListener(this) { tarea ->
                if (tarea.isSuccessful) {
                    abrirPanel()
                } else {
                    mostrarEstado(
                        tarea.exception?.localizedMessage ?: "No se pudo iniciar sesión."
                    )
                }
            }
    }

    private fun registrarUsuario() {
        val correo = editTextCorreo.text.toString().trim()
        val password = editTextPassword.text.toString()

        if (correo.isBlank() || password.isBlank()) {
            mostrarEstado("Escribe tu correo y contraseña para crear la cuenta.")
            return
        }

        auth.createUserWithEmailAndPassword(correo, password)
            .addOnCompleteListener(this) { tarea ->
                if (tarea.isSuccessful) {
                    Toast.makeText(this, "Cuenta creada.", Toast.LENGTH_SHORT).show()
                    abrirPanel()
                } else {
                    mostrarEstado(
                        tarea.exception?.localizedMessage ?: "No se pudo crear la cuenta."
                    )
                }
            }
    }

    private fun mostrarEstado(mensaje: String) {
        textEstadoLogin.text = mensaje
    }

    private fun abrirPanel() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
