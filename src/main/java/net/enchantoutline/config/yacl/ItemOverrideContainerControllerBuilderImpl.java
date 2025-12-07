package net.enchantoutline.config.yacl;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.impl.controller.AbstractControllerBuilderImpl;
import net.enchantoutline.config.ItemOverrideContainer;

public class ItemOverrideContainerControllerBuilderImpl extends AbstractControllerBuilderImpl<ItemOverrideContainer> implements ItemOverrideContainerControllerBuilder {
    private ItemOverrideContainer defaultValue = new ItemOverrideContainer();

    protected ItemOverrideContainerControllerBuilderImpl(Option<ItemOverrideContainer> option) {
        super(option);
    }

    @Override
    public Controller<ItemOverrideContainer> build() {
        return new ItemOverrideContainerController(option, defaultValue);
    }

    @Override
    public ItemOverrideContainerControllerBuilder setDefault(ItemOverrideContainer defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }
}
