package net.enchantoutline.config;

public class ItemOverrideContainer {
    private String item;
    private final ItemOverride override;

    public ItemOverrideContainer(){
        this("minecraft:empty", new ItemOverride());
    }

    public ItemOverrideContainer(String item, ItemOverride override){
        this.item = item;
        this.override = override;
    }

    public void setItem(String item){
        this.item = item;
    }

    public String getItem(){
        return item;
    }

    public ItemOverride getItemOverride(){
        return override;
    }
}
