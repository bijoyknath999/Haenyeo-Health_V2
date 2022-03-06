package com.HHMS.models;

import java.util.ArrayList;

public class Model {

    String id;
    ArrayList<data> data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<Model.data> getData() {
        return data;
    }

    public void setData(ArrayList<Model.data> data) {
        this.data = data;
    }

    public class data{

        String imei;
        String dt;

        public String getImei() {
            return imei;
        }

        public void setImei(String imei) {
            this.imei = imei;
        }

        public String getDt() {
            return dt;
        }

        public void setDt(String dt) {
            this.dt = dt;
        }
    }


}
