package com.rockwonitglobal.jejudiver;

import android.content.Context;
import android.content.SharedPreferences;

public class Tools {

    public static void saveID(String field, int id, Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences("hhmsdata", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(field, id);
        editor.commit();
    }

    public static int getID(String field,Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences("hhmsdata", Context.MODE_PRIVATE);
        int diverid = sharedPreferences.getInt(field,0);
        return diverid;
    }
}
