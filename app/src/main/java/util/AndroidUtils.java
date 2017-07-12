package util;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

import j4pps.pogoaudiofix.BuildConfig;

public class AndroidUtils {

    private static final String TAG = "AndroidUtils";

    @SuppressWarnings("deprecation")
    public static int getColorFromResources(Context context, int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //API 23+
            return context.getResources().getColor(resId, null);
        } else {
            //API <= 22
            return context.getResources().getColor(resId);
        }
    }

    public static boolean hasNotificationAccess(Context context) {
        try {
            return Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners").contains(context.getPackageName());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void requestNotificationAccess(Context context) {
        context.startActivity(new Intent(
                "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
    }

    public static boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context
                .getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            return (context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            return (mode == AppOpsManager.MODE_ALLOWED);
        }
    }

    public static void requestUsageStatsPermission(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                context.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS)== PackageManager.PERMISSION_DENIED) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            //No need to request this for API < 23
        }
    }

    /** Open another app.
     * @param context current Context, like Activity, App, or Service
     * @param packageName the full package name of the app to open
     * @return true if likely successful, false if unsuccessful
     */
    public static boolean openApp(Context context, String packageName) {
        Log.d(TAG, "Starting package " + packageName);
        PackageManager manager = context.getPackageManager();
        try {
            Intent i = manager.getLaunchIntentForPackage(packageName);
            if (i == null) {
                Log.e(TAG, String.format(Locale.ENGLISH, "Package \'%s\' not found.", packageName));
                return false;
                //throw new PackageManager.NameNotFoundException();
            }
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(i);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void sendDebugEmail(Context context, String address) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        String[] emailAddresses = new String[1];
        emailAddresses[0] = address;
        intent.putExtra(Intent.EXTRA_EMAIL, emailAddresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, "PoGO Audio Fix debug");

        ArrayList<Uri> uris = new ArrayList<>();

        //logs
            //add some info to email
            StringBuilder text = new StringBuilder();
            text.append("\n\n\n\n\n********** Device info **********").append(System.lineSeparator());;
            text.append("App version: ").append(BuildConfig.VERSION_NAME).append(" / ").append(BuildConfig.VERSION_CODE).append(System.lineSeparator());
            text.append("Device:\t\t").append(Build.DEVICE).append(System.lineSeparator());
            text.append("Manufacturer:\t").append(Build.MANUFACTURER).append(System.lineSeparator());
            text.append("Model:\t\t").append(Build.MODEL).append(System.lineSeparator());

            //memory info
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            text.append(String.format(Locale.ENGLISH, "Total RAM:\t\t%.2f GB", (float) memoryInfo.totalMem / 1000000000)).append(System.lineSeparator());
            text.append(String.format(Locale.ENGLISH, "Available RAM:\t\t%.2f MB", (float) memoryInfo.availMem / 1000000)).append(System.lineSeparator());
            text.append(String.format(Locale.ENGLISH, "Threshold:\t\t%.2f MB", (float) memoryInfo.threshold  / 1000000)).append(System.lineSeparator());
            text.append("***********************************")
            .append(System.lineSeparator())
            .append(System.lineSeparator());

            intent.putExtra(Intent.EXTRA_TEXT, text.toString());

            BufferedReader bufferedReader = null;

            try {
                Process process = Runtime.getRuntime().exec("logcat -d");
                bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    text.append(line);
                    text.append("\n");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving logs:");
                e.printStackTrace();
                Toast.makeText(context, "Could not generate debugging logs", Toast.LENGTH_SHORT).show();
            }

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "\"It seems that you do not have an email app installed.\"", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
