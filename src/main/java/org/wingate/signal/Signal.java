package org.wingate.signal;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author util2
 */
public class Signal {

    public static void main(String[] args) {
        SignalData signalData = new SignalData();
        signalData.addSignalListener(new SignalListener(){
            @Override
            public void getSignal(SignalImageEvent event) {
                try {
                    if(event.hasWaveform()){
                        ImageIO.write(event.getWaveformImage(), "png", new File("C:\\Users\\util2\\Desktop\\test2.png"));
                    }                    
                    if(event.hasSpectrogram()){
                        ImageIO.write(event.getSpectrogramImage(), "png", new File("C:\\Users\\util2\\Desktop\\test3.png"));
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Signal.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        signalData.setMediaFile("C:\\Users\\util2\\Documents\\03 Gin & Juice.m4a");
        signalData.get(30000L, 40000L, 700, 260, ImageMode.Both, SearchMode.Absolute);
    }
}
