package com.greg.uberclone

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.droidman.ktoasty.KToasty
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    //----------------------- Firebase -------------------------------------------------------------
    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var auth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private var currentUser: FirebaseUser? = null
    private lateinit var authMethodPickerLayout: AuthMethodPickerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeLogin()
    }

    override fun onStart() {
        super.onStart()
        delayedSplashScreen()
        updateUI(getCurrentUser())
    }

    override fun onStop() {
        removeListenerOnFirebaseAuth()
        super.onStop()
    }

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

    private fun updateUI(currentUser: FirebaseUser?){
        if (currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

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
        listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (getCurrentUser() != null){
                KToasty.success(
                    this, "Welcome ${currentUser!!.uid}",
                    Toast.LENGTH_SHORT
                ).show()
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
    //----------------------- Create custom authentication ----------------------------------------------------
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
            updateUI(getCurrentUser())
            // ...
        } else {
            Log.w(TAG, "signInWithCredential:failure", response!!.error)
            KToasty.error(this, "Authentication failed", Toast.LENGTH_SHORT).show()
            updateUI(null)
        }
    }
}