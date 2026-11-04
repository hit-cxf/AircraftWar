package edu.hitsz.ui;

import edu.hitsz.application.Difficulty;
import edu.hitsz.dao.ScoreDao;
import edu.hitsz.dao.ScoreRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** 使用组件内对话层和内存存储验证排行榜流程，不创建任何桌面窗口。 */
class RankingPanelTest {
    private static final LocalDateTime ENDED = LocalDateTime.of(2026, 11, 4, 16, 30);

    @BeforeAll
    static void theme() throws Exception {
        edt(GameTheme::install);
    }

    @Test
    void constructionDoesNotReadAndReloadSortsWithoutAskingForAName() throws Exception {
        MemoryDao dao = new MemoryDao();
        dao.records.add(new ScoreRecord("低分", 20, ENDED));
        dao.records.add(new ScoreRecord("较晚", 100, ENDED.plusMinutes(1)));
        dao.records.add(new ScoreRecord("较早", 100, ENDED.minusMinutes(1)));
        edt(() -> {
            RankingPanel panel = panel(dao);
            DialogHost host = host(panel);
            assertEquals(0, dao.reads);
            panel.reload();
            JTable table = components(panel, JTable.class).getFirst();
            assertEquals(1, dao.reads);
            assertEquals(3, table.getRowCount());
            assertEquals("较早", table.getValueAt(0, 1));
            assertEquals("较晚", table.getValueAt(1, 1));
            assertEquals("低分", table.getValueAt(2, 1));
            assertFalse(host.isDialogOpen());
        });
    }

    @Test
    void onShownOnlyReadsTheRankingAndNeverAutomaticallyAsksForAName() throws Exception {
        MemoryDao dao = new MemoryDao();
        dao.failRead = true;
        edt(() -> {
            RankingPanel panel = panel(dao);
            DialogHost host = host(panel);
            panel.onShown();
            assertEquals(1, dao.reads);
            assertTrue(host.isDialogOpen());
            assertTrue(components(host, JTextField.class).isEmpty());
            assertEquals(0, dao.saved);
            host.dismiss();
            dao.failRead = false;
            panel.onShown();
            assertEquals(2, dao.reads);
            assertEquals(0, dao.saved);
            assertFalse(host.isDialogOpen());
        });
    }

    @Test
    void cancellingInitialNamePromptClosesItBeforeShowingAndLoadingTheRanking() throws Exception {
        MemoryDao dao = new MemoryDao();
        AtomicReference<RankingPanel> page = new AtomicReference<>();
        AtomicReference<DialogHost> overlay = new AtomicReference<>();
        AtomicInteger shown = new AtomicInteger();
        edt(() -> {
            page.set(panel(dao));
            JPanel cards = new JPanel(new CardLayout());
            cards.add(new JPanel(), "game");
            cards.add(page.get(), "ranking");
            overlay.set(new DialogHost(cards));
            overlay.get().setSize(512, 768);
            overlay.get().doLayout();
            page.get().promptToSave(() -> {
                assertFalse(overlay.get().isDialogOpen());
                assertEquals(0, dao.reads);
                ((CardLayout) cards.getLayout()).show(cards, "ranking");
                shown.incrementAndGet();
                page.get().onShown();
            });
            assertFalse(page.get().isVisible());
            assertEquals(0, dao.reads);
            assertEquals(0, dao.saved);
            assertEquals(0, shown.get());
            overlay.get().dismiss();
        });
        flushEdt();
        edt(() -> {
            assertEquals(1, shown.get());
            assertTrue(page.get().isVisible());
            assertEquals(1, dao.reads);
            assertEquals(0, dao.saved);
            assertFalse(overlay.get().isDialogOpen());
        });
    }

    @Test
    void invalidNameAndFailedSaveKeepTheInputForRetryAndSuccessSavesOnlyOnce() throws Exception {
        MemoryDao dao = new MemoryDao();
        AtomicReference<RankingPanel> page = new AtomicReference<>();
        AtomicReference<DialogHost> overlay = new AtomicReference<>();
        AtomicInteger shown = new AtomicInteger();
        edt(() -> {
            page.set(panel(dao));
            overlay.set(host(page.get()));
            page.get().promptToSave(() -> {
                assertFalse(overlay.get().isDialogOpen());
                assertEquals(0, dao.reads);
                shown.incrementAndGet();
                page.get().onShown();
            });
            assertEquals(0, dao.reads);
            named(overlay.get(), JTextField.class, "dialog-input").setText("  ");
            named(overlay.get(), JButton.class, "dialog-submit").doClick(0);
        });
        flushEdt();
        edt(() -> {
            assertTrue(overlay.get().isDialogOpen());
            assertEquals("  ", named(overlay.get(), JTextField.class, "dialog-input").getText());
            assertEquals(0, dao.saved);
            assertEquals(0, dao.reads);
            assertEquals(0, shown.get());
            dao.failSave = true;
            named(overlay.get(), JTextField.class, "dialog-input").setText("  飞行员  ");
            named(overlay.get(), JButton.class, "dialog-submit").doClick(0);
        });
        flushEdt();
        edt(() -> {
            assertTrue(overlay.get().isDialogOpen());
            assertEquals("  飞行员  ", named(overlay.get(), JTextField.class, "dialog-input").getText());
            assertEquals(0, dao.saved);
            assertEquals(0, dao.reads);
            assertEquals(0, shown.get());
            dao.failSave = false;
            named(overlay.get(), JButton.class, "dialog-submit").doClick(0);
        });
        flushEdt();
        edt(() -> {
            assertEquals(1, dao.saved);
            assertEquals("飞行员", dao.records.getFirst().getPlayerName());
            assertEquals(120, dao.records.getFirst().getScore());
            assertEquals(ENDED, dao.records.getFirst().getRecordedAt());
            assertFalse(overlay.get().isDialogOpen());
            assertEquals(1, shown.get());
            assertEquals(1, dao.reads);
            assertEquals(1, components(page.get(), JTable.class).getFirst().getRowCount());
            page.get().promptToSave();
            assertFalse(overlay.get().isDialogOpen());
            assertEquals(1, dao.saved);
        });
    }

    @Test
    void rankingSaveButtonRefreshesOnlyAfterItsPromptHasClosed() throws Exception {
        MemoryDao dao = new MemoryDao();
        AtomicReference<RankingPanel> page = new AtomicReference<>();
        AtomicReference<DialogHost> overlay = new AtomicReference<>();
        edt(() -> {
            page.set(panel(dao));
            overlay.set(host(page.get()));
            page.get().onShown();
            assertEquals(1, dao.reads);
            page.get().promptToSave();
            assertTrue(overlay.get().isDialogOpen());
            assertEquals(1, dao.reads);
            overlay.get().dismiss();
        });
        flushEdt();
        edt(() -> {
            assertFalse(overlay.get().isDialogOpen());
            assertEquals(2, dao.reads);
            assertEquals(0, dao.saved);
        });
    }

    @Test
    void deletingRequiresASelectionAndConfirmationThenRefreshesTheRanking() throws Exception {
        MemoryDao dao = new MemoryDao();
        dao.records.add(new ScoreRecord("第一", 200, ENDED));
        dao.records.add(new ScoreRecord("第二", 100, ENDED));
        AtomicReference<RankingPanel> page = new AtomicReference<>();
        AtomicReference<DialogHost> overlay = new AtomicReference<>();
        edt(() -> {
            page.set(panel(dao));
            overlay.set(host(page.get()));
            page.get().reload();
            named(page.get(), JButton.class, "deleteRecord").doClick(0);
            assertTrue(overlay.get().isDialogOpen());
            assertEquals(0, dao.deleted);
            overlay.get().dismiss();
            components(page.get(), JTable.class).getFirst().setRowSelectionInterval(0, 0);
            named(page.get(), JButton.class, "deleteRecord").doClick(0);
            overlay.get().dismiss();
        });
        flushEdt();
        edt(() -> {
            assertEquals(0, dao.deleted);
            named(page.get(), JButton.class, "deleteRecord").doClick(0);
            named(overlay.get(), JButton.class, "dialog-submit").doClick(0);
        });
        flushEdt();
        edt(() -> {
            assertEquals(1, dao.deleted);
            assertEquals(1, dao.records.size());
            JTable table = components(page.get(), JTable.class).getFirst();
            assertEquals(1, table.getRowCount());
            assertEquals("第二", table.getValueAt(0, 1));
            assertFalse(overlay.get().isDialogOpen());
        });
    }

    @Test
    void refreshFailureAfterSuccessfulSaveDoesNotPermitADuplicateSave() throws Exception {
        MemoryDao dao = new MemoryDao();
        dao.failReadAfterSave = true;
        AtomicReference<RankingPanel> page = new AtomicReference<>();
        AtomicReference<DialogHost> overlay = new AtomicReference<>();
        edt(() -> {
            page.set(panel(dao));
            overlay.set(host(page.get()));
            page.get().promptToSave();
            named(overlay.get(), JTextField.class, "dialog-input").setText("飞行员");
            named(overlay.get(), JButton.class, "dialog-submit").doClick(0);
        });
        flushEdt();
        edt(() -> {
            assertEquals(1, dao.saved);
            assertTrue(overlay.get().isDialogOpen());
            assertTrue(components(overlay.get(), JTextField.class).isEmpty());
            page.get().promptToSave();
            assertTrue(components(overlay.get(), JTextField.class).isEmpty());
            assertEquals(1, dao.saved);
            overlay.get().dismiss();
            dao.failRead = false;
            page.get().reload();
            assertEquals(1, components(page.get(), JTable.class).getFirst().getRowCount());
        });
    }

    private static RankingPanel panel(MemoryDao dao) {
        return new RankingPanel(dao, Difficulty.NORMAL, 120, ENDED, () -> { });
    }

    private static DialogHost host(RankingPanel page) {
        DialogHost host = new DialogHost(page);
        host.setSize(512, 768);
        host.doLayout();
        return host;
    }

    private static void edt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    private static void flushEdt() throws Exception {
        edt(() -> { });
        edt(() -> { });
    }

    private static <T extends Component> T named(Container parent, Class<T> type, String name) {
        return components(parent, type).stream().filter(component -> name.equals(component.getName()))
                .findFirst().orElseThrow();
    }

    private static <T extends Component> List<T> components(Container parent, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) found.add(type.cast(child));
            if (child instanceof Container nested) found.addAll(components(nested, type));
        }
        return found;
    }

    private static final class MemoryDao implements ScoreDao {
        private final List<ScoreRecord> records = new ArrayList<>();
        private int reads;
        private int saved;
        private int deleted;
        private boolean failRead;
        private boolean failSave;
        private boolean failReadAfterSave;

        @Override public List<ScoreRecord> findAll() {
            reads++;
            if (failRead) throw new IllegalStateException("模拟读取失败");
            return new ArrayList<>(records);
        }
        @Override public void save(ScoreRecord record) {
            if (failSave) throw new IllegalStateException("模拟写入失败");
            records.add(record);
            saved++;
            if (failReadAfterSave) failRead = true;
        }
        @Override public boolean delete(ScoreRecord record) {
            deleted++;
            return records.remove(record);
        }
    }
}
