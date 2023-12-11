package net.stargw.applist;

import static android.content.Context.LAUNCHER_APPS_SERVICE;

import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by swatts on 17/11/15.
 */
public class AppInfoAdapterBackup extends ArrayAdapter<AppInfo> {

    // private Context mContext;
    LauncherApps launcher = (LauncherApps) getContext().getSystemService(LAUNCHER_APPS_SERVICE);

    PackageManager pManager;

    public AppInfoAdapterBackup(Context context, ArrayList<AppInfo> apps) {
        super(context, 0, apps);

        pManager = context.getPackageManager();

        this.appsOriginal = new ArrayList<AppInfo>();

        this.appsOriginal.addAll(apps);
    }

    private ArrayList<AppInfo> appsSorted; // for filtering
    private ArrayList<AppInfo> appsOriginal; // for filtering

    /*
    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
    */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        AppInfo apps = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.app_row, parent, false);
        }
        // Lookup view for data population
        TextView text1 = (TextView) convertView.findViewById(R.id.rowText1);
        TextView text2 = (TextView) convertView.findViewById(R.id.rowText2);


        // ImageView icon = (ImageView) convertView.findViewById(R.id.rowImage);
        ImageView icon = (ImageView) convertView.findViewById(R.id.rowImage);

        // Populate the data into the template view using the data object
        text1.setText(apps.name + " (" + apps.versionName + ")");
        text2.setText(apps.packageName);

        if (apps.icon != null)
        {
            icon.setImageDrawable(apps.icon);
        }

        if (apps.enabled == false)
        {
            text1.setPaintFlags(text1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            text1.setPaintFlags( text1.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
        }

        text1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppInfo app = getItem(position);
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + app.packageName));
                getContext().startActivity(intent);
            }
        });

        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppInfo app = getItem(position);
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + app.packageName));
                getContext().startActivity(intent);
            }
        });

        // Return the completed view to render on screen
        return convertView;

    }


    @Override
    public Filter getFilter() {

        Filter filter = new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                appsSorted = (ArrayList<AppInfo>)results.values;
                notifyDataSetChanged();
                clear();
                for(int i = 0, l = appsSorted.size(); i < l; i++)
                    add(appsSorted.get(i));
                notifyDataSetInvalidated();

                notifyDataSetChanged();

            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();

                constraint = constraint.toString().toLowerCase();
                FilterResults result = new FilterResults();
                if(constraint != null && constraint.toString().length() > 0)
                {
                    ArrayList<AppInfo> filteredItems = new ArrayList<AppInfo>();

                    for(int i = 0, l = appsOriginal.size(); i < l; i++)
                    {
                        AppInfo app = appsOriginal.get(i);
                        if( (app.packageName.toString().toLowerCase().contains(constraint)) ||
                                (app.name.toString().toLowerCase().contains(constraint)) )
                            filteredItems.add(app);

                    }
                    result.count = filteredItems.size();
                    result.values = filteredItems;
                }
                else
                {
                    synchronized(this)
                    {
                        result.values = appsOriginal;
                        result.count = appsOriginal.size();
                    }
                }
                return result;
            }

        };

        return filter;
    }


}
