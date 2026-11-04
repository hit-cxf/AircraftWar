package edu.hitsz.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/** 主窗口内部的非阻塞对话层；业务回调由 EDT 异步执行，不创建额外窗口。 */
public final class DialogHost extends JLayeredPane {
    private final JComponent content;
    private final JPanel mask;
    private volatile Session session;

    public DialogHost(JComponent content) {
        this.content = Objects.requireNonNull(content);
        setLayout(null);
        setOpaque(true);
        setBackground(GameTheme.BACKGROUND);
        setPreferredSize(content.getPreferredSize());
        add(content, DEFAULT_LAYER);
        mask = new JPanel(null) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setColor(new Color(2, 10, 20, 195));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        mask.setName("dialog-mask");
        mask.setOpaque(false);
        mask.setVisible(false);
        mask.setFocusCycleRoot(true);
        mask.setFocusTraversalPolicy(new DialogFocusPolicy());
        MouseAdapter intercept = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { e.consume(); }
            @Override public void mouseReleased(MouseEvent e) { e.consume(); }
            @Override public void mouseClicked(MouseEvent e) { e.consume(); }
            @Override public void mouseMoved(MouseEvent e) { e.consume(); }
            @Override public void mouseDragged(MouseEvent e) { e.consume(); }
            @Override public void mouseWheelMoved(MouseWheelEvent e) { e.consume(); }
        };
        mask.addMouseListener(intercept);
        mask.addMouseMotionListener(intercept);
        mask.addMouseWheelListener(intercept);
        bind(KeyEvent.VK_ESCAPE, "dialog-cancel", () -> dismiss());
        bind(KeyEvent.VK_ENTER, "dialog-submit", () -> {
            Session current = session;
            if (current != null) current.accept();
        });
        add(mask, MODAL_LAYER);
    }

    public static DialogHost find(Component owner) {
        for (Component component = owner; component != null; component = component.getParent()) {
            if (component instanceof DialogHost host) return host;
        }
        throw new IllegalStateException("未找到 DialogHost：请将页面放在主窗口的 DialogHost 内，再显示对话框。");
    }

    public void message(String title, String text) {
        onEdt(() -> show(new Session(title, text, null, null, null, null)));
    }

    public void confirm(String title, String text, Consumer<Boolean> answer) {
        Objects.requireNonNull(answer, "确认回调不能为空");
        onEdt(() -> show(new Session(title, text, answer, null, null, null)));
    }

    /** submit 返回 null 表示成功；返回错误文字则保留输入并允许再次提交。 */
    public void prompt(String title, String text, String initial, Function<String, String> submit) {
        prompt(title, text, initial, submit, null);
    }

    /** 成功或取消关闭后，在 EDT 异步执行一次 afterClosed；校验错误不会执行。 */
    public void prompt(String title, String text, String initial, Function<String, String> submit,
            Runnable afterClosed) {
        Objects.requireNonNull(submit, "输入提交回调不能为空");
        onEdt(() -> show(new Session(title, text, null, initial == null ? "" : initial, submit, afterClosed)));
    }

    public boolean isDialogOpen() { return session != null; }

    /** 与取消按钮相同；确认框回调一次 false，输入框取消不执行提交。 */
    public void dismiss() {
        onEdt(() -> {
            Session current = session;
            if (current != null) current.cancel();
        });
    }

    @Override public void doLayout() {
        content.setBounds(0, 0, getWidth(), getHeight());
        mask.setBounds(0, 0, getWidth(), getHeight());
        Session current = session;
        if (current != null) current.layoutCard();
    }

    private void bind(int key, String name, Runnable action) {
        KeyStroke stroke = KeyStroke.getKeyStroke(key, 0);
        mask.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, name);
        mask.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(stroke, name);
        mask.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { action.run(); }
        });
    }

    private void show(Session next) {
        Session previous = session;
        Component focused = previous == null
                ? KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
                : previous.previousFocus;
        if (previous != null) previous.cancel();
        next.previousFocus = focused != null && SwingUtilities.isDescendingFrom(focused, this)
                ? focused : null;
        next.root = SwingUtilities.getRootPane(this);
        if (next.root != null) {
            next.previousDefault = next.root.getDefaultButton();
            next.root.setDefaultButton(next.primary);
        }
        session = next;
        mask.add(next.card);
        mask.setVisible(true);
        revalidate();
        doLayout();
        repaint();
        SwingUtilities.invokeLater(() -> {
            if (session == next) next.focusFirst();
        });
    }

    /** 只关闭发起操作的会话，避免业务回调打开的新对话框被旧操作关闭。 */
    private boolean close(Session closing) {
        if (session != closing || closing.closed) return false;
        closing.closed = true;
        session = null;
        mask.remove(closing.card);
        mask.setVisible(false);
        if (closing.root != null) closing.root.setDefaultButton(closing.previousDefault);
        revalidate();
        repaint();
        if (closing.previousFocus != null && closing.previousFocus.isShowing()
                && closing.previousFocus.isEnabled()) {
            closing.previousFocus.requestFocusInWindow();
        }
        if (closing.afterClosed != null) SwingUtilities.invokeLater(closing.afterClosed);
        return true;
    }

    private static void onEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) action.run();
        else SwingUtilities.invokeLater(action);
    }

    private static JTextArea textArea(String text, Color color) {
        JTextArea area = new JTextArea(text == null ? "" : text);
        area.setFont(GameTheme.font(14, Font.PLAIN));
        area.setForeground(color);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setAlignmentX(LEFT_ALIGNMENT);
        area.setBorder(null);
        return area;
    }

    private static void sizeText(JTextArea area, int width) {
        area.setPreferredSize(null);
        area.setSize(Math.max(1, width), Short.MAX_VALUE);
        int height = Math.max(area.getFontMetrics(area.getFont()).getHeight(), area.getPreferredSize().height);
        area.setPreferredSize(new Dimension(Math.max(1, width), height));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    private final class Session {
        private final JPanel card = GameTheme.card();
        private final JPanel body = GameTheme.transparent(null);
        private final JTextArea description;
        private final JTextArea error = textArea("", new Color(255, 165, 141));
        private final JTextField input;
        private final JButton primary;
        private final JButton cancel;
        private final Consumer<Boolean> answer;
        private final Function<String, String> submit;
        private final Runnable afterClosed;
        private Component previousFocus;
        private JRootPane root;
        private JButton previousDefault;
        private boolean busy;
        private boolean closed;

        private Session(String title, String text, Consumer<Boolean> answer, String initial,
                Function<String, String> submit, Runnable afterClosed) {
            this.answer = answer;
            this.submit = submit;
            this.afterClosed = afterClosed;
            card.setName("dialog-card");
            card.setLayout(new BorderLayout(0, 16));
            JLabel heading = GameTheme.title(title == null ? "提示" : title, 22);
            heading.setHorizontalAlignment(SwingConstants.LEFT);
            card.add(heading, BorderLayout.NORTH);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            description = textArea(text, GameTheme.MUTED);
            body.add(description);
            if (submit != null) {
                input = new JTextField(initial);
                input.setName("dialog-input");
                input.setFont(GameTheme.font(16, Font.PLAIN));
                input.setForeground(GameTheme.TEXT);
                input.setBackground(GameTheme.BACKGROUND);
                input.setCaretColor(GameTheme.ACCENT);
                input.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(GameTheme.LINE), new EmptyBorder(9, 10, 9, 10)));
                input.setAlignmentX(LEFT_ALIGNMENT);
                input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
                input.addActionListener(event -> accept());
                body.add(Box.createVerticalStrut(14));
                body.add(input);
            } else input = null;
            error.setName("dialog-error");
            error.setBorder(new EmptyBorder(10, 0, 0, 0));
            error.setVisible(false);
            body.add(error);
            JScrollPane scroll = GameTheme.scroll(body);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            card.add(scroll, BorderLayout.CENTER);
            primary = GameTheme.button(submit != null ? "保存" : answer != null ? "确认" : "知道了", true);
            primary.setName("dialog-submit");
            primary.setFont(GameTheme.font(15, Font.BOLD));
            primary.addActionListener(event -> accept());
            JPanel actions = GameTheme.transparent(new GridLayout(1, answer != null || submit != null ? 2 : 1, 10, 0));
            if (answer != null || submit != null) {
                cancel = GameTheme.button("取消", false);
                cancel.setName("dialog-cancel");
                cancel.addActionListener(event -> cancel());
                actions.add(cancel);
            } else cancel = null;
            actions.add(primary);
            card.add(actions, BorderLayout.SOUTH);
        }

        private void accept() {
            if (session != this || closed || busy) return;
            if (submit == null) {
                if (close(this) && answer != null) SwingUtilities.invokeLater(() -> answer.accept(true));
                return;
            }
            busy = true;
            primary.setEnabled(false);
            String entered = input.getText();
            SwingUtilities.invokeLater(() -> {
                if (session != this || closed) return;
                String failure;
                try {
                    failure = submit.apply(entered);
                } catch (RuntimeException problem) {
                    failure = problem.getMessage();
                    if (failure == null || failure.isBlank()) failure = "操作失败，请重试。";
                }
                if (session != this || closed) return;
                if (failure == null) {
                    close(this);
                } else {
                    busy = false;
                    primary.setEnabled(true);
                    error.setText(failure);
                    error.setVisible(true);
                    layoutCard();
                    card.revalidate();
                    card.repaint();
                    input.requestFocusInWindow();
                }
            });
        }

        private void cancel() {
            if (close(this) && answer != null) SwingUtilities.invokeLater(() -> answer.accept(false));
        }

        private void focusFirst() {
            if (input != null) {
                input.requestFocusInWindow();
                input.selectAll();
            } else primary.requestFocusInWindow();
        }

        private void layoutCard() {
            int width = Math.max(0, Math.min(430, getWidth() - 32));
            int innerWidth = Math.max(1, width - 48);
            sizeText(description, innerWidth);
            sizeText(error, innerWidth);
            int height = Math.min(Math.max(0, getHeight() - 32), card.getPreferredSize().height);
            card.setBounds((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
            card.doLayout();
        }
    }

    private final class DialogFocusPolicy extends FocusTraversalPolicy {
        private List<Component> choices() {
            List<Component> choices = new ArrayList<>();
            Session current = session;
            if (current != null) {
                if (current.input != null && current.input.isEnabled()) choices.add(current.input);
                if (current.cancel != null && current.cancel.isEnabled()) choices.add(current.cancel);
                if (current.primary.isEnabled()) choices.add(current.primary);
            }
            return choices;
        }

        private Component adjacent(Component component, int direction) {
            List<Component> choices = choices();
            if (choices.isEmpty()) return mask;
            int index = choices.indexOf(component);
            return choices.get(index < 0 ? 0 : Math.floorMod(index + direction, choices.size()));
        }

        @Override public Component getComponentAfter(Container root, Component component) { return adjacent(component, 1); }
        @Override public Component getComponentBefore(Container root, Component component) { return adjacent(component, -1); }
        @Override public Component getFirstComponent(Container root) { return adjacent(null, 1); }
        @Override public Component getLastComponent(Container root) {
            List<Component> choices = choices();
            return choices.isEmpty() ? mask : choices.get(choices.size() - 1);
        }
        @Override public Component getDefaultComponent(Container root) { return getFirstComponent(root); }
    }
}
