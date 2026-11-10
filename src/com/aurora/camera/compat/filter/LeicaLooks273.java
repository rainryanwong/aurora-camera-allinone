package com.aurora.camera.compat.filter;

import com.aurora.camera.compat.Host;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import static de.robv.android.xposed.XposedHelpers.*;

/** Stock 1770's Leica Looks resources and existing render identities. */
public final class LeicaLooks273 {
    private static final int[][] ITEMS={
        {51,0x7f14051b,0x7f080b94,0x7f0808f3},
        {52,0x7f140515,0x7f080b88,0x7f0808f0},
        {248,0x7f14050b,0x7f080b80,0x7f0808eb},
        {249,0x7f14050d,0x7f080b81,0x7f0808ec},
        {250,0x7f14050f,0x7f080b82,0x7f0808ed},
        {251,0x7f140511,0x7f080b83,0x7f0808ee},
        {252,0x7f140505,0x7f080b7f,0x7f0808e8},
        {253,0x7f140519,0x7f080b85,0x7f0808f2},
        {53,0x7f140509,0x7f080b6b,0x7f0808ea},
        {54,0x7f140507,0x7f080b6a,0x7f0808e9},
        {254,0x7f140513,0x7f080b84,0x7f0808ef},
        {56,0x7f140503,0x7f080b68,0x7f0808e7}
    };
    private static final int VIDEO_BASE=0x41000000;
    private static final Map<Integer,Integer> VIDEO_RANKS=new HashMap<>();
    private LeicaLooks273(){}
    public static int rank(int id){int ordinal=id&65535;for(int i=0;i<ITEMS.length;i++)if(ITEMS[i][0]==ordinal)return i;return Integer.MAX_VALUE;}
    static int videoValue(int ordinal){return VIDEO_BASE|ordinal;}
    static int videoOrdinal(int value){int ordinal=value&65535;return (value&0xffff0000)==VIDEO_BASE&&ordinal>=248&&ordinal<=254?ordinal:0;}
    static int videoRank(int value){synchronized(VIDEO_RANKS){return VIDEO_RANKS.getOrDefault(value,Integer.MAX_VALUE);}}
    static String resource(int ordinal){return new String[]{"lut_l_chr","lut_l_cls","lut_l_cnt","lut_l_etn","lut_l_brs","lut_l_tel","lut_l_ia"}[ordinal-248];}
    static void applyVideo(Host host,List<Object> rows,List<?> rear)throws ReflectiveOperationException{
        Class<?> type=host.type("i3.b");
        for(int index=0;index<ITEMS.length;index++){
            int[] item=ITEMS[index];int ordinal=item[0],videoOrdinal=ordinal<100?ordinal+152:ordinal;
            // Native S0()==6 builds video entries from still enums; the older
            // catalog uses video enums (+152). Keep either catalog's HAL ID.
            Object found=null;for(Object row:rows){int actual=(Integer)getObjectField(row,"b");if(actual==ordinal||actual==videoOrdinal){found=row;break;}}
            if(found==null&&ordinal<100&&rear!=null)for(Object row:rear){int actual=(Integer)getObjectField(row,"b");if(actual==ordinal||actual==videoOrdinal){found=row;rows.add(found);break;}}
            if(found==null&&ordinal>=248){found=newInstance(type,7,ordinal,item[1],item[2],rows.size());setIntField(found,"n",videoValue(ordinal));rows.add(found);}
            if(found==null)throw new IllegalStateException("Native Leica video filter missing: "+videoOrdinal);
            setIntField(found,"c",item[1]);setIntField(found,"e",item[2]);setIntField(found,"d",item[3]);
            synchronized(VIDEO_RANKS){VIDEO_RANKS.put((Integer)getObjectField(found,"n"),index);}
        }
    }
    public static boolean contains(int id){int ordinal=id&65535;for(int[] item:ITEMS)if(item[0]==ordinal)return true;return false;}
    static void apply(Host host,int category,List<Object> rows)throws ReflectiveOperationException{
        Class<?> type=host.type("i3.b");
        for(int[] item:ITEMS){
            Object found=null;
            for(Object row:rows)if((Integer)getObjectField(row,"b")==item[0]){found=row;break;}
            if(found==null){
                found=newInstance(type,category,item[0],item[1],item[2],rows.size());rows.add(found);
                setIntField(found,"f",item[2]);
            }
            setIntField(found,"c",item[1]);setIntField(found,"e",item[2]);setIntField(found,"d",item[3]);
        }
    }
}
