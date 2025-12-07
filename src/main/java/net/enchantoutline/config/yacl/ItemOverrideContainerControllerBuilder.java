package net.enchantoutline.config.yacl;

import dev.isxander.yacl3.api.Option;
import net.enchantoutline.config.ItemOverrideContainer;

public interface ItemOverrideContainerControllerBuilder {
    ItemOverrideContainerControllerBuilder setDefault(ItemOverrideContainer defaultValue);

    static ItemOverrideContainerControllerBuilder create(Option<ItemOverrideContainer> option){return new ItemOverrideContainerControllerBuilderImpl(option);}
}
