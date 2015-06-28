package com.tbg.bitpaypos.app;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class DisplayConfirmationActivity extends ActionBarActivity {

    private DisplayConfirmationActivity currentClass = this;
    private android.os.Handler handler = new Handler();

    /**
     * Displays a confirmation message then loads the main activity
     *
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_confirmation);
        getActionBar().setTitle("Confirmation Screen");
        getSupportActionBar().setTitle("Confirmation Screen");
        final android.content.Intent intent = new Intent(currentClass, MainActivity.class);
        if(!this.getIntent().getStringExtra("COINBASE_ORDER_ID").equals("")) {
            intent.putExtra("COINBASE_ORDER_ID", this.getIntent().getStringExtra("COINBASE_ORDER_ID"));
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(intent);
                // finish this activity & get it off the stack
                finish();
            }
        }, 3500);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.display_confirmation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Goes back to main activity
     *
     */
    public void goToMain() {
        android.content.Intent intent = new Intent(currentClass, MainActivity.class);
        startActivity(intent);
        // finish this activity & get it off the stack
        finish();
    }
}
