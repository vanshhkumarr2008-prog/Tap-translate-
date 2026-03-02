package com.vansh.taptranslatepro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val input = findViewById<EditText>(R.id.inputText)
        val translateBtn = findViewById<Button>(R.id.btnTranslate)
        val proBtn = findViewById<Button>(R.id.proButton)

        translateBtn.setOnClickListener {
            val text = input.text.toString()
            input.setText("Translated: $text")
        }

        proBtn.setOnClickListener {
            startActivity(Intent(this, ProActivity::class.java))
        }
    }
}
