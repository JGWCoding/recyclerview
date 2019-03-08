package com.xingzy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SampleAdapter implements RecyclerView.Adapter {

    private Context context;

    public SampleAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getItemViewType(int type) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public View onCreateViewHolder(int position, View convertView, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.activity_main_item, parent, false);
    }

    @Override
    public View onBinderViewHolder(int position, View convertView, ViewGroup parent) {
        TextView textView = convertView.findViewById(R.id.text_item);
        textView.setText("这是第" + position + "条测试数据");
        return convertView;
    }

    @Override
    public int getHeight(int index) {
        return 200;
    }

    @Override
    public int getCount() {
        return 30;
    }
}
