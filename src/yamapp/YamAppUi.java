package yamapp;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.synth.SynthSliderUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.math.BigDecimal;
import java.math.RoundingMode;

final class YamAppUi implements ActionListener {
    private static final int HEIGHT = 500;
    private static final int WIDTH = 1000;
    private static final int TASKBAR_HEIGHT = 100;
    private final YamAppCore core;
    private JLabel readout;
    private JSlider volSlider;
    private JFrame yamFrame;

    YamAppUi(final YamAppCore core) {
        this.core = core;
    }

    private static String prettifyVolume(final int vol) {
        return String.valueOf(BigDecimal.valueOf(vol).divide(BigDecimal.TEN, BigDecimal.ROUND_DOWN).setScale(1, BigDecimal.ROUND_DOWN));
    }

    private static void showOnScreen(final int screen, final JFrame frame) {
        final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] devices = env.getScreenDevices();
        if (screen > -1 && screen < devices.length) {
            double maxY = devices[screen].getDefaultConfiguration().getBounds().getMaxY();
            final Double yDouble = maxY - HEIGHT - TASKBAR_HEIGHT;
            frame.setLocation(devices[screen].getDefaultConfiguration().getBounds().x, yDouble.intValue());
        } else if (devices.length > 0) {
            frame.setLocation(devices[0].getDefaultConfiguration().getBounds().x, frame.getY());
        } else {
            throw new RuntimeException("No Screens Found");
        }
    }

    void drawUi() {
        for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(laf.getName())) {
                try {
                    UIManager.setLookAndFeel(laf.getClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        yamFrame = new JFrame("Amp Volume");
        yamFrame.addMouseListener(new RelocationListener(yamFrame));
        yamFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        yamFrame.getContentPane().setLayout(new BorderLayout());
        yamFrame.setUndecorated(true);

        final VolumePoller poller = new VolumePoller(core);
        readout = createVolumeReadout();
        volSlider = createVolumeSlider(poller);

        setVolumeOnUi(core.getVolume());

        JPanel panel = new JPanel(new GridLayout(0, 1, 20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.darkGray);
        panel.add(volSlider);
        panel.add(readout);
        panel.add(createMuteButton());

        yamFrame.getContentPane().add(panel, BorderLayout.CENTER);
        yamFrame.pack();
        yamFrame.setVisible(true);
        yamFrame.setSize(WIDTH, HEIGHT);
        yamFrame.setResizable(false);

        showOnScreen(1, yamFrame);

        final Thread pollThread = new Thread(poller);
        pollThread.start();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals("mute")) {
            core.toggleMute();
        }
    }

    private void setVolumeOnUi(final int volume) {
        final String volStr = prettifyVolume(volume);
        readout.setText(String.valueOf(volStr));
        volSlider.setValue(volume);
        yamFrame.setTitle(volStr + " - Amp");
    }

    private JLabel createVolumeReadout() {
        final JLabel volText = new JLabel("", SwingConstants.CENTER);
        volText.setBackground(Color.darkGray);
        volText.setForeground(Color.RED);
        volText.setFont(new Font("monospaced", Font.BOLD, 76));
        return volText;
    }

    private JSlider createVolumeSlider(final VolumePoller poller) {
        final JSlider slider = new JSlider(-600, 0, -400);
        final BigDecimal sliderMin = BigDecimal.valueOf(slider.getMinimum());
        slider.setUI(new SynthSliderUI(slider) {
            @Override
            protected void scrollDueToClickInTrack(int dir) {
                final BigDecimal sliderTrackWidth = BigDecimal.valueOf(slider.getBounds().getWidth());
                final double x = slider.getMousePosition().getX();
                final BigDecimal fraction = BigDecimal.valueOf(x).divide(sliderTrackWidth, 4, RoundingMode.DOWN);
                final BigDecimal newVal = sliderMin.subtract(sliderMin.multiply(fraction));
                final int value = newVal.setScale(-1, RoundingMode.DOWN).intValue();
                slider.setValue(value);
            }
        });
        final UIDefaults sliderDefaults = new UIDefaults();
        sliderDefaults.put("Slider.thumbWidth", 25);
        sliderDefaults.put("Slider.thumbHeight", 25);
        sliderDefaults.put("Slider:SliderThumb.backgroundPainter", (Painter<JComponent>) (g, c, w, h) -> {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(3f));
            g.setColor(Color.RED);
            g.fillOval(1, 1, w + 5, h + 5);
            g.setColor(Color.WHITE);
            g.drawOval(1, 1, w + 5, h + 5);
        });
        sliderDefaults.put("Slider:SliderTrack.backgroundPainter", (Painter<JComponent>) (g, c, w, h) -> {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(2f));
            g.setColor(Color.GRAY);
            g.fillRoundRect(0, 6, w - 1, 8, 8, 8);
            g.setColor(Color.WHITE);
            g.drawRoundRect(0, 6, w - 1, 8, 8, 8);
        });

        slider.setMajorTickSpacing(10);
        slider.setSnapToTicks(true);
        slider.putClientProperty("Nimbus.Overrides", sliderDefaults);
        slider.putClientProperty("Nimbus.Overrides.InheritDefaults", false);
        slider.addChangeListener(new VolumeListener(slider, core, poller));
        return slider;
    }

    private JButton createMuteButton() {
        final JButton button = new JButton("MUTE");
        button.setBackground(Color.BLACK);
        button.setForeground(Color.RED);
        button.setFont(new Font("Helvetica", Font.BOLD, 50));
        button.setActionCommand("mute");
        button.addActionListener(this);
        return button;
    }

    private class VolumeListener implements ChangeListener {
        private final JSlider slider;
        private final YamAppCore core;
        private final VolumePoller poller;

        VolumeListener(final JSlider slider, final YamAppCore core, final VolumePoller poller) {
            this.slider = slider;
            this.core = core;
            this.poller = poller;
        }

        @Override
        public void stateChanged(final ChangeEvent e) {
            poller.resetSecondsLeft();
            if (!slider.getValueIsAdjusting()) {
                final int vol = slider.getValue();
                core.setVolumeTo(vol);
                setVolumeOnUi(vol);
            }
        }
    }

    private class VolumePoller implements Runnable {
        final YamAppCore core;
        int secondsLeft = 10;

        private VolumePoller(YamAppCore core) {
            this.core = core;
        }

        @Override
        public void run() {
            try {
                for (; ; ) {
                    Thread.sleep(1000);
                    secondsLeft--;
                    if (1 > secondsLeft) {
                        setVolumeOnUi(core.getVolume());
                        resetSecondsLeft();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        void resetSecondsLeft() {
            secondsLeft = 10;
        }
    }

    private class RelocationListener implements MouseListener {
        private JFrame yamFrame;
        private int screen = 1;

        public RelocationListener(final JFrame yamFrame) {
            this.yamFrame = yamFrame;
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            int clickCount = e.getClickCount();
            if (clickCount == 2) {
                showOnScreen(screen, yamFrame);
                if (screen == 1) {
                    screen = 0;
                } else {
                    screen = 1;
                }
            }
        }

        @Override
        public void mousePressed(final MouseEvent e) {
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
        }

        @Override
        public void mouseEntered(final MouseEvent e) {
        }

        @Override
        public void mouseExited(final MouseEvent e) {
        }
    }
}
