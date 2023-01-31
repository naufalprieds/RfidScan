package com.prieds.rfidlibrary.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class UhfTagInfoCustom implements Serializable, Parcelable {
    private String epc;
    private float qty;
    private Integer delete;

    public UhfTagInfoCustom(String epc, float qty, Integer delete) {
        this.epc = epc;
        this.qty = qty;
        this.delete = delete;
    }

    public UhfTagInfoCustom() {
    }

    public String getEpc() {
        return epc;
    }

    public void setEpc(String epc) {
        this.epc = epc;
    }

    public float getQty() {
        return qty;
    }

    public void setQty(float qty) {
        this.qty = qty;
    }

    public Integer getDelete() {
        return delete;
    }

    public void setDelete(Integer delete) {
        this.delete = delete;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.epc);
        dest.writeValue(this.qty);
        dest.writeValue(this.delete);
    }

    public void readFromParcel(Parcel source) {
        this.epc = source.readString();
        this.qty = (Float) source.readValue(Float.class.getClassLoader());
        this.delete = (Integer) source.readValue(Integer.class.getClassLoader());
    }

    protected UhfTagInfoCustom(Parcel in) {
        this.epc = in.readString();
        this.qty = (Float) in.readValue(Float.class.getClassLoader());
        this.delete = (Integer) in.readValue(Integer.class.getClassLoader());
    }

    public static final Creator<UhfTagInfoCustom> CREATOR = new Creator<UhfTagInfoCustom>() {
        @Override
        public UhfTagInfoCustom createFromParcel(Parcel source) {
            return new UhfTagInfoCustom(source);
        }

        @Override
        public UhfTagInfoCustom[] newArray(int size) {
            return new UhfTagInfoCustom[size];
        }
    };
}
