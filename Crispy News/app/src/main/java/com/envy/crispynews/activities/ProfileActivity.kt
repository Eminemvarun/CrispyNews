package com.envy.crispynews.activities
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.Toolbar
import com.envy.crispynews.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        //Widgets
        val name : TextView = findViewById(R.id.profile_name)
        val logoutBTN : AppCompatButton = findViewById(R.id.profile_logout)
        val interestsBTN : Button =findViewById(R.id.profile_button_interests)
        val firebaseAuth = FirebaseAuth.getInstance()
        val toolbar: Toolbar = findViewById<View>(R.id.toolbar_profile) as Toolbar
        val deleteBTN : Button = findViewById(R.id.delete_account)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true);
        name.text = ("""Welcome ${firebaseAuth.currentUser?.displayName}""")

        logoutBTN.setOnClickListener {
            firebaseAuth.signOut()
            val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
            setResult(RESULT_OK,null)
            startActivity(intent)
            overridePendingTransition(
                com.google.android.material.R.anim.abc_fade_in,
                com.google.android.material.R.anim.abc_fade_out)
            finish()
        }

        deleteBTN.setOnClickListener{
            FirebaseFirestore.getInstance().collection("users").document(firebaseAuth.uid.toString())
                .delete().addOnCompleteListener{
                    firebaseAuth.currentUser?.delete()
                        ?.addOnCompleteListener{
                            Log.i("ENVYLOGS", "Deletion done with ${it.result}")
                            firebaseAuth.signOut()
                            setResult(RESULT_OK,null)
                            startActivity(Intent(this@ProfileActivity, LoginActivity::class.java))
                            finish()
                        }
                }
        }

        interestsBTN.setOnClickListener {
            val intent =  Intent(this@ProfileActivity,EnterInterests::class.java)
            startActivity(intent)
            overridePendingTransition(androidx.appcompat.R.anim.abc_fade_in, androidx.appcompat.R.anim.abc_fade_out)
        }

    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(
            androidx.appcompat.R.anim.abc_fade_in, androidx.appcompat.R.anim.abc_fade_out)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}