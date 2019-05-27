package com.uama.redpacket;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import java.util.List;


public class OpenAccessibilitySettingHelper {

    public static void jumpToSettingPage(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ignore) {
            Log.i("Accessibility","页面未找到");
        }
    }


    /**o
     * 判断是否有辅助功能权限
     * @return true 已开启
     *          false 未开启
     */
    public static boolean isAccessibilitySettingsOn(Context context,String className){
        if (context == null){
            return false;
        }
        AccessibilityManager activityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        assert activityManager != null;
        List<AccessibilityServiceInfo> runningServices = activityManager.getInstalledAccessibilityServiceList();
        if (runningServices.size()<=0){
            return false;
        }
        for (int i=0;i<runningServices.size();i++){
            String service = runningServices.get(i).getId().replace("/","");
            if (service.equals(className)){
                return true;
            }
        }
        return false;
    }

}