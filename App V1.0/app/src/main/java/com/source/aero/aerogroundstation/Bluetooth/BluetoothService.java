package com.source.aero.aerogroundstation.Bluetooth;

import com.source.aero.aerogroundstation.R;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothService {
    // TAG for localizing debug messages
    private static final String TAG = "BluetoothService";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static UUID MY_UUID_SECURE;
    private static UUID MY_UUID_INSECURE;

    // Member fields for Bluetooth functionality and UI
    private final BluetoothAdapter  mBluetoothAdapter;
    private final Handler           mHandler;

    // Threading objects for the stages of Bluetooth communication
    private AcceptIncConnThread     mSecureConnAcceptThread;
    private AcceptIncConnThread    mInsecureConnAcceptThread;
    private ConnectThread           mConnectThread;
    private ConnectedThread         mConnectedThread;

    // Store Bluetooth state
    private int mState;
    private int mNewState;

    // State indicators
    public static final int STATE_NONE          = 0;
    public static final int STATE_LISTENING     = 1;
    public static final int STATE_CONNECTING    = 2;
    public static final int STATE_CONNECTED     = 3;

    private byte[] validPacket;
    private boolean started;
    private boolean ended;
    private int index = 0;

    /**
     * Constructor. Prepares a new Bluetooth service session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothService(Context context, Handler handler){
        // Set member variables
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = handler;

        // Set unique user id based on value in strings.xml
        MY_UUID_SECURE = UUID.fromString(context.getResources().getString(R.string.secure_UUID));
        MY_UUID_INSECURE = UUID.fromString(context.getResources().getString(R.string.insecure_UUID));

        validPacket = new byte[47];
        started = false;
        ended = false;
        index = 0;
    }

    /**
     * Return the current state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Send update signal to the UI
     */
    private synchronized void updateUserInterfaceScope() {
        mState = getState();
        Log.d(TAG, "updateUserInterfaceTitle() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(BluetoothConstantsInterface.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    /**
     * Start the bluetooth service. This encompasses starting the accept incoming connection threads
     */

    public synchronized void start(){
        Log.d(TAG, "Bluetooth service start");

        // Cancel every thread besides the accept threads
        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start both secure and insecure accept incoming conn threads
        if(mSecureConnAcceptThread == null){
            mSecureConnAcceptThread = new AcceptIncConnThread(true);
            mSecureConnAcceptThread.start();
        }
        if(mInsecureConnAcceptThread == null){
            mInsecureConnAcceptThread = new AcceptIncConnThread(false);
            mInsecureConnAcceptThread.start();
        }

        // After we have started the bluetooth incoming conn accept threads, update the UI
        updateUserInterfaceScope();
    }

    /**
     * Start the connect thread to initiate a connection to some bluetooth device.
     *
     * @param device The BluetoothDevice to connect
     * @param secureType Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secureType) {
        Log.d(TAG, "Bluetooth service connecting to: " + device);

        // Cancel any already existing thread that is trying to make a connection
        if(mState ==  STATE_CONNECTING){
            if(mConnectThread != null){
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start a new thread to handle connecting to the bluetooth device
        mConnectThread = new ConnectThread(device, secureType);
        mConnectThread.start();

        // After we have started the bluetooth connecting thread, update the UI
        updateUserInterfaceScope();
    }

    /**
     * Start the connected thread to begin maintaining a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel any already existing thread that has finished making a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel both secure and insecure accept incoming conn threads
        if (mSecureConnAcceptThread != null) {
            mSecureConnAcceptThread.cancel();
            mSecureConnAcceptThread = null;
        }
        if (mInsecureConnAcceptThread != null) {
            mInsecureConnAcceptThread.cancel();
            mInsecureConnAcceptThread = null;
        }

        // Create new thread encompassing bluetooth connection management
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the device that was connected back to the UI handler
        Message msg = mHandler.obtainMessage(BluetoothConstantsInterface.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothConstantsInterface.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // After getting the device name, now update the UI handler
        updateUserInterfaceScope();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "Bluetooth service is stopping all threads");

        // Cancel any connect thread
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel both secure and insecure accept incoming conn threads
        if (mSecureConnAcceptThread != null) {
            mSecureConnAcceptThread.cancel();
            mSecureConnAcceptThread = null;
        }
        if (mInsecureConnAcceptThread != null) {
            mInsecureConnAcceptThread.cancel();
            mInsecureConnAcceptThread = null;
        }

        mState = STATE_NONE;

        // Notify UI of state of the bluetooth service
        updateUserInterfaceScope();
    }

    /**
     * Write a byte array to the connected thread unsynchronized as to not disturb thread operation
     *
     * @param out Which are bytes to be written
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out){
        // Create temporary thread for synchronized dodging
        ConnectedThread tempVar;

        // Create copy of thread while synchronized
        synchronized(this){
            // Cant write to device that is not connected
            if(mState != STATE_CONNECTED){
                Log.d(TAG, "Bluetooth service is not connected, write failed");
                return;
            }
            tempVar = mConnectedThread;
        }

        // Unsynchronized call to wriote
        tempVar.write(out);
    }

    /**
     * Tell UI handler that the connection attempted has failed
     */
    private void connectionFailed(){
        // Send the failure message back to the UI handler
        Message msg = mHandler.obtainMessage(BluetoothConstantsInterface.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothConstantsInterface.TOAST, "Bluetooth service unable to connect to device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Clear state
        mState = STATE_NONE;

        // Update UI with failure message
        updateUserInterfaceScope();

        // Restart this service to start make at the accept incoming connection requests stage
        BluetoothService.this.start();
    }

    /**
     * Tell UI handler that the connection has been lost
     */
    private void connectionLost(){
        // Send the failure message back to the UI handler
        Message msg = mHandler.obtainMessage(BluetoothConstantsInterface.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothConstantsInterface.TOAST, "Bluetooth service connection lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Clear state
        mState = STATE_NONE;

        // Update UI with failure message
        updateUserInterfaceScope();

        // Restart this service to start make at the accept incoming connection requests stage
        BluetoothService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptIncConnThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptIncConnThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
            mState = STATE_LISTENING;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case STATE_LISTENING:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. The connection either succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[BluetoothConstantsInterface.MESSAGE_BUFFER_SIZE];
            int bytes;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Print bytes received and how many we received
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < bytes; ++j){
                        sb.append(String.format("%02X ", buffer[j]));
                    }
                    Log.d(TAG, "Incoming bluetooth data string bytes len: : " + String.valueOf(bytes) + " contents: " + sb.toString());


                    // For every received byte, verify how it fits in the packet
                    for(int i = 0; i < bytes; ++i)
                    {
                        // Get a received byte from the buffer
                        byte inByte = buffer[i];

                        // If byte == the start byte and we have not yet started, clear the valid packet and insert the first byte
                        if((Byte.compare(inByte, (byte) 10) == 0) && started == false)
                        {
                            validPacket = new byte[47];
                            validPacket[index] = inByte;
                            started = true;
                            index += 1;
                        }
                        // If byte == the end byte and we have started, finish the valid packet
                        else if((Byte.compare(inByte, (byte) 255) == 0) && started == true && index == 46)
                        {
                            // 47 bytes in a message, therefore index should be 45 before the end byte is received
                            if(index !=  46)
                            {
                                Log.d(TAG, "End of msg index error");
                            }
                            validPacket[index] = inByte;
                            ended = true;
                        }
                        // If we have started reading in data, then the incoming byte is somewhere in the packet
                        else if(started == true)
                        {
                            if(index > 46)
                            {
                                Log.d(TAG, "Index overflow");
                            }

                            validPacket[index] = inByte;
                            index += 1;
                        }

                        // If we have hit both the start and end flags, send the valid packet to the handler and reset the packet
                        if(started == true && ended == true)
                        {
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(BluetoothConstantsInterface.MESSAGE_READ, validPacket.length, -1, validPacket)
                                    .sendToTarget();

                            // Reset flags and index and fail counter
                            started = false;
                            ended = false;
                            index = 0;

                            validPacket = new byte[]{};
                        }

                    }

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(BluetoothConstantsInterface.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

}
