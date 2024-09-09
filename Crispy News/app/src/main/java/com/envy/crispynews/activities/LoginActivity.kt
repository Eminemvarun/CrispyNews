package com.envy.crispynews.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.envy.crispynews.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


class LoginActivity : AppCompatActivity() {

    private var firebaseAuth = FirebaseAuth.getInstance()
    lateinit var loginProgressBar: ProgressBar
    private lateinit var videoview: VideoView
    private lateinit var signInBTN: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        //Widgets
        videoview = findViewById(R.id.videoView)
        loginProgressBar = findViewById(R.id.loginProgress)
        signInBTN = findViewById(R.id.loginBTN)

        //Videoview setup
        val uri = Uri.parse("android.resource://" + packageName + "/" + R.raw.video_background)
        videoview.setVideoURI(uri)
        videoview.setOnPreparedListener { mp ->
            mp.isLooping = true
        }

        //Redirect if user is logged in already
        if (firebaseAuth.currentUser != null) {
            val intent: Intent = Intent(
                this@LoginActivity,
                MainActivity::class.java
            )
            startActivity(intent)
            finish()
        }
        //When User clicks sign in button
        signInBTN.setOnClickListener {
            loginProgressBar.visibility = View.VISIBLE
            firebaseAuth.signInAnonymously().addOnCompleteListener(this) {
                if (it.isSuccessful) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Log.e("ENVYLOGS", "signInAnonymously:failure", it.exception)
                    Toast.makeText(this, "Sign In Failed!", Toast.LENGTH_SHORT).show()
                }
                loginProgressBar.visibility = View.GONE
            }
        }
    }

    //Activity Result launcher after login to start main activity
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val signInAccount = accountTask.getResult(ApiException::class.java)
                    val authCredential =
                        GoogleAuthProvider.getCredential(signInAccount.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(authCredential)
                        .addOnSuccessListener { authResult ->
                            val firebaseUser = authResult.user
                            Toast.makeText(
                                this@LoginActivity,
                                "Welcome ${firebaseUser?.displayName}",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            overridePendingTransition(
                                androidx.appcompat.R.anim.abc_grow_fade_in_from_bottom,
                                androidx.appcompat.R.anim.abc_fade_out
                            )
                            finish()
                        }
                        .addOnFailureListener { e ->
                            loginProgressBar.visibility = View.INVISIBLE
                            Toast.makeText(
                                this@LoginActivity,
                                "Sign In Failed!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } catch (e: ApiException) {
                    e.printStackTrace()
                    loginProgressBar.visibility = View.VISIBLE
                    Toast.makeText(this@LoginActivity, "Error", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onResume() {
        super.onResume()
        videoview.start()
    }

    override fun onPause() {
        super.onPause()
        videoview.pause()
    }

    fun setupSignInGoogle() {
        //Google Sign In Logic and BUtton Set
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail().build()
        val signInClient = GoogleSignIn.getClient(this, gso)
        // Main Activity put activityResultLauncher.launch(i)
    }
}