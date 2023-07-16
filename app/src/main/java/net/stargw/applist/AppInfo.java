package net.stargw.applist;

import android.graphics.drawable.Drawable;

import java.io.Serializable;

/**
 * Created by swatts on 17/11/15.
 */
public class AppInfo implements Serializable {



    public AppInfo() {
        // TODO Auto-generated constructor stub
    }

    public String name;
    public String packageName;
    public Drawable icon;
    // public Bitmap icon;
    public String versionName;
    public String sourcePath;
    public boolean system = false;
    public boolean enabled = true;

    // Where do I put the method to build this?


    public AppInfo (String app, String packName,Drawable icon, Boolean k, Boolean s)
    {
        name = app;
        packageName = packName;
    }




}
