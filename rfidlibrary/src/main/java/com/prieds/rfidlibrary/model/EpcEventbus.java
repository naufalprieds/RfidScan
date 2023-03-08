package com.prieds.rfidlibrary.model;


public class EpcEventbus {
    String epc;
    String type;
    int flag;
    @androidx.annotation.Nullable
    Object obj;

    public EpcEventbus(String epc, String type, int flag, @androidx.annotation.Nullable Object obj) {
        this.epc = epc;
        this.type = type;
        this.flag = flag;
        this.obj = obj;
    }

    public String getEpc() {
        return epc;
    }

    public void setEpc(String epc) {
        this.epc = epc;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    @androidx.annotation.Nullable
    public Object getObj() {
        return obj;
    }

    public void setObj(@androidx.annotation.Nullable Object obj) {
        this.obj = obj;
    }
}
