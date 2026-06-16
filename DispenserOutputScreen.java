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
    private static final int SLOT_SIZE = 26;
    private static final int SLOT_GAP = 5;
    private static final int PANEL_MARGIN = 10;
    private static final int HOTBAR_MARGIN = 42;

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

        panelWidth = Math.min(540, this.width - PANEL_MARGIN * 2);
        panelHeight = Math.min(this.height - PANEL_MARGIN * 2, Math.max(280, this.height - PANEL_MARGIN - HOTBAR_MARGIN));
        panelLeft = (this.width - panelWidth) / 2;
        panelTop = PANEL_MARGIN;
        gridLeft = panelLeft + 14;
        gridTop = panelTop + 134;
        gridWidth = panelWidth - 28;
        gridHeight = panelHeight - 182;
        columns = Math.max(1, gridWidth / (SLOT_SIZE + SLOT_GAP));

        searchField = new TextFieldWidget(this.textRenderer, panelLeft + 14, panelTop + 102, panelWidth - 28, 20, Text.literal("Search items"));
        searchField.setMaxLength(80);
        searchField.setChangedListener(value -> {
            scrollOffset = 0;
            refreshVisibleItems();
        });
        addDrawableChild(searchField);

        int halfWidth = (panelWidth - 36) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Output A"), button -> selectedOutputSlot = 0)
                .dimensions(panelLeft + 14, panelTop + 76, halfWidth, 18)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Output B"), button -> selectedOutputSlot = 1)
                .dimensions(panelLeft + panelWidth / 2 + 4, panelTop + 76, halfWidth, 18)
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
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xFF20242B);
        context.drawStrokedRectangle(panelLeft, panelTop, panelWidth, panelHeight, 0xFFB9C2D0);
        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 30, 0xFF303743);
        context.fill(panelLeft + 1, panelTop + 30, panelLeft + panelWidth - 1, panelTop + 31, 0xFF101217);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, panelTop + 10, 0xFFFFFFFF);

        drawChoiceSlot(context, panelLeft + 14, panelTop + 40, 0, itemA);
        drawChoiceSlot(context, panelLeft + panelWidth / 2 + 4, panelTop + 40, 1, itemB);

        context.drawTextWithShadow(this.textRenderer, Text.literal("Search items"), panelLeft + 14, panelTop + 91, 0xFFDCE4F0);
        context.fill(gridLeft - 3, gridTop - 3, gridLeft + gridWidth + 3, gridTop + gridHeight + 3, 0xFF111318);
        context.drawStrokedRectangle(gridLeft - 3, gridTop - 3, gridWidth + 6, gridHeight + 6, 0xFF4E5868);

        drawItemGrid(context, mouseX, mouseY);
        drawScrollBar(context);
        super.render(context, mouseX, mouseY, delta);

        Text mode = Text.literal("Click an item to set " + (editingSlot == 0 ? "Item A" : "Item B")
                + " | Output: " + (selectedOutputSlot == 0 ? "Item A" : "Item B"));
        context.fill(panelLeft + 1, panelTop + panelHeight - 47, panelLeft + panelWidth - 1, panelTop + panelHeight - 46, 0xFF101217);
        context.drawTextWithShadow(this.textRenderer, mode, panelLeft + 14, panelTop + panelHeight - 30, 0xFFFFE08A);
    }

    private void drawChoiceSlot(DrawContext context, int x, int y, int slot, Item item) {
        int width = (panelWidth - 36) / 2;
        int background = editingSlot == slot ? 0xFF4B5D7A : 0xFF333740;
        int outline = selectedOutputSlot == slot ? 0xFFFFD45A : 0xFF7A808C;

        context.fill(x, y, x + width, y + 30, background);
        context.drawStrokedRectangle(x, y, width, 30, outline);
        context.drawItem(new ItemStack(item), x + 7, y + 7);
        context.drawTextWithShadow(this.textRenderer, Text.literal(slot == 0 ? "Item A" : "Item B"), x + 30, y + 4, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal(shortItemId(item)), x + 30, y + 16, 0xFFB7C4D8);
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

            context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, hovered ? 0xFF586579 : 0xFF2D323B);
            context.drawStrokedRectangle(x, y, SLOT_SIZE, SLOT_SIZE, selected ? 0xFFFFD45A : 0xFF666C78);
            context.drawItem(new ItemStack(item), x + 5, y + 5);

            if (hovered) {
                context.drawTooltip(this.textRenderer, Text.literal(itemId(item)), mouseX, mouseY);
            }
        }

        if (visibleItems.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("No items found"), this.width / 2, gridTop + gridHeight / 2 - 4, 0xFFFFFFFF);
        }
    }

    private void drawScrollBar(DrawContext context) {
        int rows = Math.max(1, gridHeight / (SLOT_SIZE + SLOT_GAP));
        int totalRows = (int) Math.ceil(visibleItems.size() / (double) columns);
        int maxOffset = Math.max(0, totalRows - rows);
        if (maxOffset <= 0) {
            return;
        }

        int barX = gridLeft + gridWidth - 5;
        int barTop = gridTop + 3;
        int barHeight = gridHeight - 6;
        int thumbHeight = Math.max(18, barHeight * rows / totalRows);
        int thumbY = barTop + (barHeight - thumbHeight) * scrollOffset / maxOffset;
        context.fill(barX, barTop, barX + 3, barTop + barHeight, 0xFF252932);
        context.fill(barX, thumbY, barX + 3, thumbY + thumbHeight, 0xFF8E98A8);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (isInside(mouseX, mouseY, panelLeft + 14, panelTop + 40, (panelWidth - 36) / 2, 30)) {
            editingSlot = 0;
            if (doubled) {
                selectedOutputSlot = 0;
            }
            return true;
        }

        if (isInside(mouseX, mouseY, panelLeft + panelWidth / 2 + 4, panelTop + 40, (panelWidth - 36) / 2, 30)) {
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
        int localX = (int) (mouseX - gridLeft) % (SLOT_SIZE + SLOT_GAP);
        int localY = (int) (mouseY - gridTop) % (SLOT_SIZE + SLOT_GAP);
        if (column < 0 || column >= columns || localX >= SLOT_SIZE || localY >= SLOT_SIZE) {
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

    private static String shortItemId(Item item) {
        String id = itemId(item);
        return id.length() <= 28 ? id : "..." + id.substring(id.length() - 25);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
