package com.mycompany.myfirstindoorsapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/**
 * Created by Tijs on 29/03/2015.
 */
public class ServerConnectActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_connect);
    }


    public void connectToServer(View view) {
        EditText serverIP = (EditText) findViewById(R.id.server_ip);
        String ipString = serverIP.getText().toString();
        Intent i = new Intent(this, ClientActivity.class);
        i.putExtra("ip", ipString);
        startActivity(i);

    }
}
