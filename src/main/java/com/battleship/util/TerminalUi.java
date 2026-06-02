package com.battleship.util;

import java.awt.Frame;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;

public class TerminalUi {

    private static final Pattern OPTION_PATTERN = Pattern.compile("^\\[(\\d+)]\\s*(.*)$");
    private static final Pattern HP_PATTERN = Pattern.compile("(\\d+)/(\\d+)");

    private static final TextColor BG = TextColor.Factory.fromString("#050816");
    private static final TextColor PANEL = TextColor.Factory.fromString("#0f172a");
    private static final TextColor PANEL_ALT = TextColor.Factory.fromString("#111c34");
    private static final TextColor PANEL_SOFT = TextColor.Factory.fromString("#16213f");
    private static final TextColor BORDER = TextColor.Factory.fromString("#2dd4bf");
    private static final TextColor BORDER_ALT = TextColor.Factory.fromString("#60a5fa");
    private static final TextColor ACCENT = TextColor.Factory.fromString("#f59e0b");
    private static final TextColor ALERT = TextColor.Factory.fromString("#fb7185");
    private static final TextColor TEXT = TextColor.Factory.fromString("#e2e8f0");
    private static final TextColor SUBTLE = TextColor.Factory.fromString("#94a3b8");
    private static final TextColor SUCCESS = TextColor.Factory.fromString("#86efac");

    private final InputHelper input;
    private Screen screen;
    private boolean started;
    private int terminalColumns = 80;
    private int terminalRows = 24;

    public TerminalUi(InputHelper input) {
        this.input = input;
    }

    public void showScreen(String title, List<String> body) {
        showScreen(title, body, null);
    }

    public void showScreen(String title, List<String> body, String footer) {
        try {
            ensureScreen();
            renderScreen(title, normalize(body), footer, null, -1, "", 0);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal menampilkan terminal UI", e);
        }
    }

    public void pause(String title, List<String> body) {
        List<String> lines = normalize(body);
        int scrollOffset = 0;
        try {
            ensureScreen();
            drainInput();
            while (true) {
                scrollOffset = clampScrollOffset(lines, null, -1, scrollOffset);
                renderScreen(title, lines, "Enter / Space lanjut | Up/Down scroll", null, -1, "", scrollOffset);
                KeyStroke key = screen.readInput();
                if (key == null) {
                    continue;
                }
                if (key.getKeyType() == KeyType.Enter || key.getKeyType() == KeyType.EOF) {
                    return;
                }
                if (key.getKeyType() == KeyType.ArrowUp || key.getKeyType() == KeyType.PageUp) {
                    scrollOffset = Math.max(0, scrollOffset - scrollStep(key));
                    continue;
                }
                if (key.getKeyType() == KeyType.ArrowDown || key.getKeyType() == KeyType.PageDown) {
                    scrollOffset += scrollStep(key);
                    continue;
                }
                if (key.getKeyType() == KeyType.Home) {
                    scrollOffset = 0;
                    continue;
                }
                if (key.getKeyType() == KeyType.End) {
                    scrollOffset = maxScrollOffset(lines, null, -1);
                    continue;
                }
                if (key.getKeyType() == KeyType.Character && key.getCharacter() == ' ') {
                    return;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca input terminal", e);
        }
    }

    public String promptText(String title, List<String> body, String prompt) {
        StringBuilder value = new StringBuilder();
        List<String> lines = normalize(body);
        int scrollOffset = 0;
        try {
            ensureScreen();
            drainInput();
            while (true) {
                scrollOffset = clampScrollOffset(lines, null, -1, scrollOffset);
                renderScreen(title, lines, prompt, null, -1, value.toString(), scrollOffset);
                KeyStroke key = screen.readInput();
                if (key == null) {
                    continue;
                }

                if (key.getKeyType() == KeyType.Enter) {
                    return value.toString().trim();
                }
                if (key.getKeyType() == KeyType.ArrowUp || key.getKeyType() == KeyType.PageUp) {
                    scrollOffset = Math.max(0, scrollOffset - scrollStep(key));
                    continue;
                }
                if (key.getKeyType() == KeyType.ArrowDown || key.getKeyType() == KeyType.PageDown) {
                    scrollOffset += scrollStep(key);
                    continue;
                }
                if (key.getKeyType() == KeyType.Home) {
                    scrollOffset = 0;
                    continue;
                }
                if (key.getKeyType() == KeyType.End) {
                    scrollOffset = maxScrollOffset(lines, null, -1);
                    continue;
                }
                if (key.getKeyType() == KeyType.Backspace || key.getKeyType() == KeyType.Delete) {
                    if (!value.isEmpty()) {
                        value.deleteCharAt(value.length() - 1);
                    }
                    continue;
                }
                if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
                    char ch = key.getCharacter();
                    if (!Character.isISOControl(ch)) {
                        value.append(ch);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca input terminal", e);
        }
    }

    public int promptChoice(String title, List<String> body, int min, int max, int defaultValue, String prompt) {
        List<String> lines = normalize(body);
        List<ChoiceGroup> groups = extractChoiceGroups(lines, min, max);
        if (groups.isEmpty()) {
            return defaultValue;
        }
        int selected = Math.max(0, Math.min(groups.size() - 1, defaultValue - min));
        int scrollOffset = 0;

        try {
            ensureScreen();
            drainInput();
            while (true) {
                int selectedValue = groups.isEmpty() ? defaultValue : groups.get(selected).option().value();
                scrollOffset = clampScrollOffset(lines, groups, selectedValue, scrollOffset);
                renderScreen(title, lines, prompt, groups, selectedValue, "", scrollOffset);
                KeyStroke key = screen.readInput();
                if (key == null) {
                    continue;
                }

                if (key.getKeyType() == KeyType.ArrowUp || key.getKeyType() == KeyType.ArrowLeft
                        || key.getKeyType() == KeyType.ReverseTab) {
                    selected = (selected - 1 + groups.size()) % groups.size();
                    continue;
                }
                if (key.getKeyType() == KeyType.ArrowDown || key.getKeyType() == KeyType.ArrowRight
                        || key.getKeyType() == KeyType.Tab) {
                    selected = (selected + 1) % groups.size();
                    continue;
                }
                if (key.getKeyType() == KeyType.Home) {
                    selected = 0;
                    continue;
                }
                if (key.getKeyType() == KeyType.End) {
                    selected = groups.size() - 1;
                    continue;
                }
                if (key.getKeyType() == KeyType.PageUp) {
                    scrollOffset = Math.max(0, scrollOffset - scrollStep(key));
                    continue;
                }
                if (key.getKeyType() == KeyType.PageDown) {
                    scrollOffset += scrollStep(key);
                    continue;
                }
                if (key.getKeyType() == KeyType.Escape) {
                    return defaultValue;
                }
                if (key.getKeyType() == KeyType.Enter) {
                    return groups.isEmpty() ? defaultValue : groups.get(selected).option().value();
                }
                if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
                    char command = Character.toLowerCase(key.getCharacter());
                    if (command == 'w' || command == 'k') {
                        selected = (selected - 1 + groups.size()) % groups.size();
                        continue;
                    }
                    if (command == 's' || command == 'j') {
                        selected = (selected + 1) % groups.size();
                        continue;
                    }
                    int digit = Character.digit(key.getCharacter(), 10);
                    if (digit >= min && digit <= max) {
                        return digit;
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca input terminal", e);
        }
    }

    public List<String> wrapParagraph(String text) {
        return wrap(text, 48);
    }

    public void shutdown() {
        if (screen == null) {
            return;
        }
        try {
            if (started) {
                screen.stopScreen();
            }
            screen.close();
        } catch (IOException ignored) {
        } finally {
            screen = null;
            started = false;
        }
    }

    private void ensureScreen() throws IOException {
        if (screen != null) {
            screen.doResizeIfNecessary();
            return;
        }

        DefaultTerminalFactory factory = new DefaultTerminalFactory();
        factory.setInitialTerminalSize(new TerminalSize(132, 40));
        Terminal terminal;
        if (isWindows()) {
            SwingTerminalFrame terminalFrame = factory.createSwingTerminal();
            terminalFrame.setTitle("Runewater");
            terminalFrame.setSize(1120, 760);
            terminalFrame.setLocationRelativeTo(null);
            terminalFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
            terminalFrame.setVisible(true);
            terminal = terminalFrame;
        } else {
            terminal = factory.createTerminal();
        }

        screen = new TerminalScreen(terminal);
        screen.startScreen();
        screen.setCursorPosition(null);
        started = true;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private void renderScreen(
            String title,
            List<String> body,
            String footer,
            List<ChoiceGroup> groups,
            int selectedValue,
            String inputValue,
            int scrollOffset
    ) throws IOException {
        screen.clear();
        screen.setCursorPosition(null);
        screen.doResizeIfNecessary();
        TerminalSize size = screen.getTerminalSize();
        terminalColumns = Math.max(1, size.getColumns());
        terminalRows = Math.max(1, size.getRows());
        TextGraphics g = screen.newTextGraphics();

        g.setBackgroundColor(BG);
        g.setForegroundColor(TEXT);
        g.fill(' ');

        drawBanner(g, size, title);
        if (title.toLowerCase().contains("battle")) {
            renderBattleScreen(g, size, body, footer, groups, selectedValue, scrollOffset);
        } else {
            renderGenericScreen(g, size, body, footer, groups, selectedValue, inputValue, scrollOffset);
        }

        screen.refresh(Screen.RefreshType.COMPLETE);
    }

    private void drawBanner(TextGraphics g, TerminalSize size, String title) {
        int width = size.getColumns() - 4;
        drawPanel(g, 2, 1, width, 5, BORDER, PANEL_SOFT);
        drawText(g, 5, 2, "RUNEWATER", SUCCESS, PANEL_SOFT, true);
        drawText(g, 5, 3, trim(title, Math.max(12, width - 35)), TEXT, PANEL_SOFT, false);
        if (size.getColumns() >= 84) {
            drawText(g, size.getColumns() - 32, 3, "Arrows / Tab | Enter", SUBTLE, PANEL_SOFT, false);
            drawText(g, size.getColumns() - 39, 2, "~ tide ~ rune ~ tide ~", BORDER_ALT, PANEL_SOFT, false);
        } else {
            drawText(g, Math.max(5, size.getColumns() - 18), 3, "Enter", SUBTLE, PANEL_SOFT, false);
        }
    }

    private void renderGenericScreen(
            TextGraphics g,
            TerminalSize size,
            List<String> body,
            String footer,
            List<ChoiceGroup> groups,
            int selectedValue,
            String inputValue,
            int scrollOffset
    ) {
        int mainX = 2;
        int mainY = 7;
        int mainW = size.getColumns() - 4;
        int mainH = size.getRows() - 9;

        drawPanel(g, mainX, mainY, mainW, mainH, BORDER_ALT, PANEL);

        List<String> introLines = extractIntroLines(body);
        ChoiceGroup selectedGroup = findSelectedGroup(groups, selectedValue);

        if (shouldUseCardLayout(groups) && mainW >= 86 && mainH >= 24) {
            renderCardSelection(g, mainX, mainY, mainW, mainH, introLines, footer, groups, selectedValue, inputValue);
            return;
        }

        boolean hasChoices = groups != null && !groups.isEmpty();
        if (hasChoices && mainW < 88) {
            renderCompactGeneric(g, mainX, mainY, mainW, mainH, introLines, footer,
                    groups, selectedValue, inputValue, scrollOffset);
            return;
        }

        int leftW = hasChoices ? Math.max(42, mainW - 32) : mainW - 4;
        int rightW = hasChoices ? mainW - leftW - 3 : 0;

        drawPanel(g, mainX + 2, mainY + 2, leftW, mainH - 7, BORDER, PANEL_ALT);
        drawText(g, mainX + 4, mainY + 3, "Info", SUCCESS, PANEL_ALT, true);

        int textWidth = leftW - 4;
        int visibleRows = Math.max(1, mainH - 13);
        List<String> infoLines = buildInfoLines(introLines, selectedGroup);
        int totalRows = drawWrappedLines(g, infoLines, mainX + 4, mainY + 5, textWidth,
                visibleRows, scrollOffset, TEXT, PANEL_ALT);
        drawScrollStatus(g, mainX + 4, mainY + mainH - 9, textWidth, scrollOffset, totalRows, visibleRows, PANEL_ALT);

        if (hasChoices) {
            int menuX = mainX + leftW + 3;
            drawPanel(g, menuX, mainY + 2, rightW - 1, mainH - 7, ACCENT, PANEL_SOFT);
            drawText(g, menuX + 2, mainY + 3, "Command", ACCENT, PANEL_SOFT, true);

            int optionRow = mainY + 5;
            int labelWidth = rightW - 6;
            for (ChoiceGroup group : groups) {
                if (optionRow >= mainY + mainH - 8) {
                    break;
                }
                boolean selected = group.option().value() == selectedValue;
                TextColor bg = selected ? ACCENT : PANEL_SOFT;
                TextColor fg = selected ? PANEL : TEXT;
                String label = (selected ? "> " : "  ") + group.option().label();
                drawText(g, menuX + 2, optionRow++, pad(label, labelWidth), fg, bg, selected);
            }
        }

        if (footer != null && !footer.isBlank()) {
            drawPanel(g, mainX + 2, mainY + mainH - 4, mainW - 4, 3, BORDER_ALT, PANEL_SOFT);
            String footerText = inputValue.isBlank() ? footer : footer + " " + inputValue + "_";
            drawText(g, mainX + 4, mainY + mainH - 3, footerText, TEXT, PANEL_SOFT, false);
        }
    }

    private void renderCardSelection(
            TextGraphics g,
            int mainX,
            int mainY,
            int mainW,
            int mainH,
            List<String> introLines,
            String footer,
            List<ChoiceGroup> groups,
            int selectedValue,
            String inputValue
    ) {
        drawPanel(g, mainX + 2, mainY + 2, mainW - 4, 6, BORDER, PANEL_ALT);
        drawText(g, mainX + 4, mainY + 3, "Encounter", SUCCESS, PANEL_ALT, true);

        int introRow = mainY + 4;
        for (String line : introLines) {
            for (String wrapped : wrap(line, mainW - 10)) {
                if (introRow >= mainY + 7) {
                    break;
                }
                drawText(g, mainX + 4, introRow++, wrapped, TEXT, PANEL_ALT, false);
            }
        }

        int gap = 2;
        int cardY = mainY + 10;
        int cardW = (mainW - 8 - (gap * (groups.size() - 1))) / groups.size();

        for (int i = 0; i < groups.size(); i++) {
            ChoiceGroup group = groups.get(i);
            boolean selected = group.option().value() == selectedValue;
            int cardX = mainX + 2 + i * (cardW + gap);
            TextColor fill = selected ? PANEL_SOFT : PANEL;
            TextColor border = selected ? ACCENT : BORDER_ALT;
            drawPanel(g, cardX, cardY, cardW, mainH - 15, border, fill);
            drawText(g, cardX + 2, cardY + 1, "[" + group.option().value() + "]", selected ? ACCENT : SUBTLE, fill, true);
            drawText(g, cardX + 6, cardY + 1, trim(group.option().label(), cardW - 10), TEXT, fill, true);

            int row = cardY + 3;
            for (String detail : group.details()) {
                for (String wrapped : wrap(detail, cardW - 4)) {
                    if (row >= cardY + mainH - 18) {
                        break;
                    }
                    drawText(g, cardX + 2, row++, wrapped, selected ? TEXT : SUBTLE, fill, false);
                }
            }

            if (selected) {
                drawText(g, cardX + 2, cardY + mainH - 17, "> ready <", ACCENT, fill, true);
            }
        }

        if (footer != null && !footer.isBlank()) {
            drawPanel(g, mainX + 2, mainY + mainH - 4, mainW - 4, 3, BORDER_ALT, PANEL_SOFT);
            String footerText = inputValue.isBlank() ? footer : footer + " " + inputValue + "_";
            drawText(g, mainX + 4, mainY + mainH - 3, footerText, TEXT, PANEL_SOFT, false);
        }
    }

    private void renderCompactGeneric(
            TextGraphics g,
            int mainX,
            int mainY,
            int mainW,
            int mainH,
            List<String> introLines,
            String footer,
            List<ChoiceGroup> groups,
            int selectedValue,
            String inputValue,
            int scrollOffset
    ) {
        ChoiceGroup selectedGroup = findSelectedGroup(groups, selectedValue);
        int panelW = mainW - 4;
        boolean hasFooter = footer != null && !footer.isBlank();
        int footerTop = hasFooter ? mainY + mainH - 4 : mainY + mainH - 1;
        int maxChoiceH = Math.max(4, footerTop - (mainY + 8));
        int choiceH = Math.min(Math.max(6, groups.size() + 3), maxChoiceH);
        int commandY = Math.max(mainY + 8, footerTop - choiceH - 1);
        int infoH = Math.max(4, commandY - (mainY + 2) - 1);

        drawPanel(g, mainX + 2, mainY + 2, panelW, infoH, BORDER, PANEL_ALT);
        drawText(g, mainX + 4, mainY + 3, "Info", SUCCESS, PANEL_ALT, true);

        int textWidth = panelW - 4;
        int visibleRows = Math.max(1, infoH - 4);
        List<String> infoLines = buildInfoLines(introLines, selectedGroup);
        int totalRows = drawWrappedLines(g, infoLines, mainX + 4, mainY + 5, textWidth,
                visibleRows, scrollOffset, TEXT, PANEL_ALT);
        drawScrollStatus(g, mainX + 4, mainY + infoH, textWidth, scrollOffset, totalRows, visibleRows, PANEL_ALT);

        drawPanel(g, mainX + 2, commandY, panelW, choiceH, ACCENT, PANEL_SOFT);
        drawText(g, mainX + 4, commandY + 1, "Command", ACCENT, PANEL_SOFT, true);
        drawChoiceRows(g, groups, selectedValue, mainX + 4, commandY + 3, panelW - 4,
                Math.max(1, choiceH - 4), PANEL_SOFT);

        if (hasFooter) {
            drawPanel(g, mainX + 2, mainY + mainH - 4, panelW, 3, BORDER_ALT, PANEL_SOFT);
            String footerText = inputValue.isBlank() ? footer : footer + " " + inputValue + "_";
            drawText(g, mainX + 4, mainY + mainH - 3, footerText, TEXT, PANEL_SOFT, false);
        }
    }

    private void renderBattleScreen(
            TextGraphics g,
            TerminalSize size,
            List<String> body,
            String footer,
            List<ChoiceGroup> groups,
            int selectedValue,
            int scrollOffset
    ) {
        String playerLine = body.isEmpty() ? "" : body.get(0);
        String enemyLine = body.size() > 1 ? body.get(1) : "";
        String stageLine = body.size() > 3 ? body.get(3) : "";

        List<String> logLines = new ArrayList<>();
        boolean inLog = false;
        for (String line : body) {
            if (line.startsWith("Log tempur:")) {
                inLog = true;
                continue;
            }
            if (line.startsWith("Pilihan:")) {
                break;
            }
            if (inLog && !line.isBlank()) {
                logLines.add(line.trim());
            }
        }

        int width = size.getColumns();
        if (width < 92 || size.getRows() < 32) {
            renderCompactBattleScreen(g, size, playerLine, enemyLine, stageLine, logLines, footer, groups, selectedValue);
            return;
        }

        drawPanel(g, 3, 8, width - 6, 5, BORDER_ALT, PANEL_SOFT);
        drawText(g, 5, 9, "Enemy", ALERT, PANEL_SOFT, true);
        drawText(g, width - 38, 9, trim(stageLine, 32), SUBTLE, PANEL_SOFT, false);
        drawBattleBar(g, 5, 10, width - 16, enemyLine, ALERT, PANEL_SOFT);

        drawPanel(g, 3, 14, width - 6, 5, BORDER, PANEL_ALT);
        drawText(g, 5, 15, "Crew", SUCCESS, PANEL_ALT, true);
        drawBattleBar(g, 5, 16, width - 16, playerLine, SUCCESS, PANEL_ALT);

        drawPanel(g, 3, 20, width - 32, size.getRows() - 22, BORDER, PANEL);
        drawText(g, 5, 21, "Battle Log", SUCCESS, PANEL, true);
        int logRow = 23;
        int logWidth = width - 40;
        List<String> visibleLogs = logLines.size() > 9 ? logLines.subList(logLines.size() - 9, logLines.size()) : logLines;
        for (String line : visibleLogs) {
            for (String wrapped : wrap(line, logWidth)) {
                if (logRow >= size.getRows() - 3) {
                    break;
                }
                drawText(g, 5, logRow++, wrapped, TEXT, PANEL, false);
            }
        }

        drawPanel(g, width - 27, 20, 24, size.getRows() - 22, ACCENT, PANEL_SOFT);
        drawText(g, width - 25, 21, "Command", ACCENT, PANEL_SOFT, true);
        int optionRow = 23;
        int labelWidth = 18;
        for (ChoiceGroup group : groups == null ? Collections.<ChoiceGroup>emptyList() : groups) {
            if (optionRow >= size.getRows() - 4) {
                break;
            }
            boolean selected = group.option().value() == selectedValue;
            TextColor bg = selected ? ACCENT : PANEL_SOFT;
            TextColor fg = selected ? PANEL : TEXT;
            String label = (selected ? "> " : "  ") + group.option().label();
            drawText(g, width - 25, optionRow++, pad(label, labelWidth), fg, bg, selected);
        }

        ChoiceGroup selectedGroup = findSelectedGroup(groups, selectedValue);
        if (selectedGroup != null) {
            int focusY = Math.min(size.getRows() - 9, optionRow + 1);
            drawText(g, width - 25, focusY, "Focus", SUBTLE, PANEL_SOFT, true);
            drawText(g, width - 25, focusY + 1, trim(selectedGroup.option().label(), 18), TEXT, PANEL_SOFT, false);
            int detailRow = focusY + 3;
            for (String detail : selectedGroup.details()) {
                for (String wrapped : wrap(detail, 18)) {
                    if (detailRow >= size.getRows() - 3) {
                        break;
                    }
                    drawText(g, width - 25, detailRow++, wrapped, SUBTLE, PANEL_SOFT, false);
                }
            }
        }

        if (footer != null && !footer.isBlank()) {
            drawText(g, width - 25, size.getRows() - 3, footer, SUBTLE, PANEL_SOFT, false);
        }
    }

    private void renderCompactBattleScreen(
            TextGraphics g,
            TerminalSize size,
            String playerLine,
            String enemyLine,
            String stageLine,
            List<String> logLines,
            String footer,
            List<ChoiceGroup> groups,
            int selectedValue
    ) {
        int width = size.getColumns();
        int rows = size.getRows();

        drawPanel(g, 2, 7, width - 4, 4, BORDER_ALT, PANEL_SOFT);
        drawText(g, 4, 8, trim(stageLine, width - 10), SUBTLE, PANEL_SOFT, false);
        drawBattleBar(g, 4, 9, width - 10, enemyLine, ALERT, PANEL_SOFT);

        drawPanel(g, 2, 12, width - 4, 4, BORDER, PANEL_ALT);
        drawText(g, 4, 13, "Crew", SUCCESS, PANEL_ALT, true);
        drawBattleBar(g, 4, 14, width - 10, playerLine, SUCCESS, PANEL_ALT);

        int commandH = groups == null || groups.isEmpty() ? 4 : Math.min(8, Math.max(5, groups.size() + 3));
        int commandY = Math.max(17, rows - commandH - 1);
        int logH = Math.max(4, commandY - 17);

        drawPanel(g, 2, 17, width - 4, logH, BORDER, PANEL);
        drawText(g, 4, 18, "Battle Log", SUCCESS, PANEL, true);
        int logWidth = width - 10;
        int logRows = Math.max(1, logH - 3);
        List<String> wrappedLogs = wrapLines(logLines, logWidth);
        int start = Math.max(0, wrappedLogs.size() - logRows);
        for (int i = 0; i < logRows && start + i < wrappedLogs.size(); i++) {
            drawText(g, 4, 20 + i, wrappedLogs.get(start + i), TEXT, PANEL, false);
        }

        drawPanel(g, 2, commandY, width - 4, commandH, ACCENT, PANEL_SOFT);
        drawText(g, 4, commandY + 1, "Command", ACCENT, PANEL_SOFT, true);
        drawChoiceRows(g, groups, selectedValue, 4, commandY + 3, width - 8,
                Math.max(1, commandH - 4), PANEL_SOFT);

        if (footer != null && !footer.isBlank()) {
            drawText(g, 4, rows - 2, trim(footer, width - 8), SUBTLE, PANEL_SOFT, false);
        }
    }

    private void drawBattleBar(TextGraphics g, int x, int y, int width, String line, TextColor fillColor, TextColor bgColor) {
        HpStats stats = parseHpStats(line);
        String cleaned = line.replace("PLAYER :", "").replace("MUSUH  :", "").trim();
        String shipName = cleaned.contains("|") ? cleaned.substring(0, cleaned.indexOf('|')).trim() : cleaned;
        int safeWidth = Math.max(12, width);
        int nameWidth = Math.min(Math.max(8, safeWidth / 3), Math.max(8, safeWidth - 18));
        drawText(g, x, y, trim(shipName, nameWidth), TEXT, bgColor, false);

        int barX = x + nameWidth + 2;
        int barW = Math.max(4, safeWidth - nameWidth - 12);
        drawBar(g, barX, y, barW, stats.current(), stats.max(), hpColor(stats, fillColor), bgColor);
        drawText(g, barX + barW + 1, y, stats.current() + "/" + stats.max(), TEXT, bgColor, true);
    }

    private TextColor hpColor(HpStats stats, TextColor healthyColor) {
        double ratio = (double) Math.max(0, stats.current()) / Math.max(1, stats.max());
        if (ratio <= 0.30) {
            return ALERT;
        }
        if (ratio <= 0.60) {
            return ACCENT;
        }
        return healthyColor;
    }

    private void drawBar(TextGraphics g, int x, int y, int width, int current, int max, TextColor fillColor, TextColor bgColor) {
        int safeMax = Math.max(1, max);
        int safeWidth = Math.max(0, width);
        int filled = (int) Math.round((double) Math.max(0, current) / safeMax * safeWidth);
        filled = Math.max(0, Math.min(safeWidth, filled));
        for (int i = 0; i < safeWidth; i++) {
            boolean on = i < filled;
            drawText(g, x + i, y, " ", TEXT, on ? fillColor : PANEL, false);
        }
    }

    private void drawChoiceRows(
            TextGraphics g,
            List<ChoiceGroup> groups,
            int selectedValue,
            int x,
            int y,
            int width,
            int visibleRows,
            TextColor baseBg
    ) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        int labelWidth = Math.max(1, width);
        int selectedIndex = 0;
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).option().value() == selectedValue) {
                selectedIndex = i;
                break;
            }
        }
        int start = Math.max(0, selectedIndex - Math.max(0, visibleRows - 1));
        if (start + visibleRows > groups.size()) {
            start = Math.max(0, groups.size() - visibleRows);
        }
        for (int row = 0; row < visibleRows && start + row < groups.size(); row++) {
            ChoiceGroup group = groups.get(start + row);
            boolean selected = group.option().value() == selectedValue;
            TextColor bg = selected ? ACCENT : baseBg;
            TextColor fg = selected ? PANEL : TEXT;
            String label = (selected ? "> " : "  ") + group.option().label();
            drawText(g, x, y + row, pad(label, labelWidth), fg, bg, selected);
        }
    }

    private List<String> buildInfoLines(List<String> introLines, ChoiceGroup selectedGroup) {
        List<String> lines = new ArrayList<>(introLines);
        if (selectedGroup != null && hasPreviewDetails(selectedGroup)) {
            if (!lines.isEmpty()) {
                lines.add("");
            }
            lines.add("Selected: " + selectedGroup.option().label());
            lines.addAll(selectedGroup.details());
        }
        return lines;
    }

    private int drawWrappedLines(
            TextGraphics g,
            List<String> lines,
            int x,
            int y,
            int width,
            int visibleRows,
            int scrollOffset,
            TextColor fg,
            TextColor bg
    ) {
        List<String> wrapped = wrapLines(lines, width);
        int safeVisibleRows = Math.max(0, visibleRows);
        int start = Math.max(0, Math.min(scrollOffset, Math.max(0, wrapped.size() - safeVisibleRows)));
        for (int i = 0; i < safeVisibleRows && start + i < wrapped.size(); i++) {
            drawText(g, x, y + i, wrapped.get(start + i), fg, bg, false);
        }
        return wrapped.size();
    }

    private void drawScrollStatus(
            TextGraphics g,
            int x,
            int y,
            int width,
            int scrollOffset,
            int totalRows,
            int visibleRows,
            TextColor bg
    ) {
        if (totalRows <= visibleRows) {
            return;
        }
        int max = Math.max(0, totalRows - visibleRows);
        String status = String.format("Scroll %d/%d  PgUp/PgDn", Math.min(scrollOffset, max), max);
        drawText(g, x, y, trim(status, width), SUBTLE, bg, false);
    }

    private List<String> wrapLines(List<String> lines, int width) {
        List<String> wrapped = new ArrayList<>();
        for (String line : lines == null ? Collections.<String>emptyList() : lines) {
            wrapped.addAll(wrap(line, width));
        }
        return wrapped;
    }

    private int scrollStep(KeyStroke key) {
        return key.getKeyType() == KeyType.PageUp || key.getKeyType() == KeyType.PageDown ? 6 : 1;
    }

    private int clampScrollOffset(List<String> body, List<ChoiceGroup> groups, int selectedValue, int scrollOffset) {
        return Math.max(0, Math.min(scrollOffset, maxScrollOffset(body, groups, selectedValue)));
    }

    private int maxScrollOffset(List<String> body, List<ChoiceGroup> groups, int selectedValue) {
        if (screen == null) {
            return 0;
        }
        TerminalSize size = screen.getTerminalSize();
        int mainW = Math.max(20, size.getColumns() - 4);
        int mainH = Math.max(8, size.getRows() - 9);
        boolean hasChoices = groups != null && !groups.isEmpty();
        int textWidth = hasChoices && mainW >= 88 ? Math.max(8, Math.max(42, mainW - 32) - 4) : Math.max(8, mainW - 8);
        int visibleRows = hasChoices && mainW < 88
                ? Math.max(1, Math.max(5, mainH - Math.min(Math.max(6, groups.size() + 3), Math.max(5, mainH / 3)) - 5) - 4)
                : Math.max(1, mainH - 13);

        ChoiceGroup selectedGroup = findSelectedGroup(groups, selectedValue);
        List<String> infoLines = buildInfoLines(extractIntroLines(body), selectedGroup);
        int totalRows = wrapLines(infoLines, textWidth).size();
        return Math.max(0, totalRows - visibleRows);
    }

    private ChoiceGroup findSelectedGroup(List<ChoiceGroup> groups, int selectedValue) {
        if (groups == null) {
            return null;
        }
        for (ChoiceGroup group : groups) {
            if (group.option().value() == selectedValue) {
                return group;
            }
        }
        return groups.isEmpty() ? null : groups.get(0);
    }

    private boolean shouldUseCardLayout(List<ChoiceGroup> groups) {
        if (groups == null || groups.isEmpty() || groups.size() > 3) {
            return false;
        }
        for (ChoiceGroup group : groups) {
            if (group.details().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPreviewDetails(ChoiceGroup group) {
        return group != null && !group.details().isEmpty();
    }

    private List<String> extractIntroLines(List<String> body) {
        List<String> intro = new ArrayList<>();
        for (String line : body) {
            Matcher matcher = OPTION_PATTERN.matcher(line.trim());
            if (!matcher.matches()) {
                intro.add(line);
            } else {
                break;
            }
        }
        return intro;
    }

    private void drawPanel(TextGraphics g, int x, int y, int width, int height, TextColor border, TextColor fill) {
        if (x < 0) {
            width += x;
            x = 0;
        }
        if (y < 0) {
            height += y;
            y = 0;
        }
        width = Math.min(width, terminalColumns - x);
        height = Math.min(height, terminalRows - y);
        if (width <= 1 || height <= 1) {
            return;
        }
        g.setBackgroundColor(fill);
        g.setForegroundColor(border);
        g.fillRectangle(new TerminalPosition(x, y), new TerminalSize(width, height), ' ');
        String horizontal = "-".repeat(Math.max(0, width - 2));
        drawText(g, x, y, "+" + horizontal + "+", border, fill, true);
        for (int row = 1; row < height - 1; row++) {
            drawText(g, x, y + row, "|", border, fill, true);
            drawText(g, x + width - 1, y + row, "|", border, fill, true);
        }
        drawText(g, x, y + height - 1, "+" + horizontal + "+", border, fill, true);
    }

    private void drawText(TextGraphics g, int x, int y, String text, TextColor fg, TextColor bg, boolean bold) {
        if (text == null || y < 0 || y >= terminalRows || x >= terminalColumns) {
            return;
        }
        String visible = text;
        int drawX = x;
        if (drawX < 0) {
            int skipped = Math.min(visible.length(), -drawX);
            visible = visible.substring(skipped);
            drawX = 0;
        }
        int available = terminalColumns - drawX;
        if (available <= 0 || visible.isEmpty()) {
            return;
        }
        if (visible.length() > available) {
            visible = visible.substring(0, available);
        }

        g.setForegroundColor(fg);
        g.setBackgroundColor(bg);
        if (bold) {
            g.enableModifiers(SGR.BOLD);
        } else {
            g.disableModifiers(SGR.BOLD);
        }
        g.putString(drawX, y, visible);
    }

    private List<ChoiceGroup> extractChoiceGroups(List<String> lines, int min, int max) {
        List<ChoiceGroup> groups = new ArrayList<>();
        ChoiceGroupBuilder current = null;

        for (String line : lines) {
            Matcher matcher = OPTION_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                int value = Integer.parseInt(matcher.group(1));
                if (value >= min && value <= max) {
                    if (current != null) {
                        groups.add(current.build());
                    }
                    current = new ChoiceGroupBuilder(new OptionLine(value, matcher.group(2).trim()));
                    continue;
                }
            }

            if (current != null && !line.isBlank()) {
                current.addDetail(line.trim());
            }
        }

        if (current != null) {
            groups.add(current.build());
        }

        if (groups.isEmpty()) {
            for (int value = min; value <= max; value++) {
                groups.add(new ChoiceGroup(new OptionLine(value, "Option " + value), List.of()));
            }
        }

        return groups;
    }

    private List<String> normalize(List<String> body) {
        return body == null ? Collections.emptyList() : body;
    }

    private List<String> wrap(String raw, int width) {
        int safeWidth = Math.max(8, width);
        String text = raw == null ? "" : raw.trim();
        if (text.isBlank()) {
            return List.of("");
        }

        List<String> wrapped = new ArrayList<>();
        String[] paragraphs = text.split("\\R", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                wrapped.add("");
                continue;
            }

            String[] words = paragraph.trim().split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                while (word.length() > safeWidth) {
                    if (!line.isEmpty()) {
                        wrapped.add(line.toString());
                        line = new StringBuilder();
                    }
                    wrapped.add(word.substring(0, safeWidth));
                    word = word.substring(safeWidth);
                }
                if (line.isEmpty()) {
                    line.append(word);
                    continue;
                }

                if (line.length() + 1 + word.length() > safeWidth) {
                    wrapped.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line.append(' ').append(word);
                }
            }
            if (!line.isEmpty()) {
                wrapped.add(line.toString());
            }
        }
        return wrapped;
    }

    private String pad(String text, int width) {
        if (width <= 0) {
            return "";
        }
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        return text + " ".repeat(width - text.length());
    }

    private String trim(String text, int width) {
        if (text == null || width <= 0) {
            return "";
        }
        if (text.length() <= width) {
            return text;
        }
        if (width <= 3) {
            return text.substring(0, width);
        }
        return text.substring(0, width - 3) + "...";
    }

    private HpStats parseHpStats(String line) {
        Matcher matcher = HP_PATTERN.matcher(line);
        if (matcher.find()) {
            return new HpStats(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        }
        return new HpStats(1, 1);
    }

    private void drainInput() throws IOException {
        if (screen == null) {
            return;
        }
        while (screen.pollInput() != null) {
            // buang input lama
        }
    }

    private record OptionLine(int value, String label) { }

    private record ChoiceGroup(OptionLine option, List<String> details) { }

    private record HpStats(int current, int max) { }

    private static final class ChoiceGroupBuilder {
        private final OptionLine option;
        private final List<String> details = new ArrayList<>();

        private ChoiceGroupBuilder(OptionLine option) {
            this.option = option;
        }

        private void addDetail(String detail) {
            details.add(detail);
        }

        private ChoiceGroup build() {
            return new ChoiceGroup(option, List.copyOf(details));
        }
    }
}
