package com.example.testytplaylist

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.youtube.player.*
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.PlaylistListResponse
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.Executors

open class MainActivity : BaseYoutubePlaylistActivity() {
    private val APPLICATION_NAME = "YouTubePlaylist Checker"

    private val PLAYLIST_PATH = "https://www.youtube.com/watch_videos?video_ids=AwyRYse4kss,QoitiIbdeaM,drlB2RT_XiA"

    // Global instance of the HTTP transport.
    private val HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport()
    // * Global instance of the JSON factory.
    private val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()

    private val API_KEY =  "AIzaSyCL91bZwoiKhAacW5uMW0RLGLU2ilFzotY"

    private var mYtInst: YouTube? = null
    private var playlistUrl: String? = null

    private var youtubeFragment: YouTubePlayerSupportFragmentX? = null
    private val youtubeVideoArrayList: ArrayList<String>? = null
    //youtube player to play video when new video selected
    private var youTubePlayer: YouTubePlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //createStandalonePlayer()
        initializeYoutubePlayer()

        val loginButton: Button = findViewById(R.id.login)
        loginButton.setOnClickListener {
            signIn(true)
        }
        val playlistButton: Button = findViewById(R.id.list_playlist)
        playlistButton.setOnClickListener {
            try {
                listPlaylists()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
        val debugButton: Button = findViewById(R.id.test_button)
        debugButton.setOnClickListener {
            runDebugButton()
        }
    }

    private fun runDebugButton() {
        try {
            val obj = JSONObject(loadJSONFromAsset())
            //val obj = JSONObject("")
            val userArray = obj.getJSONArray("items")
            for (i in 0 until userArray.length()) {
                val playlistDetail = userArray.getJSONObject(i)
                val snippet = playlistDetail.getJSONObject("snippet")
                val title = snippet.getString("title")
                Log.d("MAIN", "The title of the playlist is: $title")
                //personName.add(userDetail.getString("name"))
                //emailId.add(userDetail.getString("email"))
                //val contact = userDetail.getJSONObject("contact")
                //mobileNumbers.add(contact.getString("mobile"))
            }
        }
        catch (e: JSONException) {
            e.printStackTrace()
        }

        // Final URL will look like: https://m.youtube.com/watch?v=AwyRYse4kss&list=TLGGShmZwWHrpk0xNjA2MjAyMg
        getRedirectUrl(PLAYLIST_PATH) { result: String -> playlistUrl = result }

    }

    private fun createStandalonePlayer() {
        val intent =
            YouTubeStandalonePlayer.createPlaylistIntent(this, API_KEY, "RDCLAK5uy_k5n4srrEB1wgvIjPNTXS9G1ufE9WQxhnA")
            //YouTubeStandalonePlayer.createVideoIntent(this, API_KEY, "gHnuQZFxHt0")
        startActivity(intent) // https://www.youtube.com/watch?v=CIMmK86vNYo&list=TLGGXcjgW8GO36YxNjA2MjAyMg&
    }

    private fun initializeYoutubePlayer() {
        youtubeFragment = ((supportFragmentManager.findFragmentById(R.id.youtubeFragment) as YouTubePlayerSupportFragmentX?)
            ?: return)
        youtubeFragment!!.initialize(
            API_KEY,
            object : YouTubePlayer.OnInitializedListener {
                override fun onInitializationSuccess(
                    provider: YouTubePlayer.Provider?,
                    player: YouTubePlayer?,
                    wasRestored: Boolean
                ) {
                    if (!wasRestored) {
                        youTubePlayer = player
                        //set the player style default
                        youTubePlayer?.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT)
                        //cue the 1st video by default
                        youTubePlayer?.loadVideo("dQw4w9WgXcQ")
                    }
                }

                // Inside onInitializationFailure
                // implement the failure functionality
                // Here we will show toast
                override fun onInitializationFailure(
                    p0: YouTubePlayer.Provider?,
                    p1: YouTubeInitializationResult?
                ) {
                    Toast.makeText(this@MainActivity , "Video player Failed" , Toast.LENGTH_LONG).show()
                }
            })

    }

    private fun listPlaylists() {
        /*
        if (mYtInst == null) {
            Toast.makeText(this@MainActivity , "Unable to contact Youtube service"
                , Toast.LENGTH_LONG).show()
            return
        }

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            /*
            * Your task will be executed here
            * you can execute anything here that
            * you cannot execute in UI thread
            * for example a network operation
            * This is a background thread and you cannot
            * access view elements here
            *
            * its like doInBackground()
            * */

            // Define and execute the API request
            val request: YouTube.Playlists.List = mYtInst!!.playlists()
                .list(mutableListOf("snippet,contentDetails"))
            val response: PlaylistListResponse = request.setMaxResults(25L)
                .setMine(true)
                .execute()
            println("DDDD")
            println(response)
            //handler.post {
                /*
                * You can perform any operation that
                * requires UI Thread here.
                *
                * its like onPostExecute()
                * */
            //}
        }
        */
        if (playlistUrl == null) {
            getRedirectUrl(PLAYLIST_PATH) { result: String -> playlistUrl = result }
        }

        if (playlistUrl != null) {
            Log.d("MAIN", "DDDD current url: $playlistUrl")
            val listTag = getListTag(playlistUrl!!)
            youTubePlayer?.loadPlaylist(listTag)
            youTubePlayer?.play()
            playlistUrl = null
        } else {
            Toast.makeText(this@MainActivity , "Failed to get playlist" , Toast.LENGTH_LONG).show()
        }
    }

    private fun getListTag(url: String) : String {
        val regex = ".*?&list=([^&]*)&?".toRegex()
        val matchResult = regex.find(url)
        if (matchResult != null) {
            val (result) = matchResult.destructured
            Log.d("MAIN", "DDDD matchResult $result")
            return result
        }
        throw IllegalArgumentException("Unable to match list from $url")
    }

    override fun onYtClientReady(displayName: String?, email: String?, avatar: Uri?) {
        // Build a new authorized API client service.
        mYtInst = YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredential())
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    private fun loadJSONFromAsset(): String {
        val json: String?
        try {
            //val inputStream = assets.open("sample.json")
            val inputStream = resources.assets.open("sample.json")
            //val inputStream = Resources.getResource("sample.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            val charset = Charsets.UTF_8
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer, charset)
        }
        catch (ex: IOException) {
            ex.printStackTrace()
            return ""
        }

        return json
    }

    private fun getRedirectUrl(
        url:String,
        callback: (String) -> Unit
    ) {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            var urlTmp: URL? = null
            var connection: HttpURLConnection? = null
            try {
                urlTmp = URL(url)
            } catch (e1: MalformedURLException) {
                e1.printStackTrace()
            }
            try {
                connection = urlTmp!!.openConnection() as HttpURLConnection
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                connection!!.responseCode
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val redUrl: String = connection!!.url.toString()
            connection.disconnect()
            Log.d("MAIN","DDDD url is $redUrl")
            callback(redUrl)
        }
        //handler.post {
        //    Log.d("Redirected URL",""+result)
        //}
        //return finalUrl
    }

}