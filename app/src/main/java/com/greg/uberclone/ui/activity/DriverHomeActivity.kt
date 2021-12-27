package com.greg.uberclone.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.greg.uberclone.Common
import com.greg.uberclone.LogOutDialogBox
import com.greg.uberclone.R

class DriverHomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var navController: NavController

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
                showLogOutDialogBox()
            }
            true
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Log out dialog box ------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun showLogOutDialogBox() {
        val logOutDialogBox = LogOutDialogBox()
        logOutDialogBox.show(supportFragmentManager, "LogOutDialogBox")
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Set driver information --------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun setDriverInformation(){
        val headerView = navView.getHeaderView(0)
        val name = headerView.findViewById<View>(R.id.name_tv) as TextView
        val phoneNumber = headerView.findViewById<View>(R.id.phone_tv) as TextView
        val rating = headerView.findViewById<View>(R.id.rating_tv) as TextView

        name.text = Common.buildWelcomeMessage()
        phoneNumber.text = Common.currentDriver!!.phoneNumber
        rating.text = StringBuilder().append(Common.currentDriver!!.rating)//Common.currentDriver!!.rating.toString()
    }

    //----------------------------------------------------------------------------------------------
    //----------------------- Go to Driver home activity -------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun goToDriverActivity() {
        startActivity(Intent(this, DriverHomeActivity::class.java))
    }
}