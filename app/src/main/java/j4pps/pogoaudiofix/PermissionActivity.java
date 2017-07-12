package j4pps.pogoaudiofix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import util.AndroidUtils;

public class PermissionActivity extends AppCompatActivity {

    private static final String TAG = "PermissionActivity";
    private static final int REQUEST_MEDIA_PERMISSION = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int time = 1000 + 500 * progress;
                String s = String.valueOf((float) time / 1000f) + "s";
                ((TextView) findViewById(R.id.textView_secs)).setText(s);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int time = 1000 + 500 * seekBar.getProgress();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                editor.putInt(VolumeCheckService.PREF_CHECK_PAUSE, time);
                editor.apply();
            }
        });
        seekBar.setProgress((preferences.getInt(VolumeCheckService.PREF_CHECK_PAUSE, 1000) - 1000)/500);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateButtons();
    }

    private void updateButtons() {
        Button button;

        boolean notification = AndroidUtils.hasNotificationAccess(this);
        button = (Button) findViewById(R.id.button_notifcation);
        if(notification) {
            button.setText("Granted");
            button.setTextColor(AndroidUtils.getColorFromResources(this, R.color.colorGranted));
            button.setEnabled(false);
        } else {
            button.setText("Declined");
            button.setTextColor(AndroidUtils.getColorFromResources(this, R.color.colorDenied));
            button.setEnabled(true);
        }

        boolean usage = AndroidUtils.hasUsageStatsPermission(this);
        button = (Button) findViewById(R.id.button_usage);
        if(usage) {
            button.setText("Granted");
            button.setTextColor(AndroidUtils.getColorFromResources(this, R.color.colorGranted));
            button.setEnabled(false);
        } else {
            button.setText("Declined");
            button.setTextColor(AndroidUtils.getColorFromResources(this, R.color.colorDenied));
            button.setEnabled(true);
        }

        button = (Button) findViewById(R.id.button_start);
        button.setEnabled(notification && usage);
    }

    public void onButtonClick(View v) {
        if(v == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.button_notifcation:
                AndroidUtils.requestNotificationAccess(this);
                break;
            case R.id.button_usage:
                AndroidUtils.requestUsageStatsPermission(this);
                break;
            case R.id.button_start:
                startService();
                break;
            case R.id.button_stop:
                stopService();
                break;
        }
    }

    private void startService() {
        startService(new Intent(this, VolumeCheckService.class));
    }

    private void stopService() {
        stopService(new Intent(this, VolumeCheckService.class));
    }

    @Override
    protected void onDestroy() {
        stopService();
        super.onDestroy();
    }
}
