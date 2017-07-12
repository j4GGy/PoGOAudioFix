package j4pps.pogoaudiofix;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import util.AndroidUtils;

public class VolumeCheckService extends Service
        implements ForegroundAppChecker.OnAppInForegroundListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "VolumeCheckService";
    static final String PREF_CHECK_PAUSE = "pref_check_pause";
    static final String POGO_PACKAGE_NAME ="com.nianticlabs.pokemongo";

    ForegroundAppChecker mForegroundAppChecker = null;
    private boolean firstForegroundChange = true;
    private Handler mHandler;
    private ComponentName mNLComponentName;
    private Runnable mCheckMediaSessionsRunnable = new Runnable() {
        @Override
        public void run() {
            checkMediaSessions();
        }
    };

    private MediaSessionManager.OnActiveSessionsChangedListener mActiveSessionChangeListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNLComponentName = new ComponentName(getApplicationContext(), NotificationListener.class);

        mActiveSessionChangeListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
            @Override
            public void onActiveSessionsChanged(@Nullable List<MediaController> controllers) {
                Log.d(TAG, "onActiveSessionsChanged()");
                if(isSomePlaybackActive(controllers)) {
                    startTrackingPoGO();
                } else {
                    Log.d(TAG, "Pausing foreground tracking since no media session is playing");
                    if(mForegroundAppChecker != null) {
                        mForegroundAppChecker.pause();
                    }
                }
            }
        };

        mHandler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStart()");

        //check if all permissions are there
        if(!AndroidUtils.hasNotificationAccess(getApplicationContext())) {
            Toast.makeText(this, "Need notification access!", Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        if(!AndroidUtils.hasUsageStatsPermission(getApplicationContext())) {
            Toast.makeText(this, "Need usage stats permission!", Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        Toast.makeText(this, "PoGO Audio fix: Background service started", Toast.LENGTH_SHORT).show();

        //listen to preference changes
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);

        //listen to media session changes
        getMediaSessionManager().addOnActiveSessionsChangedListener(mActiveSessionChangeListener, mNLComponentName);

        firstForegroundChange = true;
        if(isSomePlaybackActive(getMediaSessionManager().getActiveSessions(mNLComponentName))) {
            startTrackingPoGO();
        } else {
            Log.d(TAG, "No need to start tracking, yet: no media session is playing.");
        }

        AndroidUtils.openApp(getApplicationContext(), POGO_PACKAGE_NAME);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Stopping service");
        if(mForegroundAppChecker != null) {
            mForegroundAppChecker.removeListener();
            mForegroundAppChecker.pause();
            mForegroundAppChecker.cleanUp();
        }

        getMediaSessionManager().removeOnActiveSessionsChangedListener(mActiveSessionChangeListener);

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);

        Toast.makeText(this, "PoGO Audio fix: Background service stopped", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        Toast.makeText(this, "PoGO Audio fix: Low memory warning", Toast.LENGTH_SHORT).show();
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        Toast.makeText(this, "PoGO Audio fix: Trim memory warning. Level: " + level, Toast.LENGTH_SHORT).show();
        super.onTrimMemory(level);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PREF_CHECK_PAUSE:
                if(mForegroundAppChecker != null) {
                    mForegroundAppChecker.setTimeBetweenChecks(sharedPreferences.getInt(key, 1000));
                }
                break;
        }
    }



    private void startTrackingPoGO() {
        if(mForegroundAppChecker == null) {
            Log.d(TAG, "Tracking foreground mode of " + POGO_PACKAGE_NAME);
            mForegroundAppChecker = new ForegroundAppChecker(getApplicationContext(), POGO_PACKAGE_NAME);
            mForegroundAppChecker.setListener(this);
            mForegroundAppChecker.setTimeBetweenChecks(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(PREF_CHECK_PAUSE, 1000));
            mForegroundAppChecker.run();
        }
    }

    private boolean isSomePlaybackActive(List<MediaController> controllers) {
        if(controllers == null) {
            Log.w(TAG, "No MediaControllers received.");
            return false;
        }
        Log.d(TAG, "Media sessions: " + controllers.size());
        for (MediaController mc : controllers) {
            if (mc.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) {
                return true;
            }
        }
        return false;
    }

    private void checkMediaSessions() {
        try {
            adjustVolume(getMediaSessionManager().getActiveSessions(mNLComponentName));
        } catch (Exception e) {
            Toast.makeText(this, e.getClass().getSimpleName(),Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void adjustVolume(List<MediaController> controllers) {
        if(controllers == null) {
            Log.d(TAG, "No MediaControllers received.");
            return;
        }
        Log.d(TAG, "Media sessions: " + controllers.size());
        for (MediaController mc : controllers) {
            Log.d(TAG, mc.getPackageName());
            Log.d(TAG, String.format("Volume: %d/%d", mc.getPlaybackInfo().getCurrentVolume(), mc.getPlaybackInfo().getMaxVolume()));
            if (mc.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) {
                Log.d(TAG, "Sending Pause&Play commands");
                mc.getTransportControls().pause();
                mc.getTransportControls().play();
            }
        }
    }


    MediaSessionManager getMediaSessionManager() {
        return(MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
    }


    //ForegroundAppChecker callbacks
    @Override
    public void onAppSwitchedToForeground() {
        Log.d(TAG, "Pokemon GO switched to foreground");

        mHandler.post(mCheckMediaSessionsRunnable);
        if(firstForegroundChange) {
            Log.d(TAG, "Launching some extra volume checks because of first start");
            mHandler.postDelayed(mCheckMediaSessionsRunnable, 5000);
            mHandler.postDelayed(mCheckMediaSessionsRunnable, 15000);
            mHandler.postDelayed(mCheckMediaSessionsRunnable, 20000);
            mHandler.postDelayed(mCheckMediaSessionsRunnable, 25000);
            firstForegroundChange = false;
        }
    }

    @Override
    public void onAppSwitchedToBackground() {
        //Log.d(TAG, "onAppSwitchedToBackground()");
    }
}
