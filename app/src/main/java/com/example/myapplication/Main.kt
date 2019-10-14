package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient.FileChooserParams.parseResult
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.exitProcess
import java.io.*


class Main : AppCompatActivity() {

    companion object {
        //global
    }

    private lateinit var pref: SharedPreferences

    private val APP_PREFERENCES = "settings"
    private val APP_PREFERENCES_IP = "ip"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT > 9) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }

        val url = URL(getUrlfromStorage())
        GetAsyncTask().execute(url)

        //fire
        val switch4:Switch = findViewById(R.id.switch4)
        switch4.setOnCheckedChangeListener { buttonView, isChecked ->
            onSwithChange("fire_alarm", switch4.isChecked)
        }

        //
        val switch3:Switch = findViewById(R.id.switch3)
        switch3.setOnCheckedChangeListener { buttonView, isChecked ->
            onSwithChange("electro_alarm", switch3.isChecked)
        }

        //
        val switch2:Switch = findViewById(R.id.switch2)
        switch2.setOnCheckedChangeListener { buttonView, isChecked ->
            onSwithChange("secure_alarm", switch2.isChecked)
        }


        val switch1:Switch = findViewById(R.id.switch1)
        switch1.setOnCheckedChangeListener { buttonView, isChecked ->
            onSwithChange("water_alarm", switch1.isChecked)
        }
    }

    private fun getUrlfromStorage (): String? {
        //data from DB
        pref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE)

        if (pref.contains(APP_PREFERENCES_IP)) {
            // Получаем IP из настроек
            var ip = pref.getString(APP_PREFERENCES_IP, null)
            // Выводим на экран данные из настроек
            textView6.text = ip
            textView9.text = "Connecting..."

            return "http://$ip:8080/"

        } else {
            val UpdateButton = Toast.makeText(this, "IP not set. Please go to SETTINGS!", Toast.LENGTH_LONG)
            UpdateButton.show()
            textView6.text = "No IP"
            textView9.text = "No connection"
            return "http://127.0.0.1/"
        }
    }



    /**
     * updating status from Server
     */
    fun updateMe  (view: View) {

        progressBar.visibility = View.GONE

        val UpdateButton = Toast.makeText(this, "Reload...", Toast.LENGTH_SHORT )
        UpdateButton.show()

        val url = URL(getUrlfromStorage())
        GetAsyncTask().execute(url)
    }

    /**
     * parsing resilts
     */
    fun parseResult (data:String) {

        if(!data.isNullOrEmpty()){
            //check server response
            try {
                //Debug.text = data
                val jsonObject = JSONObject(data)
                switch1.isChecked = jsonObject["water_alarm"].toString().toInt() > 0
                switch4.isChecked = jsonObject["fire_alarm"].toString().toInt() > 0
                switch3.isChecked = jsonObject["electro_alarm"].toString().toInt() > 0
                switch2.isChecked = jsonObject["secure_alarm"].toString().toInt() > 0

                textView9.text = "Connected"
            } catch (ex: Exception) {
                textView9.text = "Bad server response!"
            }


        } else {
            switch1.isChecked = false
            switch4.isChecked = false
            switch3.isChecked = false
            switch2.isChecked = false
            textView9.text = "Server offline!"
        }

        progressBar.visibility = View.GONE
    }

    /**
     * start settings
     */
    fun settings (view: View) {
        //redirect to settings
        val settings = Intent(this, Settings::class.java)
        startActivity(settings)
    }


    fun onSwithChange(name: String, value: Boolean) {

        textView9.text = "Connecting..."

        val url = URL(getUrlfromStorage())

        //POST
        PostAsyncTask().execute (url.toString(), name, value.toString())

        //GET
        GetAsyncTask().execute(url)
    }

    //--------------------------------------------------------------------
    //POST
    inner class PostAsyncTask : AsyncTask<String, String, String>() {

        private var result: String = ""

        override fun onPreExecute() {
            progressBar.visibility = View.VISIBLE
            super.onPreExecute()
        }

        override fun doInBackground(vararg params: String): String {

            var conn: HttpURLConnection? = null

            try {
                //POST
                sendForm (params[0], params[1], params[2].toBoolean())

            } catch (ex: Exception) {
                Log.d("", "Error in doInBackground " + ex.message)
            } finally {
                conn?.disconnect()
            }
            return result
        }

        override fun onProgressUpdate(vararg values: String?) { }

        override fun onPostExecute(result: String?) {
            // Done
            super.onPostExecute(result)
            progressBar.visibility = View.GONE
        }
    }
//--------------------------------------------------------------------

    /**
     * specific Bool var
     */
    fun Boolean.toInt() = if (this) 1 else 0

    /**
     * POST method
     */
    fun sendForm(urlRow: String, key: String, value: Boolean ) {
        var result = ""

        try {
            val value = value.toInt()
            val urlParameters = "$key=$value"
            val url = URL(urlRow)
            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 1000
            conn.connectTimeout = 1000
            conn.doOutput = true

            val writer = OutputStreamWriter(conn.getOutputStream())
            writer.write(urlParameters)
            writer.flush()
            val reader = BufferedReader(InputStreamReader(conn.getInputStream()))
            reader.lineSequence().forEach {
                println(it)
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
            //set buttons to OFF
            parseResult(result)
        }
    }






    //---------------------------------------------------------------------------------
    //GET
    // AsyncTask inner class
    inner class GetAsyncTask : AsyncTask<URL, Int, String>() {

        private var result: String = ""

        override fun onPreExecute() {
            super.onPreExecute()
            progressBar.visibility = View.VISIBLE
        }


        override fun doInBackground(vararg params: URL?): String {

            var connect: HttpURLConnection? = null
            try {
                connect = params[0]?.openConnection() as HttpURLConnection
                connect.readTimeout = 4000
                connect.connectTimeout = 4000
                connect.requestMethod = "GET"
                connect.connect()

                val responseCode: Int = connect.responseCode
                if (responseCode == 200) {
                    result = streamToString(connect.inputStream)
                }

            }
            catch (ex: Exception) {
                Log.d("", "Error in doInBackground " + ex.message)
            } finally {
                if (connect != null) {
                    connect.disconnect()
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            //send result to parser
            parseResult(result.toString())
        }
    }

    fun streamToString(inputStream: InputStream): String {

        val bufferReader = BufferedReader(InputStreamReader(inputStream))
        var line: String
        var result = ""

        try {
            do {
                line = bufferReader.readLine()
                if (line != null) {
                    result += line
                }
            } while (line != null)
            inputStream.close()
        } catch (ex: Exception) {

        }
        return result
    }

    /**
     * EXIT
     */
    fun exitApp (view: View) {
        moveTaskToBack(true)
        exitProcess(-1)
    }

}
