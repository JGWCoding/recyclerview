package com.xingzy.recyclerview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * @author roy.xing
 * @date 2019-05-06
 */
public class RecyclerView extends ViewGroup {

    /**
     * 缓存已经加载到屏幕上的View,这些View不存在回收池中，需要集合表示，方便后续查找和移除
     */
    private List<View> viewList;

    /**
     * 记录在Y轴上滑动的距离
     */
    private int currentY;

    /**
     * 记录在RecyclerView加载的总数据
     */
    private int rowCount;

    /**
     * 记录在屏幕中的第一个View在数据内容中的位置，比如目前是第34个元素在屏幕的一个位置
     */
    private int firstRow;

    /**
     * 持有一个回收池引用
     */
    private Recycler recycler;

    /**
     * RecyclerView中第一个View的左上顶点离屏幕的距离
     */
    private int scrollY;

    private Adapter adapter;

    /**
     * 获取最小滑动距离
     */
    private int touchSlop;

    /**
     * 初始化，第一屏最慢
     */
    private boolean needRelayout;

    /**
     *
     */
    private int width;

    /**
     *
     */
    private int height;

    /**
     * 列表item高度集合
     */
    private int[] heights;

    public RecyclerView(Context context) {
        this(context, null);
    }

    public RecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
        if (adapter != null) {
            recycler = new Recycler(adapter.getViewTypeCount());
            scrollY = 0;
            firstRow = 0;
            needRelayout = true;
            requestLayout();
        }
    }

    private void init(Context context) {
        setClickable(true);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
        viewList = new ArrayList<>();
        needRelayout = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int h = 0;
        if (adapter != null) {
            rowCount = adapter.getCount();
            heights = new int[rowCount];
            for (int i = 0; i < heights.length; i++) {
                heights[i] = adapter.getItemHeight(i);
            }
        }

        //数据的高度
        int tempH = sumArray(heights, 0, heights.length);
        h = Math.min(heightSize, tempH);

        setMeasuredDimension(widthSize, h);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * @param array
     * @param firstIndex
     * @param count
     * @return
     */
    private int sumArray(int[] array, int firstIndex, int count) {
        int sum = 0;
        count += firstIndex;
        for (int i = firstIndex; i < count; i++) {
            sum += array[i];
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
                width = r - l;
                height = b - t;
                int left, top = 0, right, bottom;
                for (int i = 0; i < rowCount && top < height; i++) {
                    right = width;
                    bottom = top + heights[i];
                    View view = makeAndStep(i, 0, top, right, bottom);
                    viewList.add(view);

                    top = bottom;
                }
            }
        }
    }

    private View makeAndStep(int row, int left, int top, int right, int bottom) {
        View view = obtainView(row, right - left, bottom - top);
        view.layout(left, top, right, bottom);
        return view;
    }

    private View obtainView(int row, int width, int height) {
        if (adapter != null) {
            int itemType = adapter.getItemViewType(row);
            View recyclerView = recycler.get(itemType);
            View view;
            if (recyclerView == null) {
                view = adapter.onCreateViewHolder(row, recyclerView, this);
                if (view == null) {
                    throw new RuntimeException("onCreateViewHolder 必须填充布局");
                }
            } else {
                view = adapter.onBinderViewHolder(row, recyclerView, this);
            }

            view.setTag(R.id.tag_type_view, itemType);
            view.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            addView(view, 0);
            return view;
        }
        return null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercpet = false;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentY = (int) ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(currentY - (int) ev.getRawY()) - touchSlop > 0) {
                    intercpet = true;
                }
                break;
            default:
                break;
        }
        return intercpet;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                int y2 = (int) event.getRawY();
                //上滑正， 下滑负
                int diffY = currentY - y2;
                scrollBy(0, diffY);
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void scrollBy(int x, int y) {
        //scrollY表示，第一个可见的Item的左上顶点 距离屏幕的坐上定点定点距离
        scrollY += y;
        scrollY = scrollYBounds(scrollY);
        if (scrollY > 0) {
            //上滑移除
            while (scrollY - heights[firstRow] > 0) {
                removeView(viewList.remove(0));
                scrollY -= heights[firstRow];
                firstRow++;
            }

            //上滑加载
            while (getFillHeight() < height) {
                int addList = firstRow + viewList.size();
                View view = obtainView(addList, width, heights[addList]);
                viewList.add(viewList.size(), view);
            }

        } else if (scrollY < 0) {
            //下滑添加
            while (scrollY < 0) {
                int firstAddRow = firstRow - 1;
                View view = obtainView(firstAddRow, width, heights[firstAddRow]);
                viewList.add(0, view);
                scrollY += heights[firstRow];
                firstRow--;
            }
            //下滑移除
            while (sumArray(heights, firstRow, viewList.size()) - scrollY - heights[firstRow + viewList.size() - 1] >= height) {
                removeView(viewList.remove(viewList.size() - 1));
            }

        } else {
        }
        repositionViews();
    }

    private int scrollYBounds(int scrollY) {
        //上滑
        if (scrollY > 0) {
            scrollY = Math.min(scrollY, sumArray(heights, firstRow, heights.length - firstRow) - height);
        } else if (scrollY < 0) {
            scrollY = Math.max(scrollY, -sumArray(heights, 0, firstRow));
        }
        return scrollY;
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

    private int getFillHeight() {
        return sumArray(heights, firstRow, viewList.size()) - scrollY;
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        recycler.put(view, (int) view.getTag(R.id.tag_type_view));
    }

    interface Adapter {
        /**
         * @param position
         * @param convertView
         * @param parent
         * @return
         */
        View onCreateViewHolder(int position, View convertView, ViewGroup parent);

        /**
         * @param position
         * @param convertView
         * @param parent
         * @return
         */
        View onBinderViewHolder(int position, View convertView, ViewGroup parent);

        /**
         * @param position
         * @return 返回当前item的Type
         */
        int getItemViewType(int position);

        /**
         * @return 返回布局类型个数
         */
        int getViewTypeCount();

        /**
         * @return 返回数据个数
         */
        int getCount();

        /**
         * @param index
         * @return 获取item 高度
         */
        int getItemHeight(int index);
    }
}
