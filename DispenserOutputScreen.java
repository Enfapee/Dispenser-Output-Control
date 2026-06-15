package com.example.dispensercontrol;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

public class DispenserOutputScreen extends Screen {
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_GAP = 4;

    private final List<Item> allItems = new ArrayList<>();
    private final List<Item> visibleItems = new ArrayList<>();

    private TextFieldWidget searchField;
    private Item itemA;
    private Item itemB;
    private int selectedOutputSlot;
    private int editingSlot;
    private int scrollOffset;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int gridLeft;
    private int gridTop;
    private int gridWidth;
    private int gridHeight;
    private int columns;

    protected DispenserOutputScreen() {
        super(Text.literal("Dispenser Output Control"));
        this.itemA = itemFromId(DispenserOutputControlClient.getItemA(), Items.ARROW);
        this.itemB = itemFromId(DispenserOutputControlClient.getItemB(), Items.FIRE_CHARGE);
        this.selectedOutputSlot = DispenserOutputControlClient.getSelectedSlot();
        this.editingSlot = this.selectedOutputSlot;
    }

    @Override
    protected void init() {
        allItems.clear();
        Registries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .sorted(Comparator.comparing(item -> Registries.ITEM.getId(item).toString()))
                .forEach(allItems::add);

        panelWidth = Math.min(430, this.width - 24);
        panelHeight = Math.min(316, this.height - 24);
        panelLeft = (this.width - panelWidth) / 2;
        panelTop = (this.height - panelHeight) / 2;
        gridLeft = panelLeft + 18;
        gridTop = panelTop + 138;
        gridWidth = panelWidth - 36;
        gridHeight = panelHeight - 192;
        columns = Math.max(1, gridWidth / (SLOT_SIZE + SLOT_GAP));

        searchField = new TextFieldWidget(this.textRenderer, panelLeft + 18, panelTop + 106, panelWidth - 36, 20, Text.literal("Search items"));
        searchField.setMaxLength(80);
        searchField.setChangedListener(value -> {
            scrollOffset = 0;
            refreshVisibleItems();
        });
        addDrawableChild(searchField);

        int halfWidth = (panelWidth - 46) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Output A"), button -> selectedOutputSlot = 0)
                .dimensions(panelLeft + 18, panelTop + 80, halfWidth, 18)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Output B"), button -> selectedOutputSlot = 1)
                .dimensions(panelLeft + panelWidth / 2 + 8, panelTop + 80, halfWidth, 18)
                .build());

        int buttonY = panelTop + panelHeight - 36;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> {
                    DispenserOutputControlClient.saveSelection(itemId(itemA), itemId(itemB), selectedOutputSlot);
                    close();
                })
                .dimensions(panelLeft + panelWidth - 162, buttonY, 70, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(panelLeft + panelWidth - 86, buttonY, 68, 20)
                .build());

        refreshVisibleItems();
        setInitialFocus(searchField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xA0000000);
        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xFF25272C);
        context.drawStrokedRectangle(panelLeft, panelTop, panelWidth, panelHeight, 0xFFE6E6E6);
        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 32, 0xFF353942);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, panelTop + 11, 0xFFFFFFFF);

        drawChoiceSlot(context, panelLeft + 18, panelTop + 44, 0, itemA);
        drawChoiceSlot(context, panelLeft + panelWidth / 2 + 8, panelTop + 44, 1, itemB);

        context.drawTextWithShadow(this.textRenderer, Text.literal("Search"), panelLeft + 18, panelTop + 95, 0xFFE0E0E0);
        context.fill(gridLeft - 2, gridTop - 2, gridLeft + gridWidth + 2, gridTop + gridHeight + 2, 0xFF15161A);
        context.drawStrokedRectangle(gridLeft - 2, gridTop - 2, gridWidth + 4, gridHeight + 4, 0xFF555A66);

        drawItemGrid(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);

        Text mode = Text.literal("Click an item to set " + (editingSlot == 0 ? "Item A" : "Item B")
                + " | Output: " + (selectedOutputSlot == 0 ? "Item A" : "Item B"));
        context.drawTextWithShadow(this.textRenderer, mode, panelLeft + 18, panelTop + panelHeight - 30, 0xFFFFE08A);
    }

    private void drawChoiceSlot(DrawContext context, int x, int y, int slot, Item item) {
        int width = (panelWidth - 46) / 2;
        int background = editingSlot == slot ? 0xFF4B5D7A : 0xFF333740;
        int outline = selectedOutputSlot == slot ? 0xFFFFD45A : 0xFF7A808C;

        context.fill(x, y, x + width, y + 32, background);
        context.drawStrokedRectangle(x, y, width, 32, outline);
        context.drawItem(new ItemStack(item), x + 8, y + 8);
        context.drawTextWithShadow(this.textRenderer, Text.literal(slot == 0 ? "Item A" : "Item B"), x + 30, y + 5, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal(itemId(item)), x + 30, y + 17, 0xFFB7C4D8);
    }

    private void drawItemGrid(DrawContext context, int mouseX, int mouseY) {
        int rowHeight = SLOT_SIZE + SLOT_GAP;
        int rows = Math.max(1, gridHeight / rowHeight);
        int start = scrollOffset * columns;
        int end = Math.min(visibleItems.size(), start + rows * columns);

        for (int index = start; index < end; index++) {
            int gridIndex = index - start;
            int column = gridIndex % columns;
            int row = gridIndex / columns;
            int x = gridLeft + column * (SLOT_SIZE + SLOT_GAP);
            int y = gridTop + row * rowHeight;
            Item item = visibleItems.get(index);
            boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
            boolean selected = item == itemA || item == itemB;

            context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, hovered ? 0xFF5A6474 : 0xFF30333A);
            context.drawStrokedRectangle(x, y, SLOT_SIZE, SLOT_SIZE, selected ? 0xFFFFD45A : 0xFF666C78);
            context.drawItem(new ItemStack(item), x + 4, y + 4);

            if (hovered) {
                context.drawTooltip(this.textRenderer, Text.literal(itemId(item)), mouseX, mouseY);
            }
        }

        if (visibleItems.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("No items found"), this.width / 2, gridTop + gridHeight / 2 - 4, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (isInside(mouseX, mouseY, panelLeft + 18, panelTop + 44, (panelWidth - 46) / 2, 32)) {
            editingSlot = 0;
            if (doubled) {
                selectedOutputSlot = 0;
            }
            return true;
        }

        if (isInside(mouseX, mouseY, panelLeft + panelWidth / 2 + 8, panelTop + 44, (panelWidth - 46) / 2, 32)) {
            editingSlot = 1;
            if (doubled) {
                selectedOutputSlot = 1;
            }
            return true;
        }

        Item clickedItem = itemAt(mouseX, mouseY);
        if (clickedItem != null) {
            if (editingSlot == 0) {
                itemA = clickedItem;
            } else {
                itemB = clickedItem;
            }
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isInside(mouseX, mouseY, gridLeft, gridTop, gridWidth, gridHeight)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int rows = Math.max(1, gridHeight / (SLOT_SIZE + SLOT_GAP));
        int maxOffset = Math.max(0, (int) Math.ceil(visibleItems.size() / (double) columns) - rows);
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset - (int) Math.signum(verticalAmount)));
        return true;
    }

    private Item itemAt(double mouseX, double mouseY) {
        if (!isInside(mouseX, mouseY, gridLeft, gridTop, gridWidth, gridHeight)) {
            return null;
        }

        int column = (int) ((mouseX - gridLeft) / (SLOT_SIZE + SLOT_GAP));
        int row = (int) ((mouseY - gridTop) / (SLOT_SIZE + SLOT_GAP));
        if (column < 0 || column >= columns) {
            return null;
        }

        int index = (scrollOffset + row) * columns + column;
        return index >= 0 && index < visibleItems.size() ? visibleItems.get(index) : null;
    }

    private void refreshVisibleItems() {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        visibleItems.clear();
        for (Item item : allItems) {
            String id = itemId(item).toLowerCase(Locale.ROOT);
            String name = new ItemStack(item).getName().getString().toLowerCase(Locale.ROOT);
            if (query.isEmpty() || id.contains(query) || name.contains(query)) {
                visibleItems.add(item);
            }
        }
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static Item itemFromId(String id, Item fallback) {
        for (Item item : Registries.ITEM) {
            if (itemId(item).equals(id)) {
                return item;
            }
        }
        return fallback;
    }

    private static String itemId(Item item) {
        return Registries.ITEM.getId(item).toString();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
