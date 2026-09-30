package com.github.tvbox.osc.ui.activity

import android.content.Intent
import android.text.TextUtils
import android.view.View
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.bean.Source
import com.github.tvbox.osc.bean.Subscription
import com.github.tvbox.osc.databinding.ActivitySubscriptionBinding
import com.github.tvbox.osc.ui.adapter.SubscriptionAdapter
import com.github.tvbox.osc.ui.dialog.ChooseSourceDialog
import com.github.tvbox.osc.ui.dialog.MultiLinePreviewDialog
import com.github.tvbox.osc.ui.dialog.SubsTipDialog
import com.github.tvbox.osc.ui.dialog.SubsciptionDialog
import com.github.tvbox.osc.ui.dialog.SubsciptionDialog.OnSubsciptionListener
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.SubscriptionHealthChecker
import com.github.tvbox.osc.util.Utils
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.lxj.xpopup.XPopup
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import com.obsez.android.lib.filechooser.ChooserDialog
import com.orhanobut.hawk.Hawk
import java.util.function.Consumer

class SubscriptionActivity : BaseVbActivity<ActivitySubscriptionBinding>() {

    private var mBeforeUrl = Hawk.get(HawkConfig.API_URL, "")
    private var mSelectedUrl = ""
    private var mSubscriptions: MutableList<Subscription> = Hawk.get(HawkConfig.SUBSCRIPTIONS, ArrayList())
    private var mSubscriptionAdapter = SubscriptionAdapter()
    private val mSources: MutableList<Source> = ArrayList()

    override fun init() {

        mBinding.rv.setAdapter(mSubscriptionAdapter)
        mSubscriptions.forEach(Consumer { item: Subscription ->
            if (item.isChecked) {
                mSelectedUrl = item.url
            }
        })

        mSubscriptionAdapter.setNewData(mSubscriptions)
        refreshEmptyState()

        mBinding.btnPasteAdd.setOnClickListener { pasteAddFromClipboard() }
        mBinding.btnAdd.setOnClickListener { showAddSubscriptionDialog() }
        mBinding.btnCheck.setOnClickListener { checkAllSubscriptions(true) }
        mBinding.btnEmptyPaste.setOnClickListener { pasteAddFromClipboard() }
        mBinding.btnEmptyAdd.setOnClickListener { showAddSubscriptionDialog() }
        // 进入页面自动检测一次有效性（后台）
        mBinding.rv.post { checkAllSubscriptions(false) }

        mBinding.ivUseTip.setOnClickListener {
            XPopup.Builder(this)
                .asCustom(SubsTipDialog(this))
                .show()
        }

        mBinding.titleBar.rightView.setOnClickListener { showAddSubscriptionDialog() }

        mSubscriptionAdapter.setOnItemChildClickListener { _: BaseQuickAdapter<*, *>?, view: View, position: Int ->
            LogUtils.d("删除订阅")
            if (view.id == R.id.iv_del) {
                if (mSubscriptions.get(position).isChecked) {
                    ToastUtils.showShort("不能删除当前使用的订阅")
                    return@setOnItemChildClickListener
                }
                XPopup.Builder(this@SubscriptionActivity)
                    .asConfirm("删除订阅", "确定删除订阅吗？") {
                        mSubscriptions.removeAt(position)
                        //删除/选择只刷新,不触发重新排序
                        mSubscriptionAdapter.notifyDataSetChanged()
                    }.show()
            }
        }

        mSubscriptionAdapter.setOnItemClickListener { _: BaseQuickAdapter<*, *>?, _: View?, position: Int ->  //选择订阅
            for (i in mSubscriptions.indices) {
                val subscription = mSubscriptions[i]
                if (i == position) {
                    subscription.setChecked(true)
                    mSelectedUrl = subscription.url
                } else {
                    subscription.setChecked(false)
                }
            }
            // 立即持久化，避免仅 onPause 时才写入导致异常退出丢失
            Hawk.put(HawkConfig.API_URL, mSelectedUrl)
            Hawk.put(HawkConfig.SUBSCRIPTIONS, mSubscriptions)
            mSubscriptionAdapter.notifyDataSetChanged()
            ToastUtils.showShort("已切换：" + mSubscriptions[position].name)
        }

        mSubscriptionAdapter.onItemLongClickListener =
            BaseQuickAdapter.OnItemLongClickListener { adapter: BaseQuickAdapter<*, *>?, view: View, position: Int ->
                val item = mSubscriptions[position]
                XPopup.Builder(this)
                    .atView(view.findViewById(R.id.tv_name))
                    .hasShadowBg(false)
                    .asAttachList(
                        arrayOf(
                            if (item.isTop) "取消置顶" else "置顶",
                            "重命名",
                            "复制地址"
                        ), null
                    ) { index: Int, _: String? ->
                        when (index) {
                            0 -> {
                                item.isTop = !item.isTop
                                mSubscriptions[position] = item
                                mSubscriptionAdapter.setNewData(mSubscriptions)
                                refreshEmptyState()
                            }
                            1 -> {
                                XPopup.Builder(this)
                                    .asInputConfirm(
                                        "更改为",
                                        "",
                                        item.name,
                                        "新的订阅名",
                                        { text ->
                                            if (!TextUtils.isEmpty(text)) {
                                                if (text.trim { it <= ' ' }.length > 8) {
                                                    ToastUtils.showShort("不要过长,不方便记忆")
                                                } else {
                                                    item.name = text.trim { it <= ' ' }
                                                    mSubscriptionAdapter.notifyItemChanged(position)
                                                }
                                            }
                                        },
                                        null,
                                        R.layout.dialog_input
                                    ).show()
                            }
                            2 -> {
                                ClipboardUtils.copyText(mSubscriptions.get(position).url)
                                ToastUtils.showLong("已复制")
                            }
                        }
                    }.show()
                true
            }
    }

    private fun showPermissionTipPopup(checked: Boolean) {
        XPopup.Builder(this@SubscriptionActivity)
            .isDarkTheme(Utils.isDarkTheme())
            .asConfirm("提示", "这将访问您设备文件的读取权限") {
                XXPermissions.with(this)
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (all) {
                                pickFile(checked)
                            } else {
                                ToastUtils.showLong("部分权限未正常授予,请授权")
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                ToastUtils.showLong("读写文件权限被永久拒绝，请手动授权")
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(
                                    this@SubscriptionActivity,
                                    permissions
                                )
                            } else {
                                ToastUtils.showShort("获取权限失败")
                                showPermissionTipPopup(checked)
                            }
                        }
                    })
            }.show()
    }

    /**
     *
     * @param checked 与showPermissionTipPopup一样,只记录并传递选中状态
     */
    private fun pickFile(checked: Boolean) {
        ChooserDialog(this@SubscriptionActivity, R.style.FileChooser)
            .withFilter(false, false, "txt", "json")
            .withStartFile(
                if (TextUtils.isEmpty(Hawk.get("before_selected_path"))) "/storage/emulated/0/Download" else Hawk.get(
                    "before_selected_path"
                )
            )
            .withChosenListener(ChooserDialog.Result { _, pathFile ->
                Hawk.put("before_selected_path", pathFile.parent)
                val clanPath =
                    pathFile.absolutePath.replace("/storage/emulated/0", "clan://localhost")
                for (item in mSubscriptions) {
                    if (item.url == clanPath) {
                        ToastUtils.showLong("订阅地址与" + item.name + "相同")
                        return@Result
                    }
                }
                addSubscription(pathFile.name, clanPath, checked)
            })
            .build()
            .show()
    }

    private fun addSubscription(name: String, url: String, checked: Boolean) {
        if (url.startsWith("clan://")) {
            addSub2List(name, url, checked)
            mSubscriptionAdapter.setNewData(mSubscriptions)
            refreshEmptyState()
        } else if (url.startsWith("http")) {
            showLoadingDialog()
            OkGo.get<String>(url)
                .tag("get_subscription")
                .execute(object : AbsCallback<String?>() {
                    override fun onSuccess(response: Response<String?>) {
                        dismissLoadingDialog()
                        try {
                            val json = JsonParser.parseString(response.body()).asJsonObject
                            // 多线路?
                            val urls = json["urls"]
                            // 多仓?
                            val storeHouse = json["storeHouse"]
                            if (urls != null && urls.isJsonArray) { // 多线路 → 预览勾选导入
                                val urlList = urls.asJsonArray
                                if (urlList != null && urlList.size() > 0 && urlList[0].isJsonObject
                                    && urlList[0].asJsonObject.has("url")
                                    && urlList[0].asJsonObject.has("name")
                                ) {
                                    val lines = ArrayList<MultiLinePreviewDialog.LineItem>()
                                    for (i in 0 until urlList.size()) {
                                        val obj = urlList[i] as JsonObject
                                        val name = obj["name"].asString.trim { it <= ' ' }
                                            .replace("<|>|《|》|-".toRegex(), "")
                                        val lineUrl = obj["url"].asString.trim { it <= ' ' }
                                        if (lineUrl.isNotEmpty()) {
                                            lines.add(MultiLinePreviewDialog.LineItem(name.ifEmpty { "线路${i + 1}" }, lineUrl))
                                        }
                                    }
                                    if (lines.isEmpty()) {
                                        ToastUtils.showShort("多线路为空")
                                    } else {
                                        XPopup.Builder(this@SubscriptionActivity)
                                            .asCustom(
                                                MultiLinePreviewDialog(
                                                    this@SubscriptionActivity,
                                                    lines
                                                ) { selected ->
                                                    importLines(selected, checked)
                                                }
                                            ).show()
                                    }
                                    return
                                }
                            } else if (storeHouse != null && storeHouse.isJsonArray) { // 多仓
                                val storeHouseList = storeHouse.asJsonArray
                                if (storeHouseList != null && storeHouseList.size() > 0 && storeHouseList[0].isJsonObject
                                    && storeHouseList[0].asJsonObject.has("sourceName")
                                    && storeHouseList[0].asJsonObject.has("sourceUrl")
                                ) { //多仓格式
                                    mSources.clear()
                                    for (i in 0 until storeHouseList.size()) {
                                        val obj = storeHouseList[i] as JsonObject
                                        val name = obj["sourceName"].asString.trim { it <= ' ' }
                                            .replace("<|>|《|》|-".toRegex(), "")
                                        val url = obj["sourceUrl"].asString.trim { it <= ' ' }
                                        mSources.add(Source(name, url))
                                    }
                                    XPopup.Builder(this@SubscriptionActivity)
                                        .asCustom(
                                            ChooseSourceDialog(
                                                this@SubscriptionActivity,
                                                mSources
                                            ) { position: Int, _: String? ->
                                                // 再根据多线路格式获取配置,如果仓内是正常多线路模式,name没用,直接使用线路的命名
                                                addSubscription(
                                                    mSources[position].sourceName,
                                                    mSources[position].sourceUrl,
                                                    checked
                                                )
                                            })
                                        .show()
                                }
                            } else { // 单线路/其余
                                addSub2List(name, url, checked)
                            }
                        } catch (th: Throwable) {
                            addSub2List(name, url, checked)
                        }
                        mSubscriptionAdapter.setNewData(mSubscriptions)
                        refreshEmptyState()
                    }

                    @Throws(Throwable::class)
                    override fun convertResponse(response: okhttp3.Response): String {
                        return response.body()!!.string()
                    }

                    override fun onError(response: Response<String?>) {
                        super.onError(response)
                        dismissLoadingDialog()
                        ToastUtils.showLong("订阅失败,请检查地址或网络状态")
                    }
                })
        } else {
            ToastUtils.showShort("订阅格式不正确")
        }
    }

    /**
     * 仅当选中本地文件和添加的为单线路时,使用此订阅生效。多线路会直接解析全部并添加,多仓会展开并选择,最后也按多线路处理,直接添加
     * @param name
     * @param url
     * @param checkNewest
     */
    private fun addSub2List(name: String, url: String, checkNewest: Boolean) {
        if (checkNewest) { //选中最新的,清除以前的选中订阅
            for (subscription in mSubscriptions) {
                if (subscription.isChecked) {
                    subscription.setChecked(false)
                }
            }
            mSelectedUrl = url
            mSubscriptions.add(Subscription(name, url).setChecked(true))
        } else {
            mSubscriptions.add(Subscription(name, url).setChecked(false))
        }
    }



    private fun importLines(selected: List<Subscription>, useFirst: Boolean) {
        if (selected.isEmpty()) return
        var added = 0
        for ((index, sub) in selected.withIndex()) {
            var exists = false
            for (item in mSubscriptions) {
                if (item.url == sub.url) {
                    exists = true
                    break
                }
            }
            if (exists) continue
            if (useFirst && added == 0) {
                for (s in mSubscriptions) s.setChecked(false)
                sub.setChecked(true)
                mSelectedUrl = sub.url
                Hawk.put(HawkConfig.API_URL, mSelectedUrl)
            }
            mSubscriptions.add(sub)
            added++
        }
        Hawk.put(HawkConfig.SUBSCRIPTIONS, mSubscriptions)
        mSubscriptionAdapter.setNewData(mSubscriptions)
        refreshEmptyState()
        ToastUtils.showShort("已导入 " + added + " 条线路")
        checkAllSubscriptions(true)
    }

    private fun checkAllSubscriptions(showToast: Boolean) {
        if (mSubscriptions.isEmpty()) return
        SubscriptionHealthChecker.checkAll(mSubscriptions, object : SubscriptionHealthChecker.Callback {
            override fun onOneFinished(item: Subscription?, index: Int) {
                if (index >= 0 && index < mSubscriptionAdapter.itemCount) {
                    mSubscriptionAdapter.notifyItemChanged(index)
                }
            }

            override fun onAllFinished(ok: Int, fail: Int) {
                // 有效性排序：有效 > 检测中/未知 > 失效，置顶与选中优先保持
                mSubscriptions.sortWith(Comparator { a, b ->
                    fun rank(s: Subscription): Int {
                        if (s.isTop) return -100
                        if (s.isChecked) return -50
                        return when (s.healthStatus) {
                            Subscription.STATUS_OK -> 0
                            Subscription.STATUS_CHECKING -> 1
                            Subscription.STATUS_UNKNOWN -> 2
                            else -> 3
                        }
                    }
                    rank(a) - rank(b)
                })
                mSubscriptionAdapter.setNewData(mSubscriptions)
                refreshEmptyState()
                Hawk.put(HawkConfig.SUBSCRIPTIONS, mSubscriptions)
                if (showToast) {
                    ToastUtils.showShort("检测完成：有效 " + ok + " / 失效 " + fail + "（已按有效性排序）")
                }
                for (s in mSubscriptions) {
                    if (s.isChecked && s.healthStatus == Subscription.STATUS_FAIL) {
                        ToastUtils.showLong("当前订阅可能已失效：" + s.name + "（" + s.healthMsg + "）")
                        Hawk.put("last_sub_fail_msg", s.name + ": " + s.healthMsg)
                        break
                    }
                }
            }
        })
    }

    private fun refreshEmptyState() {
        val empty = mSubscriptions.isEmpty()
        mBinding.llEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        mBinding.rv.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun showAddSubscriptionDialog() {
        XPopup.Builder(this)
            .autoFocusEditText(false)
            .asCustom(
                SubsciptionDialog(
                    this,
                    "订阅" + (mSubscriptions.size + 1),
                    object : OnSubsciptionListener {
                        override fun onConfirm(name: String, url: String, checked: Boolean) {
                            for (item in mSubscriptions) {
                                if (item.url == url) {
                                    ToastUtils.showLong("订阅地址与" + item.name + "相同")
                                    return
                                }
                            }
                            addSubscription(name, url, checked)
                        }

                        override fun chooseLocal(checked: Boolean) {
                            if (!XXPermissions.isGranted(
                                    mContext,
                                    Permission.MANAGE_EXTERNAL_STORAGE
                                )
                            ) {
                                showPermissionTipPopup(checked)
                            } else {
                                pickFile(checked)
                            }
                        }
                    })
            ).show()
    }

    private fun pasteAddFromClipboard() {
        try {
            val clip = ClipboardUtils.getText()?.toString()?.trim().orEmpty()
            if (clip.isEmpty()) {
                ToastUtils.showShort("剪贴板为空，请先复制订阅地址")
                return
            }
            val url = SubsciptionDialog.normalizeUrl(clip)
            if (!SubsciptionDialog.looksLikeUrl(url) && !url.startsWith("clan://")) {
                // 仍打开弹窗，方便用户改
                showAddSubscriptionDialog()
                ToastUtils.showShort("剪贴板内容不像链接，请确认")
                return
            }
            for (item in mSubscriptions) {
                if (item.url == url) {
                    ToastUtils.showLong("已存在相同地址：" + item.name)
                    return
                }
            }
            val name = SubsciptionDialog.suggestName(url)
            addSubscription(name, url, true)
            ToastUtils.showShort("已从剪贴板添加")
        } catch (e: Throwable) {
            e.printStackTrace()
            showAddSubscriptionDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        // 更新缓存
        Hawk.put(HawkConfig.API_URL, mSelectedUrl)
        Hawk.put<List<Subscription>?>(HawkConfig.SUBSCRIPTIONS, mSubscriptions)
    }

    override fun finish() {
        //切换了订阅地址
        if (!TextUtils.isEmpty(mSelectedUrl) && mBeforeUrl != mSelectedUrl) {
            ToastUtils.showShort("正在应用新订阅…")
            val intent = Intent(this, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        OkGo.getInstance().cancelTag("get_subscription")
    }
}
