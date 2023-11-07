package org.wingate.signal;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.wingate.signal.wave.ImageEvent;
import org.wingate.signal.wave.Waveform;
import org.wingate.signal.wave.WaveformListener;

/**
 *
 * @author util2
 */
public class Signal {

    public static void main(String[] args) {
//        VideoFrameTest frm = new VideoFrameTest();
//        frm.setLocationRelativeTo(null);
//        frm.setVisible(true);
        Waveform wf = new Waveform();
        wf.addWaveformListener(new WaveformListener(){
            @Override
            public void getImage(ImageEvent event) {
                try {
                    ImageIO.write(event.getImage(), "png", new File("C:\\Users\\util2\\Desktop\\essai.png"));
                } catch (IOException ex) {
                    Logger.getLogger(Signal.class.getName()).log(Level.SEVERE, null, ex);
                }
            }        
        });
        wf.setMediaFile("C:\\Users\\util2\\Documents\\03 Gin & Juice.m4a");
        wf.get(0L, 10000L, 800, 200, Waveform.SearchMode.Absolute);
    }
}
