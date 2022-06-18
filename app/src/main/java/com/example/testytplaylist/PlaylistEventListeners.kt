package com.example.testytplaylist

import android.util.Log
import com.google.android.youtube.player.YouTubePlayer.PlaylistEventListener


private class MyPlaylistEventListener : PlaylistEventListener {
    val TAG: String = "PlaylistEventListener"

    override fun onNext() {
        Log.d(TAG, "NEXT VIDEO")
    }

    override fun onPrevious() {
        Log.d(TAG, "PREVIOUS VIDEO")
    }

    override fun onPlaylistEnded() {
        Log.d(TAG, "PLAYLIST ENDED")
    }
}