package com.example.testyni

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.BoringLayout
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.net.Socket


class MainActivity : AppCompatActivity() {

    lateinit var broadCastReceiver: BroadcastReceiver
    var pressed = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    val filter = IntentFilter()
    filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION )
    broadCastReceiver = object : BroadcastReceiver() {
        // listen for wifi connectivity
        override fun onReceive(contxt: Context?, intent: Intent?) {
            val action = intent!!.action
            Log.d("Info1", "action is $action")
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION )) {
                    if(pressed) {
                        Log.d("Info1", "Some Wifi connected")
                        val scnObj = IntentIntegrator(this@MainActivity)
                        scnObj.initiateScan()
                        pressed = false
                    }

            }
        }

    }
    registerReceiver(broadCastReceiver, filter)
    setContentView(R.layout.activity_main)
    scan_qr.setOnClickListener {
        if(isInternetAvailable(applicationContext)){
            val scnObj = IntentIntegrator(this@MainActivity)
            scnObj.initiateScan()
        }
        else{
            pressed = true
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == Activity.RESULT_OK){
            val result =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null) {
                if (result.contents == null) {
                    Toast.makeText(this, "Request Cancelled", Toast.LENGTH_LONG).show()
                } else {
                    val qr_data = result.contents
                    Toast.makeText(this, "Successfully Scanned $qr_data", Toast.LENGTH_SHORT)
                        .show()
                    Thread {
                        val response = sendToServer(qr_data)
                        runOnUiThread {
                            if(response){
                                Toast.makeText(this, "QR code sent Successfully", Toast.LENGTH_LONG).show()

                            }
                            else{
                                Toast.makeText(this, "Something went wrong check TCP server", Toast.LENGTH_LONG).show()

                            }
                        }

                    }.start()

                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadCastReceiver)
    }

    fun sendToServer(data: String): Boolean{
        // send input data to TCP server over socket
        try{
            val client = Socket("192.168.8.121", 4097)
            client.outputStream.write("{'data': $data}".toByteArray())
            client.close()
            return true

        }catch (e:Exception){
            Log.d("Info1", "Error : ${e.message}")
            return false
        }

    }


    private fun isInternetAvailable(context: Context): Boolean {
        // checks weather internet is available on device or not
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }

                }
            }
        }

        return result
    }
}
