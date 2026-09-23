package com.emran.scohelper;

interface IUserService {
    boolean startSco();
    boolean stopSco();
    boolean isScoOn();
    void destroy();
}
