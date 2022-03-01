package com.tangrun.mschat.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class User implements Serializable {
    private String id, displayName, avatar;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public JSONObject toJsonObj() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("displayName", displayName);
            jsonObject.put("avatar", avatar);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
