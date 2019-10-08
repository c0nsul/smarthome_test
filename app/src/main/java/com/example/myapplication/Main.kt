package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.exitProcess
import android.R.attr.data
import android.webkit.WebChromeClient.FileChooserParams.parseResult
import android.widget.Button
import android.widget.Switch
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


        if (android.os.Build.VERSION.SDK_INT > 9) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }

        val url = URL(getUrlfromStorage())
        MyAsyncTask().execute(url)

        //fire
        val switch4:Switch = findViewById(R.id.switch4)
        switch4.setOnClickListener {
            onSwithChange("fire_alarm", switch4.isChecked)
        }

        //
        val switch3:Switch = findViewById(R.id.switch3)
        switch3.setOnClickListener {
            onSwithChange("electro_alarm", switch4.isChecked)
        }

        //
        val switch2:Switch = findViewById(R.id.switch2)
        switch2.setOnClickListener {
            onSwithChange("secure_alarm", switch4.isChecked)
        }

        //
        val switch1:Switch = findViewById(R.id.switch1)
        switch1.setOnClickListener {
            //Debug.text = switch1.isChecked.toString()
            onSwithChange("water_alarm", switch4.isChecked)
        }

    }

    private fun getUrlfromStorage (): String? {
        //data from DB
        pref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE)

        if (pref.contains(APP_PREFERENCES_IP)) {
            // Получаем число из настроек
            var ip = pref.getString(APP_PREFERENCES_IP, null)
            // Выводим на экран данные из настроек
            textView6.text = ip
            textView9.text = "Connected"

            return "http://$ip:8080/"

        } else {
            val UpdateButton = Toast.makeText(this, "IP not set. Please go to SETTINGS!", Toast.LENGTH_LONG)
            UpdateButton.show()
            textView6.text = "no IP"
            textView9.text = "no connection"
            return "http://127.0.0.1/"
        }
    }


    /**
     * updating status from Server
     */
    fun updateMe  (view: View) {

        val UpdateButton = Toast.makeText(this, "Updating...", Toast.LENGTH_SHORT)
        UpdateButton.show()

        val url = URL(getUrlfromStorage())
        MyAsyncTask().execute(url)
    }

    /**
     * parsing resilts
     */
    fun parseResult (data:String) {

        if(!data.isNullOrEmpty()){
            val jsonObject = JSONObject(data)
            switch1.isChecked = jsonObject["water_alarm"].toString().toInt() > 0
            switch4.isChecked = jsonObject["fire_alarm"].toString().toInt() > 0
            switch3.isChecked = jsonObject["electro_alarm"].toString().toInt() > 0
            switch2.isChecked = jsonObject["secure_alarm"].toString().toInt() > 0
            //Debug.text = jsonObject["secure_alarm"].toString()
        }
    }

    /**
     * start settings
     */
    fun settings (view: View) {
        //redirect to settings
        val settings = Intent(this, Settings::class.java)
        startActivity(settings)
    }


    fun onSwithChange(name: String, value: Boolean?) {
        val url = URL(getUrlfromStorage())
        MyAsyncTask().execute(url)
    }

    //POST
    inner class GetAsyncTaskPost : AsyncTask<String, String, String>() {

        override fun onPreExecute() {
            super.onPreExecute()
            progressBar.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpURLConnection? = null

            try {
                val url = URL(urls[0])

                urlConnection = url.openConnection() as HttpURLConnection


                var inString = streamToString(urlConnection.inputStream)

                publishProgress(inString)

            } catch (ex: Exception) {

            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect()
                }
            }

            return " "
        }

        override fun onProgressUpdate(vararg values: String?) {

            /*
            try {
                var json = JSONObject(values[0])

                val query = json.getJSONObject("query")
                val results = query.getJSONObject("results")
                val channel = results.getJSONObject("channel")

                val location = channel.getJSONObject("location")
                val city = location.get("city")
                val country = location.get("country")

                val humidity = channel.getJSONObject("atmosphere").get("humidity")
                val condition = channel.getJSONObject("item").getJSONObject("condition")
                val temp = condition.get("temp")
                val text = condition.get("text")

                tvWeatherInfo.text =
                    "Location: " + city + " - " + country + "\n" +
                            "Humidity: " + humidity + "\n" +
                            "Temperature: " + temp + "\n" +
                            "Status: " + text

            } catch (ex: Exception) {

            }
            */
        }

        override fun onPostExecute(result: String?) {
            // Done
            super.onPostExecute(result)
            progressBar.visibility = View.GONE
        }


    }


    //GET

    // AsyncTask inner class
    inner class MyAsyncTask : AsyncTask<URL, Int, String>() {

        private var result: String = ""

        override fun onPreExecute() {
            super.onPreExecute()
            progressBar.visibility = View.VISIBLE
        }


        override fun doInBackground(vararg params: URL?): String {
            try {
                val connect = params[0]?.openConnection() as HttpURLConnection
                connect.readTimeout = 8000
                connect.connectTimeout = 8000
                connect.requestMethod = "GET"
                connect.connect()

                val responseCode: Int = connect.responseCode
                if (responseCode == 200) {
                    result = streamToString(connect.inputStream)
                }
            } catch (ex: Exception) {
                Log.d("", "Error in doInBackground " + ex.message)
            }
            return result
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            progressBar.visibility = View.GONE
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
    override fun finish () {
        moveTaskToBack(true)
        exitProcess(-1)
    }

}
