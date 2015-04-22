package com.mycompany.myfirstindoorsapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.EditText;
import com.mycompany.myfirstindoorsapp.ImageTargets.ImageTargets;

/**
 * Created by Tijs on 29/03/2015.
 */
public class ServerConnectActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_connect);
        EditText userNameText = (EditText) findViewById(R.id.username);
        userNameText.setFilters(new InputFilter[] { filter });

    }


    public void connectToServer(View view) {
        EditText serverIPText = (EditText) findViewById(R.id.server_ip);
        String serverIP = serverIPText.getText().toString();
        EditText userNameText = (EditText) findViewById(R.id.username);
        String username = userNameText.getText().toString();
        Intent i = new Intent(this, ImageTargets.class);

//        Intent i = new Intent(this, ClientActivity.class);
        i.putExtra("ip", serverIP);
        i.putExtra("username", username);
        startActivity(i);

    }

    //This prevents the using from using ":" in his/her username
    //Since this character is used to separate different messages sent to the server.
    private String blockCharacterSet = ":";

    private InputFilter filter = new InputFilter() {

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

            if (source != null && blockCharacterSet.contains(("" + source))) {
                return "";
            }
            return null;
        }
    };


}
