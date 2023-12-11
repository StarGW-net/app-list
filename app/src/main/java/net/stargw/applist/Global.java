package net.stargw.applist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import static android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS;
import static android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;

public class Global extends Application {


	public static final String TAG = "LOGAPK";

	private static Context mContext;

	static ArrayList<AppInfo> appList = new ArrayList<AppInfo>();

	public static boolean showSystem = false;

	private static boolean debug = false;

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;

	}

	static final int CHUNK = (1024*50);

	static boolean packageDone = false;
	static int packageMax = 0;

	static final String APPSLOADED_INTENT = "net.stargw.applist.intent.action.APPSLOADED";
	static final String APPSLOADING_INTENT = "net.stargw.applist.intent.action.APPSLOADING";
	static final String TOAST_INTENT = "net.stargw.applist.intent.action.TOAST";

	public static Context getContext() {
		return mContext;
	}

	public static void infoMessageLeft(final Context context, String header, String message) {
		infoMessageDo(context, header, message, Gravity.LEFT);
	}

	//
	// Display a popup info screen
	//
	public static void infoMessageDo(final Context context, String header, String message, int i) {
		final Dialog info = new Dialog(context);

		info.setContentView(R.layout.dialog_info);
		info.setTitle(header);

		TextView text = (TextView) info.findViewById(R.id.infoMessage);
		text.setText(message);
		text.setGravity(i);

		Button dialogButton = (Button) info.findViewById(R.id.infoButton);


		dialogButton.setOnClickListener(new OnClickListener() {
			// @Override
			public void onClick(View v) {
				// notificationCancel(context);
				info.cancel();
			}
		});


		info.show();
		Global.myLog(header + ":" + message, 3);
	}


	public static void getAppList()
	{

		packageDone = false;

		List<PackageInfo> packageInfoList = Global.getContext().getPackageManager().getInstalledPackages(MATCH_DISABLED_COMPONENTS | MATCH_UNINSTALLED_PACKAGES);

		PackageInfo packageInfo;

		packageMax = packageInfoList.size();


		for(int i = 0; i<packageInfoList.size();i++)
		{
			try {
				packageInfo = packageInfoList.get(i);
				// Global.appList.add(Global.getAppListApp(packageInfo));
			} catch (Exception e) {
				Global.myLog("Cannot get package info...skipping", 3);
				continue;
			}

			AppInfo myApp = getAppDetail(packageInfo);
			if (myApp != null)
			{
				appList.add(myApp);
			}
			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction(Global.APPSLOADING_INTENT);
			mContext.sendBroadcast(broadcastIntent);

		}
		// Global.myLog("Built an installed app list of: " + packageInfoList.size(),2);

		Collections.sort(appList, new Comparator<AppInfo>() {
			public int compare(AppInfo appInfoA, AppInfo appInfoB) {
				return appInfoA.name.compareToIgnoreCase(appInfoB.name);
			}
		});
		packageDone = true;

		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
		p.edit().putInt("NumberOfApps", packageInfoList.size()).commit();

		// writeAppDetails(appList);
	}

	public static AppInfo getAppDetail(PackageInfo packageInfo) {

		AppInfo app = new AppInfo();

		ApplicationInfo applicationInfo = packageInfo.applicationInfo;

		PackageManager pManager = mContext.getPackageManager();
		// ActivityManager aManager = (ActivityManager) mContext.getSystemService(mContext.ACTIVITY_SERVICE);

		app.packageName = packageInfo.packageName;
		app.versionName = packageInfo.versionName;
		app.sourcePath = applicationInfo.sourceDir;

		app.enabled = applicationInfo.enabled;

		app.name = pManager.getApplicationLabel(applicationInfo).toString();

		// Look only for system apps
		if (!((applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0)) {
			//return null;
			app.system = true;
		}


		Global.myLog("App Name:      " + app.name, 3);
		Global.myLog("Package Name:  " + app.packageName, 3);
		Global.myLog("App Version:      " + app.versionName, 3);
		Global.myLog("System:        " + app.system, 3);
		Global.myLog("Path:        " + app.sourcePath, 3);


		// drawable is not seralizable so cannot save it...


		try {
			app.icon = pManager.getApplicationIcon(app.packageName);
			// app.icon = applicationInfo.loadIcon(pManager);
/*
			LauncherApps launcher = (LauncherApps) getContext().getSystemService(LAUNCHER_APPS_SERVICE);
			List<LauncherActivityInfo> activityList = launcher.getActivityList(app.packageName, android.os.Process.myUserHandle());
			app.icon = activityList.get(0).getBadgedIcon(0);
 */
		} catch (Exception e) {
			app.icon = getContext().getResources().getDrawable(R.drawable.robot);
			Global.myLog("Cannot get icon!", 3);
		}

		// build up a separate class object with the icons in - need to do on discover and load...
		/*
        try {
            // app.icon = pManager.getApplicationIcon(app.packageName);
            Drawable d = pManager.getApplicationIcon(apps.packageName);
            icon.setImageDrawable(d);
        } catch (Exception e) {
            icon.setImageDrawable(getContext().getResources().getDrawable(R.drawable.robot));
            Global.myLog("Cannot get icon!", 3);
        }
        */

		return app;

	}

	public static void myLog(String buf,int level)
	{
		if (debug == true) {
			Log.w(TAG, buf);
		}

	}

	/*

	public String name;
    public String packageName;
    public Drawable icon;
    public String versionName;
    public String sourcePath;
    public long size = 0;
    public boolean system = false;
    public boolean enabled = true;
	*/

	// public static void writeAppDetails(Map<String, String, Drawable, String, String, boolean, boolean> data) {
	public static void writeAppDetails(ArrayList<AppInfo> data) {
		File appDir = mContext.getFilesDir();
		File mypath=new File(appDir,"myAppDetails");
		try {
			FileOutputStream myFile = new FileOutputStream(mypath);
			// FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			ObjectOutputStream os = new ObjectOutputStream(myFile);
			os.writeObject(data);
			os.close();
			myFile.close();
		} catch (IOException e) {
			Log.w(TAG, e);
		}
	}

	public static ArrayList<AppInfo> readAppDetails() {
		Properties properties = new Properties();
		File appDir = mContext.getFilesDir();
		File mypath=new File(appDir,"myAppDetails");
		ArrayList<AppInfo> apps = new ArrayList<AppInfo>();
		try {
			FileInputStream myFile = new FileInputStream(mypath);
			ObjectInputStream is = new ObjectInputStream(myFile);
			try {
				apps = (ArrayList<AppInfo>) is.readObject();
			} catch (ClassNotFoundException e) {
				Log.w(TAG, e);
			}
			is.close();
			myFile.close();
		} catch (IOException e) {
			Log.w(TAG, e);
		}
		return  apps;
	}



}
