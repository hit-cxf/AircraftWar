package edu.hitsz.ui;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DialogHostTest {
    @Test
    void promptRetainsInvalidInputThenSubmitsSuccessOnlyOnce() throws Exception {
        AtomicReference<DialogHost> reference = new AtomicReference<>();
        AtomicInteger submissions = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        edt(() -> {
            DialogHost host = host();
            reference.set(host);
            host.prompt("记录成绩", "输入玩家姓名。", "A", name -> {
                submissions.incrementAndGet();
                return name.length() < 2 ? "请输入至少两个字符。" : null;
            }, completions::incrementAndGet);
            button(host, "dialog-submit").doClick(0);
            button(host, "dialog-submit").doClick(0);
            assertEquals(0, submissions.get(), "提交回调异步执行");
        });
        edt(() -> {
            DialogHost host = reference.get();
            assertTrue(host.isDialogOpen());
            assertEquals(1, submissions.get());
            assertEquals(0, completions.get(), "校验错误不得触发关闭回调");
            JTextField input = named(host, "dialog-input", JTextField.class);
            assertEquals("A", input.getText());
            assertTrue(named(host, "dialog-error", JTextArea.class).getText().contains("两个字符"));
            input.setText("Alice");
            input.postActionEvent();
            input.postActionEvent();
        });
        edt(() -> { }); // submit 的异步任务关闭后，再等待 afterClosed 的异步任务。
        edt(() -> {
            assertFalse(reference.get().isDialogOpen());
            assertEquals(2, submissions.get(), "一次失败重试和一次成功；连点不能重复提交");
            assertEquals(1, completions.get(), "成功关闭仅完成一次");
            reference.get().dismiss();
            reference.get().dismiss();
        });
        edt(() -> assertEquals(1, completions.get(), "已关闭后重复 dismiss 不得再次完成"));
    }

    @Test
    void promptCancelAndExplicitDismissCompleteOnceWithoutSubmitting() throws Exception {
        AtomicReference<DialogHost> reference = new AtomicReference<>();
        AtomicInteger submissions = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        edt(() -> {
            DialogHost host = host();
            reference.set(host);
            host.prompt("记录成绩", "输入玩家姓名。", "Player", text -> {
                submissions.incrementAndGet();
                return null;
            }, () -> {
                assertFalse(host.isDialogOpen());
                completions.incrementAndGet();
            });
            JButton cancel = button(host, "dialog-cancel");
            cancel.doClick(0);
            cancel.doClick(0);
            host.dismiss();
            assertEquals(0, completions.get(), "关闭回调异步执行");
        });
        edt(() -> {
            assertEquals(1, completions.get());
            DialogHost host = reference.get();
            host.prompt("记录成绩", "输入玩家姓名。", "Player", text -> {
                submissions.incrementAndGet();
                return null;
            }, completions::incrementAndGet);
            button(host, "dialog-submit").doClick(0);
            host.dismiss(); // 取消尚未执行的异步提交，也只能完成一次。
            host.dismiss();
        });
        edt(() -> {
            assertEquals(2, completions.get(), "每个取消会话只完成一次");
            assertEquals(0, submissions.get());
            assertFalse(reference.get().isDialogOpen());
        });
    }

    @Test
    void confirmDismissAndAcceptEachCallAnswerOnceAfterClosingMask() throws Exception {
        AtomicReference<DialogHost> reference = new AtomicReference<>();
        List<Boolean> answers = new ArrayList<>();
        edt(() -> {
            DialogHost host = host();
            reference.set(host);
            host.confirm("删除", "确定删除？", accepted -> {
                assertFalse(host.isDialogOpen(), "回调前先关闭当前对话层");
                answers.add(accepted);
            });
            host.dismiss();
            host.dismiss();
            assertTrue(answers.isEmpty());
        });
        edt(() -> {
            DialogHost host = reference.get();
            assertEquals(List.of(false), answers);
            host.confirm("删除", "确定删除？", accepted -> {
                assertFalse(host.isDialogOpen());
                answers.add(accepted);
                host.message("完成", "已删除。");
            });
            JButton accept = button(host, "dialog-submit");
            accept.doClick(0);
            accept.doClick(0);
        });
        edt(() -> {
            assertEquals(List.of(false, true), answers);
            assertTrue(reference.get().isDialogOpen(), "旧确认框不能关闭回调新打开的消息框");
            reference.get().dismiss();
        });
    }

    @Test
    void maskResizesWithContentAndRestoresDefaultButtonOnClose() throws Exception {
        edt(() -> {
            JPanel content = new JPanel();
            JButton original = new JButton("原页面按钮");
            content.add(original);
            DialogHost host = new DialogHost(content);
            JRootPane root = new JRootPane();
            root.setContentPane(host);
            root.setDefaultButton(original);
            host.setSize(512, 768);
            host.message("提示", "显示在原窗口内，保持页面可见。\n第二行提示文字。");
            host.doLayout();
            JPanel mask = named(host, "dialog-mask", JPanel.class);
            JPanel card = named(host, "dialog-card", JPanel.class);
            assertTrue(mask.isVisible());
            assertEquals(new Rectangle(0, 0, 512, 768), mask.getBounds());
            assertEquals(new Rectangle(0, 0, 512, 768), content.getBounds());
            assertTrue(card.getWidth() <= 430);
            assertEquals((host.getWidth() - card.getWidth()) / 2, card.getX());
            assertNotSame(original, root.getDefaultButton());
            host.setSize(320, 480);
            host.doLayout();
            assertEquals(new Rectangle(0, 0, 320, 480), mask.getBounds());
            assertTrue(card.getWidth() <= 288);
            assertTrue(card.getHeight() <= 448);
            assertTrue(card.getHeight() > 100);
            assertTrue(card.getX() >= 0 && card.getY() >= 0);
            host.dismiss();
            assertFalse(mask.isVisible());
            assertSame(original, root.getDefaultButton());
            assertSame(host, DialogHost.find(original));
            assertThrows(IllegalStateException.class, () -> DialogHost.find(new JPanel()));
        });
    }

    @Test
    void enterSubmitsEscapeCancelsAndFocusTraversalStaysWithinDialog() throws Exception {
        AtomicReference<DialogHost> reference = new AtomicReference<>();
        AtomicInteger submissions = new AtomicInteger();
        edt(() -> {
            DialogHost host = host();
            reference.set(host);
            host.prompt("输入", "Esc 可以取消。", "Player", text -> {
                submissions.incrementAndGet();
                return null;
            });
            JPanel mask = named(host, "dialog-mask", JPanel.class);
            FocusTraversalPolicy policy = mask.getFocusTraversalPolicy();
            Component input = named(host, "dialog-input", JTextField.class);
            Component next = policy.getComponentAfter(mask, input);
            assertSame(button(host, "dialog-cancel"), next);
            Component primary = policy.getComponentAfter(mask, next);
            assertSame(input, policy.getComponentAfter(mask, primary));
            mask.getActionMap().get("dialog-cancel").actionPerformed(new ActionEvent(mask, 0, "escape"));
            assertFalse(host.isDialogOpen());
            assertEquals(0, submissions.get());
            host.prompt("输入", "Enter 可以保存。", "Player", text -> {
                submissions.incrementAndGet();
                return null;
            });
            mask.getActionMap().get("dialog-submit").actionPerformed(new ActionEvent(mask, 0, "enter"));
            mask.getActionMap().get("dialog-submit").actionPerformed(new ActionEvent(mask, 0, "enter"));
        });
        edt(() -> {
            assertEquals(1, submissions.get());
            assertFalse(reference.get().isDialogOpen());
        });
    }

    private static DialogHost host() {
        DialogHost host = new DialogHost(new JPanel());
        host.setSize(512, 768);
        return host;
    }

    private static JButton button(Container root, String name) { return named(root, name, JButton.class); }

    private static <T extends Component> T named(Container root, String name, Class<T> type) {
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName()) && type.isInstance(component)) return type.cast(component);
            if (component instanceof Container child) {
                T found = findNamed(child, name, type);
                if (found != null) return found;
            }
        }
        throw new AssertionError("找不到控件 " + name);
    }

    private static <T extends Component> T findNamed(Container root, String name, Class<T> type) {
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName()) && type.isInstance(component)) return type.cast(component);
            if (component instanceof Container child) {
                T found = findNamed(child, name, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void edt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }
}
