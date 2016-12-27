package com.devashishsharma.capstone;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by devashish.sharma on 12/27/2016.
 */
public class ExpensesApplication extends Application {
    private Tracker mTracker;
    public static GoogleAnalytics analytics;
    //public static Tracker tracker;

    @Override
    public void onCreate() {
        super.onCreate();

        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(1800);
        mTracker = analytics.newTracker("UA-89524615-1");
        mTracker.enableExceptionReporting(true);
        mTracker.enableAdvertisingIdCollection(true);
        mTracker.enableAutoActivityTracking(true);
    }

    synchronized public Tracker getDefaultTracker() {
        return mTracker;
    }
}
