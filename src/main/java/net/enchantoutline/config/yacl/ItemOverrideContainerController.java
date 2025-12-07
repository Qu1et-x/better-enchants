package net.enchantoutline.config.yacl;

import com.mojang.logging.LogUtils;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.ItemControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.enchantoutline.config.ItemOverrideContainer;
import net.minecraft.item.Item;
import net.minecraft.text.Text;

public class ItemOverrideContainerController implements Controller<ItemOverrideContainer> {
    private final Option<ItemOverrideContainer> option;
    private final Option<Item> itemOption;

    public ItemOverrideContainerController(Option<ItemOverrideContainer> option, ItemOverrideContainer defaultItemOverrideContainer){
        this.option = option;
        itemOption = Option.<Item>createBuilder()
                .name(Text.translatable("controller.enchantoutline.itemoverride.item"))
                .description(OptionDescription.of(Text.translatable("tooltip.controller.enchantoutline.itemoverride.item")))
                .binding(defaultItemOverrideContainer.getItem(),() -> {return option.pendingValue().getItem();},
                        (item) -> {
                            ItemOverrideContainer pending = new ItemOverrideContainer(option.pendingValue());
                            pending.setItem(item);
                            LogUtils.getLogger().info("saved item: {}", pending.getItemString());
                            option.requestSet(pending);})
                .controller(ItemControllerBuilder::create).build();
    }

    @Override
    public Option<ItemOverrideContainer> option() {
        return option;
    }

    @Override
    public Text formatValue() {
        return Text.literal(option.toString());
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new ItemOverrideContainerElement(this, screen, widgetDimension);
    }

    public Option<Item>getItemOption(){
        return itemOption;
    }
}
