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

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int h;
        if (adapter != null) {
            this.rowCount = adapter.getCount();
            heights = new int[rowCount];
            for (int i = 0; i < heights.length; i++) {
                heights[i] = adapter.getHeight(i);
            }
        }

        int tmpH = sumArray(heights, 0, heights.length);
        h = Math.min(heightSize, tmpH);

        setMeasuredDimension(widthSize, h);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * 计算出所有item的高度和
     *
     * @param heights
     * @param start
     * @param length
     * @return
     */
    private int sumArray(int[] heights, int start, int length) {
        int sum = 0;
        for (int i = start; i < length; i++) {
            sum += heights[i];
        }
        return sum;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (needRelayout || changed) {
            needRelayout = false;

            viewList.clear();
            removeAllViews();
            if (adapter != null) {
                width = r -l;
            }


        }
    }

    interface Adapter {

        /**
         * 获取item 的类型
         *
         * @param type
         * @return item 的类型
         */
        int getItemViewType(int type);

        /**
         * @return item 类型数量
         */
        int getViewTypeCount();

        View onCreateViewHolder(int position, View convertView, ViewGroup parent);

        View onBinderViewHolder(int position, View convertView, ViewGroup parent);

        /**
         * 获取所有item 高度
         *
         * @param index
         * @return
         */
        int getHeight(int index);

        /**
         * 获取item个数
         *
         * @return item个数
         */
        int getCount();
    }
}
