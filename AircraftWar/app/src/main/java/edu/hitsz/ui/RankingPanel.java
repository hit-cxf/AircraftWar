package edu.hitsz.ui;

import edu.hitsz.application.Difficulty;
import edu.hitsz.dao.RankingService;
import edu.hitsz.dao.ScoreDao;
import edu.hitsz.dao.ScoreRecord;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/** 标准模式的主窗口内排行榜；构造阶段不访问成绩存储。 */
public final class RankingPanel extends BackgroundPanel {
    private final RankingService service;
    private final int score;
    private final LocalDateTime endedAt;
    private final Runnable back;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JButton saveButton = GameTheme.button("保存本局成绩", true);
    private final JPanel rankingCards = GameTheme.transparent(new CardLayout());
    private final JLabel recordCount = GameTheme.label("尚未加载", 12, GameTheme.MUTED);
    private List<ScoreRecord> records = List.of();
    private boolean saved;
    private boolean resolved;

    public RankingPanel(ScoreDao dao, Difficulty difficulty, int score,
                        LocalDateTime endedAt, Runnable back) {
        this.service = new RankingService(Objects.requireNonNull(dao));
        this.score = score;
        this.endedAt = Objects.requireNonNull(endedAt);
        this.back = Objects.requireNonNull(back);
        Objects.requireNonNull(difficulty);
        setPreferredSize(new Dimension(512, 768));
        setLayout(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(24, 22, 24, 22));

        JPanel header = GameTheme.transparent(null);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(GameTheme.label("FLIGHT RECORDS", 12, GameTheme.GOLD));
        header.add(Box.createVerticalStrut(9));
        header.add(GameTheme.title("本次飞行结束", 28));
        header.add(Box.createVerticalStrut(15));
        JPanel points = GameTheme.card();
        points.setLayout(new BorderLayout(20, 0));
        points.setMaximumSize(new Dimension(Integer.MAX_VALUE, 106));
        points.setPreferredSize(new Dimension(460, 106));
        JPanel total = GameTheme.transparent(new BorderLayout(0, 2));
        total.add(GameTheme.label("本局得分", 12, GameTheme.MUTED), BorderLayout.NORTH);
        JLabel value = GameTheme.title(Integer.toString(score), 42);
        value.setForeground(GameTheme.GOLD);
        total.add(value, BorderLayout.CENTER);
        points.add(total, BorderLayout.WEST);
        points.add(GameTheme.label("<html><div style='text-align:right'><b>"
                + difficulty.displayName() + "模式</b><br>挑战下一次纪录</div></html>", 14, GameTheme.TEXT),
                BorderLayout.EAST);
        header.add(points);
        add(header, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"名次", "玩家名", "得分", "时间"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        table.setName("rankingTable");
        styleTable();
        JScrollPane scroll = GameTheme.scroll(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(GameTheme.SURFACE);
        scroll.getColumnHeader().setBackground(GameTheme.SURFACE);
        rankingCards.add(scroll, "table");
        JPanel empty = GameTheme.transparent(new GridBagLayout());
        empty.add(GameTheme.label("<html><div style='text-align:center'><b>天空还没有留下纪录</b>"
                + "<br><br>保存本局成绩，留下你的飞行纪录。</div></html>", 14, GameTheme.MUTED));
        rankingCards.add(empty, "empty");
        JPanel ranking = GameTheme.card();
        ranking.setLayout(new BorderLayout(0, 12));
        JPanel rankingHeader = GameTheme.transparent(new BorderLayout());
        rankingHeader.add(GameTheme.title("得分排行榜", 17), BorderLayout.WEST);
        rankingHeader.add(recordCount, BorderLayout.EAST);
        ranking.add(rankingHeader, BorderLayout.NORTH);
        ranking.add(rankingCards, BorderLayout.CENTER);
        add(ranking, BorderLayout.CENTER);

        JPanel actions = GameTheme.transparent(new BorderLayout(0, 10));
        saveButton.setPreferredSize(new Dimension(460, 46));
        saveButton.addActionListener(e -> promptToSave());
        actions.add(saveButton, BorderLayout.NORTH);
        JPanel secondary = GameTheme.transparent(new GridLayout(1, 2, 10, 0));
        JButton delete = GameTheme.button("删除选中记录", false);
        delete.setName("deleteRecord");
        delete.addActionListener(e -> deleteSelected());
        JButton menu = GameTheme.button("返回难度选择", false);
        menu.addActionListener(e -> returnToMenu());
        secondary.add(delete);
        secondary.add(menu);
        actions.add(secondary, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    private void styleTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(50);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(GameTheme.SURFACE);
        table.setForeground(GameTheme.TEXT);
        table.setSelectionBackground(new Color(35, 85, 95));
        table.setSelectionForeground(GameTheme.TEXT);
        table.setFont(GameTheme.font(13, Font.PLAIN));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));
        DefaultTableCellRenderer heading = new DefaultTableCellRenderer();
        heading.setOpaque(true);
        heading.setBackground(GameTheme.BACKGROUND);
        heading.setForeground(GameTheme.MUTED);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        heading.setFont(GameTheme.font(12, Font.BOLD));
        heading.setBorder(new EmptyBorder(7, 4, 7, 4));
        table.getTableHeader().setDefaultRenderer(heading);
        int[] widths = {40, 124, 66, 132};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setMinWidth(i == 3 ? 104 : 36);
        }
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                if (!selected) {
                    setBackground(row % 2 == 0 ? GameTheme.SURFACE : new Color(21, 47, 63));
                    setForeground(column == 0 && row < 3 ? GameTheme.GOLD : GameTheme.TEXT);
                }
                setFont(GameTheme.font(column == 3 ? 11 : 13, column == 2 ? Font.BOLD : Font.PLAIN));
                setHorizontalAlignment(column == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 5, 0, 5));
                if (value instanceof LocalDateTime time) {
                    setText("<html><div style='text-align:center'>"
                            + time.format(DateTimeFormatter.ISO_LOCAL_DATE) + "<br>"
                            + time.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "</div></html>");
                    setToolTipText(time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                } else {
                    setToolTipText(value == null ? null : value.toString());
                }
                return this;
            }
        });
    }

    /** 手动刷新不主动打开姓名输入框。 */
    public void reload() {
        refreshRanking();
    }

    /** 姓名输入层关闭后进入页面，仅刷新排行，不再自动询问姓名。 */
    public void onShown() {
        if (!resolved) reload();
    }

    public void promptToSave() {
        promptToSave(this::reload);
    }

    /** 可以在本页尚未显示时覆盖游戏画面，关闭输入层后再由路由显示本页。 */
    public void promptToSave(Runnable afterClosed) {
        Objects.requireNonNull(afterClosed);
        if (saved || resolved) return;
        DialogHost.find(this).prompt("记录本局成绩",
                "本局得分：" + score + "。请输入姓名保存成绩；取消则不保存。",
                "", this::submitScore, afterClosed);
    }

    /** 返回错误说明时输入层保留原文本；成功返回 null。 */
    String submitScore(String name) {
        if (saved || resolved) return null;
        try {
            service.save(name, score, endedAt);
            saved = true;
            saveButton.setEnabled(false);
            saveButton.setText("本局成绩已保存");
            return null;
        } catch (IllegalArgumentException error) {
            return error.getMessage();
        } catch (RuntimeException error) {
            return "保存失败：" + errorText(error) + "。请检查后重试，姓名已保留。";
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            DialogHost.find(this).message("请选择记录", "请先选择一条成绩记录。");
            return;
        }
        ScoreRecord record = records.get(table.convertRowIndexToModel(row));
        DialogHost.find(this).confirm("确认删除",
                "删除 " + record.getPlayerName() + " 的 " + record.getScore() + " 分记录？", confirmed -> {
                    if (!confirmed || resolved) return;
                    try {
                        service.delete(record);
                        refreshRanking();
                    } catch (RuntimeException error) {
                        showError("删除失败", error);
                    }
                });
    }

    private boolean refreshRanking() {
        try {
            List<ScoreRecord> latest = service.ranking();
            records = latest;
            tableModel.setRowCount(0);
            for (int i = 0; i < records.size(); i++) {
                ScoreRecord record = records.get(i);
                tableModel.addRow(new Object[]{i + 1, record.getPlayerName(), record.getScore(), record.getRecordedAt()});
            }
            recordCount.setText(records.isEmpty() ? "暂无纪录" : "共 " + records.size() + " 条纪录");
            ((CardLayout) rankingCards.getLayout()).show(rankingCards, records.isEmpty() ? "empty" : "table");
            return true;
        } catch (RuntimeException error) {
            showError("读取排行榜失败", error);
            return false;
        }
    }

    private void returnToMenu() {
        if (resolved) return;
        resolved = true;
        back.run();
    }

    private void showError(String title, RuntimeException error) {
        DialogHost.find(this).message(title, errorText(error));
    }

    private static String errorText(RuntimeException error) {
        return error.getMessage() == null ? "发生未知错误" : error.getMessage();
    }
}
