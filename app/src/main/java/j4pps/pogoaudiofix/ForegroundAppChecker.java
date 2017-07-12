package j4pps.pogoaudiofix;


import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

class ForegroundAppChecker extends Thread{

    private static final String TAG = ForegroundAppChecker.class.getSimpleName();


    public interface OnAppInForegroundListener {
        void onAppSwitchedToForeground();
        void onAppSwitchedToBackground();
    }

    private UsageStatsManager mUsageManager = null;
    private Handler mHandler = null;
    private String mPackageName;
    private int mCheckPause = 1000;
    private boolean mIsPaused = false;

    private OnAppInForegroundListener mListener = null;

    ForegroundAppChecker(Context context, String packageName){
        mPackageName = packageName;
        mUsageManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
    }

    public void setListener(OnAppInForegroundListener listener) {
        mListener = listener;
    }

    public void removeListener() {
        mListener = null;
    }

    public void setTimeBetweenChecks(int time) { mCheckPause = time; }

    public void pause() {
        mIsPaused = true;
    }

    public void run(){

        Log.d(TAG, "Starting to listen for "  + mPackageName);

        if(Looper.myLooper() == null) {
            Looper.prepare();
        }
        if(mHandler == null) {
            mHandler = new Handler(Looper.myLooper());
        }

        mIsPaused = false;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mListener != null) {
                    checkNow();
                } else {
                    Log.w(TAG, "OnAppForegroundListener is null");
                }

                if(mIsPaused) {
                    Log.d(TAG, "Foreground check is cancelled. Launching no more checks");
                } else {
                    Log.d(TAG, "Launching next check in " + String.valueOf((float) mCheckPause / 1000f) + "s");
                    mHandler.postDelayed(this, mCheckPause);
                    Looper.loop();
                }
            }
        });
    }

    void checkNow() {
        long t = System.currentTimeMillis();
        UsageEvents events = mUsageManager.queryEvents(t - mCheckPause * 5 / 4, t);
        UsageEvents.Event event = new UsageEvents.Event();
        while (events.getNextEvent(event)) {
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                //Log.d(TAG, "Foreground app: " + event.getPackageName());
                //Log.d(TAG, "Foreground activity: " + event.getClassName());
                if (event.getPackageName().equals(mPackageName)) {
                        mListener.onAppSwitchedToForeground();
                }
            }
        }
    }

    void cleanUp() {
        mUsageManager = null;
        mListener = null;
        mHandler = null;
    }
}
