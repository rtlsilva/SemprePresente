/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.me.semprepresente.client;

import android.app.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.regex.*;

/**
 *
 * @author Ricardo
 */
public class MainActivity extends ListActivity {

    private boolean connected = false;
    private TextView tvAddress;
    private TextView tvInfo;
    private EditText etAddress;
    private Button btnConnect;
    private Button btnSettings;

    static Socket clientSocket;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        tvAddress = (TextView) findViewById(R.id.main_address_tv);
        tvInfo = (TextView) findViewById(R.id.main_info_tv);
        etAddress = (EditText) findViewById(R.id.main_address_et);
        btnConnect = (Button) findViewById(R.id.main_connect_button);
        btnSettings = (Button) findViewById(R.id.main_settings_button);

        btnConnect.setOnClickListener(new BtnConnectListener());
    }

    private class BtnConnectListener implements OnClickListener {

        public void onClick(View v) {
            Matcher matcher = Patterns.IP_ADDRESS.matcher(etAddress.getText().toString());
            if (matcher.matches()) {
                Thread cThread = new Thread(new ClientThread());
                cThread.start();
            } else {
                tvInfo.setText("Incorrect IP address");
            }
        }
    }

    public class ClientThread implements Runnable {

        public void run() {
            tvInfo.setText("Connecting...");
            InetAddress serverAddress;
            Log.d("MainActivity", "Connecting...");
            try {
                serverAddress = InetAddress.getByName(etAddress.getText().toString());
                Socket socket = new Socket(serverAddress, 3333);
                connected = true;

                ObjectOutputStream oos = sendDataToServer(socket);
                ObjectInputStream ois = receiveDataFromServer(socket);
                oos.close();
                ois.close();
                socket.close();

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Client error", e);
                connected = false;
            }
        }

        private ObjectOutputStream sendDataToServer(Socket socket) throws IOException {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            //oos.writeObject

            return oos;
        }

        public ObjectInputStream receiveDataFromServer(Socket socket) throws IOException {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            //ois.readObject

            return ois;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // The activity is about to become visible.
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The activity has become visible (it is now "resumed").
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Another activity is taking focus (this activity is about to be "paused").
    }

    @Override
    protected void onStop() {
        super.onStop();
        // The activity is no longer visible (it is now "stopped")
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // The activity is about to be destroyed.
    }
}
