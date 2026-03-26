package com.my.campusauthapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ScannerActivity : AppCompatActivity() {
    private lateinit var codeScanner: CodeScanner
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        }

        val scannerView = findViewById<CodeScannerView>(R.id.scanner_view)
        codeScanner = CodeScanner(this, scannerView)

        codeScanner.decodeCallback = DecodeCallback { result ->
            runOnUiThread {
                if (result.text.contains("session=")) {
                    val sessionId = result.text.substringAfter("session=")
                    triggerBiometrics(sessionId)
                }
            }
        }
    }

    private fun triggerBiometrics(sessionId: String) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                sendAuthToServer(sessionId)
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                codeScanner.startPreview()
            }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Campus Login")
            .setSubtitle("Verify fingerprint to authenticate")
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(info)
    }

    private fun sendAuthToServer(sessionId: String) {
        val prefs = getSharedPreferences("CampusAuthPrefs", Context.MODE_PRIVATE)
        val rollNo = prefs.getString("ROLL_NO", "")

        if (rollNo.isNullOrEmpty()) {
            runOnUiThread {
                Toast.makeText(this, "Please Register Device First", Toast.LENGTH_LONG).show()
                finish()
            }
            return
        }

        val json = JSONObject().apply {
            put("sessionId", sessionId)
            put("rollNo", rollNo)
            put("deviceId", prefs.getString("DEVICE_UUID", ""))
            put("currentIp", prefs.getString("DEVICE_IP", ""))
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://nonlacteally-vengeful-melodie.ngrok-free.dev/api/verify-scan")// Change to physical computer IP for real testing
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ScannerActivity, "Connection Failed", Toast.LENGTH_SHORT).show()
                    codeScanner.startPreview()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@ScannerActivity, "Login Success!", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onResume() { super.onResume(); if (::codeScanner.isInitialized) codeScanner.startPreview() }
    override fun onPause() { if (::codeScanner.isInitialized) codeScanner.releaseResources(); super.onPause() }
}