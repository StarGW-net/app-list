package net.stargw.applist;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS;
import static android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;

public class ActivityMain extends Activity {

    private static long currentProgress = 0;
    private static String toastMessage = "none";

    private static AppInfo appBackup;

    private static Dialog dialogProgress = null;

    private BroadcastListener mReceiver;

    private ListView listView;

    private static Context myContext;

    private AppInfoAdapterBackup adapter;


    //
    // We use this to handle events so GUI is not blocked
    //
    private class BroadcastListener extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            //
            // Loads apps
            //
            // Global.myLog("App Received intent", 2);
            if (Global.APPSLOADED_INTENT.equals(intent.getAction())) {
                Global.myLog("App Received intent to update screen", 2);
                // update dialog
                if (dialogProgress != null) {
                    Global.myLog("Close dialog", 2);
                    dialogProgress.dismiss();
                    dialogProgress = null;
                }
                appRefresh();
            }
            if (Global.APPSLOADING_INTENT.equals(intent.getAction())) {
                // Global.myLog("App Received intent to update saving", 2);
                // close dialog just in case we are stil showing it
                if (dialogProgress != null) {
                    ProgressBar progBar = (ProgressBar) dialogProgress.findViewById(R.id.progBar);
                    currentProgress = currentProgress + 1;
                    int x1 = (int) (currentProgress /(float)Global.packageMax*100);
                    // Global.myLog("Progress = " + x1, 2);
                    // Global.myLog("Progress = " + currentProgress + "/" +Global.packageMax, 2);
                    progBar.setProgress(x1);
                }
            }

            //
            // Copy single app
            //
            if (Global.TOAST_INTENT.equals(intent.getAction())) {
                Global.myLog("App Received intent to display toast", 2);
                Toast.makeText(Global.getContext(), toastMessage, Toast.LENGTH_SHORT).show();
            }
        }
    };

    //
    // display App Loading Dialog
    //
    void displayAppLoadDialog()
    {
        currentProgress = 0;

        // Display a loading dialog box until the app list is prepared by the service.
        dialogProgress = new Dialog(myContext);
        // dialogProgress = new ProgressDialog(myContext);

        dialogProgress.setContentView(R.layout.dialog_progress);
        dialogProgress.setTitle("Loading App List");

        TextView text = (TextView) dialogProgress.findViewById(R.id.infoMessage);
        text.setText("Building App List");

        text = (TextView) dialogProgress.findViewById(R.id.infoMessage2);
        text.setText("This may take a little while");
        // text.setGravity(i);

        // Runs on GUI thread (should I use a service)
        Thread thread = new Thread() {
            @Override
            public void run() {
                Global.appList.clear();
                Global.getAppList();
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(Global.APPSLOADED_INTENT);
                myContext.sendBroadcast(broadcastIntent);
            }
        };

        dialogProgress.show();

        thread.start();

    }



    //
    // Refresh the list view if the apps have been reloaded
    //
    void appRefresh()
    {
/*
        Iterator<Integer> it = Global.appListFW.keySet().iterator();
        appInfoSource = new ArrayList<AppInfo>();

        while (it.hasNext())
        {
            int key = it.next();
            AppInfo thisApp = Global.appListFW.get(key);
            if (thisApp.system == Global.settingsEnableExpert) {
                appInfoSource.add(thisApp);
                Logs.myLog("Add GUI app: " + thisApp.name, 3);
            }
        }
*/
        Global.myLog("Creating adaptor. System = " + Global.showSystem, 2);

        ArrayList apps = new ArrayList<AppInfo>();

        for(int i = 0, l = Global.appList.size(); i < l; i++) {
            AppInfo app = Global.appList.get(i);

            if ((app.system == Global.showSystem))
            //if ((app.enabled == true) && (app.system == Global.showSystem))
            {
                Global.myLog("Add app: " + app.name + " system = " +app.system, 2);
                apps.add(app);
            }
        }

        adapter = new AppInfoAdapterBackup(myContext, apps);
        // adapter.updateFull();
        // notify?
        adapter.notifyDataSetChanged();
        listView = (ListView) findViewById(R.id.listViewApps);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                AppInfo app = adapter.getItem(position);

                Global.myLog("Selected: " + app.packageName + " system = " + app.system, 3);

                // Do nothing yet!
                // displayCopyDialog(app);
                // backupApp(app);
            }
        });
    }

    @Override
     protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);



        myContext = this;

        Global.showSystem = false;

        Global.myLog("ActivityMain App Created", 2);

        createGUI();

    }



    @Override
    protected void onResume() {
        super.onResume();

        // Set the NAV bar the same as the top status bar
        /*
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // getWindow().setNavigationBarColor(ContextCompat.getColor(myContext, 0xff000000));
            getWindow().setNavigationBarColor(Color.BLUE);  // getWindow().getStatusBarColor()
            // window.statusBarColor = Color.BLACK;
        }
        */

        // Runs TWICE on startup. WHY? WHY? WHY?
        Global.myLog("App Resumed", 2);

        mReceiver = new BroadcastListener();
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Global.APPSLOADED_INTENT);
        mIntentFilter.addAction(Global.APPSLOADING_INTENT);
        mIntentFilter.addAction(Global.TOAST_INTENT);
        registerReceiver(mReceiver, mIntentFilter);

        // Always build an app list - packages may have changed version

        // test here ...
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());

        int appSave = p.getInt("NumberOfApps", 0);

        List<PackageInfo> packageInfoList = Global.getContext().getPackageManager().getInstalledPackages(MATCH_DISABLED_COMPONENTS | MATCH_UNINSTALLED_PACKAGES);

        PackageInfo packageInfo;

        int appNew = packageInfoList.size();

        int appLoaded = Global.appList.size();

        Global.myLog("Saved Apps = " + appSave + " : Loaded Apps = " + appLoaded + " : New Apps = " + appNew, 2);

        if ( appLoaded < 1) {
            Global.myLog("No apps loaded, so loading...",2);
            displayAppLoadDialog();
            /*
            if (appSave != appNew) {
                displayAppLoadDialog();
            } else {
                Global.appList = Global.readAppDetails();
                appRefresh();
            }
            */
        } else {
            Global.myLog("Apps loaded = " + appLoaded,2);
            if (appSave != appNew) {
                Global.myLog("New apps loaded, so loading...",2);
                displayAppLoadDialog();
            } else {
                Global.myLog("No new apps",2);
                appRefresh();
            }
        }

        /*
        // Count packages - a quick and dirty way to see if there have been any changes
        // Rather than using a package broadcast receiver
        List<PackageInfo> packageInfoList = Global.getContext().getPackageManager().getInstalledPackages(0);
        int packages = packageInfoList.size();

        Global.myLog("Previous packages = " + Global.packageMax, 2);
        Global.myLog("Current packages = " + packages, 2);

        // App build list should be underway
        if ( (Global.packageDone == false) || (Global.packageMax != packages) ) {
            displayAppLoadDialog();
        } else {
            appRefresh();
        }
        */

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dialogProgress != null)
        {
            dialogProgress.dismiss();
        }
        if(mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }


    @Override
    protected void onStop() {
        super.onStop();  // Always call the superclass method first

        Global.myLog("App Stopped", 2);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        // Global.myLog("Preparing menu", 1);

        /*
        if (Global.settingsDisableAppsMenu)
        {
            menu.findItem(R.id.action_apps_disable).setEnabled(true);
        } else {
            menu.findItem(R.id.action_apps_disable).setEnabled(false) ;
        }
        */
        return true;
    }



    private void createGUI() {

        final Context c = myContext;

        TextView text1 = (TextView) findViewById(R.id.activity_main_title);
        // text1.setText(R.string.activity_main_menu_title);
        text1.setText("User Apps");

        ImageView btn = (ImageView) findViewById(R.id.activity_main_options);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showOptionsMenu(v);
                // displayAppLoadDialog();
                // changeAppView(myContext);
            }
        });

        EditText myFilter = (EditText) findViewById(R.id.activity_main_filter_text);
        myFilter.setVisibility(View.GONE);

        ImageView mySearch = (ImageView) findViewById(R.id.activity_main_filter_icon);

        mySearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //v.getId() will give you the image id

                EditText myFilter = (EditText) findViewById(R.id.activity_main_filter_text);

                if (myFilter.getVisibility() == View.GONE) {

                    myFilter.setVisibility(View.VISIBLE);
                    myFilter.setFocusableInTouchMode(true);
                    myFilter.requestFocus();

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

                    myFilter.addTextChangedListener(new TextWatcher() {

                        public void afterTextChanged(Editable s) {
                        }

                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                            // Global.myLog("Filter on text: " + s , 3);
                            adapter.getFilter().filter(s.toString());
                        }
                    });
                } else {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(myFilter.getWindowToken(), 0);

                    myFilter.setVisibility(View.GONE);
                    myFilter.setText("");
                    // adapter.getFilter().filter(null);
                }

            }
        });
    }


    public void showOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.menu_main);
        // getMenuInflater().inflate(R.menu.menu_main, menu);

        final Menu m = popup.getMenu();
        MenuItem item;

        if (Global.showSystem == true) {
            item = m.findItem(R.id.action_system);
            item.setChecked(true);
        } else {
            item = m.findItem(R.id.action_user);
            item.setChecked(true);
        }

        /*
        item = m.findItem(R.id.action_view_logs);
        file = new File(getFilesDir() + "/" + Global.FILE_LOG_ERRORS);
        if(file.exists()) {
            item.setVisible(true);
        } else {
            item.setVisible(false);
        }
        */

        invalidateOptionsMenu();

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.action_user:
                        if (Global.showSystem == true) {
                            changeAppView(myContext);
                        }
                        return true;

                    case R.id.action_system:
                        if (Global.showSystem == false) {
                            changeAppView(myContext);
                        }
                        return true;

                    case R.id.action_refresh:
                        displayAppLoadDialog();
                        return true;
                    case R.id.action_export:
                        exportApps();
                        return true;
                    case R.id.action_help:
                        showHelp();
                        return true;

                    default:
                        return false;
                }
            }

        });
        popup.show();
    }


    public void changeAppView(final Context context)
    {

        TextView text1 = (TextView) findViewById(R.id.activity_main_title);

        // ImageView icon = (ImageView) findViewById(R.id.allSave);



        if (Global.showSystem == true) {
            Global.showSystem = false;
            text1.setText("User Apps");
            // icon.setImageResource(R.drawable.robot);
        } else {
            Global.showSystem = true;
            text1.setText("System Apps");
            // icon.setImageResource(R.drawable.user);
        }

        appRefresh();

    }

    //
    // display toast from anywhere using a global and a broadcast intent
    //
    private static void displayToast(String message)
    {
        toastMessage = message;

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Global.TOAST_INTENT);
        myContext.sendBroadcast(broadcastIntent);
    }

    //
    // Display the help screen
    //
    private void showHelp()
    {

        String verName = "latest";
        try {

            PackageInfo pInfo = myContext.getPackageManager().getPackageInfo(getPackageName(), 0);
            verName = pInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            verName = "unknown";
        }

        String url = "https://www.stargw.net/apps/log/help.html?ver=" + verName;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);

    }


    private static void exportApps() {

        Calendar today = new GregorianCalendar();
        SimpleDateFormat format = new SimpleDateFormat(myContext.getString(R.string.fileFormat));
        String humanDate = format.format(today.getTime());

        File pdfDirPath = new File(myContext.getCacheDir(), "temp");
        pdfDirPath.mkdirs();

        // File dirPath = new File(myContext.getFilesDir(), "secure");
        File file = new File(pdfDirPath, "apps-" + humanDate + ".csv");

        Global.myLog("WRITE PATH = " + file.toString(),3);
        /*
        String iPath = myContext.getExternalCacheDir() + "/apps-" + humanDate + ".csv";
        File file = new File(iPath);
        */
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
        } catch (IOException e) {
            e.toString();
            Toast.makeText(Global.getContext(), "Error Exporting to file!", Toast.LENGTH_SHORT).show();
            Global.myLog("Error Exporting: " +e,3);
            return;
        }

        String header = "app.name,app.versionName,app.packageName,app.system,app.enabled\n";
        try {
            fos.write(header.getBytes());
        } catch (IOException e) {
            e.toString();
            Toast.makeText(Global.getContext(), "Error Exporting to file!", Toast.LENGTH_SHORT).show();
            return;
        }

        // appList
        for (int i = 0; i < Global.appList.size(); i++) {
            AppInfo app = (AppInfo) Global.appList.get(i);

            // String out = String.format(java.util.Locale.US, "%s,%s,%s,%s,%s,%s%\n", app.name, app.versionName, app.packageName, app.sourcePath, String.valueOf(app.system), String.valueOf(app.enabled));

            String out = app.name + "," + app.versionName + "," +  app.packageName + "," + String.valueOf(app.system) + "," +  String.valueOf(app.enabled) + "\n";


            try {
                fos.write(out.getBytes());
            } catch (IOException e) {
                e.toString();
                Toast.makeText(Global.getContext(), "Error Exporting to file!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            fos.close();
        } catch (IOException e) {
            e.toString();
            Toast.makeText(Global.getContext(), "Error Exporting to file!", Toast.LENGTH_SHORT).show();
        }

        launchApp("apps-" + humanDate + ".csv");
        /*
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/csv");
        // Uri bmpUri = Uri.fromFile(file);
        Uri bmpUri = Uri.fromFile(file);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
        myContext.startActivity(Intent.createChooser(sharingIntent, "Share"));
        */
    }

    private static void launchApp(String fn)
    {
        File dirPath = new File(myContext.getCacheDir(), "temp");

        File f = new File(dirPath, fn);
        Global.myLog("SHARE PATH = " + f.toString(),3);

        // This provides a read only content:// for other apps
        Uri uri2 = FileProvider.getUriForFile(myContext,"net.stargw.log.fileprovider",f);
        Global.myLog("URI PATH = " + uri2.toString(),3);

        Intent intent2 = new Intent(Intent.ACTION_SEND);
        intent2.putExtra(Intent.EXTRA_STREAM, uri2);
        intent2.setType("text/csv");
        intent2.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        myContext.startActivity(intent2);


    }


}
