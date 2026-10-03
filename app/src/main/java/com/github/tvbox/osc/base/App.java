package com.github.tvbox.osc.base;

import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.util.LOG;

import android.text.TextUtils;

import androidx.multidex.MultiDexApplication;

import com.github.catvod.crawler.JsLoader;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.Subscription;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.data.AppDataManager;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.activity.MainActivity;
import com.github.tvbox.osc.util.EpgUtil;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.ConfigStore;
import com.github.tvbox.osc.util.HawkLegacyMigrator;
import com.github.tvbox.osc.util.NetworkClient;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.PlayerHelper;
import com.github.tvbox.osc.util.Utils;
import com.kingja.loadsir.core.LoadSir;
import com.p2p.P2PClass;
import com.whl.quickjs.android.QuickJSLoader;

import java.util.ArrayList;
import java.util.List;

import cat.ereza.customactivityoncrash.config.CaocConfig;
import me.jessyan.autosize.AutoSizeConfig;
import me.jessyan.autosize.unit.Subunits;

/**
 * @author pj567
 * @date :2020/12/17
 * @description:
 */
public class App extends MultiDexApplication {
    private static App instance;

    private static P2PClass p;
    public static String burl;

    public boolean isNormalStart;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initParams();
        // OKGo
        OkGoHelper.init(); //台标获取
        EpgUtil.init();
        // 初始化Web服务器
        ControlManager.init(this);
        //初始化数据库
        AppDataManager.init();
        LoadSir.beginBuilder()
                .addCallback(new EmptyCallback())
                .addCallback(new LoadingCallback())
                .commit();
        AutoSizeConfig.getInstance()
                .setExcludeFontScale(true)
                .setCustomFragment(true)
                .getUnitsManager()
                .setSupportDP(false)
                .setSupportSP(false)
                .setSupportSubunits(Subunits.MM);
        PlayerHelper.init();
        QuickJSLoader.init();
        FileUtils.cleanPlayerCache();
        initCrashConfig();
        Utils.initTheme();
        checkPythonRuntime();
    }

    private void checkPythonRuntime() {
        try {
            if (!BuildConfig.HAS_PYTHON) {
                LOG.i("Python runtime: standard variant (not bundled)");
                return;
            }
            Class<?> pyClz = Class.forName("com.chaquo.python.Python");
            boolean started = Boolean.TRUE.equals(pyClz.getMethod("isStarted").invoke(null));
            if (!started) {
                Object platform = Class.forName("com.chaquo.python.android.AndroidPlatform")
                        .getConstructor(android.content.Context.class)
                        .newInstance(this);
                for (java.lang.reflect.Method m : pyClz.getMethods()) {
                    if ("start".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        m.invoke(null, platform);
                        break;
                    }
                }
            }
            LOG.i("Python runtime: ready, started=" + pyClz.getMethod("isStarted").invoke(null));
        } catch (Throwable e) {
            LOG.e("Python runtime init failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    private void initParams() {
        ConfigStore.init(this);
        NetworkClient.init(this);
        HawkLegacyMigrator.migrateIfNeeded(this);
        ConfigStore.putBool(HawkConfig.DEBUG_OPEN, false);

        ConfigStore.putDefault(HawkConfig.HOME_REC, 0);                  //推荐: 0=豆瓣热播, 1=站点推荐
        ConfigStore.putDefault(HawkConfig.PLAY_TYPE, 2);                 //播放器: 0=系统, 1=IJK, 2=Exo
        ConfigStore.putDefault(HawkConfig.IJK_CODEC, "硬解码");           //IJK解码: 软解码, 硬解码
        ConfigStore.putDefault(HawkConfig.BACKGROUND_PLAY_TYPE, 2);      //后台播放: 0 关闭,1 开启,2 画中画
        ConfigStore.putDefault(HawkConfig.DOH_URL, 0);                   //安全DNS
        ConfigStore.putDefault(HawkConfig.PLAY_SCALE, 0);                //画面缩放
        ConfigStore.putDefault(HawkConfig.HISTORY_NUM, 2);
        ConfigStore.putDefault(HawkConfig.VIDEO_PURIFY_LEVEL, 1); // 标准净化
        putDefaultApi();
    }

    private void putDefaultApi() {
        String[] apis = getResources().getStringArray(R.array.api);
        if (!ConfigStore.hasApiUrl() && TextUtils.isEmpty(ConfigStore.getApiUrl()) && !TextUtils.isEmpty(apis[0])) {
            List<Subscription> subscriptions = new ArrayList<>();
            for (int i = 0; i < apis.length; i++) {
                if (i == 0) {
                    subscriptions.add(new Subscription("订阅: 1", apis[0]).setChecked(true));
                    ConfigStore.setApiUrl(apis[0]);
                } else {
                    subscriptions.add(new Subscription("订阅: " + (i + 1), apis[i]));
                }
            }
            ConfigStore.setSubscriptions(subscriptions);
        }
    }

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        JsLoader.load();
    }

    private VodInfo vodInfo;
    public void setVodInfo(VodInfo vodinfo){
        this.vodInfo = vodinfo;
    }
    public VodInfo getVodInfo(){
        return this.vodInfo;
    }

    public static P2PClass getp2p() {
        try {
            if (p == null) {
                p = new P2PClass(instance.getExternalCacheDir().getAbsolutePath());
            }
            return p;
        } catch (Exception e) {
            LOG.e(e.toString());
            return null;
        }
    }

    private void initCrashConfig(){
        //配置全局异常崩溃操作
        CaocConfig.Builder.create()
                .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) //背景模式,开启沉浸式
                .enabled(true) //是否启动全局异常捕获
                .showErrorDetails(true) //是否显示错误详细信息
                .showRestartButton(true) //是否显示重启按钮
                .trackActivities(true) //是否跟踪Activity
                .minTimeBetweenCrashesMs(2000) //崩溃的间隔时间(毫秒)
                .errorDrawable(R.drawable.app_icon) //错误图标
                .restartActivity(MainActivity.class) //重新启动后的activity
                .apply();
    }

}