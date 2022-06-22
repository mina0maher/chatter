package com.mina.chatter.listeners;

import android.widget.TextView;

import com.mina.chatter.models.User;

public interface ConversionListener {
    void onConversionClicked(User user, TextView textView);
}
