package com.rockwonitglobal.jejudiver;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class Tools {

    public static void saveID(String field, String id, Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences("hhmsdata", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(field, id);
        editor.commit();
    }

    public static String getID(String field,Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences("hhmsdata", Context.MODE_PRIVATE);
        String diverid = sharedPreferences.getString(field,"0");
        return diverid;
    }

    public static void saveField(String field, int id, Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences("hhmsdata", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(field, id);
        editor.commit();
    }

    public static int getField(String field,Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences("hhmsdata", Context.MODE_PRIVATE);
        int id = sharedPreferences.getInt(field,0);
        return id;
    }

    public static String getData(String str, String key)
    {

        Map<String, String> map = new HashMap<String, String>();


        if(str !=null && !"".equals(str))
        {

            for(String str1 : str.split("\\^\\^") )
            {
                String[] str2 = str1.split("\\|\\|");

                if(str2 != null && str2.length == 2)
                {

                    String key1 = str2[0].trim();
                    String value1 = str2[1].trim();

                    map.put(key1, value1);

                }

            }
        }

        return  (map.get(key) == null ? "" : String.valueOf(map.get(key)));

    }
}
