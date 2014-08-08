package com.joeschwartz.iconch_android;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends Activity implements Concher.OnRecTimerListener {

    private enum STATE {STOPPED, CONCHING}

    private STATE state = STATE.STOPPED;

    private static final String TAG = "MainActivity";

    private Concher concher = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        concher = new Concher(getApplicationContext(), this);
        setCustomFonts();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopConching();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopConching();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

    void setCustomFonts() {
        try {
            Context ctx = getApplicationContext();
            //String s = getResources().getString(R.string.custom_font);
            Typeface tf = Typeface.createFromAsset(ctx.getAssets(), getResources().getString(R.string.custom_font));
            TextView tv = (TextView) findViewById(R.id.app_title);
            tv.setTypeface(tf);
            tv = (TextView) findViewById(R.id.doit_button);
            tv.setTypeface(tf);
            tv = (TextView) findViewById(R.id.power_label);
            tv.setTypeface(tf);
            tf = null;
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());

        }

    }

    @SuppressWarnings("WeakerAccess")
    protected void startConching() {
        Button btn = (Button)findViewById(R.id.doit_button);
        btn.setText(getResources().getString(R.string.label_stop));
        concher.startConching();
        state = STATE.CONCHING;
    }

    void stopConching() {
        Button btn = (Button)findViewById(R.id.doit_button);
        btn.setText(getResources().getString(R.string.label_blow));
        concher.stopConching();
        state = STATE.STOPPED;
    }

    public void doitClicked(View view) {


        switch (state) {
            case STOPPED:
                startConching();
                break;
            case CONCHING:
                stopConching();
                break;
        }
    }

    @Override
    public void onRecTimer(double recLevel) {
        TextView pwrLabel = (TextView)findViewById(R.id.power_label);
        Resources res = getResources();
        pwrLabel.setText(String.format("%s %3.2f %s", res.getString(R.string.power_label_prefix), recLevel, res.getString(R.string.power_label_suffix)));
    }
}
