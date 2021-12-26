package com.greg.uberclone.ui.activity

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.droidman.ktoasty.KToasty
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.greg.uberclone.Common
import com.greg.uberclone.Constant.Companion.DRIVER_INFORMATION
import com.greg.uberclone.R
import com.greg.uberclone.databinding.SplashProgressBarBinding
import com.greg.uberclone.model.Driver
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: SplashProgressBarBinding
    //----------------------- Firebase -------------------------------------------------------------
    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var auth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private var currentUser: FirebaseUser? = null
    private lateinit var authMethodPickerLayout: AuthMethodPickerLayout
    //----------------------- Firebase database ----------------------------------------------------
    private lateinit var database: FirebaseDatabase
    private lateinit var driverInformationReference: DatabaseReference
    private lateinit var driver: Driver
    //----------------------- Registration dialog --------------------------------------------------
    private lateinit var builder: AlertDialog.Builder
    private lateinit var dialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SplashProgressBarBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        initializeLogin()
    }

    override fun onStart() {
        super.onStart()
        delayedSplashScreen()
        //updateUI(getCurrentUser())
    }

    override fun onStop() {
        removeListenerOnFirebaseAuth()
        super.onStop()
    }

    /**---------------------------------------------------------------------------------------------
     *                        Firebase
     ---------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //----------------------- Initialize firebase --------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun firebaseAuth(){
        auth = FirebaseAuth.getInstance()
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Get current user -----------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun getCurrentUser(): FirebaseUser? {
        currentUser = auth.currentUser
        return currentUser
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Update UI if user successfully connected -----------------------------
    //----------------------------------------------------------------------------------------------

    /*private fun updateUI(currentUser: FirebaseUser?){
        if (currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
        }
    }*/

    //----------------------------------------------------------------------------------------------
    //----------------------- Timer ----------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun delayedSplashScreen(){
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                addListenerOnFirebaseAuth()
            }
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Add listener on firebase auth ----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun addListenerOnFirebaseAuth(){
        auth.addAuthStateListener(listener)
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Remove listener on firebase auth -------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun removeListenerOnFirebaseAuth(){
        if (auth != null && listener != null){
            auth.removeAuthStateListener(listener)
        }
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Initialize login -----------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializeLogin(){
        firebaseDatabase()
        providers = listOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
        )
        firebaseAuth()
        initializeListener()
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Initialize listener --------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializeListener() {
        listener = FirebaseAuth.AuthStateListener {
            if (getCurrentUser() != null){
                checkUserFromFirebase()
            }
            else{
                showLoginLayout()
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Show login layout ----------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun showLoginLayout() {
        authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.activity_splash_screen)
                .setPhoneButtonId(R.id.phone_btn)
                .setGoogleButtonId(R.id.google_btn)
                .build()
        createCustomAuthentication()
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Create custom authentication -----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun createCustomAuthentication(){
        val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build()
        launcher.launch(signInIntent)
    }

    //----------------------- Launcher method because startActivityForResult is deprecated ---------

    private val launcher = registerForActivityResult(FirebaseAuthUIActivityResultContract()){ result ->
        onSignInResult(result)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            KToasty.success(
                this, "$response, Welcome ${getCurrentUser()} ",
                Toast.LENGTH_SHORT
            ).show()
            //updateUI(getCurrentUser())
            // ...
        } else {
            Log.w(TAG, "signInWithCredential:failure", response!!.error)
            KToasty.error(this, "Authentication failed", Toast.LENGTH_SHORT).show()
            //updateUI(null)
        }
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Firebase database ------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //----------------------- Initialize firebase database -----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun firebaseDatabase(){
        database = Firebase.database
        initializeDriver()
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Initialize driver ----------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializeDriver(){
        driverInformationReference = database.getReference(DRIVER_INFORMATION)
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Check user from Firebase ---------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun checkUserFromFirebase() {
        driverInformationReference
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        //KToasty.success(this@SplashScreenActivity, "User already registered !", Toast.LENGTH_SHORT).show()
                        val currentDriver = snapshot.getValue(Driver::class.java)
                        goToDriverHomeActivity(currentDriver)
                    }
                    else{
                        showRegisterLayout()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    KToasty.error(this@SplashScreenActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Show register layout -------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun showRegisterLayout() {
        builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.register_dialog, null)

        val firstNameEt =  itemView.findViewById<View>(R.id.first_name_et) as TextInputEditText
        val lastNameEt =  itemView.findViewById<View>(R.id.last_name_et) as TextInputEditText
        val phoneNumberEt =  itemView.findViewById<View>(R.id.phone_number_et) as TextInputEditText
        val registerBtn =  itemView.findViewById<View>(R.id.register_btn) as Button

        if (getCurrentUser()!!.phoneNumber != null && !TextUtils.isDigitsOnly(getCurrentUser()!!.phoneNumber)){
            phoneNumberEt.setText(getCurrentUser()!!.phoneNumber)
        }

        builder.setView(itemView)
        dialog = builder.create()
        dialog.show()

        registerBtn.setOnClickListener {
            when {
                TextUtils.isDigitsOnly(firstNameEt.text.toString()) -> {
                    firstNameEt.error = "Please enter a first name"
                    //return@setOnClickListener
                }
                TextUtils.isDigitsOnly(lastNameEt.text.toString()) -> {
                    lastNameEt.error = "Please enter a last name"
                    //return@setOnClickListener
                }
                TextUtils.isDigitsOnly(phoneNumberEt.text.toString()) -> {
                    phoneNumberEt.error = "Please enter a phone number"
                    //return@setOnClickListener
                }
                else -> {
                    val firstName = firstNameEt.text.toString()
                    val lastName = lastNameEt.text.toString()
                    val phoneNumber = phoneNumberEt.text.toString()
                    val rating = 0.0

                    driver = Driver(firstName, lastName, phoneNumber, rating)
                    checkRegistration()
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Check Registration ---------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun checkRegistration() {
        Log.d("Path Fire:", auth.currentUser!!.uid)
        driverInformationReference.child(auth.currentUser!!.uid)
                .setValue(driver)
                .addOnSuccessListener {
                    KToasty.success(this, "Registration successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    goToDriverHomeActivity(driver)
                    binding.progressBar.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    KToasty.error(this, "$e.message", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    binding.progressBar.visibility = View.GONE
                }
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Go to Driver home activity -------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun goToDriverHomeActivity(currentDriver: Driver?) {
        Common.currentDriver = currentDriver
        startActivity(Intent(this, DriverHomeActivity::class.java))
    }
}