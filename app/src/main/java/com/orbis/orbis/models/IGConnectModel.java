package com.orbis.orbis.models;

import com.google.gson.annotations.SerializedName;

public class IGConnectModel {
    @SerializedName("status")
    private String status;
    @SerializedName("authLink")
    private String authLink;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuthLink() {
        return authLink;
    }

    public void setAuthLink(String authLink) {
        this.authLink = authLink;
    }
}
