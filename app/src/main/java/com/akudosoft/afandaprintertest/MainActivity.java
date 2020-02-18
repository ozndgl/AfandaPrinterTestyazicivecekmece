package com.akudosoft.afandaprintertest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private Handler myHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Constant.M_CONTEXT = MainActivity.this;

        Button btnTest = findViewById(R.id.btnTest);

        myHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {

                switch (msg.what) {
                    case 1:
                        Toast.makeText(MainActivity.this, "" + msg.obj,
                                Toast.LENGTH_LONG).show();
                        break;
                    default:
                        new AlertDialog.Builder(MainActivity.this).setTitle("INFO")
                                .setMessage(msg.obj + "")
                                .setNegativeButton("OK", null).show();
                        break;
                }

            };
        };

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread() {
                    public void run() {
                        new Printer(2,
                                "19200",
                                new Printer.CallBack() {

                                    @Override
                                    public void onFailure(String err) {
                                        // TODO Auto-generated method
                                        // stub
                                        System.err.println(err);
                                        Message msg = myHandler
                                                .obtainMessage();
                                        msg.obj = MainActivity.this
                                                .getString(R.string.new16);
                                        msg.sendToTarget();
                                    }

                                    @Override
                                    public void onSuccess(
                                            Printer printer) {
                                        // TODO Auto-generated method
                                        // stub
                                        Message msg = new Message();
                                        msg.what = 1;
                                        msg.obj = MainActivity.this
                                                .getString(R.string.new30);
                                        myHandler.sendMessage(msg);

                                        printer.writeHex("1B40");// VARSAYILAN MODA AYARLA
                                        printer.writeHex("1B6101");// ORTA HİZALAMA
                                        printer.writeHex("1C5701");// (Çift yükseklik arka genişliğini ayarlayın)
                                        printer.writeHex("1B4501");// YAZI TİPİ KALIN

                                        printer.write("ORTA HİZALANMIŞ SATIR");
                                        printer.writeHex("0A");

                                        printer.writeHex("1C5700");// (Çift yükseklik arka genişliğini ayarlama)
                                        printer.writeHex("1B6100");// Karakter hizalama modunu seçin (sol hizalama)
                                        printer.writeHex("1B700028");//çekmece açtıkmak için
                                        for (int i = 0; i < 20; i++) {
                                            printer.write("YAZDIRILAN SATIR:" + (i++));

                                            printer.writeHex("0A");// Yazdır ve sar
                                        }
                                    }
                                });
                    };
                }.start();
            }
        });
    }
}
