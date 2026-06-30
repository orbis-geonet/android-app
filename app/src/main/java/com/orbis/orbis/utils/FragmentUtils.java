package com.orbis.orbis.utils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.orbis.orbis.R;


public class FragmentUtils {

    public static void launchFragmentWithDefaultAnimation(FragmentActivity activity, int containerId, Fragment fragment, FragmentLaunchMode mode, Boolean addToBackStack) {
        FragmentTransaction transaction = activity.getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, 0, 0, R.anim.slide_out_right);

        switch (mode) {
            case ADD:
                transaction.add(containerId, fragment);
            case REPLACE:
                transaction.replace(containerId, fragment);
        }

        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }

        transaction.commitAllowingStateLoss();
    }

    public static void launchFragmentWithNoAnimation(FragmentActivity activity, int containerId, Fragment fragment, FragmentLaunchMode mode, Boolean addToBackStack) {
        FragmentTransaction transaction = activity.getSupportFragmentManager()
                .beginTransaction();


        switch (mode) {
            case ADD: transaction.add(containerId, fragment);
            case REPLACE: transaction.replace(containerId, fragment);
        }

        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }

        transaction.commitAllowingStateLoss();
    }


    public enum FragmentLaunchMode {
        ADD,
        REPLACE
    }

}
