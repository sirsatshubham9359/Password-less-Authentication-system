package com.my.campusauthapp

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.net.NetworkInterface
import java.util.Collections
import java.util.UUID

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etFullName = findViewById<EditText>(R.id.et_full_name)
        val etRollNo = findViewById<EditText>(R.id.et_roll_no)
        val etMobileNo = findViewById<EditText>(R.id.et_mobile_no)
        val etIpAddress = findViewById<EditText>(R.id.et_device_ip)

        val currentIp = getMobileIPAddress()
        etIpAddress.setText("Device IP: $currentIp")

        findViewById<Button>(R.id.btn_save_fingerprint).setOnClickListener {
            val name = etFullName.text.toString()
            val roll = etRollNo.text.toString()
            val mobile = etMobileNo.text.toString()

            if (name.isNotEmpty() && roll.isNotEmpty() && mobile.isNotEmpty()) {
                promptFingerprintRegistration(name, roll, mobile, currentIp)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun promptFingerprintRegistration(name: String, roll: String, mobile: String, ip: String) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                saveRegistrationData(name, roll, mobile, ip)
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(applicationContext, "Fingerprint Required", Toast.LENGTH_SHORT).show()
            }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Secure Registration")
            .setSubtitle("Touch the sensor to link your biometric data to this device.")
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(info)
    }

    private fun saveRegistrationData(name: String, roll: String, mobile: String, ip: String) {
        val prefs = getSharedPreferences("CampusAuthPrefs", Context.MODE_PRIVATE)
        val deviceUUID = UUID.randomUUID().toString()

        prefs.edit().apply {
            putString("FULL_NAME", name)
            putString("ROLL_NO", roll)
            putString("MOBILE_NO", mobile)
            putString("DEVICE_IP", ip)
            putString("DEVICE_UUID", deviceUUID)
            apply()
        }

        Toast.makeText(this, "Registration Complete!", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun getMobileIPAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr.hostAddress.indexOf(':') < 0) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (ex: Exception) { ex.printStackTrace() }
        return "IP_NOT_FOUND"
    }
}