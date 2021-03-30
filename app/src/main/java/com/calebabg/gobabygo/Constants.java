/*
 * Adapted from: https://github.com/googlearchive/android-BluetoothChat
 */

package com.calebabg.gobabygo;

/**
 * Defines several constants used between {@link GBGBluetoothService} and the UI.
 */
public class Constants {
    public static final String TAG = "GoBabyGoApp";

    // Blue-Smirf
    // public static final String GoBabyGoBTMAC = "00:06:66:F2:34:F8";

    // HC-06
    public static final String GoBabyGoBTMAC = "98:D3:B1:FD:32:CE";

    public static final int SENSOR_DATA_ID = 0xD7;
    public static final int STOP_MOTORS_ID = 0xE0;
    public static final int PARENTAL_OVERRIDE_ID = 0xDF;

    // Message types sent from the GBGBluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the GBGBluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

}
