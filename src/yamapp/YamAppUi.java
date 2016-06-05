package yamapp;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;

final class YamAppUi implements ActionListener {
    private final YamAppCore core;

    public YamAppUi(final YamAppCore core) {
        this.core = core;
    }

    private static String prettifyVolume(final int vol) {
        return String.valueOf(BigDecimal.valueOf(vol).divide(BigDecimal.TEN, BigDecimal.ROUND_DOWN).setScale(1, BigDecimal.ROUND_DOWN));
    }

    public static void showOnScreen(final int screen, final JFrame frame) {
        final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] devices = env.getScreenDevices();
        if (screen > -1 && screen < devices.length) {
            frame.setLocation(devices[screen].getDefaultConfiguration().getBounds().x, 3200);
        } else if (devices.length > 0) {
            frame.setLocation(devices[0].getDefaultConfiguration().getBounds().x, frame.getY());
        } else {
            throw new RuntimeException("No Screens Found");
        }
    }

    public void drawUi() {
        for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(laf.getName())) {
                try {
                    UIManager.setLookAndFeel(laf.getClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        final JFrame yamFrame = new JFrame("Amp Volume");
        yamFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        yamFrame.getContentPane().setLayout(new BorderLayout());
        JPanel panel = new JPanel(new GridLayout(0, 1, 20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.darkGray);
        final int volume = core.getVolume();

        final JLabel volText = createVolumeReadout(volume);
        final JSlider volumeSlider = createVolumeSlider(volume, volText);
        panel.add(volumeSlider);
        panel.add(volText);
        panel.add(createMuteButton());
        yamFrame.setUndecorated(true);
        yamFrame.getContentPane().add(panel, BorderLayout.CENTER);
        yamFrame.pack();
        yamFrame.setVisible(true);
        yamFrame.setSize(800, 400);
        yamFrame.setResizable(false);
        showOnScreen(1, yamFrame);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals("mute")) {
            final boolean success = core.toggleMute();
        }
    }

    private JLabel createVolumeReadout(final int volume) {
        final JLabel volText = new JLabel(String.valueOf(prettifyVolume(volume)), SwingConstants.CENTER);
        volText.setBackground(Color.darkGray);
        volText.setForeground(Color.RED);
        volText.setFont(new Font("monospaced", Font.BOLD, 56));
        return volText;
    }

    private JButton createMuteButton() {
        final JButton button = new JButton("MUTE");
        button.setBackground(Color.BLACK);
        button.setForeground(Color.RED);
        button.setFont(new Font("Helvetica", Font.BOLD, 40));
        button.setActionCommand("mute");
        button.addActionListener(this);
        return button;
    }

    private JSlider createVolumeSlider(final int volume, final JLabel volText) {
        final JSlider slider = new JSlider(-600, 0, volume);
        final UIDefaults sliderDefaults = new UIDefaults();
        sliderDefaults.put("Slider.thumbWidth", 20);
        sliderDefaults.put("Slider.thumbHeight", 20);
        sliderDefaults.put("Slider:SliderThumb.backgroundPainter", new Painter<JComponent>() {
            public void paint(Graphics2D g, JComponent c, int w, int h) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setStroke(new BasicStroke(3f));
                g.setColor(Color.RED);
                g.fillOval(1, 1, w + 5, h + 5);
                g.setColor(Color.WHITE);
                g.drawOval(1, 1, w + 5, h + 5);
            }
        });
        sliderDefaults.put("Slider:SliderTrack.backgroundPainter", new Painter<JComponent>() {
            public void paint(Graphics2D g, JComponent c, int w, int h) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setStroke(new BasicStroke(2f));
                g.setColor(Color.GRAY);
                g.fillRoundRect(0, 6, w - 1, 8, 8, 8);
                g.setColor(Color.WHITE);
                g.drawRoundRect(0, 6, w - 1, 8, 8, 8);
            }
        });
        slider.putClientProperty("Nimbus.Overrides", sliderDefaults);
        slider.putClientProperty("Nimbus.Overrides.InheritDefaults", false);
        slider.addChangeListener(new VolumeListener(slider, volText, core));
        return slider;
    }

    private class VolumeListener implements ChangeListener {
        private final JSlider slider;
        private final JLabel volText;
        private final YamAppCore core;

        VolumeListener(final JSlider slider, JLabel volText, final YamAppCore core) {
            this.slider = slider;
            this.volText = volText;
            this.core = core;
        }

        @Override
        public void stateChanged(final ChangeEvent e) {
            final int vol = slider.getValue();
            if (!slider.getValueIsAdjusting()) {
                final boolean success = core.setVolumeTo(vol);
            }
            volText.setText(prettifyVolume(vol));
        }
    }
}
