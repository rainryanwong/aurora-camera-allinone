package com.aurora.camera.compat.style;

import android.util.SparseArray;
import java.util.ArrayList;
import java.util.List;

/** Exact z3.d consumers using the live preference, without writing z3.d.i. */
public final class StyleDebugUi {
    private final StyleRuntime r;
    public StyleDebugUi(StyleRuntime runtime){r=runtime;}

    public ArrayList<Integer> tokens() throws Throwable {
        ArrayList<Integer> result=new ArrayList<>();result.add(27);
        if(r.debug())for(int value:new int[]{33,32,34,35,36,39,41,20,21,22,23,25,24})result.add(value);
        result.add(4);result.add(7);result.add(43);return result;
    }

    public Object quickMenu(Object owner)throws Throwable {
        return r.debug()?r.call("debug.a",owner):new ArrayList<Object>();
    }

    public ArrayList<Object> extraMenu()throws Throwable {
        ArrayList<Object> result=new ArrayList<>();
        if(r.debug())for(int id:new int[]{0xd40,0xdf,0xc2,0xd1})result.add(r.call("menu.item",null,id));
        return result;
    }

    public List<Object> topMenu(Object owner)throws Throwable {
        if(!r.debug())return null;
        ArrayList<Object> result=new ArrayList<>(6);
        Object manual=r.call("repo.j",null);
        if(r.boolField("manual.M",manual)){
            if(r.bool("raw.support",r.component("repo.a","r2.U"),167)){
                Object builder=r.builder(false,32);r.set("builder.o",builder,1);
                Object icons=r.call("icon.provider",r.get("icon.instance",null));
                r.set("builder.d",builder,r.call("icon.resource",icons,0x7f08070b));
                r.set("builder.g",builder,0x7f140a06);
                r.set("builder.j",builder,r.bool("manual.q",r.component("repo.j","v2.j0"),167));
                r.set("builder.listener",builder,r.construct("click.raw",3));
                result.add(r.call("builder.g",builder));
            }
        }else{
            Object pe=r.get("pe.instance",null);
            if(r.bool("device.k3",r.get("pe.device",pe))){
                Object builder=r.builder(false,32);r.set("builder.o",builder,1);
                r.set("builder.d",builder,0x7f08070a);r.set("builder.g",builder,0x7f140a03);
                r.set("builder.j",builder,r.bool("manual.n",r.component("repo.j","v2.i0")));
                r.set("builder.listener",builder,r.construct("click.normal",2));
                result.add(r.call("builder.g",builder));
            }
        }
        boolean global=r.bool("prefs.L",r.call("repo.g",null));
        Object beauty=r.component("repo.j","v2.m0");
        Object pe=r.get("pe.instance",null);r.call("pe.j1",pe);
        boolean first=r.bool("beauty.f0",beauty);
        Object factory=r.get("menu.factory",owner);
        if(first){
            Object camera=r.call("camera.repo",null);int id=(Integer)r.call("camera.id",camera);
            r.call("cap.m5",null,r.call("camera.forId",camera,id));
            result.add(r.call("ui.a",factory));
        }
        if(r.bool("beauty.e0",beauty)){
            Object provider=r.call("beauty.app",r.get("external.instance",null));
            if(!r.bool("beauty.external",provider))result.add(r.call("ui.i",factory,first?4:3));
        }
        if(r.bool("pe.F0",pe)&&!global&&!r.bool("pe.a2",pe)&&!r.bool("pe.Z1",pe)){
            Object builder=r.builder(true,2);r.set("builder.layout",builder,0x7f0e004f);
            r.set("builder.o",builder,0);r.set("builder.binding",builder,r.statelessBinding());
            r.set("builder.k",builder,false);result.add(r.call("builder.f",builder));
        }
        return result;
    }

    /** z3.d.h differs only by its one append of 202 after the original super call. */
    @SuppressWarnings("unchecked") public void adjustProtocolMap(Object owner,boolean hadProtocolSix)throws Throwable {
        boolean wanted=r.debug(),original=r.boolField("debug.original",null);
        if(wanted==original)return;
        SparseArray<List<Integer>> map=(SparseArray<List<Integer>>)r.get("menu.map",owner);
        if(wanted){r.call("menu.n",owner,6,new int[]{202});return;}
        List<Integer> items=map.get(6);
        // With stock i=true this invocation just appended exactly one 202.
        int appended=items.lastIndexOf(202);
        if(appended<0)throw new IllegalStateException("AI protocol 202 append contract changed");
        items.remove(appended);
        if(items.isEmpty()&&!hadProtocolSix)map.remove(6);
    }
}
