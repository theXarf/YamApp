package yamapp;

/**
 * Created by Phil on 31/05/2016.
 */
public class YamRunner {
    public static void main(String[] args) {
        final YamAppCore core = new YamAppCore();
        final YamAppUi ui = new YamAppUi(core);
        ui.drawUi();
    }
}
