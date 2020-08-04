package com.anecoz.billie;

public interface IAPIObserver {
    void onAPIEvent(APIEvent event, Response response);
}
