package com.example.filipe.socketcontroller.charts;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.filipe.socketcontroller.R;

import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.PieModel;

import java.util.HashMap;

import static com.example.filipe.socketcontroller.util.UI.colors;

public class DynamicPieChart extends PieChart
{
    private HashMap<String,PieModel> values;
    private int currentIndex;
    private static final int NONE = -1;

    @SuppressLint("CustomViewStyleable")
    public DynamicPieChart(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        loadAttributes(context,attrs);
        init();
    }

    private void loadAttributes(Context context, AttributeSet attrs)
    {
        setLayoutParams(new LinearLayout.LayoutParams(context,attrs));
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DynamicCharts, 0, 0);
        try
        {
            String unit = ta.getString(R.styleable.DynamicCharts_unitShown);
            this.setInnerValueUnit(unit);
        }
        finally
        {
            ta.recycle();
        }
    }

    private void init()
    {
        values = new HashMap<>();
        currentIndex = NONE;
        this.setUsePieRotation(false);
        this.setOnClickListener((v)->switchSlice());
    }

    private void adjustParams()
    {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        ViewGroup.LayoutParams params = getLayoutParams();
        int input_width = params.width;
        int input_height = params.height;
        if(input_width == LinearLayout.LayoutParams.MATCH_PARENT || input_width == LinearLayout.LayoutParams.WRAP_CONTENT)
        {
            params.width = metrics.widthPixels;
        }
        if(input_height == LinearLayout.LayoutParams.MATCH_PARENT || input_height == LinearLayout.LayoutParams.WRAP_CONTENT)
        {
            params.height = metrics.heightPixels;
        }
    }

    public void switchSlice()
    {
        if(values.size() > 1)
        {
            switchSlice((currentIndex + 1) % values.size());
        }
    }

    public void switchSlice(int index)
    {
        currentIndex = index;
        setCurrentItem(currentIndex);
    }

    private void add(String key)
    {
        PieModel slice = new PieModel(key,0,Color.parseColor(colors[values.size() % colors.length]));
        values.put(key,slice);
        refresh();
    }
    private void refresh()
    {
        clearChart();
        if(currentIndex == NONE)
        {
            adjustParams();
            currentIndex = 0;
        }
        for(PieModel p : values.values())
        {
            addPieSlice(p);
        }
        setCurrentItem(currentIndex);
    }
    public void setValue(String key,float value)
    {
        if(!contains(key))
        { add(key); }

        values.get(key).setValue(value);
        refresh();
    }
    public void incValue(String key, float value)
    {
        if(!contains(key))
        { add(key); }

        float old = values.get(key).getValue();
        values.get(key).setValue(old + value);
        refresh();
    }
    public boolean contains(String key)
    {
        return values.containsKey(key);
    }
}
