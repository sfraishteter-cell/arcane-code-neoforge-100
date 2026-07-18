package dev.sergey.arcanecode.client;

import dev.sergey.arcanecode.network.ArcaneNetwork;
import dev.sergey.arcanecode.network.RuneProgramPayload;
import dev.sergey.arcanecode.spell.PatternNormalizer;
import dev.sergey.arcanecode.spell.RuneDefinition;
import dev.sergey.arcanecode.spell.RuneLibrary;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RuneCastingScreen extends Screen {
    private final List<String> program;
    private final List<Node> nodes = new ArrayList<>();
    private final List<Node> path = new ArrayList<>();
    private final Map<Long, Node> byCoord = new HashMap<>();
    private boolean drawing;
    private Component status = Component.literal("Нарисуй руну на решётке");
    private int panelLeft;
    private int panelTop;

    public RuneCastingScreen(List<String> initialProgram) {
        super(Component.literal("Палата Начертания"));
        this.program = new ArrayList<>(initialProgram);
    }

    @Override
    protected void init() {
        panelLeft = (width - 780) / 2;
        panelTop = (height - 440) / 2;
        buildGrid(panelLeft + 255, panelTop + 220, 30, 5);
        int y = panelTop + 400;
        addRenderableWidget(Button.builder(Component.literal("Отменить"), b -> undo()).bounds(panelLeft + 16, y, 88, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Очистить"), b -> { program.clear(); status = Component.literal("Программа очищена"); }).bounds(panelLeft + 108, y, 88, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Сохранить"), b -> send(ArcaneNetwork.SAVE)).bounds(panelLeft + 500, y, 82, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Запустить"), b -> send(ArcaneNetwork.CAST)).bounds(panelLeft + 586, y, 82, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Кодекс"), b -> minecraft.setScreen(new ArcaneCodexScreen())).bounds(panelLeft + 672, y, 88, 22).build());
    }

    private void buildGrid(int centerX, int centerY, int spacing, int radius) {
        nodes.clear(); byCoord.clear();
        for (int q = -radius; q <= radius; q++) {
            int rMin = Math.max(-radius, -q - radius);
            int rMax = Math.min(radius, -q + radius);
            for (int r = rMin; r <= rMax; r++) {
                int x = centerX + (int)Math.round((q + r * 0.5) * spacing);
                int y = centerY + (int)Math.round(r * spacing * 0.8660254);
                Node node = new Node(q, r, x, y);
                nodes.add(node); byCoord.put(key(q, r), node);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Node node = nearest(mouseX, mouseY, 12);
            if (node != null) {
                drawing = true; path.clear(); path.add(node); return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (drawing && button == 0) {
            Node node = nearest(mouseX, mouseY, 12);
            if (node != null && !path.isEmpty() && adjacent(path.get(path.size() - 1), node) && !node.equals(path.get(path.size() - 1))) {
                path.add(node);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (drawing && button == 0) {
            drawing = false;
            recognize();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void recognize() {
        if (path.size() < 3) { status = Component.literal("§cРуна слишком короткая"); return; }
        List<Integer> directions = new ArrayList<>();
        for (int i = 1; i < path.size(); i++) directions.add(direction(path.get(i - 1), path.get(i)));
        RuneDefinition rune = RuneLibrary.byPattern(PatternNormalizer.key(directions));
        if (rune == null) { status = Component.literal("§cНеизвестная геометрия"); return; }
        program.add(rune.id());
        status = Component.literal("§dДобавлено: ").append(rune.name());
    }

    private void undo() {
        if (!program.isEmpty()) program.remove(program.size() - 1);
        status = Component.literal("Последняя руна удалена");
    }

    private void send(int action) {
        if (program.isEmpty()) { status = Component.literal("§cПрограмма пуста"); return; }
        ClientPacketDistributor.sendToServer(new RuneProgramPayload(String.join(",", program), action));
        status = Component.literal(action == ArcaneNetwork.SAVE ? "§aОтправлено на сохранение" : "§aОтправлено на выполнение");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(panelLeft, panelTop, panelLeft + 780, panelTop + 440, 0xF0140C1D);
        graphics.fill(panelLeft + 8, panelTop + 8, panelLeft + 490, panelTop + 390, 0xFF20112B);
        graphics.fill(panelLeft + 498, panelTop + 8, panelLeft + 772, panelTop + 390, 0xFF17101F);
        graphics.drawCenteredString(font, title, panelLeft + 390, panelTop + 14, 0xFFF0DEFF);
        graphics.drawString(font, "Зажми ЛКМ и проведи через соседние узлы", panelLeft + 18, panelTop + 34, 0xFFB9A7C7, false);

        for (Node node : nodes) {
            graphics.fill(node.x - 2, node.y - 2, node.x + 3, node.y + 3, 0xFF725086);
        }
        for (int i = 1; i < path.size(); i++) drawLine(graphics, path.get(i-1).x, path.get(i-1).y, path.get(i).x, path.get(i).y, 0xFFE4A8FF);
        for (Node node : path) graphics.fill(node.x - 4, node.y - 4, node.x + 5, node.y + 5, 0xFFC46BFF);

        graphics.drawString(font, "Программа: " + program.size() + " рун", panelLeft + 512, panelTop + 36, 0xFFE6D7EF, false);
        int start = Math.max(0, program.size() - 22);
        for (int i = start; i < program.size(); i++) {
            RuneDefinition rune = RuneLibrary.byId(program.get(i));
            String name = rune == null ? program.get(i) : rune.name().getString();
            graphics.drawString(font, (i + 1) + ". " + trim(name, 28), panelLeft + 512, panelTop + 56 + (i - start) * 14, 0xFFCDBED6, false);
        }
        graphics.drawString(font, status, panelLeft + 18, panelTop + 376, 0xFFE6C5FF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Node nearest(double x, double y, double radius) {
        Node best = null; double bestDistance = radius * radius;
        for (Node node : nodes) {
            double dx = node.x - x, dy = node.y - y, distance = dx * dx + dy * dy;
            if (distance < bestDistance) { bestDistance = distance; best = node; }
        }
        return best;
    }

    private static boolean adjacent(Node a, Node b) {
        int dq = b.q - a.q, dr = b.r - a.r;
        for (int i=0;i<6;i++) if (dq==DQ[i] && dr==DR[i]) return true;
        return false;
    }

    private static int direction(Node a, Node b) {
        int dq=b.q-a.q, dr=b.r-a.r;
        for(int i=0;i<6;i++) if(dq==DQ[i]&&dr==DR[i]) return i;
        return -1;
    }

    private static long key(int q, int r) { return (((long)q) << 32) ^ (r & 0xffffffffL); }
    private static String trim(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 1) + "…"; }

    private static void drawLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx=Math.abs(x1-x0), sx=x0<x1?1:-1, dy=-Math.abs(y1-y0), sy=y0<y1?1:-1, error=dx+dy;
        while(true){ graphics.fill(x0-1,y0-1,x0+2,y0+2,color); if(x0==x1&&y0==y1)break; int e2=2*error; if(e2>=dy){error+=dy;x0+=sx;} if(e2<=dx){error+=dx;y0+=sy;} }
    }

    @Override public boolean isPauseScreen() { return false; }
    private record Node(int q, int r, int x, int y) {}
    private static final int[] DQ={1,1,0,-1,-1,0};
    private static final int[] DR={0,-1,-1,0,1,1};
}
