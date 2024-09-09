package com.envy.newssync

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.envy.newssync.repository.FirestoreRepository
import com.envy.newssync.worker.DownloadNewsWorker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var syncRadioGroup: RadioGroup
    private lateinit var timestampTV: TextView
    private lateinit var syncBTN : AppCompatButton
    private var currentSyncRequestId: UUID? = null
    private lateinit var sharepreference : SharedPreferences
    private lateinit var deleteBTN : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        syncRadioGroup = findViewById(R.id.syncRadioGroup)
        timestampTV = findViewById(R.id.sync_timestamp)
        syncBTN = findViewById(R.id.syncButton)
        deleteBTN = findViewById(R.id.deleteButton)

        sharepreference = getSharedPreferences("sync_prefs", Context.MODE_PRIVATE);


        val savedRadioButton = sharepreference.getInt("selected_sync_interval", -1)
        if(savedRadioButton!= -1){
            syncRadioGroup.check(savedRadioButton)
        }
        syncRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedRadioButton = findViewById<RadioButton>(checkedId)
            saveSelectedSyncInterval(checkedId)
            when (selectedRadioButton.id) {
                R.id.sync1hour -> scheduleSync(1)
                R.id.sync2hour -> scheduleSync(2)
                R.id.sync4hour -> scheduleSync(4)
            }
        }

        syncBTN.setOnClickListener(View.OnClickListener {
            performOneTimeSync()
        })

        displayLastSyncTime(sharepreference)
        sharepreference.registerOnSharedPreferenceChangeListener(listener)

        deleteBTN.setOnClickListener { view ->
            val oneWeekAgo = System.currentTimeMillis() - 7 * DateUtils.DAY_IN_MILLIS
            FirebaseFirestore.getInstance().collection("news")
                .whereGreaterThan("published_at", oneWeekAgo)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    querySnapshot.forEach { snapshot: QueryDocumentSnapshot ->
                        snapshot.reference.delete()
                    }
                    Toast.makeText(applicationContext, "Deleted Successfully: ${querySnapshot.size()}", Toast.LENGTH_SHORT).show()
                }
        }

    }

    private fun scheduleSync(hours: Long) {
        // Cancel any existing work
        currentSyncRequestId?.let { id ->
            WorkManager.getInstance(this).cancelWorkById(id)
        }

        //Constraints
        val myConstraint = Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build()
        // Create a new PeriodicWorkRequest with the selected interval
        val syncRequest = PeriodicWorkRequestBuilder<DownloadNewsWorker>(hours, TimeUnit.HOURS)
            .build()

        // Save the request ID
        currentSyncRequestId = syncRequest.id

        // Enqueue the work
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            syncRequest
        )

        //Check when done
        WorkManager.getInstance().getWorkInfoByIdLiveData(syncRequest.id)
            .observe(this, Observer { workInfo ->
                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    Toast.makeText(this.applicationContext, "Sync Success", Toast.LENGTH_SHORT)
                        .show()
                }
            })

    }

    private fun getLastSyncTime(context: Context): Long{
        return sharepreference.getLong("last_sync_time", -1)
    }

    private fun displayLastSyncTime(sharePreference :SharedPreferences) {
        val lastSyncTime = sharePreference.getLong("last_sync_time", -1)
        if (lastSyncTime != -1L) {
            // Convert the timestamp to a readable format
            val lastSyncDate = Date(lastSyncTime)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedDate = dateFormat.format(lastSyncDate)
            timestampTV.text = formattedDate

        } else {
            timestampTV.text = "Not Synced Yet"
        }
    }


    private fun performOneTimeSync() {
        // Create a OneTimeWorkRequest for DownloadNewsWorker
        val oneTimeSyncRequest = OneTimeWorkRequest.Builder(DownloadNewsWorker::class.java)
            .build()

        // Enqueue the work
        WorkManager.getInstance(applicationContext).enqueue(oneTimeSyncRequest)

    }


    private fun saveSelectedSyncInterval(selectedId: Int) {
        // Get SharedPreferences editor
        val sharedPreferences = getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        // Save the ID of the selected RadioButton
        editor.putInt("selected_sync_interval", selectedId)
        editor.apply()  // Save changes
    }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        // Handle the preference change here
        // For example, you can check the key and respond accordingly
        if (key == "last_sync_time") {
            displayLastSyncTime(sharedPreferences)
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        sharepreference.unregisterOnSharedPreferenceChangeListener(listener)
    }

    override fun onStart() {
        super.onStart()
        //Google Sign In Logic
        if(FirebaseAuth.getInstance().currentUser == null) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail().build()
            val signInClient = GoogleSignIn.getClient(this, gso)
            val i: Intent = signInClient.getSignInIntent()
            //activityResultLauncher.launch(i)

        }
    }
}