package com.mc.group28.brainnet;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by bhargav on 11/18/17.
 */

public class AuthResponse implements Serializable {


    @SerializedName("authenticated")
    @Expose
    private Boolean authenticated;

    public Boolean getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(Boolean authenticated) {
        this.authenticated = authenticated;
    }
}
