package com.anecoz.billie;

public interface ControlSwitchListener {
    void onTaillight(boolean value);
    void onLock(boolean value);
    void onRecenter();
    void onLog(boolean value);
}
