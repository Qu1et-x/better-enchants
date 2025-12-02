package net.enchantoutline.config;

public class ItemOverride {
    public String item;
    public boolean render = true;
    public boolean render_solid = false;
    public float[] render_solid_outline_color = null;

    public void setItem(String item){
        this.item = item;
    }
    public String getItem(){
        return item;
    }
}
