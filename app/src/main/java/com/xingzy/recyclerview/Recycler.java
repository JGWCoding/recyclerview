package com.xingzy.recyclerview;

import android.view.View;

import java.util.Stack;

/**
 * @author roy.xing
 * @date 2019-05-06
 */
public class Recycler {

    private Stack<View>[] views;

    public Recycler(int viewType) {
        views = new Stack[viewType];
        for (int i = 0; i < viewType; i++) {
            views[i] = new Stack<>();
        }
    }

    public void put(View view, int type) {
        views[type].push(view);
    }

    public View get(int type) {
        try {
            return views[type].pop();
        } catch (Exception e) {
            return null;
        }
    }
}
