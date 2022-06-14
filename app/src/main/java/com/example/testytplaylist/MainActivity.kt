package com.example.testytplaylist

//import androidx.appcompat.app.AppCompatActivity
//import android.view.View

import android.annotation.SuppressLint
import android.net.Uri
import android.os.AsyncTask
//import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.PlaylistListResponse
import java.io.IOException
import java.util.concurrent.Executors


/*
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}

 */


/*
class MainActivity : YouTubeBaseActivity() {

    // Change the AppCompactActivity to YouTubeBaseActivity()

    // Add the api key that you had
    // copied from google API
    // This is a dummy api key
    private val apiKey =  "AIzaSyCL91bZwoiKhAacW5uMW0RLGLU2ilFzotY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get reference to the view of Video player
        val ytPlayer = findViewById<YouTubePlayerView>(R.id.ytPlayer)

        ytPlayer.initialize(apiKey, object : YouTubePlayer.OnInitializedListener{
            // Implement two methods by clicking on red error bulb
            // inside onInitializationSuccess method
            // add the video link or the
            // playlist link that you want to play
            // In here we also handle the play and pause functionality
            override fun onInitializationSuccess(
                provider: YouTubePlayer.Provider?,
                player: YouTubePlayer?,
                p2: Boolean
            ) {
                player?.loadVideo("dQw4w9WgXcQ")
                //player?.loadVideo("HzeK7g8cD0Y")
                player?.play()
            }

            // Inside onInitializationFailure
            // implement the failure functionality
            // Here we will show toast
            override fun onInitializationFailure(
                p0: YouTubePlayer.Provider?,
                p1: YouTubeInitializationResult?
            ) {
                Toast.makeText(this@MainActivity , "Video player Failed" , Toast.LENGTH_SHORT).show()
            }
        })

        val loginButton: Button = findViewById(R.id.login)
        loginButton.setOnClickListener {
            val ytActivity = BaseYoutubePlaylistActivity()
            ytActivity.signIn()
        }
        //findViewById<View>(R.id.login).setOnClickListener(View.OnClickListener() {
        //    fun onClick(view: View?) {
        //        signIn()
        //    }
        //})

        //val playlist = PlaylistList()
        //playlist.printPlaylists(applicationContext)
    }
}
 */

public open class MainActivity : BaseYoutubePlaylistActivity() {
    private val APPLICATION_NAME = "YouTubePlaylist Checker"

    /**
     * Global instance of the HTTP transport.
     */
    private val HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport()

    /**
     * Global instance of the JSON factory.
     */
    private val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()

    private var mYtInst: YouTube? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
    }

    private fun listPlaylists() {
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
    }


    /*
    **TODO
    fun listPlaylists() {
        // Print the names and IDs for up to 10 files.
        val result: FileList = mYtInst.files().list()
            .setPageSize(10)
            .setFields("nextPageToken, files(id, name)")
            .execute()
        val files: List<File> = result.getFiles()
        if (files == null || files.isEmpty()) {
            println("No files found.")
        } else {
            println("Files:")
            for (file in files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId())
            }
        }
    }
     */

    override fun onYtClientReady(displayName: String?, email: String?, avatar: Uri?) {
        // Build a new authorized API client service.
        mYtInst = YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredential())
            .setApplicationName(APPLICATION_NAME)
            .build()
        /*
        ** TODO
        try {
            listPlaylists()
        } catch (e: IOException) {
            e.printStackTrace()
        }
         */
    }

}