package com.uama.redpacket

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
   private lateinit var config: Config
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startService(Intent(this,MyAccessibilityService::class.java))
        toggleNotificationListenerService()
        startService(Intent(this,QHBNotificationService::class.java))
        config = Config.getConfig(this)
        checkbox_close.isChecked = config.selfCloseDetailEnable
        check_open.isChecked = config.isEnableWechat
        check_info.isChecked = config.isOnlyEnableWechatGroup
        checkbox_close.setOnCheckedChangeListener { buttonView, isChecked ->
            config.selfCloseDetailEnable = isChecked
        }
        check_open.setOnCheckedChangeListener{buttonView, isChecked ->
            config.isEnableWechat = isChecked
            startActivity(Intent(this@MainActivity,TestActivity::class.java))
        }
        check_info.setOnCheckedChangeListener { buttonView, isChecked ->
            config.setOblyEnableWechatGroup(isChecked)
        }
    }

    private fun toggleNotificationListenerService() {
        val pm = packageManager
        pm.setComponentEnabledSetting(ComponentName(this, QHBNotificationService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(ComponentName(this, QHBNotificationService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    }

    private fun openRedNotification(){
        try {
            val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } else {
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onResume() {
        super.onResume()
        if (!isFunctionSettingsOn(this)) {
            AlertDialog.Builder(this)
                    .setTitle("大佬，请于无障碍设置中，打开抢红包服务。")
                    .setMessage("本技术基于无障碍模块开发")
                    .setPositiveButton("确定"){_,_->
                        OpenAccessibilitySettingHelper.jumpToSettingPage(this)
                    }
                    .setNegativeButton("取消"){_,_->}
                    .create()
                    .show()
        }
        if(!notificationListenerEnable()){
            AlertDialog.Builder(this)
                    .setTitle("收到微信红包通知自动抢红包。")
                    .setMessage("请允许本应用监听消息")
                    .setPositiveButton("确定"){_,_->
                        openRedNotification()
                    }
                    .setNegativeButton("取消"){_,_->}
                    .create()
                    .show()
        }
    }

    private fun notificationListenerEnable():Boolean {
        var enable = false
        val packageName = QHBNotificationService::class.java.name.toLowerCase()
        val flat = Settings.Secure.getString (contentResolver, "enabled_notification_listeners")
        if (flat != null) {
            enable = flat.toLowerCase().contains(packageName)
        }
        return enable
    }

    // 此方法用来判断当前应用的辅助功能服务是否开启
   private fun isFunctionSettingsOn(context: Context): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.contentResolver,android.provider.Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            Log.i("Accessibility", e.message)
        }

        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (services != null) {
                return services.toLowerCase().contains(MyAccessibilityService::class.java.name.toLowerCase())
            }
        }
        return false
    }
}
