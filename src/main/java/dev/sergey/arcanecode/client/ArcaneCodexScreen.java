package dev.sergey.arcanecode.client;

import dev.sergey.arcanecode.guide.GuideChapter;
import dev.sergey.arcanecode.guide.GuideContent;
import dev.sergey.arcanecode.registry.ModItems;
import dev.sergey.arcanecode.spell.RuneDefinition;
import dev.sergey.arcanecode.spell.RuneLibrary;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ArcaneCodexScreen extends Screen {
    private int chapterIndex;
    private int runeScroll;
    private int craftIndex;
    private EditBox search;

    public ArcaneCodexScreen() {
        super(Component.literal("Арканный Кодекс"));
    }

    @Override
    protected void init() {
        int left = (width - 760) / 2;
        int top = (height - 430) / 2;
        search = new EditBox(font, left + 190, top + 12, 350, 20, Component.literal("Поиск"));
        search.setHint(Component.literal("Поиск по рунам, категориям и сигнатурам"));
        search.setResponder(value -> runeScroll = 0);
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("←"), b -> previousChapter()).bounds(left + 548, top + 12, 28, 20).build());
        addRenderableWidget(Button.builder(Component.literal("→"), b -> nextChapter()).bounds(left + 580, top + 12, 28, 20).build());
        addRenderableWidget(Button.builder(Component.literal("На главную"), b -> chapterIndex = 0).bounds(left + 612, top + 12, 120, 20).build());
    }

    private void previousChapter() { chapterIndex = Math.floorMod(chapterIndex - 1, GuideContent.CHAPTERS.size()); runeScroll = 0; }
    private void nextChapter() { chapterIndex = Math.floorMod(chapterIndex + 1, GuideContent.CHAPTERS.size()); runeScroll = 0; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (width - 760) / 2;
        int top = (height - 430) / 2;
        for (int i = 0; i < GuideContent.CHAPTERS.size(); i++) {
            if (mouseX >= left + 12 && mouseX <= left + 174 && mouseY >= top + 48 + i * 28 && mouseY <= top + 70 + i * 28) {
                chapterIndex = i;
                runeScroll = 0;
                return true;
            }
        }
        if (GuideContent.CHAPTERS.get(chapterIndex).id().equals("crafts")) {
            int listX = left + 438;
            int listY = top + 78;
            for (int i = 0; i < CRAFTS.size(); i++) {
                if (mouseX >= listX && mouseX <= left + 742 && mouseY >= listY + i * 30 && mouseY <= listY + i * 30 + 25) {
                    craftIndex = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (GuideContent.CHAPTERS.get(chapterIndex).id().equals("runes")) {
            runeScroll = Math.max(0, runeScroll - (int)Math.signum(scrollY) * 3);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (width - 760) / 2;
        int top = (height - 430) / 2;
        graphics.fill(left, top, left + 760, top + 430, 0xF0181024);
        graphics.fill(left + 4, top + 4, left + 180, top + 426, 0xFF24162F);
        graphics.fill(left + 184, top + 4, left + 756, top + 426, 0xFF16101F);
        graphics.drawCenteredString(font, title, left + 90, top + 14, 0xFFE8D4FF);

        for (int i = 0; i < GuideContent.CHAPTERS.size(); i++) {
            GuideChapter chapter = GuideContent.CHAPTERS.get(i);
            int color = i == chapterIndex ? 0xFFB66CFF : 0xFFD4C3E0;
            graphics.fill(left + 10, top + 46 + i * 28, left + 176, top + 70 + i * 28, i == chapterIndex ? 0xFF482760 : 0xFF2B1B37);
            graphics.drawString(font, chapter.title(), left + 16, top + 54 + i * 28, color, false);
        }

        GuideChapter chapter = GuideContent.CHAPTERS.get(chapterIndex);
        graphics.drawString(font, chapter.title(), left + 198, top + 48, 0xFFEEDBFF, false);
        if (chapter.id().equals("runes")) renderRunes(graphics, left + 198, top + 72);
        else if (chapter.id().equals("crafts")) renderCrafts(graphics, left + 198, top + 76);
        else renderParagraphs(graphics, chapter, left + 198, top + 78, 530);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderParagraphs(GuiGraphics graphics, GuideChapter chapter, int x, int y, int maxWidth) {
        int cursor = y;
        for (String paragraph : chapter.paragraphs()) {
            for (var line : font.split(Component.literal(paragraph), maxWidth)) {
                graphics.drawString(font, line, x, cursor, 0xFFD8CBDD, false);
                cursor += 12;
            }
            cursor += 10;
        }
    }


    private void renderCrafts(GuiGraphics graphics, int x, int y) {
        craftIndex = Math.max(0, Math.min(craftIndex, CRAFTS.size() - 1));
        CraftPage page = CRAFTS.get(craftIndex);
        graphics.drawString(font, "Выбери предмет справа", x, y, 0xFFAA96B7, false);
        graphics.drawString(font, page.title(), x, y + 20, 0xFFEBD9F5, false);

        int gridX = x + 18;
        int gridY = y + 56;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int cellX = gridX + col * 34;
                int cellY = gridY + row * 34;
                graphics.fill(cellX, cellY, cellX + 28, cellY + 28, 0xFF362143);
                graphics.fill(cellX + 2, cellY + 2, cellX + 26, cellY + 26, 0xFF1B1223);
                ItemStack ingredient = page.grid()[row][col];
                if (!ingredient.isEmpty()) graphics.renderItem(ingredient, cellX + 6, cellY + 6);
            }
        }
        graphics.drawString(font, "→", gridX + 112, gridY + 39, 0xFFD69BFF, false);
        graphics.fill(gridX + 136, gridY + 29, gridX + 170, gridY + 63, 0xFF4A2A5C);
        graphics.renderItem(page.result(), gridX + 145, gridY + 38);

        int noteY = gridY + 118;
        for (var line : font.split(Component.literal(page.note()), 220)) {
            graphics.drawString(font, line, x, noteY, 0xFFCDBDD5, false);
            noteY += 12;
        }

        int listX = x + 240;
        for (int i = 0; i < CRAFTS.size(); i++) {
            CraftPage craft = CRAFTS.get(i);
            int rowY = y + i * 30;
            graphics.fill(listX, rowY, listX + 304, rowY + 25, i == craftIndex ? 0xFF4C2960 : 0xFF25172F);
            graphics.renderItem(craft.result(), listX + 5, rowY + 5);
            graphics.drawString(font, craft.title(), listX + 27, rowY + 8, i == craftIndex ? 0xFFE5B8FF : 0xFFD1C1DA, false);
        }
    }

    private static ItemStack v(net.minecraft.world.item.Item item) { return item.getDefaultInstance(); }
    private static ItemStack m(net.neoforged.neoforge.registries.DeferredItem<? extends net.minecraft.world.item.Item> item) { return item.toStack(); }
    private static ItemStack[][] grid(ItemStack... values) {
        ItemStack[][] result = new ItemStack[3][3];
        for (int i = 0; i < 9; i++) result[i / 3][i % 3] = i < values.length ? values[i] : ItemStack.EMPTY;
        return result;
    }

    private record CraftPage(String title, ItemStack result, ItemStack[][] grid, String note) {}

    private static final List<CraftPage> CRAFTS = List.of(
        new CraftPage("Резонансный жернов", m(ModItems.RESONANCE_MILL), grid(
            v(Items.STONE), v(Items.IRON_INGOT), v(Items.STONE),
            v(Items.AMETHYST_SHARD), v(Items.COPPER_INGOT), v(Items.AMETHYST_SHARD),
            v(Items.STONE), v(Items.IRON_INGOT), v(Items.STONE)),
            "Долговечный ручной инструмент. Держи железо или аметист во второй руке и нажми ПКМ."),
        new CraftPage("Арканный Кодекс", m(ModItems.ARCANE_CODEX), grid(
            v(Items.AMETHYST_SHARD), v(Items.PAPER), v(Items.AMETHYST_SHARD),
            v(Items.BOOK), m(ModItems.ARCANE_DUST), v(Items.BOOK),
            v(Items.AMETHYST_SHARD), v(Items.GOLD_INGOT), v(Items.AMETHYST_SHARD)),
            "Выдаётся на первом входе, но его можно создать повторно."),
        new CraftPage("Посох Искры", m(ModItems.SPARK_STAFF), grid(
            ItemStack.EMPTY, v(Items.AMETHYST_SHARD), m(ModItems.ARCANE_DUST),
            ItemStack.EMPTY, v(Items.STICK), v(Items.AMETHYST_SHARD),
            v(Items.STICK), ItemStack.EMPTY, ItemStack.EMPTY),
            "Учебный посох с запасом 2000 медиа."),
        new CraftPage("Посох Резонанса", m(ModItems.RESONANCE_STAFF), grid(
            ItemStack.EMPTY, v(Items.GOLD_INGOT), v(Items.QUARTZ),
            ItemStack.EMPTY, m(ModItems.ARCANE_DUST), m(ModItems.SPARK_STAFF),
            v(Items.BLAZE_ROD), ItemStack.EMPTY, ItemStack.EMPTY),
            "Вторая ступень: 8000 медиа и уменьшенная стоимость рун."),
        new CraftPage("Посох Архитектора", m(ModItems.ARCHITECT_STAFF), grid(
            ItemStack.EMPTY, v(Items.NETHER_STAR), v(Items.ENDER_PEARL),
            ItemStack.EMPTY, m(ModItems.ARCANE_DUST), m(ModItems.RESONANCE_STAFF),
            v(Items.END_ROD), ItemStack.EMPTY, ItemStack.EMPTY),
            "Строительный посох: 32000 медиа и эффективность 70%."),
        new CraftPage("Посох Владыки Плетения", m(ModItems.SOVEREIGN_STAFF), grid(
            ItemStack.EMPTY, v(Items.BEACON), v(Items.DRAGON_BREATH),
            ItemStack.EMPTY, m(ModItems.ARCANE_DUST), m(ModItems.ARCHITECT_STAFF),
            v(Items.NETHERITE_INGOT), ItemStack.EMPTY, ItemStack.EMPTY),
            "Высший посох: 128000 медиа и эффективность 55%."),
        new CraftPage("Свиток Рун", m(ModItems.RUNE_SCROLL), grid(
            v(Items.PAPER), v(Items.GLOW_INK_SAC), m(ModItems.ARCANE_DUST)),
            "Бесформенный рецепт. Создаёт два переносных свитка."),
        new CraftPage("Фокус-призма", m(ModItems.FOCUS_PRISM), grid(
            v(Items.AMETHYST_SHARD), m(ModItems.ARCANE_DUST), v(Items.AMETHYST_SHARD),
            m(ModItems.ARCANE_DUST), v(Items.GLASS), m(ModItems.ARCANE_DUST),
            v(Items.AMETHYST_SHARD), m(ModItems.ARCANE_DUST), v(Items.AMETHYST_SHARD)),
            "Многоразовый носитель сложных программ."),
        new CraftPage("Артефакт Плетения", m(ModItems.WOVEN_ARTIFACT), grid(
            v(Items.NETHERITE_SCRAP), m(ModItems.FOCUS_PRISM), v(Items.NETHERITE_SCRAP),
            m(ModItems.ARCANE_DUST), v(Items.CONDUIT), m(ModItems.ARCANE_DUST),
            v(Items.NETHERITE_SCRAP), m(ModItems.FOCUS_PRISM), v(Items.NETHERITE_SCRAP)),
            "Старший огнестойкий носитель программы."));

    private void renderRunes(GuiGraphics graphics, int x, int y) {
        String query = search.getValue().toLowerCase(Locale.ROOT).strip();
        List<RuneDefinition> filtered = new ArrayList<>();
        for (RuneDefinition rune : RuneLibrary.all()) {
            String haystack = (rune.id() + " " + rune.category() + " " + rune.signature() + " " + rune.name().getString()).toLowerCase(Locale.ROOT);
            if (query.isBlank() || haystack.contains(query)) filtered.add(rune);
        }
        int visible = 14;
        runeScroll = Math.min(runeScroll, Math.max(0, filtered.size() - visible));
        graphics.drawString(font, "Найдено: " + filtered.size(), x, y - 16, 0xFF9F8CAB, false);
        for (int row = 0; row < visible && row + runeScroll < filtered.size(); row++) {
            RuneDefinition rune = filtered.get(row + runeScroll);
            int rowY = y + row * 23;
            graphics.fill(x, rowY, x + 528, rowY + 20, 0xFF21162A);
            drawPattern(graphics, rune, x + 22, rowY + 10, 4.0);
            graphics.drawString(font, rune.name(), x + 48, rowY + 3, 0xFFE6D5F2, false);
            graphics.drawString(font, rune.signature(), x + 250, rowY + 3, 0xFFA997B5, false);
            graphics.drawString(font, "◆" + rune.mediaCost(), x + 468, rowY + 3, 0xFFB56DFF, false);
        }
    }

    private void drawPattern(GuiGraphics graphics, RuneDefinition rune, int centerX, int centerY, double scale) {
        int q = 0, r = 0;
        int lastX = centerX, lastY = centerY;
        for (int direction : rune.directions()) {
            q += DQ[direction]; r += DR[direction];
            int x = centerX + (int)Math.round((q + r * 0.5) * scale * 1.7);
            int y = centerY + (int)Math.round(r * scale * 1.5);
            drawLine(graphics, lastX, lastY, x, y, 0xFFB86CFF);
            lastX = x; lastY = y;
        }
    }

    private static void drawLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            graphics.fill(x0 - 1, y0 - 1, x0 + 2, y0 + 2, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * error;
            if (e2 >= dy) { error += dy; x0 += sx; }
            if (e2 <= dx) { error += dx; y0 += sy; }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static final int[] DQ = {1, 1, 0, -1, -1, 0};
    private static final int[] DR = {0, -1, -1, 0, 1, 1};
}
