package com.aurora.camera.compat.trace;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Bounded, default-off diagnostic window; never changes camera state. */
public final class TraceWindow222 {
    public static final int MAX_RECORDS=24000;
    private final BooleanSupplier armed;
    private final LongSupplier clock;
    private final Consumer<Map<String,Object>> sink;
    private boolean open,limited;
    private int window,records;
    private long sequence;
    public TraceWindow222(BooleanSupplier armed,LongSupplier clock,Consumer<Map<String,Object>> sink){this.armed=armed;this.clock=clock;this.sink=sink;}
    public synchronized boolean requested(){boolean flag=armed.getAsBoolean();if(!flag)poll(false);return flag;}
    public synchronized boolean poll(boolean eligible){
        boolean flag=armed.getAsBoolean();
        if(!flag){if(open)send("end",fields("records",records,"limited",limited));open=false;limited=false;return false;}
        if(!eligible||limited)return false;
        if(!open){open=true;window++;records=0;send("begin",fields("max_records",MAX_RECORDS,"clock","elapsedRealtimeNanos","provider_query_observed",false));}
        return true;
    }
    public synchronized void record(String event,Map<String,Object> data){
        if(!open||limited)return;
        if(records==MAX_RECORDS){limited=true;send("limit",fields("records",records,"reason","record_limit","complete",false));return;}
        records++;send(event,data);
    }
    private void send(String event,Map<String,Object> data){
        Map<String,Object> row=new LinkedHashMap<>();row.put("schema",222);row.put("window",window);row.put("seq",++sequence);row.put("elapsed_ns",clock.getAsLong());row.put("event",event);row.put("data",data);sink.accept(row);
    }
    public static Map<String,Object> fields(Object... pairs){Map<String,Object> row=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)row.put((String)pairs[i],pairs[i+1]);return row;}
}
