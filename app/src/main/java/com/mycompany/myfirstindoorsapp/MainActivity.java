package com.mycompany.myfirstindoorsapp;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import com.mycompany.myfirstindoorsapp.ImageTargets.*;
/**
 * Sample Android project, powered by indoo.rs :)
 * 
 * @author indoo.rs | Philipp Koenig
 * 
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
    }
    
    public void startPlay(View view) { startActivity(new Intent(this, ImageTargets.class));
    }
    
    public void startMap(View view) {
        startActivity(new Intent(this, MapActivity.class));
    }

    public void startServerConnect(View view) {
        startActivity(new Intent(this,ServerConnectActivity.class));
    }
}
