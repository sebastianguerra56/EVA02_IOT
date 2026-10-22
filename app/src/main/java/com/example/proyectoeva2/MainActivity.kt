package com.example.proyectoeva2

import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var alarmasRef: DatabaseReference
    private lateinit var historialRef: DatabaseReference

    private lateinit var listaAlarmas: LinearLayout
    private lateinit var listaHistorial: LinearLayout
    private lateinit var textSaludo: TextView
    private lateinit var textEstadoConexion: TextView
    private lateinit var textUltimaSincronizacion: TextView


    private data class Alarma(
        val id: String,
        val hora: String,
        val nombre: String,
        val activa: Boolean
    )

    // Actualiza la pantalla cuando cambian las alarmas en Firebase.
    private val listenerAlarmas = object : ValueEventListener {
        override fun onDataChange(datosFirebase: DataSnapshot) {
            mostrarAlarmas(datosFirebase)
            textUltimaSincronizacion.text = "Alarmas sincronizadas con Firebase"
        }

        override fun onCancelled(error: DatabaseError) {
            textEstadoConexion.text = "No se pudo leer la base de datos"

            Toast.makeText(
                this@MainActivity,
                "Error al leer alarmas: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Actualiza la lista cuando cambia el historial en Firebase.
    private val listenerHistorial = object : ValueEventListener {
        override fun onDataChange(datos: DataSnapshot) {
            listaHistorial.removeAllViews()

            if (!datos.exists()) {
                agregarTextoHistorial("Aún no hay alarmas registradas.")
                return
            }

            datos.children.forEach { registro ->
                val nombre = registro.child("nombre")
                    .getValue(String::class.java) ?: "Medicamento"

                val fecha = registro.child("fechaActivacion")
                    .getValue(String::class.java) ?: "Fecha desconocida"

                agregarTextoHistorial("$nombre — sonó el $fecha")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(
                this@MainActivity,
                "No se pudo cargar el historial: ${error.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Indica si la app está conectada a Firebase.
    private val referenciaConexion =
        FirebaseDatabase.getInstance().getReference(".info/connected")

    private val listenerConexion = object : ValueEventListener {
        override fun onDataChange(datosFirebase: DataSnapshot) {
            val conectado = datosFirebase.getValue(Boolean::class.java) ?: false

            textEstadoConexion.text = if (conectado) {
                "Conectado a Firebase"
            } else {
                "Sin conexión a Firebase"
            }
        }

        override fun onCancelled(error: DatabaseError) {
            textEstadoConexion.text = "Estado de conexión no disponible"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ajustarPantalla()

        auth = FirebaseAuth.getInstance()
        val usuario = auth.currentUser

        // Si no hay sesión, vuelve al login.
        if (usuario == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        conectarVistas()
        textSaludo.text =
            "Hola, ${usuario.email?.substringBefore("@") ?: "usuario"}"

        // Cada usuario usa su propio espacio de alarmas e historial.
        val usuarioRef = FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(usuario.uid)

        alarmasRef = usuarioRef.child("alarmas")
        historialRef = usuarioRef.child("historial")

        // Cambia entre las pantallas de alarmas e historial.
        findViewById<LinearLayout>(R.id.btnNavAlarmas).setOnClickListener {
            mostrarPagina(historial = false)
        }

        findViewById<LinearLayout>(R.id.btnNavHistorial).setOnClickListener {
            mostrarPagina(historial = true)
        }

        mostrarPagina(historial = false)

        findViewById<Button>(R.id.btnSimularConexion).setOnClickListener {
            simularConexionYRecibirHistorial()
        }

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            cerrarSesion()
        }

        findViewById<Button>(R.id.btnAgregarAlarma).setOnClickListener {
            pedirNombreAlarma()
        }

        alarmasRef.addValueEventListener(listenerAlarmas)
        historialRef.addValueEventListener(listenerHistorial)
        referenciaConexion.addValueEventListener(listenerConexion)
    }

    private fun mostrarPagina(historial: Boolean) {
        findViewById<ScrollView>(R.id.paginaAlarmas).visibility =
            if (historial) View.GONE else View.VISIBLE

        findViewById<ScrollView>(R.id.paginaHistorial).visibility =
            if (historial) View.VISIBLE else View.GONE
    }

    // Agrega registros de prueba como si los enviara el Arduino.
    private fun simularConexionYRecibirHistorial() {
        val registros = listOf(
            mapOf(
                "alarmaId" to "alarma_1",
                "nombre" to "Medicamento de la mañana",
                "hora" to "08:00",
                "fechaActivacion" to "2026-10-19 08:00"
            ),
            mapOf(
                "alarmaId" to "alarma_2",
                "nombre" to "Medicamento de la tarde",
                "hora" to "14:30",
                "fechaActivacion" to "2026-10-19 14:30"
            )
        )

        registros.forEach { registro ->
            historialRef.push().setValue(registro)
                .addOnFailureListener { error ->
                    mostrarError("No se pudo guardar el historial", error)
                }
        }

        Toast.makeText(
            this,
            "Historial de prueba enviado a Firebase",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun agregarTextoHistorial(texto: String) {
        val vista = TextView(this).apply {
            this.text = texto
            textSize = 16f
            setPadding(dp(8), dp(12), dp(8), dp(12))
        }

        listaHistorial.addView(vista)
    }

    private fun ajustarPantalla() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
    }

    private fun conectarVistas() {
        listaAlarmas = findViewById(R.id.listaAlarmas)
        listaHistorial = findViewById(R.id.listaHistorial)
        textSaludo = findViewById(R.id.textSaludo)
        textEstadoConexion = findViewById(R.id.textEstadoConexion)
        textUltimaSincronizacion = findViewById(R.id.textUltimaSincronizacion)
    }

    private fun cerrarSesion() {
        auth.signOut()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
    }

    private fun mostrarAlarmas(datosFirebase: DataSnapshot) {
        val alarmas = datosFirebase.children.mapNotNull { dato ->
            val id = dato.key ?: return@mapNotNull null
            val hora =
                dato.child("hora").getValue(String::class.java) ?: "08:00"
            val nombre =
                dato.child("nombre").getValue(String::class.java) ?: "Medicamento"
            val activa =
                dato.child("activa").getValue(Boolean::class.java) ?: true

            Alarma(id, hora, nombre, activa)
        }.sortedBy { it.hora }

        listaAlarmas.removeAllViews()

        if (alarmas.isEmpty()) {
            mostrarMensajeListaVacia()
            return
        }

        alarmas.forEach { alarma ->
            listaAlarmas.addView(crearFilaAlarma(alarma))
        }
    }

    private fun mostrarMensajeListaVacia() {
        val mensaje = TextView(this).apply {
            text = "No tienes alarmas. Pulsa Agregar alarma para crear una."
            textSize = 15f
            setTextColor(Color.rgb(98, 115, 111))
            setPadding(dp(8), dp(18), dp(8), dp(18))
        }

        listaAlarmas.addView(mensaje)
    }

    private fun crearFilaAlarma(alarma: Alarma): LinearLayout {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(10), dp(12))
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        }

        val textos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val textoHora = TextView(this).apply {
            text = alarma.hora
            textSize = 22f
            setTextColor(Color.rgb(23, 59, 53))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            contentDescription = "Cambiar hora, ${alarma.hora}"
            setOnClickListener {
                elegirHoraAlarma(alarma)
            }
        }

        val textoNombre = TextView(this).apply {
            text = alarma.nombre
            textSize = 14f
            setTextColor(Color.rgb(98, 115, 111))
            setPadding(0, dp(3), 0, 0)
        }

        val interruptor = Switch(this).apply {
            isChecked = alarma.activa
            contentDescription = "Activar o desactivar ${alarma.nombre}"

            setOnCheckedChangeListener { _, activada ->
                alarmasRef.child(alarma.id)
                    .child("activa")
                    .setValue(activada)
                    .addOnFailureListener { error ->
                        mostrarError("No se pudo actualizar", error)
                    }
            }
        }

        val botonEliminar = TextView(this).apply {
            text = "×"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(179, 38, 30))
            contentDescription = "Eliminar alarma ${alarma.nombre}"
            setPadding(dp(12), 0, dp(4), 0)
            setOnClickListener {
                confirmarEliminacion(alarma)
            }
        }

        textos.addView(textoHora)
        textos.addView(textoNombre)
        fila.addView(textos)
        fila.addView(interruptor)
        fila.addView(
            botonEliminar,
            LinearLayout.LayoutParams(dp(38), dp(48))
        )

        return fila
    }

    private fun pedirNombreAlarma() {
        val campoNombre = EditText(this).apply {
            hint = "Ejemplo: Vitamina"
            inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine(true)
        }

        AlertDialog.Builder(this)
            .setTitle("Nueva alarma")
            .setMessage("Escribe el nombre del medicamento.")
            .setView(campoNombre)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Elegir hora") { _, _ ->
                val nombre = campoNombre.text.toString().trim()

                if (nombre.isBlank()) {
                    Toast.makeText(
                        this,
                        "Escribe un nombre.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    elegirHoraNuevaAlarma(nombre)
                }
            }
            .show()
    }

    private fun elegirHoraNuevaAlarma(nombre: String) {
        val ahora = Calendar.getInstance()

        TimePickerDialog(
            this,
            { _, hora, minuto ->
                guardarNuevaAlarma(nombre, hora, minuto)
            },
            ahora.get(Calendar.HOUR_OF_DAY),
            ahora.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun guardarNuevaAlarma(nombre: String, hora: Int, minuto: Int) {
        val id = alarmasRef.push().key

        if (id == null) {
            Toast.makeText(
                this,
                "No se pudo crear la alarma.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val horaTexto = String.format(Locale.US, "%02d:%02d", hora, minuto)

        val datosAlarma = mapOf(
            "hora" to horaTexto,
            "nombre" to nombre,
            "activa" to true
        )

        alarmasRef.child(id).setValue(datosAlarma)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Alarma agregada.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { error ->
                mostrarError("No se pudo agregar", error)
            }
    }

    private fun elegirHoraAlarma(alarma: Alarma) {
        val partesHora = alarma.hora.split(":")
        val horaActual = partesHora.getOrNull(0)?.toIntOrNull() ?: 8
        val minutoActual = partesHora.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(
            this,
            { _, hora, minuto ->
                val horaNueva =
                    String.format(Locale.US, "%02d:%02d", hora, minuto)

                alarmasRef.child(alarma.id)
                    .child("hora")
                    .setValue(horaNueva)
                    .addOnFailureListener { error ->
                        mostrarError("No se pudo cambiar la hora", error)
                    }
            },
            horaActual,
            minutoActual,
            true
        ).show()
    }

    private fun confirmarEliminacion(alarma: Alarma) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar alarma")
            .setMessage("¿Eliminar '${alarma.nombre}' de las ${alarma.hora}?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                alarmasRef.child(alarma.id).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Alarma eliminada.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { error ->
                        mostrarError("No se pudo eliminar", error)
                    }
            }
            .show()
    }

    private fun mostrarError(accion: String, error: Exception) {
        Toast.makeText(
            this,
            "$accion: ${error.localizedMessage}",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()

        if (::alarmasRef.isInitialized) {
            alarmasRef.removeEventListener(listenerAlarmas)
        }

        if (::historialRef.isInitialized) {
            historialRef.removeEventListener(listenerHistorial)
        }

        referenciaConexion.removeEventListener(listenerConexion)
    }
}