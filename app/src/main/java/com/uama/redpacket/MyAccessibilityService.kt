package com.uama.redpacket

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MyAccessibilityService : AccessibilityService() {

    private lateinit var config: Config
    override fun onServiceConnected() {
        config = Config.getConfig(this)
//        val info = AccessibilityServiceInfo()
//        info.apply {
//            // Set the type of events that this service wants to listen to. Others
//            // won't be passed to this service.
//            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
//
//            // If you only want this service to work with specific applications, set their
//            // package names here. Otherwise, when the service is activated, it will listen
//            // to events from all applications.
//            packageNames = arrayOf("com.tencent.mm", "com.uama.smartcampusfortute")
//            // Set the type of feedback your service will provide.
//            feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
//
//            // Default services are invoked only if no package-specific ones are present
//            // for the type of AccessibilityEvent generated. This service *is*
//            // application-specific, so the flag isn't necessary. If this was a
//            // general-purpose service, it would be worth considering setting the
//            // DEFAULT flag.
//
//            // flags = AccessibilityServiceInfo.DEFAULT;
//
//            notificationTimeout = 100
//        }
//
//        this.serviceInfo = info

    }

    override fun onInterrupt() {
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!config.isEnableWechat) return
        resetOldClassName(event)
        when (event?.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                handleNotification(event)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                when (oldClassName) {
                    "com.tencent.mm.ui.LauncherUI" -> {
                        traverseChatList()
                    }
                    "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI" -> {
                        if (!openPacket()) {
                            tryOpenRedPacketCount = 0
                            dealRedPacket()
                        }
                    }
                    "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI" -> {
                        if (!config.selfCloseDetailEnable) return
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                when (oldClassName) {
                    "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI" -> {
                        tryOpenRedPacketCount = 0
                        dealRedPacket()
                    }
                }
            }
        }
    }

    /**
     * 模拟点击,拆开红包
     */
    private fun openPacket(): Boolean {
        if (rootInActiveWindow != null) {
            //为了演示,直接查看了红包控件的id
            val list = rootInActiveWindow!!.findAccessibilityNodeInfosByViewId("@id/b9m")
            list.addAll(rootInActiveWindow!!.findAccessibilityNodeInfosByText("開"))
            rootInActiveWindow!!.recycle()
            for (item in list) {
                item.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            return list.isNotEmpty()
        }
        return false
    }

    /**
     * 处理通知栏信息
     *
     * 如果是微信红包的提示信息,则模拟点击
     *
     * @param event
     */
    private fun handleNotification(event: AccessibilityEvent) {
        val texts = event.text
        if (!texts.isEmpty()) {
            for (text in texts) {
                val content = text.toString()
                //如果微信红包的提示信息,则模拟点击进入相应的聊天窗口
                if (content.contains("[微信红包]")) {
                    if (event.parcelableData != null && event.parcelableData is Notification) {
                        val notification = event.parcelableData as Notification
                        val pendingIntent = notification.contentIntent
                        try {
                            pendingIntent.send()
                        } catch (e: PendingIntent.CanceledException) {
                            e.printStackTrace()
                        }

                    }
                }
            }
        }
    }

    private val redPacketPager: MutableList<String> =
            mutableListOf("com.tencent.mm.ui.LauncherUI", "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI",
                    "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI",
                    "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI")

    private var oldClassName = ""
    private fun resetOldClassName(event: AccessibilityEvent?) {
        if (redPacketPager.indexOf(event?.className) != -1) {
            oldClassName = event?.className.toString()
        }
    }

    private fun traverseChatList() {
        if (rootInActiveWindow != null) {
            findNode = null
            recycleNode(rootInActiveWindow)
            clickNode(findNode)
        }
    }

    private fun clickNode(node: AccessibilityNodeInfo?) {
        if (node == null) return
        if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.recycle()
        }
        else clickNode(node.parent)
    }

    /**
     * 递归查找当前聊天窗口中的红包信息
     *
     * 聊天窗口中的红包都存在"领取红包"一词,因此可根据该词查找红包
     *
     * @param node
     */
    private fun recycleNode(node: AccessibilityNodeInfo?) {
        if (node == null) return
        if (config.isOnlyEnableWechatGroup) {

        }
        if (isEnableNode(node)) return
        else {
            for (i in 0 until node.childCount) {
                if (node.getChild(i) != null) {
                    recycleNode(node.getChild(i))
                }
            }
        }
    }

    private var findNode: AccessibilityNodeInfo? = null
//    private var isGroupNode: Boolean = false
    private fun isEnableNode(node: AccessibilityNodeInfo): Boolean {
        if (node.childCount == 0) {
            val str = node.text?.toString() + node.contentDescription
            Log.i("MyAccessibilityService", "内容：$str")
//            isGroupNode = isGroupNode|| (str.indexOf("(") != -1 && (str.indexOf(")") != -1))
            if (str.contains("领取红包") || str.contains("微信红包")
                    || str.contains("恭喜发财，大吉大利")) {
                checkLosePacket(node)
//                if(!isGroupNode)
//                    return false
                if (!isLoseContains) {
                    findNode = node
                    return true
                }
            }
        }
        return false
    }

    /**
     * 为避免一直点开已领取，已过期红包
     * 我们于此将过期红包失效
     */
    private fun checkLosePacket(node: AccessibilityNodeInfo) {
        isLoseContains = false
        nodeContainLoseInfo(node.parent)
        nodeContainLoseInfo(node.parent.parent)
    }

    private var isLoseContains = false
    private fun nodeContainLoseInfo(node: AccessibilityNodeInfo?) {
        if (node == null) return
        if (node.childCount == 0) {
            val childInfo = node.text?.toString() + node.contentDescription
            if (childInfo.contains("已领取") || childInfo.contains("已过期")
                    || childInfo.contains("已被领完") || childInfo.contains("领取了你的")) {
                isLoseContains = true
                return
            }
        } else {
            for (i in 0 until node.childCount) {
                nodeContainLoseInfo(node.getChild(i))
            }
        }
    }

    private var tryOpenRedPacketCount = 0
    /**
     * 点开红包
     */
    private fun dealRedPacket() {
        tryOpenRedPacketCount++
        if (rootInActiveWindow != null) {
            findNode = null

            recycleRedNode(rootInActiveWindow)
            if (findNode == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && windows.isNotEmpty() && windows.size >= 3) {
                recycleRedNode(windows[2].root)
            }
            clickNode(findNode)
            if (findNode == null&&tryOpenRedPacketCount<6) {
                Log.i("MyAccessibilityService","重试$tryOpenRedPacketCount")
                Handler().postDelayed({
                    dealRedPacket()
                }, 1000)
            }
            return
        }
    }

    private fun recycleRedNode(node: AccessibilityNodeInfo?) {
        if (node == null) return
        val isOverTime = rootInActiveWindow?.findAccessibilityNodeInfosByText("查看领取详情")?.isNotEmpty() == true
        if (node.childCount == 0) {
            val str = node.className
            Log.i("MyAccessibilityService", "类名：$str")
            Log.i("MyAccessibilityService", "描述：${node.text}")
            if (str.contains("Button")) {
                findNode = node
                return
            }
            if (isOverTime && str.contains("ImageView") && node.isClickable) {
                findNode = node
                return
            }
        } else {
            for (i in 0 until node.childCount) {
                if (node.getChild(i) != null) {
                    recycleRedNode(node.getChild(i))
                }
            }
        }
    }


}