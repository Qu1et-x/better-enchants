package net.enchantoutline.config;

import net.minecraft.util.math.ColorHelper;

public class ItemOverride {
    public boolean render = true;
    public boolean override_render_solid = false;
    public boolean render_solid = false;
    public boolean override_render_solid_outline_color = false;
    public int[] render_solid_outline_color = {210,150,248};

    public void setRender(boolean render){
        this.render = render;
    }
    public boolean shouldRender(){
        return render;
    }

    public void setOverrideRenderSolid(boolean overrideRenderSolid){
        this.override_render_solid = overrideRenderSolid;
    }

    public boolean shouldOverrideRenderSolid(){
        return override_render_solid;
    }

    public void setRenderSolid(boolean renderSolid){
        this.render_solid = renderSolid;
    };

    public boolean shouldRenderSolid(){
        return render_solid;
    }

    public void setOverrideRenderSolidOutlineColor(boolean overrideRenderSolidOutlineColor){
        this.override_render_solid_outline_color = overrideRenderSolidOutlineColor;
    }

    public boolean shouldOverrideRenderSolidOutlineColor(){
        return override_render_solid_outline_color;
    }

    public void setRenderSolidOutlineColorAsInt(int color)
    {
        color = ColorHelper.withAlpha(255, color);
        int[] newOutlineColor = new int[3];
        newOutlineColor[0] = ColorHelper.getRed(color);
        newOutlineColor[1] = ColorHelper.getGreen(color);
        newOutlineColor[2] = ColorHelper.getBlue(color);
        render_solid_outline_color = newOutlineColor;
    }

    public int[] getRenderSolidOutlineColor(){
        return render_solid_outline_color;
    }
}
