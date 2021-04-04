package com.calebabg.gobabygo

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        /* Local Bluetooth adapter */
        var mBluetoothAdapter: BluetoothAdapter? = null

        // Intent request codes
        const val REQUEST_ENABLE_BT = 3

        val permissionsMap: MutableMap<String, Int> = mutableMapOf(
            Manifest.permission.BLUETOOTH to PackageManager.PERMISSION_DENIED,
            Manifest.permission.ACCESS_COARSE_LOCATION to PackageManager.PERMISSION_DENIED
        )

        var PERMISSIONS = arrayOf(
            Manifest.permission.VIBRATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        continueBtn.setOnClickListener { startCarControlActivity() }
    }

    private fun startCarControlActivity() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show()
        } else {
            if (hasPermissions(this, *PERMISSIONS)) {
                gotoCarControl()
            } else {
                requestNeededAppPermissions()
            }
        }
    }

    private fun gotoCarControl() {
        val intent = Intent(this, ControlCarActivity::class.java)
        startActivity(intent)
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        return permissions.all { ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun requestNeededAppPermissions() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_ENABLE_BT)
    }

    private fun canShowRequestPermissionsRationale() =
        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BLUETOOTH) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun neededPermissionsGranted(perms: MutableMap<String, Int>) =
        perms[Manifest.permission.BLUETOOTH] == PackageManager.PERMISSION_GRANTED &&
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == PackageManager.PERMISSION_GRANTED

    private fun showRequestPermissionsDialog() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setMessage("Bluetooth and Minimal Location permission are required for this app, allow?")
        alertDialog.setPositiveButton("Yes") { _, _ -> requestNeededAppPermissions() }
        alertDialog.setNegativeButton("No", null)
        alertDialog.setTitle(R.string.app_name)
        alertDialog.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (grantResults.isNotEmpty()) {
                    for (i in permissions.indices) permissionsMap[permissions[i]] = grantResults[i]

                    if (neededPermissionsGranted(permissionsMap)) {
                        gotoCarControl()
                    } else {
                        if (canShowRequestPermissionsRationale()) {
                            showRequestPermissionsDialog()
                        } else {
                            Toast.makeText(this, "Go to settings and enable permissions", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}