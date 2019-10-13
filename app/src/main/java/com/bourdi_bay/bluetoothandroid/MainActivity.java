package com.bourdi_bay.bluetoothandroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.AdapterView;
import android.view.View;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import java.io.*;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 2;
    // Must be the same as the one on the server.
    private static final UUID MY_UUID = UUID.fromString("B62C4E8D-62CC-404b-BBBF-BF3E3BBB1374");
    public BluetoothAdapter mBluetoothAdapter;
    ListView listView;
    private ArrayList<String> devices = new ArrayList<>();
    private Set<BluetoothDevice> pairedDevices;

    private BluetoothSocket mmSocket = null;
    private OutputStream mOutput;
    private InputStream mInput;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "No bluetooth supported", Toast.LENGTH_LONG).show();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        pairedDevices = mBluetoothAdapter.getBondedDevices();


        int count = pairedDevices.size();

        if (count > 0) {
            listView=(ListView)findViewById(R.id.deviceList);

            for (BluetoothDevice bt : pairedDevices) {
                devices.add(bt.getName());
            }
            ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, devices);
            listView.setAdapter(arrayAdapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                                                public void onItemClick(AdapterView parent, View v, int position, long id) {
                                                    String listItem = listView.getItemAtPosition(position).toString();
                                                    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                                                    for(BluetoothDevice bt : pairedDevices){
                                                        String name = bt.getName();
                                                        if (name.equals(listItem)) {
                                                            new Thread(new ConnectThread(bt)).start();
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
            );

        } else {
            Toast.makeText(this, "No device paired...", Toast.LENGTH_LONG).show();
            return;
        }
    }

    public void transmitFile(View view){
        TextView textView = (TextView) findViewById(R.id.file);
        textView.setText("");

        if(mmSocket!=null) {
            FileInputStream fin = null;

            try {
                EditText textBox = (EditText) findViewById(R.id.pathFile);
                String text = textBox.getText().toString();

                File file = new File(text);
                BufferedReader br = new BufferedReader(new FileReader(file));

                InputStream is = new FileInputStream(text);
                // creates buffer
                char[] cbuf = new char[is.available()];

                // reads characters to buffer, offset 2, len 10
                br.read(cbuf, 0, is.available());

                String sbuf = new String(cbuf);
                textView.setText(sbuf);
                br.close();
                if(mOutput!=null){
                    try {
                        int index = text.lastIndexOf('/');
                        String filename = text.substring(index+1);
                        if(filename.length()<256){
                            while (filename.length()!=256){
                                filename+=" ";
                            }
                        }
                        mOutput.write(filename.getBytes());
                        mOutput.write(sbuf.getBytes());
                        try {
                            mmSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException ex) {
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            } finally {

                try {
                    if (fin != null)
                        fin.close();
                } catch (IOException ex) {

                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
        else{
             textView.setText("No connection");
        }
    }
    private class ConnectThread extends Thread {



        public ConnectThread(BluetoothDevice device) {
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
                connectException.printStackTrace();
                return;
            }

            try {
                mOutput = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            try {
                mInput = mmSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            TextView connect = (TextView)findViewById(R.id.connect);
            connect.setText("");

            connect.setText("Connection. Success");
            try {
                TextView tfile = (TextView) findViewById(R.id.file);
                tfile.setText("");
                byte [] name = new byte[256];
                byte [] data = new byte[5000];

                mInput.read(name,0,256);
                String sname = new String(name);
                sname = sname.trim();


                mInput.read(data);
                File file = new File("/storage/emulated/0/bluetooth/"+sname);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();

                tfile.setText("File "+sname+" received");// and put in "+"/storage/emulated/0/bluetooth/"+sname);
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
