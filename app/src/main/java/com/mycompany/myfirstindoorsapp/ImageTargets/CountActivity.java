package com.mycompany.myfirstindoorsapp.ImageTargets;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.mycompany.myfirstindoorsapp.R;

/**
 * Created by Tijs on 18/03/2015.
 */
public class CountActivity extends Activity{

    private int count;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        count = 0;
    }

   /* public void onClickCollectButton(View view){


        final TextView counterText = (TextView) findViewById(R.id.counter);
        count ++;
        String countString = ""+count;
        counterText.setText(countString);
        //addOverlayView(false);

        *//*Button collectButton = (Button) findViewById(id.collect_button);
        collectButton.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {
                     final TextView counterText = (TextView) findViewById(R.id.counter);
                     count = count + 1;
                     Log.d("onClickCollectButton", "count: " + count);
                     String countString = ""+count;
                     counterText.setText(countString);
                     Log.d("onClickCollectButton", "count: " + count);
                 }
             }
        );
        *//*
    }*/
}
