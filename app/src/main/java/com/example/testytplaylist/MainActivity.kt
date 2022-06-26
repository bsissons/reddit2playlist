package com.example.testytplaylist

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.bumptech.glide.Glide
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerSupportFragmentX
import com.google.android.youtube.player.YouTubeStandalonePlayer
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


open class MainActivity : BaseYoutubePlaylistActivity() {
    private val APPLICATION_NAME = "YouTubePlaylist Checker"

    // Global instance of the HTTP transport.
    private val HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport()
    // * Global instance of the JSON factory.
    private val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()
    // YT player API key
    private val API_KEY =  "AIzaSyCL91bZwoiKhAacW5uMW0RLGLU2ilFzotY"
    // List of subreddits
    private val SUBREDDITS_LIST = "subreddits_sfw.list"

    // Instance for the Youtube login
    private var mYtInst: YouTube? = null
    // Fragment that holds the player
    private var youtubeFragment: YouTubePlayerSupportFragmentX? = null
    //youtube player to play video when new video selected
    private var youTubePlayer: YouTubePlayer? = null
    // Holds the playlist information
    private var youtubeVideoIds = mutableListOf<String>()
    private var youtubeVideoMap = hashMapOf<String, VideoInfo>()
    private var currentVideoIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //createStandalonePlayer()
        initializeYoutubePlayer()

        // Login to Google
        findViewById<Button>(R.id.login).setOnClickListener {
            signIn(true)
        }

        // Open in App
        findViewById<Button>(R.id.watch_in_app).setOnClickListener {
            // TODO open app
            Toast.makeText(this@MainActivity , "Coming soon!" , Toast.LENGTH_SHORT).show()
        }

        // Generate the playlist
        findViewById<Button>(R.id.gen_playlist).setOnClickListener {
            try {
                listPlaylists()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }

        // Callbacks for pressing the thumbnail images
        findViewById<ImageView>(R.id.prev_thumbnail).setOnClickListener {
            if (youTubePlayer?.hasPrevious() == true) {
                youTubePlayer?.previous()
            }
        }
        findViewById<ImageView>(R.id.current_thumbnail).setOnClickListener {
            youTubePlayer?.loadVideos(youtubeVideoIds, currentVideoIndex, 0)
        }
        findViewById<ImageView>(R.id.next_thumbnail).setOnClickListener {
            if (youTubePlayer?.hasNext() == true) {
                youTubePlayer?.next()
            }
        }

        // Set up the autocomplete field
        setAutoComplete()
    }

    private fun setAutoComplete() {
        val subredditInput : AutoCompleteTextView = findViewById(R.id.subreddit_input)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            populateSubreddits()
        )
        subredditInput.setAdapter(adapter)
        subredditInput.threshold = 3
    }

    private fun runDebugButton() {
        try {
            val obj = JSONObject(loadJSONFromAsset("sample.json"))
            //val obj = JSONObject("")
            val userArray = obj.getJSONArray("items")
            for (i in 0 until userArray.length()) {
                val playlistDetail = userArray.getJSONObject(i)
                val snippet = playlistDetail.getJSONObject("snippet")
                val title = snippet.getString("title")
                Log.d("MAIN", "The title of the playlist is: $title")
            }
        }
        catch (e: JSONException) {
            e.printStackTrace()
        }

        // Final URL will look like: https://m.youtube.com/watch?v=AwyRYse4kss&list=TLGGShmZwWHrpk0xNjA2MjAyMg
        //getRedirectUrl(PLAYLIST_PATH) { result: String -> playlistUrl = result }
    }

    private fun createStandalonePlayer() {
        val intent =
            YouTubeStandalonePlayer.createPlaylistIntent(this, API_KEY, "RDCLAK5uy_k5n4srrEB1wgvIjPNTXS9G1ufE9WQxhnA")
            //YouTubeStandalonePlayer.createVideoIntent(this, API_KEY, "gHnuQZFxHt0")
        startActivity(intent) // https://www.youtube.com/watch?v=CIMmK86vNYo&list=TLGGXcjgW8GO36YxNjA2MjAyMg&
    }

    private fun videoChangeCallback() {
        Log.d("MAIN", "videoChangeCallback")
        if (currentVideoIndex > 0) {
            val prevVideoId = youtubeVideoIds[currentVideoIndex-1]
            Glide.with(this)
                .load(youtubeVideoMap[prevVideoId]?.getThumbnail())
                .error(R.drawable.ic_media_previous)
                .into(findViewById(R.id.prev_thumbnail))
        } else {
            val imageView = findViewById<ImageView>(R.id.prev_thumbnail)
            imageView.setImageResource(R.drawable.ic_media_previous)
        }

        val currentVideoId = youtubeVideoIds[currentVideoIndex]
        Glide.with(this)
            .load(youtubeVideoMap[currentVideoId]?.getThumbnail())
            .error(R.drawable.ic_media_play)
            .into(findViewById(R.id.current_thumbnail))

        if (currentVideoIndex+1 < youtubeVideoIds.size) {
            val nextVideoId = youtubeVideoIds[currentVideoIndex+1]
            Glide.with(this)
                .load(youtubeVideoMap[nextVideoId]?.getThumbnail())
                .error(R.drawable.ic_media_next)
                .into(findViewById(R.id.next_thumbnail))
        } else {
            val imageView = findViewById<ImageView>(R.id.next_thumbnail)
            imageView.setImageResource(R.drawable.ic_media_next)
        }

        if (currentVideoIndex == 0) {
            youTubePlayer?.loadVideos(youtubeVideoIds, currentVideoIndex, 0)
        }
    }

    private fun prevVideo() {
        if (currentVideoIndex > 0) {
            currentVideoIndex -= 1
            videoChangeCallback()
        }
    }

    private fun nextVideo() {
        if (currentVideoIndex+1 < youtubeVideoIds.size) {
            currentVideoIndex += 1
            videoChangeCallback()
        }
    }

    private val playlistEventListener: YouTubePlayer.PlaylistEventListener =
        object : YouTubePlayer.PlaylistEventListener {
            override fun onPrevious() {
                Log.d("MAIN", "DDDD previous video")
                prevVideo()
            }
            override fun onNext() {
                Log.d("MAIN", "DDDD next video")
                nextVideo()
            }
            override fun onPlaylistEnded() {
                // TODO restart or stop?
                Log.d("MAIN", "DDDD playlist over")
                //videoChangeCallback()
            }
        }

    private fun initializeYoutubePlayer() {
        youtubeFragment = ((supportFragmentManager.findFragmentById(R.id.youtube_fragment) as YouTubePlayerSupportFragmentX?)
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
                        // Set the callback for video change
                        youTubePlayer?.setPlaylistEventListener(playlistEventListener)
                        //cue the 1st video by default ;)
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

    private fun loadPlaylists() {
        Log.d("MAIN", "DDDD loadPlaylists")
        val isPlaying : Boolean = youTubePlayer?.isPlaying == true
        val isNotEmpty = youtubeVideoIds.isNotEmpty()
        if (isPlaying) {
            Log.d("MAIN", "DDDD is playing")
        } else {
            Log.d("MAIN", "DDDD is not playing")
        }
        if (isNotEmpty) {
            Log.d("MAIN", "DDDD is not empty")
        } else {
            Log.d("MAIN", "DDDD is empty")
        }
        if (isNotEmpty) {
            currentVideoIndex = 0
            //val listTag = getListTag(url)
            youTubePlayer?.loadVideos(youtubeVideoIds)
            youTubePlayer?.play()
            videoChangeCallback()
        }
    }

    private fun getRedditPosts() : Boolean {
        Log.d("MAIN", "DDDD getRedditPosts")
        var worked = false

        // If there is a JSON exception here then something went wrong (maybe a bad subreddit name)
        try {
            val subredditName = findViewById<TextView>(R.id.subreddit_input).text.toString()
            val obj = JSONObject(getJsonFromUrl("https://www.reddit.com/r/$subredditName.json"))
            val data = obj.getJSONObject("data")
            val childArray = data.getJSONArray("children")
            for (i in 0 until childArray.length()) {
                // Wrap this stuff in a different try block since we verified that some children
                // exist so it's a valid subreddit. If this try block fails then it's not a YT video
                // and we should try to continue
                try {
                    val post = childArray.getJSONObject(i)
                    val postData = post.getJSONObject("data")
                    val url = postData.getString("url_overridden_by_dest")
                    val title = postData.getString("title")
                    Log.d("MAIN", "DDDD The url of the video is: $url")
                    val secureMedia: JSONObject = postData.getJSONObject("secure_media")
                    val oembed: JSONObject = secureMedia.getJSONObject("oembed")
                    try {
                        val tag = getVideoTag(url)
                        val thumbnailUrl = oembed.getString("thumbnail_url")
                        //val thumbnailUrl = "https://img.youtube.com/vi/$tag/1.jpg"
                        Log.d("MAIN", "DDDD thumbnailUrl '$thumbnailUrl'")
                        youtubeVideoIds.add(tag)
                        youtubeVideoMap[tag] = VideoInfo(title, thumbnailUrl)
                        worked = true
                    } catch (e: IllegalArgumentException) {
                        Log.e("MAIN", "This is not a Yt video")
                    }
                } catch (e: JSONException) {
                    Log.e("MAIN", "This is not a Yt video")
                }
            }
        }
        catch (e: JSONException) {
            Log.e("MAIN", "Failed to extract json")
            e.printStackTrace()
        }
        return worked
    }

    private fun populateSubreddits(): MutableList<String> {
        val inputStream = resources.assets.open(SUBREDDITS_LIST)
        val size = inputStream.available()
        val buffer = ByteArray(size)
        val charset = Charsets.UTF_8
        inputStream.read(buffer)
        inputStream.close()
        val subreddits = String(buffer, charset)
        Log.d("MAIN", "DDDD about to return subreddit list")
        return subreddits.split("\n") as MutableList<String>
    }

    private fun listPlaylists() {
        youTubePlayer?.pause()
        val backupYoutubeVideoIds = youtubeVideoIds
        val backupYoutubeVideoMap = youtubeVideoMap
        youtubeVideoIds.clear()
        youtubeVideoMap.clear()
        if (getRedditPosts()) {
            loadPlaylists()
        } else {
            youtubeVideoIds = backupYoutubeVideoIds
            youtubeVideoMap = backupYoutubeVideoMap
            Toast.makeText(
                this@MainActivity,
                "Invalid subreddit name",
                Toast.LENGTH_SHORT
            ).show()
        }


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
        //if (playlistUrl == null) {
        //    getRedirectUrl(PLAYLIST_PATH) { result: String -> playlistUrl = result }
        //}

        //val newPlaylistUrls = getRedirectUrl(listOf(PLAYLIST_PATH))
        //newPlaylistUrls.collect {
        //        value: String ->
        //    loadPlaylists(value)
        //    youtubePlaylistIds.add(value)
        //}
    }

    private fun getListTag(url: String) : String {
        val regex = ".*?&list=([^&]*)&?".toRegex()
        val matchResult: MatchResult? = regex.find(url)
        if (matchResult != null) {
            val (result) = matchResult.destructured
            Log.d("MAIN", "DDDD matchResult $result")
            return result
        }
        throw IllegalArgumentException("Unable to match list from $url")
    }

    private fun getVideoTag(url: String) : String {
        Log.d("MAIN", "DDDD getVideoTag")
        val regex = ".*?(shorts|v=|youtu\\.be)/?([^&]*)&?.*".toRegex()
        val matchResult = regex.find(url)
        if (matchResult != null) {
            val (before, result) = matchResult.destructured
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

    private fun loadJSONFromAsset(fileName: String): String {
        val json: String?
        try {
            //val inputStream = assets.open("sample.json")
            val inputStream = resources.assets.open(fileName)
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

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun httpGet(inputUrl: String): String {
        val result: String = withContext(Dispatchers.IO) {
            var tmpString = ""
            val inputStream: InputStream
            // create URL
            val url =  URL(inputUrl)

            // create HttpURLConnection
            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection

            // make GET request to the given URL
            conn.connect()

            // receive response as inputStream
            inputStream = conn.inputStream

            // convert inputstream to string
            if(inputStream != null)
                tmpString = convertInputStreamToString(inputStream)
            tmpString
        }
        return result
    }

    private fun convertInputStreamToString(inputStream: InputStream): String {
        val stringBuffer = StringBuffer()
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            stringBuffer.append(line)
            Log.d("MAIN", "DDDD response: '$line'")
        }
        return stringBuffer.toString()
    }

    private fun getJsonFromUrl(url: String): String {
        Log.d("MAIN", "getJsonFromUrl - inputUrl=$url")
        //var jsonObj = JSONObject()
        val jsonString = runBlocking {
            httpGet(url)
        }
        //Log.d("MAIN", "jsonString: $jsonString")
        return jsonString
    }

    /*
    private suspend fun getJsonFromUrl(url: String) : JSONObject = withContext(Dispatchers.IO) {
        Log.d("MAIN", "getJsonFromUrl")
        val stringBuffer = StringBuffer()
        var urlTmp: URL? = null
        var connection: HttpURLConnection? = null

        try {
            urlTmp = URL(url)
        } catch (e1: MalformedURLException) {
            e1.printStackTrace()
        }
        try {
            connection = urlTmp!!.openConnection() as HttpURLConnection
            connection!!.responseCode
            //val bufferedReader = urlTmp.getInputStream().bufferedReader().use(BufferedReader::readText)
            val bufferedReader = BufferedReader(InputStreamReader(connection.getInputStream()))
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuffer.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        connection?.disconnect()
        return@withContext JSONObject(stringBuffer.toString())
        //emit(redUrl)
    }
     */

    /*
    private fun getRedirectUrl(url : String) : String = doInBackground {
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
        Log.d("MAIN", "DDDD url is $redUrl")
        emit(redUrl)

    }
     */

}