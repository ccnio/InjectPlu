package com.ccnio.lib;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by jianfeng.li on 21-2-9.
 */
public class Test {
    public void test(Context context){
        Object obj = null;
        Boolean a = (Boolean) obj;
        SharedPreferences prefer = context.getSharedPreferences("text.share", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefer.edit();
//        edit.putBoolean("keys", a);
        edit.putString("keys", null);

        edit.apply();

    }
}
