package com.greg.uberclone.ui.activity

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.droidman.ktoasty.KToasty
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.greg.uberclone.R
import com.greg.uberclone.ui.dialog_box.LogOutDialog
import com.greg.uberclone.ui.dialog_box.PhotoChoiceDialog
import com.greg.uberclone.utils.Common
import com.greg.uberclone.utils.ImageConverter
import com.greg.uberclone.utils.SavePhoto
import com.greg.uberclone.utils.UserUtils
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*

class DriverHomeActivity : AppCompatActivity(), PhotoChoiceDialog.CameraListener, PhotoChoiceDialog.GalleryListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var navController: NavController
    private lateinit var headerView: View
    private lateinit var photo: CircleImageView
    private lateinit var savePhoto: SavePhoto
    private var photoFromStorage: Uri? = null
    private lateinit var imageConverter: ImageConverter
    //----------------------- Firebase storage -----------------------------------------------------
    private lateinit var storageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_home)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        val navigationHost =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navigationHost.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        savePhoto = SavePhoto()
        imageConverter = ImageConverter()
        setDriverInformation()
        clickOnNavItem()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.driver_home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Click on nav item -------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun clickOnNavItem(){
        navView.setNavigationItemSelectedListener {
            if (it.itemId == R.id.nav_home){
                goToDriverActivity()
            }
            if (it.itemId == R.id.nav_log_out){
                showLogOutDialog()
            }
            true
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Log out dialog box ------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun showLogOutDialog() {
        val logOutDialog = LogOutDialog()
        logOutDialog.show(supportFragmentManager, "LogOutDialogBox")
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Set driver information --------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun setDriverInformation(){
        firebaseStorage()
        headerView = navView.getHeaderView(0)
        val name = headerView.findViewById<View>(R.id.name_tv) as TextView
        val phoneNumber = headerView.findViewById<View>(R.id.phone_tv) as TextView
        val rating = headerView.findViewById<View>(R.id.rating_tv) as TextView
        photo = headerView.findViewById<View>(R.id.photo) as CircleImageView

        name.text = Common.buildWelcomeMessage()
        phoneNumber.text = Common.currentDriver!!.phoneNumber
        rating.text = StringBuilder().append(Common.currentDriver!!.rating)

       if (Common.currentDriver != null && Common.currentDriver!!.avatar != null && !TextUtils.isEmpty(Common.currentDriver!!.avatar)){
            Glide.with(this)
                .load(Common.currentDriver!!.avatar)
                .into(photo)
        }
        setDriverPhoto()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Set driver photo --------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun setDriverPhoto(){
        photo.setOnClickListener {
            showPhotoChoiceDialog()
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Photo choice dialog box -------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun showPhotoChoiceDialog() {
        val photoChoiceDialog = PhotoChoiceDialog(this, this)
        photoChoiceDialog.show(supportFragmentManager, "PhotoChoiceDialogBox")
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Go to Driver home activity -------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun goToDriverActivity() {
        startActivity(Intent(this, DriverHomeActivity::class.java))
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Get Bitmap from dialog box -------------------------------------------
    //----------------------------------------------------------------------------------------------

    override fun applyCameraPhoto(bitmapPhoto: Bitmap) {
        photo.setImageBitmap(bitmapPhoto)
        val tempUri: Uri? = savePhoto.getImageUri(this, bitmapPhoto)
        photoFromStorage = tempUri
        saveAvatarPhoto()
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Get Uri from dialog box ----------------------------------------------
    //----------------------------------------------------------------------------------------------

    override fun applyGalleryPhoto(uriPhoto: Uri?) {
        photo.setImageURI(uriPhoto)
        val bitmap = imageConverter.uriToBitmap(uriPhoto, this)
        val tempUri: Uri? = savePhoto.getImageUri(this, bitmap)
        photoFromStorage = tempUri
        saveAvatarPhoto()
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Firebase storage -------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //----------------------- Initialize storage ---------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun firebaseStorage(){
        storageReference = FirebaseStorage.getInstance().reference
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Save driver avatar image ---------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun saveAvatarPhoto(){
        if (photoFromStorage != null){
            val avatarFolder = storageReference.child("avatars/" + FirebaseAuth.getInstance().currentUser!!.uid)
            avatarFolder.putFile(photoFromStorage!!)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful){
                        avatarFolder.downloadUrl.addOnSuccessListener { uri ->
                            val updateAvatar = HashMap<String, Any>()
                            updateAvatar["avatar"] = uri.toString()
                            UserUtils.updateDriver(drawerLayout, updateAvatar)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Snackbar.make(drawerLayout, e.message!!, Snackbar.LENGTH_LONG).show()
                }
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0*taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    StringBuilder(getString(R.string.uploading)).append(progress).append("%")
                }
        }
    }
}