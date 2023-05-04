package br.pro.mateus.authnotify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Camera
import android.os.Build
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import br.pro.mateus.authnotify.databinding.ActivityMainBinding
import br.pro.mateus.authnotify.datastore.UserPreferencesRepository
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var auth: FirebaseAuth
    private lateinit var functions : FirebaseFunctions

    val db = Firebase.firestore
    private fun prepareFirebaseAppCheckDebug(){
        // Ajustando o AppCheck para modo depuração.
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
    }

    fun storeUserId(uid: String){
        userPreferencesRepository.uid = uid
    }

    fun getUserUid(): String{
        return userPreferencesRepository.uid;
    }

    private fun storeFcmToken(){
        Firebase.messaging.token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            // guardar esse token.
            userPreferencesRepository.fcmToken = task.result

            val user = Firebase.auth.currentUser?.email;

            //updateFcmToken();
            fun updateFcmToken(fcmtoken: String){
                val data = hashMapOf(
                    "fcmtoken" to  fcmtoken,
                    ""
                )
                functions.getHttpsCallable("sendFcmMessage").call(data)
            }
            updateFcmToken(task.result)
//            user?.let {
//                val uid = it.uid
//            }
        })
    }

    fun getFcmToken(): String{
        return userPreferencesRepository.fcmToken
    }


    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // mostrar o fragment
           navController.navigate(R.id.action_login_to_notifications_disabled)
        }
    }


    //Toast.makeText(applicationContext, "camera already enabled", Toast.LENGTH_SHORT).show()
    private fun askCameraPermission(){
//        when (ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                Toast.makeText(applicationContext, "camera already enabled", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(applicationContext, "camera not enabled", Toast.LENGTH_SHORT).show()
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA)
            }
        }


    }


//    private fun askCameraPermission() {
//        // This is only necessary for API level >= 33 (TIRAMISU)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
//                PackageManager.PERMISSION_GRANTED
//            ) {
//                // FCM SDK (and your app) can post notifications.
//            } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
//                // TODO: display an educational UI explaining to the user the features that will be enabled
//                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
//                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
//                //       If the user selects "No thanks," allow the user to continue without notifications.
//            } else {
//                // Directly ask for the permission
//                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
//            }
//        }
//    }





    /** Check if this device has a camera */
//    private fun checkCameraHardware(context: Context): Boolean {
//        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
//            /** A safe way to get an instance of the Camera object. */
//            fun getCameraInstance(): Camera? {
//                return try {
//                    Camera.open() // attempt to get a Camera instance
//                } catch (e: Exception) {
//                    // Camera is not available (in use or does not exist)
//                    null // returns null if camera is unavailable
//                }
//            }
//
//            return true
//        } else {
//            // no camera on this device
//            return false
//        }
//    }



    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        auth = Firebase.auth
        functions = Firebase.functions
        super.onCreate(savedInstanceState)

        userPreferencesRepository = UserPreferencesRepository.getInstance(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // disponibilizando o token (que deve ser colocado lá no APP CHECK do Firebase).
        prepareFirebaseAppCheckDebug()

        // guardar o token FCM pois iremos precisar.
        storeFcmToken();

        // invocar as permissões para notificar.
        askNotificationPermission();

        // invocar as permissões para camera
        askCameraPermission()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}