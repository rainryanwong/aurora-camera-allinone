package com.aurora.camera.compat.street;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Phoenix EquipStreetBgViewNew drawing, attached through the native preview-rect interface. */
public final class LegendBackground217 extends View {
    private final RectF preview=new RectF(),inner=new RectF(),outer=new RectF();
    private final Paint base=new Paint(3),second=new Paint(3),ring=new Paint(3),clear=new Paint(3);
    private final Path path=new Path();private final float density;
    public LegendBackground217(Context c){super(c);density=c.getResources().getDisplayMetrics().density;setLayerType(LAYER_TYPE_SOFTWARE,null);path.setFillType(Path.FillType.EVEN_ODD);base.setColor(0xff1a1a1a);second.setColor(0xff222222);second.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));ring.setColor(Color.BLACK);clear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));}
    public void preview(Rect rect){preview.set(rect);inner.set(rect.left-16*density,rect.top-17*density,rect.right+16*density,rect.bottom+72*density);outer.set(inner.left-2*density,inner.top-2*density,inner.right+2*density,inner.bottom+2*density);path.reset();path.addRoundRect(inner,8*density,8*density,Path.Direction.CW);path.close();path.addRoundRect(preview,8*density,8*density,Path.Direction.CW);path.close();invalidate();}
    @Override protected void onDraw(Canvas c){c.save();c.drawRect(0,0,getWidth(),getHeight(),base);c.drawRoundRect(outer,8*density,8*density,second);c.drawPath(path,ring);c.drawRoundRect(preview,8*density,8*density,clear);c.restore();}
}
