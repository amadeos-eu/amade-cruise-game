package eu.amadeos.games.cruise.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.games.cruise.repository.CruiseSettings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The settings: the music and its loudness, the loudness of the landscape, the radio station and the
 * playlist (files and folders), the time of day, the weather, the traffic and the seed word.
 */
// --8<-- [start:dialog]
final class SettingsDialog extends JDialog {
    private final Messages msg;
    private final JCheckBox music;
    private final JSlider musicVolume = new JSlider(0, 100);
    private final JSlider ambience = new JSlider(0, 100);
    private final JComboBox<CruiseSettings.Station> station = new JComboBox<>(CruiseSettings.Station.values());
    private final DefaultListModel<Path> playlist = new DefaultListModel<>();
    private final JList<Path> list = new JList<>(playlist);
    private final JCheckBox shuffle;
    private final JComboBox<CruiseSettings.TimeOfDay> time = new JComboBox<>(CruiseSettings.TimeOfDay.values());
    private final JCheckBox weather;
    private final JComboBox<CruiseSettings.Busy> traffic = new JComboBox<>(CruiseSettings.Busy.values());
    private final JTextField seed = new JTextField(14);
    private CruiseOptions result;

    SettingsDialog(Window owner, Messages msg, CruiseOptions current) {
        super(owner, Mnemonics.strip(msg.get("menu.game.settings")), ModalityType.APPLICATION_MODAL);
        this.msg = msg;
        setName("settings");
        music = Mnemonics.apply(new JCheckBox(), msg.get("settings.music"));
        shuffle = Mnemonics.apply(new JCheckBox(), msg.get("settings.shuffle"));
        weather = Mnemonics.apply(new JCheckBox(), msg.get("settings.weather"));
        music.setSelected(current.music());
        musicVolume.setValue(current.musicVolume());
        ambience.setValue(current.ambienceVolume());
        station.setSelectedItem(current.station());
        current.playlist().forEach(playlist::addElement);
        shuffle.setSelected(current.shuffle());
        time.setSelectedItem(current.time());
        weather.setSelected(current.weather());
        traffic.setSelectedItem(current.traffic());
        seed.setText(current.seed());
        station.setRenderer(named(s -> "settings.station." + s.id()));
        time.setRenderer(named(t -> "settings.time." + t.id()));
        traffic.setRenderer(named(b -> "settings.traffic." + b.id()));
        list.setVisibleRowCount(5);
        list.setName("settings-playlist");
        seed.setName("settings-seed");

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(12, 14, 6, 14));
        int row = 0;
        heading(form, row++, "settings.sound");
        full(form, row++, music);
        labelled(form, row++, "settings.music-volume", musicVolume);
        labelled(form, row++, "settings.ambience", ambience);
        labelled(form, row++, "settings.station", station);
        heading(form, row++, "settings.playlist");
        JScrollPane scroll = new JScrollPane(list);
        GridBagConstraints c = constraints(0, row++);
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        form.add(scroll, c);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(button("settings.add-files", this::addFiles));
        buttons.add(button("settings.add-folder", this::addFolder));
        buttons.add(button("settings.remove", this::remove));
        buttons.add(shuffle);
        full(form, row++, buttons);
        JLabel formats = new JLabel(msg.get("settings.formats"));
        formats.setFont(formats.getFont().deriveFont(formats.getFont().getSize2D() * 0.9f));
        full(form, row++, formats);
        heading(form, row++, "settings.drive");
        labelled(form, row++, "settings.time", time);
        full(form, row++, weather);
        labelled(form, row++, "settings.traffic", traffic);
        labelled(form, row++, "settings.seed", seed);
        JLabel seedHint = new JLabel(msg.get("settings.seed-hint"));
        seedHint.setFont(formats.getFont());
        full(form, row, seedHint);

        JButton ok = new JButton(Mnemonics.strip(msg.get("settings.ok")));
        ok.setName("settings-ok");
        ok.addActionListener(e -> {
            result = new CruiseOptions(music.isSelected(), musicVolume.getValue(), ambience.getValue(),
                    (CruiseSettings.Station) station.getSelectedItem(), entries(), shuffle.isSelected(),
                    (CruiseSettings.TimeOfDay) time.getSelectedItem(), weather.isSelected(),
                    (CruiseSettings.Busy) traffic.getSelectedItem(), seed.getText());
            dispose();
        });
        JButton cancel = new JButton(Mnemonics.strip(msg.get("settings.cancel")));
        cancel.setName("settings-cancel");
        cancel.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(ok);
        bottom.add(cancel);
        getRootPane().setDefaultButton(ok);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Shows the dialog and waits; the new settings, or empty if cancelled. */
    Optional<CruiseOptions> ask() {
        setVisible(true);
        return Optional.ofNullable(result);
    }
    // --8<-- [end:dialog]

    List<Path> entries() {
        List<Path> out = new ArrayList<>();
        for (int i = 0; i < playlist.size(); i++) out.add(playlist.get(i));
        return out;
    }

    private void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter(msg.get("settings.music-files"),
                "mp3", "ogg", "oga", "flac", "wav", "aif", "aiff", "au", "mod", "xm", "s3m", "it", "mptm", "stm", "669", "mtm", "med", "okt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File f : chooser.getSelectedFiles()) add(f.toPath());
        }
    }

    private void addFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) add(chooser.getSelectedFile().toPath());
    }

    private void add(Path p) {
        if (!playlist.contains(p)) playlist.addElement(p);
    }

    private void remove() {
        for (Path p : list.getSelectedValuesList()) playlist.removeElement(p);
    }

    private JButton button(String key, Runnable action) {
        JButton b = Mnemonics.apply(new JButton(), msg.get(key));
        b.setName(key.replace('.', '-'));
        b.addActionListener(e -> action.run());
        return b;
    }

    private <T> ListCellRenderer<T> named(java.util.function.Function<T, String> key) {
        DefaultListCellRenderer inner = new DefaultListCellRenderer();
        return (l, value, index, selected, focus) ->
                inner.getListCellRendererComponent(l, value == null ? "" : msg.get(key.apply(value)), index, selected, focus);
    }

    private static GridBagConstraints constraints(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private void heading(JPanel form, int row, String key) {
        JLabel l = new JLabel(msg.get(key));
        l.setFont(l.getFont().deriveFont(java.awt.Font.BOLD));
        GridBagConstraints c = constraints(0, row);
        c.gridwidth = 2;
        c.insets = new Insets(row == 0 ? 0 : 10, 4, 3, 4);
        form.add(l, c);
    }

    private void labelled(JPanel form, int row, String key, JComponent field) {
        JLabel l = Mnemonics.apply(new JLabel(), msg.get(key));
        l.setLabelFor(field);
        form.add(l, constraints(0, row));
        GridBagConstraints c = constraints(1, row);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(field, c);
    }

    private void full(JPanel form, int row, JComponent field) {
        GridBagConstraints c = constraints(0, row);
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }
}
