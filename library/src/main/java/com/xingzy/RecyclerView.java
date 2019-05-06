package com.xingzy;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import xingzy.com.library.R;

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
        this(context, null);
    }

    public RecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        viewList = new ArrayList<>();
        this.needRelayout = true;
        //获取最小滑动距离 28-48px之间
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.touchSlop = configuration.getScaledDoubleTapSlop();
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;

        if (adapter != null) {
            recycler = new Recycler(adapter.getViewTypeCount());
        }
        scrollY = 0;
        firstRow = 0;
        needRelayout = true;
        requestLayout();
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
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (needRelayout || changed) {
            needRelayout = false;

            viewList.clear();
            removeAllViews();
            if (adapter != null) {
                width = right - left;
                height = bottom - top;

                int realLeft, realTop = 0, realRight, realBottom;
                for (int i = 0; i < rowCount && realTop < height; i++) {
                    realBottom = realTop + heights[i];
                    View view = makeAndStep(i, 0, realTop, width, realBottom);
                    viewList.add(view);
                    realTop = realBottom;
                }
            }
        }
    }

    private View makeAndStep(int position, int left, int top, int right, int bottom) {
        View view = obtainView(position, right - left, bottom - top);

        view.layout(left, top, right, bottom);

        return view;
    }

    private View obtainView(int position, int width, int height) {
        int itemType = adapter.getItemViewType(position);
        View recyclerView = recycler.get(itemType);
        View view;
        if (recyclerView == null) {
            view = adapter.onCreateViewHolder(position, recyclerView, this);
            if (view == null) {
                throw new RuntimeException("onCreateViewHolder 必须填充布局");
            }
        } else {
            view = adapter.onBinderViewHolder(position, recyclerView, this);
        }

        view.setTag(R.id.tag_type_view, itemType);
        view.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
                , MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        addView(view, position);
        return view;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean intercept = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                int y2 = Math.abs(currentY - (int) event.getRawY());
                if (y2 > touchSlop) {
                    intercept = true;
                }
                break;
            default:
                break;
        }

        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                int y2 = (int) event.getRawY();
                int diff = currentY - y2;
                scrollBy(0, diff);
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void scrollBy(int x, int y) {
        scrollY += y;

        scrollY = scrollBounds(scrollY,firstRow,heights,height);

        //向上滑
        if (scrollY > 0) {
            while (heights[firstRow] < scrollY) {
                if (!viewList.isEmpty()) {
                    removeView(viewList.remove(0));
                    scrollY -= heights[firstRow];
                    firstRow++;
                }
            }

            while (getFilledHeight() < height) {
                int addLast = firstRow + viewList.size();
                View view = obtainView(addLast, width, heights[addLast]);
                viewList.add(viewList.size(), view);
            }
        } else if (scrollY < 0) {
            while (!viewList.isEmpty() && getFilledHeight() - heights[firstRow + viewList.size()] < 0) {
                removeView(viewList.remove(viewList.size() - 1));
            }

            while (0 > scrollY) {
                int firstAddRow = firstRow - 1;
                View view = obtainView(firstAddRow, width, heights[firstAddRow]);
                viewList.add(0, view);
                firstRow--;
                scrollY += heights[firstRow + 1];
            }
        }

        repositionViews();
        awakenScrollBars();
    }

    private int scrollBounds(int scrollY, int firstRow, int[] heights, int height) {

        if (scrollY>0){
            scrollY = Math.min(scrollY,sumArray(heights,firstRow,heights.length-firstRow)-height);
        }else {
            scrollY = Math.max(scrollY,-sumArray(heights,0,firstRow));
        }

        return scrollY;
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        int typeView = (int) view.getTag(R.id.tag_type_view);
        recycler.put(view, typeView);
    }

    private void repositionViews() {
        int left, top, right, bottom, i;
        top = -scrollY;
        i = firstRow;
        for (View view : viewList) {
            bottom = top + heights[i++];
            view.layout(0, top, width, bottom);
            top = bottom;
        }
    }

    private int getFilledHeight() {
        return sumArray(heights, firstRow, viewList.size()) - scrollY;
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
