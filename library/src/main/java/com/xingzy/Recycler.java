package com.xingzy;

import android.view.View;

import java.util.Stack;

/**
 * @author roy.xing
 * @date 2019/3/7
 */
public class Recycler {

    private Stack<View>[] views;

    public Recycler(int length) {
        views = new Stack[length];
        for (int i = 0; i < length; i++) {
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
            e.printStackTrace();
            return null;
        }
    }
}
