package com.mina.chatter.listeners;

import android.widget.TextView;

import com.mina.chatter.models.User;

public interface UserListener {
    void onUserClicked(User user, TextView tv);
}
