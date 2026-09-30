package com.github.tvbox.osc.bean;

public class Subscription {
    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_OK = 1;
    public static final int STATUS_FAIL = 2;
    public static final int STATUS_CHECKING = 3;

    public Subscription() {
    }

    public Subscription(String name, String url) {
        this.name = name;
        this.url = url;
    }

    String name;
    String url;
    boolean isChecked;
    private boolean top;
    /** 运行时健康状态，不强制持久化语义 */
    transient int healthStatus = STATUS_UNKNOWN;
    transient String healthMsg = "";

    public boolean isTop() {
        return top;
    }

    public void setTop(boolean top) {
        this.top = top;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public Subscription setChecked(boolean checked) {
        isChecked = checked;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(int healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getHealthMsg() {
        return healthMsg == null ? "" : healthMsg;
    }

    public void setHealthMsg(String healthMsg) {
        this.healthMsg = healthMsg;
    }
}
