package com.xingzy;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * @author roy.xing
 * @date 2019/3/7
 */
public class RecyclerView extends ViewGroup {

    //缓存已经加载到屏幕上的view
    private List<View> viewList;
    //记录在Y轴上滑动的距离
    private int currentY;
    //记录在RecyclerView加载的总数据
    private int rowCount;
    //记录在屏幕中的第一个View在数据内
    private int firstRow;
    //持有一个回收池的引用
    private Recycler recycler;
    //recyclerview中第一个View的左上顶点离屏幕的距离
    private int scrollY;
    //初始化 onLayout第一次
    private boolean needRelayout;
    //当前recyclerview的宽度
    private int width;
    //当前recyclerview的高度
    private int height;

    //item 高度
    private int[] heights;
    //最小滑动距离
    private int touchSlop;

    Adapter adapter;

    public RecyclerView(Context context) {
        super(context);
    }

    public RecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    interface Adapter {

        int getItemViewType(int row);

        int getViewTypeCount();

        View onCreateViewHolder(int position, View convertView, ViewGroup parent);

        View onBinderViewHolder(int position, View convertView, ViewGroup parent);

        int getHeight(int index);
    }
}
