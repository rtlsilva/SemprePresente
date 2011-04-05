package org.me.semprepresente.client;

import android.content.*;
import android.os.*;
import android.util.*;
import java.io.*;
import java.net.*;

/**
 *
 * @author Ricardo
 */
public class AudioChatService {

    // Debugging
    private static final String TAG = "ConnectionService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "ConnectionServiceSecure";
    private static final String NAME_INSECURE = "ConnectionServiceInsercure";

    // Unique UUID for this application ??
    // Connection state constants
    public static final int STATE_NONE = 0;         // Doing nothing
    public static final int STATE_LISTEN = 1;       // Listening for incoming connections
    public static final int STATE_CONNECTING = 2;   // Initiating outgoing connection
    public static final int STATE_CONNECTED = 3;    // Connected to remote device
    private int mState;
    private final ServerSocket mServerSocket;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread, mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    public AudioChatService(Context context, Handler handler) throws IOException {
        mServerSocket = new ServerSocket(8686);
        mState = STATE_NONE;
        mHandler = handler;
    }

    private synchronized void setState(int state) {
        if (D) {
            Log.d(TAG, "setState() " + state + " -> " + state);
        }
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        //mHandler.obtainMessage(AudioChat.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void start() {
        if (D) {
            Log.d(TAG, "start");
        }

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a ServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    public synchronized void connect(boolean secure) {
        if (D) {
            Log.d(TAG, "connect: ");
        }

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(Socket socket, SocketAddress address, final String socketType) {
        if (D) {
            Log.d(TAG, "connected, Socket Type:" + socketType);
        }

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(AudioChat.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(AudioChat.DEVICE_NAME, address.toString());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
        if (D) {
            Log.d(TAG, "stop");
        }

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /** Writes to the ConnectedThread in an unsynchronized fashion **/
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /** Indicate that the connection attempt failed and notify the UI Activity.*/
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(AudioChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(AudioChat.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        AudioChatService.this.start();
    }

    /** Indicate that the connection was lost and notify the UI Activity.*/
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(AudioChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(AudioChat.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        AudioChatService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {

        private final ServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            Socket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try {
                if (secure) {
                    tmp = mServerSocket.accept();
                } else {
                    tmp = mServerSocket.accept();
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
            }

            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            if (D) {
                Log.d(TAG, "Socket Type: " + mSocketType + "BEGIN mAcceptThread" + this);
            }
            setName("AcceptThread" + mSocketType);
            Socket socket = null;

            // Listen to the server socket if not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on
                    // a successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (AudioChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Start the connected thread
                                connected(socket, socket.getRemoteSocketAddress(), mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Terminate new socket
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
            if (D) {
                Log.i(TAG, "END mAcceptThread, Socket Type: " + mSocketType);
            }
        }

        public void cancel() {
            if (D) {
                Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            }
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {

        private final Socket mmSocket;
        private final SocketAddress mmAddress;
        private String mSocketType;

        public ConnectThread(SocketAddress address, boolean secure) {
            mmAddress = address;
            Socket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try {
                if (secure) {
                    //!!
                } else {
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }

            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread Socket Type: " + mSocketType);
            setName("ConnectThread" + mSocketType);

            try {
                mmSocket.connect(mmAddress);
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType + " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }
            synchronized (AudioChatService.this) {
                mConnectThread = null;
            }
            connected(mmSocket, mmAddress, mSocketType);
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

        private final Socket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(Socket socket, String socketType) {
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
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(AudioChat.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    AudioChatService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(AudioChat.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
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