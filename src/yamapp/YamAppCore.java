package yamapp;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class YamAppCore {
    private static final String YAM_URL = "http://192.168.1.101/YamahaRemoteControl/ctrl";
    private static String ZONE = "Main_Zone";
    private DocumentBuilderFactory builderFactory;
    private DocumentBuilder documentBuilder;

    public YamAppCore() {
        try {
            builderFactory = DocumentBuilderFactory.newInstance();
            documentBuilder = builderFactory.newDocumentBuilder();
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String sendXmlToReceiver(final String xml) {
        try {
            //pnt("Outgoing XML", xml);
            final HttpClient client = HttpClients.createDefault();
            final HttpPost post = new HttpPost(YAM_URL);
            post.setEntity(new StringEntity(xml));

            final HttpResponse response = client.execute(post);
            //pnt("Sending 'POST' request", "");
            //pnt("Post parameters", post.getEntity().toString());
            //pnt("Response Code",
            //        String.valueOf(response.getStatusLine().getStatusCode()));
            //pnt("Response Msg",
            //        response.getStatusLine().getReasonPhrase());

            final BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            final StringBuilder result = new StringBuilder();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            //pnt("Result", result.toString());
            return result.toString();
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static void pnt(final String t, final String s) {
        if (t != null && t.length() > 0) {
            System.out.println(t + ": " + s);
        } else {
            System.out.println(s);
        }
    }

    public boolean toggleMute() {
        final String info = getInfo();
        final String startOfVal = "<Mute>";
        final String endOfVal = "</Mute>";
        final int start = info.indexOf(startOfVal);
        final int end = info.indexOf(endOfVal, start);
        final String muteStr = info.substring(start + startOfVal.length(), end);
        final boolean muted = muteStr.equalsIgnoreCase("On");
        if(muted) {
            return muteOff();
        } else {
            return muteOn();
        }
    }

    public boolean muteOn() {
        final String command = "<YAMAHA_AV cmd=\"PUT\"><" + ZONE + "><Volume><Mute>On</Mute></Volume></" + ZONE + "></YAMAHA_AV>";
        return null != sendXmlToReceiver(command);
    }

    public boolean muteOff() {
        final String command = "<YAMAHA_AV cmd=\"PUT\"><" + ZONE + "><Volume><Mute>Off</Mute></Volume></" + ZONE + "></YAMAHA_AV>";
        return null != sendXmlToReceiver(command);
    }

    public boolean setVolumeTo(final int lvl) {
        if (0 < lvl) { //refuse to go so loud!
            return false;
        }
        final String command = "<YAMAHA_AV cmd=\"PUT\"><" + ZONE + "><Volume><Lvl><Val>" + lvl + "</Val><Exp>1</Exp><Unit>dB</Unit></Lvl></Volume></" + ZONE + "></YAMAHA_AV>";
        return null != sendXmlToReceiver(command);
    }

    public void volumeUp() {
        adjustVolumeBy(10);
    }

    public void volumeDown() {
        adjustVolumeBy(-10);
    }

    public void adjustVolumeBy(final int delta) {
        final int currentVol = getVolume();
        setVolumeTo(currentVol + delta);
    }

    public int getVolume() {
        final String info = getInfo();

        final String startOfVal = "<Volume><Lvl><Val>";
        final String endOfVal = "</Val>";
        final int start = info.indexOf(startOfVal);
        final int end = info.indexOf(endOfVal, start);
        final int currentVol = Integer.valueOf(info.substring(start + startOfVal.length(), end));

        return currentVol;
    }

    private String getInfo() {
        final String command = "<YAMAHA_AV cmd=\"GET\"><" + ZONE + "><Basic_Status>GetParam</Basic_Status></" + ZONE + "></YAMAHA_AV>";
        return sendXmlToReceiver(command);
    }
}
