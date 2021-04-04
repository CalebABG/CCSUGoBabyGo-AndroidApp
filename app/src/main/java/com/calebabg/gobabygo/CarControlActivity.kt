package com.calebabg.gobabygo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.control_layout.*
import java.lang.ref.WeakReference
import java.util.zip.CRC32
import kotlin.math.floor

class ControlCarActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        var BLUETOOTH_CONNECTED = false
        var BLUETOOTH_CONNECTING = false

        var accelMinX = 0
        var accelMinY = 0
        var accelMaxX = 0
        var accelMaxY = 0

        var gbgBTService: GBGBluetoothService? = null

        lateinit var sensorManager: SensorManager
        lateinit var accelerometer: Sensor

        fun connectToArduinoBluetooth() {
            connectDevice(Constants.GoBabyGoBTMAC)
        }

        fun disconnectFromArduinoBluetooth() {
            gbgBTService?.stop()
        }

        private fun connectDevice(address: String) {
            val device = MainActivity.mBluetoothAdapter!!.getRemoteDevice(address)

            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (gbgBTService!!.state == GBGBluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                gbgBTService!!.start()
            }

            // Attempt to connect to the device
            gbgBTService!!.connect(device, true)
        }
    }

    /**
     * The Handler that gets information back from the MyBluetoothService
     */
    private val mHandler = MyHandler(this)

    private var parentAppControlOverride = true
    private val activeShieldColor = "#32A341"
    private val inactiveShieldColor = "#AEB1AE"

    @ExperimentalUnsignedTypes
    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.control_layout)
        updateShieldIconColor()
        updateSensorInfoText()

        Log.d(Constants.TAG, "setupBT()")

        // Initialize the GBGBluetoothService to perform bluetooth connections
        gbgBTService = GBGBluetoothService(this, mHandler)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (!BLUETOOTH_CONNECTED) connectToArduinoBluetooth()

        parentalOverrideBtn.setOnClickListener {
            parentAppControlOverride = !parentAppControlOverride
            updateShieldIconColor()

            if (parentAppControlOverride) {
                Toast.makeText(this, "Activating Parental Control", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Deactivating Parental Control", Toast.LENGTH_SHORT).show()
                if (BLUETOOTH_CONNECTED) {
                    val packet = createSensorPacket(Constants.PARENTAL_CONTROL_PACKET_ID)
                    gbgBTService!!.write(packet)
                }
            }
        }

        emergencyStopBtn.setOnClickListener {
            Toast.makeText(this, "Stopping Motors!", Toast.LENGTH_SHORT).show()
            // TODO: Extract this to function
            if (BLUETOOTH_CONNECTED) {
                val packet = createSensorPacket(Constants.STOP_MOTORS_PACKET_ID)
                gbgBTService!!.write(packet)
            }
        }
        emergencyStopBtn.setOnLongClickListener {
            Toast.makeText(
                this,
                "Tooltip: ${emergencyStopBtn.contentDescription}",
                Toast.LENGTH_SHORT
            ).show()
            true
        }

        btReconnectBtn.setOnClickListener {
            when {
                BLUETOOTH_CONNECTED -> Toast.makeText(this, "Already Connected", Toast.LENGTH_SHORT).show()
                BLUETOOTH_CONNECTING -> Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()
                else -> connectToArduinoBluetooth()
            }
        }

        bt_disconnectBtn.setOnClickListener {
            if (BLUETOOTH_CONNECTED) disconnectFromArduinoBluetooth()
            else Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT).show()
        }

        accel_Switch.setOnClickListener { updateSensorInfoText() }

        sendPacketBtn.setOnClickListener {
            // Send packets
            if (BLUETOOTH_CONNECTED) {
                val packet = createSensorPacket(Constants.STOP_MOTORS_PACKET_ID)
                gbgBTService!!.write(packet)
            }
        }
    }

    private fun updateShieldIconColor() {
        val bgTintColor = if (parentAppControlOverride) Color.parseColor(activeShieldColor) else Color.parseColor(inactiveShieldColor)
        parentalOverrideBtn.backgroundTintList = ColorStateList.valueOf(bgTintColor)
    }

    private fun updateSensorInfoText() {
        val sensorText = if (accel_Switch.isChecked) "Calc" else "Raw"
        accel_Switch.text = sensorText
    }

    override fun onResume() {
        super.onResume()

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, 1000)

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (gbgBTService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (gbgBTService!!.state == GBGBluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                gbgBTService!!.start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (gbgBTService != null) {
            gbgBTService!!.stop()
        }

        finish()
    }

    @ExperimentalUnsignedTypes
    private fun createSensorPacket(packetId: Int, packetData: MutableList<Int> = mutableListOf()): ByteArray {
        /*
        Index     0:      Packet START Byte
        Index     1:      Packet ID (Type of Packet)
        Indexes   2-5:    Packet Checksum
        Index     6:      Packet ACK Byte
        Index     7:      Packet Data Length (in Bytes)
        Index     8-15:   Packet Data
        Index     16:     Packet END Byte

        Ex. Packet
        Index values are in HEX (ex. 0xff = 255) and (ex. 0x0f = 15)
        Index:    0     1     2     3     4     5     6     7     8     9     10     11     12     13     14     15     16
               {  01    03    ff    ff    ff    ff    01    08    01    02    03     04     05     06     07     08     04  }
        */

        val byteArray = ByteArray(17)

        // Set Header
        setSensorPacketHeader(packetId, byteArray)

        // Set Data
        setSensorPacketData(packetData, byteArray)

        // Set Checksum
        setSensorPacketChecksum(byteArray)

        return byteArray

    }

    private fun setSensorPacketData(packetData: MutableList<Int>, byteArray: ByteArray) {
        if (packetData.size in 1..7) {
            for (i in 0 until packetData.size) {
                byteArray[8 + i] = packetData[i].toByte()
            }
        }
    }

    private fun setSensorPacketHeader(msgId: Int, byteArray: ByteArray) {
        // Set START byte
        byteArray[0] = 0x01

        // Set id
        byteArray[1] = msgId.toByte()

        // Set ack
        byteArray[6] = 0x01

        // Set data length
        byteArray[7] = 0x02

        // Set END byte
        byteArray[16] = 0x04
    }

    @ExperimentalUnsignedTypes
    private fun setSensorPacketChecksum(byteArray: ByteArray) {
        val crC32 = CRC32()
        crC32.update(byteArray)

        val packetCRC32 = crC32.value.toUInt().toInt()
        val checksum = Utils.intToBytes(packetCRC32)

        byteArray[2] = checksum[0]
        byteArray[3] = checksum[1]
        byteArray[4] = checksum[2]
        byteArray[5] = checksum[3]
    }

    /**
     * Exit the app if user select yes.
     */
    private fun doExit() {
        val alertDialog = AlertDialog.Builder(this@ControlCarActivity)
        alertDialog.setPositiveButton("Yes") { _, _ -> finish() }
        alertDialog.setNegativeButton("No", null)
        alertDialog.setMessage("Do You Want to Exit?")
        alertDialog.setTitle(R.string.app_name)
        alertDialog.show()
    }

    override fun onBackPressed() {
        doExit()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

//    val alpha: Float = 0.8f
//    var gravity: FloatArray = FloatArray(2)
//    var linear_acceleration: FloatArray = FloatArray(2)

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    @ExperimentalUnsignedTypes
    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent) {

//        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
//        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
//        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
//        val accelX: Float = event.values[0] - gravity[0]
//        val accelY: Float = event.values[1] - gravity[1]

        val accelX: Float = event.values[0]
        val accelY: Float = event.values[1]

        if (accelX < accelMinX) {
            accelMinX = accelX.toInt()
            accelMinXTextView.text = "Accel Min X: $accelMinX"
        }

        if (accelY < accelMinY) {
            accelMinY = accelY.toInt()
            accelMinYTextView.text = "Accel Min Y: $accelMinY"
        }

        if (accelX > accelMaxX) {
            accelMaxX = accelX.toInt()
            accelMaxXTextView.text = "Accel Max X: $accelMaxX"
        }

        if (accelY > accelMaxY) {
            accelMaxY = accelY.toInt()
            accelMaxYTextView.text = "Accel Max Y: $accelMaxY"
        }

        val cAccelMinX = -10.1
        val cAccelMaxX = 12.1

        val cAccelMinY = -10.1
        val cAccelMaxY = 10.1

        val calcAccelX = floor(
            Utils.map(
                Utils.constrain(accelX.toDouble(), cAccelMinX, cAccelMaxX),
                cAccelMinX,
                cAccelMaxX,
                255.0,
                0.0
            )
        ).toLong().toUInt().toInt()
        val calcAccelY = floor(
            Utils.map(
                Utils.constrain(accelY.toDouble(), cAccelMinY, cAccelMaxY),
                cAccelMinY,
                cAccelMaxY,
                255.0,
                0.0
            )
        ).toLong().toUInt().toInt()

        if (accel_Switch.isChecked) {
            accelXTextView.text = "$calcAccelX"
            accelYTextView.text = "$calcAccelY"
        } else {
            accelXTextView.text = "${"%.3f".format(accelX)} m/s\u00B2"
            accelYTextView.text = "${"%.3f".format(accelY)} m/s\u00B2"
        }

        if (parentAppControlOverride) {
            // Send packets
            if (BLUETOOTH_CONNECTED) {
                val packet = createSensorPacket(Constants.SENSOR_DATA_PACKET_ID, mutableListOf(calcAccelX, calcAccelY))
                gbgBTService!!.write(packet)
            }
        }
    }

    // TODO: Make sure using Non-Deprecated constructor behaves as intended
    private class MyHandler(activity: ControlCarActivity) : Handler(Looper.getMainLooper()) {
        private val mActivity: WeakReference<ControlCarActivity> = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            val activity = mActivity.get()

            if (activity != null) {
                when (msg.what) {
                    Constants.MESSAGE_STATE_CHANGE -> when (msg.arg1) {
                        GBGBluetoothService.STATE_CONNECTED -> {
                            BLUETOOTH_CONNECTED = true
                            BLUETOOTH_CONNECTING = false

                            activity.btStatusTextView.text = "Connected"
                        }

                        GBGBluetoothService.STATE_CONNECTING -> {
                            BLUETOOTH_CONNECTING = true
                            activity.btStatusTextView.text = "Connecting... "
                        }

                        GBGBluetoothService.STATE_LISTEN, GBGBluetoothService.STATE_NONE -> {
                            BLUETOOTH_CONNECTED = false
                            BLUETOOTH_CONNECTING = false
                            activity.btStatusTextView.text = "Disconnected"
                        }
                    }

                    Constants.MESSAGE_WRITE -> {
                        val writeBuff: ByteArray = msg.obj as ByteArray
                        Log.d(Constants.TAG, writeBuff.toHex())
                    }

                    Constants.MESSAGE_READ -> {
                        val readBuff: ByteArray = msg.obj as ByteArray
                        Log.d(Constants.TAG, readBuff.toHex())
                    }

                    Constants.MESSAGE_TOAST -> {
                        Toast.makeText(
                            activity,
                            msg.data.getString(Constants.TOAST),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

}

private fun IntArray.toHex(): String {
    val s = StringBuilder()
    for (byte in this) s.append(String.format("%02X", byte))
    return s.toString()
}

private fun ByteArray.toHex(): String {
    val s = StringBuilder()
    for (byte in this) s.append(String.format("%02X", byte))
    return s.toString()
}