package eu.amadeos.games.cruise.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.swing.FullScreen;
import eu.amadeos.gameengine.swing.GameCanvas;
import eu.amadeos.gameengine.swing.GameMenus;
import eu.amadeos.gameengine.swing.GameTexts;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.cruise.bootstrap.AmadeCruiseApplication;
import eu.amadeos.games.cruise.ui.game.CruiseGame;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;

/**
 * The game window: a menu and the road, a {@link GameCanvas} the game loop draws on. The window can be
 * resized or switched to full screen with F11; on a wide screen the picture grows wider instead of showing
 * black bars.
 */
public final class CruiseWindow extends JFrame implements CruiseView {
    /** The widest logical picture: 16:9 at a height of 400. */
    public static final int WIDEST = 712;

    private final Messages msg;
    private final GameMenus menus = new GameMenus(GameTexts.load());
    private final GameCanvas canvas;
    private final FullScreen fullScreen;
    private final JCheckBoxMenuItem sound;
    private JMenu graphics;
    private CruisePresenter presenter;

    @Inject
    public CruiseWindow(Messages msg, CruiseGame game, Keyboard keyboard) {
        super(msg.get("window.title"));
        EdtExecutor.requireEdt();
        this.msg = msg;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        java.net.URL icon = getClass().getResource("/icons/amade-cruise-game.png");
        if (icon != null) setIconImage(new ImageIcon(icon).getImage());

        // The canvas is a heavyweight AWT component: lightweight menus would open behind it.
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        canvas = new GameCanvas(CruiseGame.WIDTH, CruiseGame.HEIGHT, game::render, game::renderOverlay);
        canvas.setMaxLogicalWidth(WIDEST);
        canvas.setPreferredSize(startSize());
        keyboard.attach(canvas);
        getContentPane().add(canvas, BorderLayout.CENTER);
        fullScreen = new FullScreen(this, canvas);
        sound = Mnemonics.apply(new JCheckBoxMenuItem(), msg.get("menu.game.sound"));
        sound.setName("game-sound");
        sound.addActionListener(e -> {
            presenter.setSound(sound.isSelected());
            canvas.requestFocusInWindow();
        });
        setJMenuBar(menuBar());
        fullScreen.whenDeactivated(() -> {
            if (presenter != null) presenter.focusLost();
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                canvas.requestFocusInWindow();
            }
        });
        pack();
    }

    /** Twice the logical size where the screen has room for it. */
    private static Dimension startSize() {
        int w = CruiseGame.WIDTH, h = CruiseGame.HEIGHT;
        if (GraphicsEnvironment.isHeadless()) return new Dimension(w, h);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        double scale = Math.max(1, Math.min(2, Math.min((screen.width - 40) / (double) w, (screen.height - 100) / (double) h)));
        return new Dimension((int) (w * scale), (int) (h * scale));
    }

    /** Connects the window with the presenter; call once before {@link CruisePresenter#start()}. */
    public void bind(CruisePresenter presenter) {
        this.presenter = presenter;
    }

    public GameCanvas canvas() {
        return canvas;
    }

    public FullScreen fullScreen() {
        return fullScreen;
    }

    /** Runs the action when the window closes, but not when it only switches to or from full screen. */
    public void whenClosed(Runnable action) {
        fullScreen.whenClosed(action);
    }

    private JMenuBar menuBar() {
        JMenu game = menus.game();
        game.add(focusing(item("menu.game.journey", "game-journey", () -> presenter.newJourney(),
                KeyStroke.getKeyStroke("F2"))));
        game.add(focusing(item("menu.game.race", "game-race", () -> presenter.race(), null)));
        game.add(focusing(item("menu.game.garage", "game-garage", () -> presenter.garage(), null)));
        game.add(focusing(item("menu.game.title", "game-title", () -> presenter.title(), null)));
        // No shortcut: Esc is the game's own pause key.
        game.add(focusing(item("menu.game.pause", "game-pause", () -> presenter.pause(), null)));
        game.addSeparator();
        game.add(sound);
        game.add(focusing(item("menu.game.settings", "game-settings", () -> presenter.editSettings(), null)));
        graphics = menus.graphics(RenderMode.DIRECT, mode -> {
            presenter.setRenderMode(mode);
            canvas.requestFocusInWindow();
        });
        game.add(graphics);
        game.add(focusing(menus.frameTimes(canvas::setShowFrameTimes)));
        game.add(focusing(menus.fullScreen(fullScreen::toggle)));
        game.addSeparator();
        game.add(menus.exit(this::dispose));
        JMenu help = menus.help();
        help.add(menus.about(msg.get("window.title"), () -> presenter.showAbout()));
        JMenuBar bar = new JMenuBar();
        bar.add(game);
        bar.add(help);
        return bar;
    }

    private JMenuItem item(String key, String name, Runnable action, KeyStroke accelerator) {
        JMenuItem item = Mnemonics.apply(new JMenuItem(), msg.get(key));
        item.setName(name);
        if (accelerator != null) item.setAccelerator(accelerator);
        item.addActionListener(e -> action.run());
        return item;
    }

    /** After a menu command the keys belong to the game again. */
    private <T extends JMenuItem> T focusing(T item) {
        item.addActionListener(e -> canvas.requestFocusInWindow());
        return item;
    }

    // ---- CruiseView ---------------------------------------------------------------------------------

    @Override
    public void showOptions(boolean soundOn, RenderMode mode) {
        sound.setSelected(soundOn);
        for (int i = 0; i < graphics.getItemCount(); i++) {
            JMenuItem item = graphics.getItem(i);
            if (item != null) item.setSelected(("game-graphics-" + mode.id()).equals(item.getName()));
        }
    }

    @Override
    public void useRenderMode(RenderMode mode) {
        canvas.setRenderMode(mode);
    }

    @Override
    public Optional<CruiseOptions> editOptions(CruiseOptions current) {
        Optional<CruiseOptions> result = new SettingsDialog(this, msg, current).ask();
        canvas.requestFocusInWindow();
        return result;
    }

    @Override
    public void showAbout(String gamepads) {
        JOptionPane.showMessageDialog(this,
                "<html><body style='width: 340px'><center><h2>"
                        + msg.get("window.title") + " " + AmadeCruiseApplication.version() + "</h2>"
                        + "<p>" + msg.get("dialog.about.text") + "</p><br>"
                        + "<p>" + msg.get("dialog.about.keys") + "</p><br>"
                        + "<p>" + msg.get("dialog.about.radio") + "</p><br>"
                        + "<p>" + gamepads + "</p><br>"
                        + "<small>amade-cruise-game &bull; &copy; 2026 datazuul.com &bull; EUPL-1.2</small>"
                        + "</center></html>",
                Mnemonics.strip(menus.texts().format("menu.help.about", msg.get("window.title"))),
                JOptionPane.INFORMATION_MESSAGE);
        canvas.requestFocusInWindow();
    }
}
