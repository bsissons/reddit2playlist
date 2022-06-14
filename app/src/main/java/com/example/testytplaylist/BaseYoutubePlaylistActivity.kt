package com.example.testytplaylist

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import java.util.*
import kotlin.collections.HashSet

abstract class BaseYoutubePlaylistActivity : AppCompatActivity() {
    private val TAG = "BaseYtPlaylistActivity"
    private val SCOPE_FILE = Scope("https://www.googleapis.com/auth/youtube")
    //private val SCOPE_FILE = Scope("https://www.googleapis.com/auth/drive.file")
    //private val SCOPE_APPFOLDER: Scope = Scope("https://www.googleapis.com/auth/drive.appdata")
    //private val SCOPE_READONLY: Scope = Scope("https://www.googleapis.com/auth/youtube.readonly")

    //Request code for Google Sign-in

    private val REQUEST_CODE_SIGN_IN = 1

    private var mToken: String? = null
    private var mCredential: GoogleAccountCredential? = null

    override fun onStart() {
        super.onStart()
        signIn(false)
    }

    // Handles resolution callbacks.
    //val getContent = registerForActivityResult(GetContent()) { uri: Uri? ->
    //    // Handle the returned Uri
    //}

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (resultCode != RESULT_OK) {
                // Sign-in may fail or be cancelled by the user. For this sample, sign-in is
                // required and is fatal. For apps where sign-in is optional, handle
                // appropriately
                Log.e(TAG, "Sign-in failed :/ 1 $resultCode")
                return
            }
            val getAccountTask: Task<GoogleSignInAccount> =
                GoogleSignIn.getSignedInAccountFromIntent(data)
            if (getAccountTask.isSuccessful) {
                initializeYtClient(getAccountTask.result)
            } else {
                Log.e(TAG, "Sign-in failed. 2")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    // Starts the sign-in process and initializes the Drive client.

    fun signIn(clean: Boolean) {
        val requiredScopes: MutableSet<Scope> = HashSet(2)
        requiredScopes.add(SCOPE_FILE)
        //requiredScopes.add(SCOPE_READONLY)
        val signInAccount = GoogleSignIn.getLastSignedInAccount(this)
        if ( !clean && signInAccount != null && signInAccount.grantedScopes.containsAll(requiredScopes)) {
            Log.d(TAG, "Last sign in is NOT null")
            initializeYtClient(signInAccount)
        } else {
            Log.d(TAG, "Last sign in is null")
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(SCOPE_FILE)
                .requestEmail()
                //.requestScopes(SCOPE_READONLY)
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, signInOptions)
            Log.d(TAG, "DDDD start activity for result")
            startActivityForResult(googleSignInClient.signInIntent, REQUEST_CODE_SIGN_IN)
        }
    }

    protected fun checkSignedIn(initClient: Boolean): Boolean {
        val requiredScopes: MutableSet<Scope> = HashSet(2)
        requiredScopes.add(SCOPE_FILE)
        //requiredScopes.add(SCOPE_READONLY)
        val signInAccount = GoogleSignIn.getLastSignedInAccount(this)
        return if (signInAccount != null && signInAccount.grantedScopes.containsAll(requiredScopes)) {
            if (initClient) {
                initializeYtClient(signInAccount)
            }
            true
        } else {
            false
        }
    }

    fun signOut() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(SCOPE_FILE)
            //.requestScopes(SCOPE_READONLY)
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, signInOptions)
        googleSignInClient.signOut()
    }

    // Continues the sign-in process, initializing the Drive clients with the current
    // user's account.
    private fun initializeYtClient(signInAccount: GoogleSignInAccount) {
        Log.d(TAG, "initializeYtClient")
        mCredential = GoogleAccountCredential.usingOAuth2(
            this,
            Collections.singleton(SCOPE_FILE.scopeUri)
        )
        mCredential?.selectedAccount = signInAccount.account
        Log.d(TAG, "DDDD the signInAccount is: " + signInAccount.account + " display:"
                + signInAccount.displayName
                + " email:" + signInAccount.email)
        onYtClientReady(signInAccount.displayName, signInAccount.email, signInAccount.photoUrl)
    }

    fun getCredential(): GoogleAccountCredential? {
        return mCredential
    }

    protected fun getToken(): String? {
        return mToken
    }

    // Called after the user has signed in and the Drive client has been initialized.
    protected abstract fun onYtClientReady(displayName: String?, email: String?, avatar: Uri?)

}