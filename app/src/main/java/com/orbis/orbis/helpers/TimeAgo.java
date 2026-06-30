package com.orbis.orbis.helpers;

import static java.lang.Math.abs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.Time;
import android.util.Log;

import com.orbis.orbis.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class TimeAgo {


    public final static String secAgo = " seconds ago";
    static int second = 1000; // milliseconds
    static int minute = 60;
    static int hour = minute * 60;
    static int day = hour * 24;
    static int week = day * 7;
    static int month = day * 30;
    static int year = month * 12;


    public static int difference(long fromDate) {
        long diff = 0;
        long ms2 = System.currentTimeMillis();
        // get difference in milli seconds
        diff = ms2 - fromDate;

        return abs((int) (diff / (second)));
    }

    @SuppressLint("SimpleDateFormat")
    public static String DateDifference(long fromDate, Context context) {
        final String monthAgo = context.getString(R.string.month_ago);
        final String weekAgo = context.getString(R.string.week_ago);
        final String daysAgo = context.getString(R.string.days_ago);
        final String hoursAgo = context.getString(R.string.hours_ago);
        final String minAgo = context.getString(R.string.minutes_ago);
        long diff = 0;
        long ms2 = System.currentTimeMillis();
        int gmtOffset2 = TimeZone.getDefault().getOffset(ms2);

        // get difference in milli seconds
        diff = ms2 - fromDate - (gmtOffset2);

        int diffInSec = abs((int) (diff / (second)));
        String difference = "";
        if (diffInSec < minute) {
            difference = "Just now";
        } else if ((diffInSec / hour) < 1) {
            difference = (diffInSec / minute) + minAgo;
        } else if ((diffInSec / day) < 1) {
            difference = (diffInSec / hour) + hoursAgo;
        } else if ((diffInSec / week) < 1) {
            difference = (diffInSec / day) + daysAgo;
        } else if ((diffInSec / month) < 1) {
            difference = (diffInSec / week) + weekAgo;
        } else if ((diffInSec / year) < 1) {
            difference = (diffInSec / month) + monthAgo;
        } else {
            // return date
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(fromDate);

            SimpleDateFormat format_before = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss");

            difference = format_before.format(c.getTime());
        }

        return context.getString(R.string.online) + difference;
    }
}