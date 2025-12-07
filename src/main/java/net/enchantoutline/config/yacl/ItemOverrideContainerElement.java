package net.enchantoutline.config.yacl;

import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ControllerWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemOverrideContainerElement extends ControllerWidget<ItemOverrideContainerController> implements ParentElement {
    private final ItemOverrideContainerController itemOverrideContainerController;

    private Element focused;

    private final AbstractWidget itemWidget;

    public ItemOverrideContainerElement(ItemOverrideContainerController control, YACLScreen screen, Dimension<Integer> dim) {
        super(control, screen, dim);
        this.itemOverrideContainerController = control;
        itemWidget = itemOverrideContainerController.getItemOption().controller().provideWidget(screen, dim);
    }

    public int getListButtonSize(){
        return 20;
    }

    @Override
    protected int getHoveredControlWidth() {
        return getUnhoveredControlWidth();
    }

    /*@Override
    protected int getTextY(){
        return super.getTextY() + 20;
    }*/

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click mouseButtonEvent, boolean doubleClick) {
        return ParentElement.super.mouseClicked(mouseButtonEvent, doubleClick);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput keyEvent) {
        return ParentElement.super.keyPressed(keyEvent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return ParentElement.super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float delta) {
        itemWidget.setDimension(itemWidget.getDimension().withY(getDimension().y()).withX(getDimension().x()).withWidth(getDimension().width()).withHeight(getDimension().height()));
        itemWidget.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends Element> children() {
        return List.of(itemWidget);
    }

    @Override
    public void unfocus() {
        itemWidget.unfocus();
    }

    @Override
    public boolean isDragging() {
        return false;
    }

    @Override
    public void setDragging(boolean dragging) {

    }

    @Override
    public @Nullable Element getFocused() {
        return focused;
    }

    @Override
    public void setFocused(@Nullable Element focused) {
        this.focused = focused;
    }
}
