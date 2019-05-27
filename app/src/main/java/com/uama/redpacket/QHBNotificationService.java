package com.uama.redpacket;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.uama.redpacket.util.NotifyHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>Created 16/2/4 下午11:16.</p>
 * <p><a href="mailto:codeboy2013@gmail.com">Email:codeboy2013@gmail.com</a></p>
 * <p><a href="http://www.happycodeboy.com">LeonLee Blog</a></p>
 *
 * @author LeonLee
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class QHBNotificationService extends NotificationListenerService {

    private static final String TAG = "QHBNotificationService";

    private static QHBNotificationService service;

    @Override
    public void onCreate() {
        super.onCreate();
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            onListenerConnected();
        }
    }

    @Override
    public void onNotificationPosted(final StatusBarNotification sbn) {
        if(BuildConfig.DEBUG) {
            Log.i(TAG, "onNotificationPosted");
            Log.i(TAG, "open" + "-----" + sbn.getPackageName());
            Log.i(TAG, "open" + "------" + sbn.getNotification().tickerText);
            Log.i(TAG, "open" + "-----" + sbn.getNotification().extras.get("android.title"));
            Log.i(TAG, "open" + "-----" + sbn.getNotification().extras.get("android.text"));
        }
//        if(!getConfig().isAgreement()) {
//            return;
//        }
//        if(!getConfig().isEnableNotificationService()) {
//            return;
//        }
        if(sbn == null) {
            return;
        }
        String packageName = sbn.getPackageName();
        if (!packageName.contains("com.tencent.mm") ) {
            return;
        }
        Notification nf = sbn.getNotification();
        String text = String.valueOf(sbn.getNotification().tickerText);
        Map<String, Object> map = getNotifeInfo(nf);
        if(TextUtils.isEmpty(text)&&map!=null){
            StringBuilder sb = new StringBuilder();
            Set<String> stringSet= map.keySet();
            for(String str:stringSet){
                sb.append(map.get(str));
            }
            text = sb.toString();
        }
        notificationEvent(text, nf);
    }

    private Map<String, Object> getNotifeInfo(Notification notification) {
        int key = 0;
        if (notification == null)
            return null;
        RemoteViews views = notification.contentView;
        if (views == null)
            return null;
        Class secretClass = views.getClass();

        try {
            Map<String, Object> text = new HashMap<>();

            Field outerFields[] = secretClass.getDeclaredFields();
            for (int i = 0; i < outerFields.length; i++) {
                if (!outerFields[i].getName().equals("mActions"))
                    continue;

                outerFields[i].setAccessible(true);

                ArrayList<Object> actions = (ArrayList<Object>) outerFields[i].get(views);
                for (Object action : actions) {
                    Field innerFields[] = action.getClass().getDeclaredFields();
                    Object value = null;
                    Integer type = null;
                    for (Field field : innerFields) {
                        field.setAccessible(true);
                        if (field.getName().equals("value")) {
                            value = field.get(action);
                        } else if (field.getName().equals("type")) {
                            type = field.getInt(action);
                        }
                    }
                    // 经验所得 type 等于9 10为短信title和内容，不排除其他厂商拿不到的情况
                    if (type != null && (type == 9 || type == 10)) {
                        if (key == 0) {
                            text.put("title", value != null ? value.toString() : "");
                        } else if (key == 1) {
                            text.put("text", value != null ? value.toString() : "");
                        } else {
                            text.put(Integer.toString(key), value != null ? value.toString() : null);
                        }
                        key++;
                    }
                }
                key = 0;

            }
            return text;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** 通知栏事件*/
    private void notificationEvent(String ticker, Notification nf) {
        String text = ticker;
        int index = text.indexOf(":");
        if(index != -1) {
            text = text.substring(index + 1);
        }
        text = text.trim();
        if(text.contains("[微信红包]")) { //红包消息
            newHongBaoNotification(nf);
        }
    }

    private void newHongBaoNotification(Notification notification) {
        //以下是精华，将微信的通知栏消息打开
        PendingIntent pendingIntent = notification.contentIntent;
        boolean lock = NotifyHelper.isLockScreen(getApplicationContext());

        if(!lock) {
            NotifyHelper.send(pendingIntent);
        } else {
            NotifyHelper.showNotify(getApplicationContext(), String.valueOf(notification.tickerText), pendingIntent);
        }

        if(lock || getConfig().getWechatMode() != Config.WX_MODE_0) {
            NotifyHelper.playEffect(getApplicationContext(), getConfig());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onNotificationRemoved(sbn);
        }
        if(BuildConfig.DEBUG) {
            Log.i(TAG, "onNotificationRemoved");
        }
    }

    private Config getConfig() {
        return Config.getConfig(this);
    }


    @Override
    public void onListenerConnected() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onListenerConnected();
        }

        Log.i(TAG, "onListenerConnected");
        service = this;
        //发送广播，已经连接上了
        Intent intent = new Intent(Config.ACTION_NOTIFY_LISTENER_SERVICE_CONNECT);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        service = null;
        //发送广播，已经连接上了
        Intent intent = new Intent(Config.ACTION_NOTIFY_LISTENER_SERVICE_DISCONNECT);
        sendBroadcast(intent);
    }

    /** 是否启动通知栏监听*/
    public static boolean isRunning() {
        if(service == null) {
            return false;
        }
        return true;
    }
}
