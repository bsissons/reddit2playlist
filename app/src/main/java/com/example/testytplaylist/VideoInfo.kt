package com.example.testytplaylist

class VideoInfo(title: String, thumbnailUrl: String) {
    private var title : String = ""
    private var thumbnailUrl : String = ""
    private var videoId : String = ""

    init {
        this.title = title
        this.thumbnailUrl = thumbnailUrl
    }

    fun getTitle() : String {
        if (title != "") {
            return title
        } else {
            throw IllegalStateException("Non-existing title found")
        }
    }

    fun getThumbnail() : String {
        if (thumbnailUrl != "") {
            return thumbnailUrl
        } else {
            throw IllegalStateException("Non-existing thumbnail found")
        }
    }

    fun getId() : String {
        if (videoId != "") {
            return videoId
        } else {
            throw IllegalStateException("Non-existing videoId found")
        }
    }
}