package com.example.mycurves;

import android.graphics.Point;

public class Line {
    Point start;
    Point end;

    public Line(){
        start = new Point(0,0);
        end = new Point(0,0);
    }

    public Line(Point start,Point end){
        this.end = end;
        this.start = start;
    }

    public Point getStart(){
        return start;
    }

    public Point getEnd(){
        return end;
    }

    public void setStart(Point start){
        this.start = start;
    }

    public void setEnd(){
        this.end = end;
    }
}
