/*
 * Copyright (C) 2023 util2
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.wingate.signal.wave;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.swing.event.EventListenerList;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

/**
 *
 * @author util2
 */
public class Waveform implements Runnable {
    
    public enum SearchMode {
        Absolute, Relative;
    }
    
    private String path;    
    private volatile Thread execThread;
    private volatile boolean inExecution = false;
    private FFmpegFrameGrabber grabber = null;
    private AudioFormat format = null;
    
    private long msLastStart;
    private long msStart, msStop;
    private int width, height;
    private float scale;

    public Waveform(){
        path = null;
        execThread = null;
        msLastStart = 0L;
        msStart = 0L;
        msStop = 0L;
        width = 0;
        height = 0;
        scale = 1f;
    }
    
    public void setMediaFile(String path){
        File file = new File(path);
        if(file.exists() && file.isFile()){
            this.path = path;
            grabber = new FFmpegFrameGrabber(path);
        }
    }
    
    /**
     * Get a piece of waveform from ms1 to ms2 if absolute
     * or to last time + ms1 to ms2 duration time if relative.
     * You must waiting for the signal event to get the result image.
     * @param ms1 start time if absolute; start value to add if relative (may be negative)
     * @param ms2 end time if absolute; duration time if relative
     * @param width width of result image if superior to 0 or signal based width specified by scale
     * @param height height of result or 100
     * @param sm mode that can be ABSOLUTE or RELATIVE
     */
    public void get(long ms1, long ms2, int width, int height, SearchMode sm){
        long from = Math.min(ms1, ms2);
        long to = Math.max(ms1, ms2);
        
        if(sm == SearchMode.Relative){
            from = msLastStart + from;
            to = from + to;
        }
        
        msLastStart = from;
        msStart = from;
        msStop = to;
        
        this.width = width;
        this.height = height;
        
        // Start the thread
        execThread = new Thread(this);
        inExecution = true;
        execThread.start();
    }
    
    public void get(long ms1, long ms2, float scale, SearchMode sm){
        this.scale = scale;
        get(ms1, ms2, 0, 0, sm);
    }
    
    private void pleaseStop(){
        if(execThread.isAlive() == true || execThread.isInterrupted() == false){
            execThread.interrupt();
            inExecution = false;
            execThread = null;
        }
    }
    
    private void createImage() throws FFmpegFrameGrabber.Exception{
        // Set value
        final int w = width != 0 ? width : 400;
        final int h = height != 0 ? height : 100;        
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        
        grabber.start();
                
        if (grabber.getAudioChannels() > 0) {
            final AudioFormat audioFormat = new AudioFormat(grabber.getSampleRate(), 16, grabber.getAudioChannels(), true, true);
            format = audioFormat;
        }
        
        if(msStart > 0L){
            grabber.setTimestamp(msStart);
        }
        
        // Processing
        Graphics2D g = image.createGraphics();
        
        boolean stop = false;
        try {
            long msBetween = msStart;
            while(stop == false){
                final Frame frame;
                frame = grabber.grab();

                if (frame == null) {
                    break; // TODO EOF
                }
                if (frame.samples != null) {
                    final ShortBuffer channelSamplesShortBuffer = (ShortBuffer) frame.samples[0];
                    channelSamplesShortBuffer.rewind();

                    final ByteBuffer outBuffer = ByteBuffer.allocate(channelSamplesShortBuffer.capacity() * 2);

                    for (int i = 0; i < channelSamplesShortBuffer.capacity(); i++) {
                        short val = channelSamplesShortBuffer.get(i);
                        outBuffer.putShort(val);
                    }

                    draw(g, w, h, format, outBuffer.array(), msBetween);
                    stop = isEnd(format, msBetween, outBuffer.array().length);
                    
                    msBetween += outBuffer.array().length;
                }
            }
        } catch (FFmpegFrameGrabber.Exception ex) {
            Logger.getLogger(Waveform.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        g.dispose();
        
        grabber.stop();
        grabber.release();
        
        // Send to user by event listener
        fireWaveformImage(new WaveformImageEvent(w, h, image));
    }
    
    private double getX(long msStart, long msStop, int width, long msCurrent){
        double total = msStop - msStart;
        double difference = msStop - msCurrent;
        double ratio = difference / Math.max(1d, total);
        return width - width * ratio;
    }
    
    private void draw(Graphics2D g, int width, int height, AudioFormat format, byte[] samples, long elapsed){
        double x;
        double y;
        
        // On tente une lecture
        if(format.getSampleSizeInBits() == 16){
            // en stéréo (16)
            // On obtient le nombre de données à traiter
            int samplesLength = samples.length / 2;
            
            // On traite le signal
            if(format.isBigEndian()){
                for(int i=0; i<samplesLength; i++){
                    // Most significant bit, Low significant bit
                    // First MSB (high order), second LSB (low order)
                    int MSB = (int)samples[2*i];
                    int LSB = (int)samples[2*i+1];
                    int value = MSB << 8 | (255 & LSB);
                    byte b = (byte)(128 * value / 32768);
                    
                    // Dessin
                    double positionInSeconds = (elapsed + i+1) / Math.max(1L, format.getFrameSize() * format.getFrameRate());
                    long msCurrent = Math.round(positionInSeconds * 1000L);
                    x = getX(msStart, msStop, width, msCurrent);
                    y = height * (128 - b) / 256;
                    Line2D shape = new Line2D.Double(x, height/2, x, y);
                    g.setColor(Color.red);
                    g.draw(shape);
                    
                    // Is the end?
                    if(isEnd(format, elapsed, i+1)){
                        break;
                    }
                }
            }else{
                for(int i=0; i<samplesLength; i++){
                    // Most significant bit, Low significant bit
                    // First LSB (low order), second MSB (high order)
                    int LSB = (int)samples[2*i];
                    int MSB = (int)samples[2*i+1];
                    int value = MSB << 8 | (255 & LSB);
                    byte b = (byte)(128 * value / 32768);
                    
                    // Dessin
                    double positionInSeconds = (elapsed + i+1) / Math.max(1L, format.getFrameSize() * format.getFrameRate());
                    long msCurrent = Math.round(positionInSeconds * 1000L);
                    x = getX(msStart, msStop, width, msCurrent);
                    y = height * (128 - b) / 256;
                    Line2D shape = new Line2D.Double(x, height/2, x, y);
                    g.setColor(Color.red);
                    g.draw(shape);
                    
                    // Is the end?
                    if(isEnd(format, elapsed, i+1)){
                        break;
                    }
                }
            }
        }else if(format.getSampleSizeInBits() == 8){
            // en mono (8)
            // On obtient le nombre de données à traiter
            int samplesLength = samples.length;
            
            if(format.getEncoding().toString().toLowerCase().startsWith("pcm_sign")){
                for(int i=0; i<samplesLength; i++){
                    byte b = (byte)samples[i];
                    
                    // Dessin
                    double positionInSeconds = (elapsed + i+1) / Math.max(1L, format.getFrameSize() * format.getFrameRate());
                    long msCurrent = Math.round(positionInSeconds * 1000L);
                    x = getX(msStart, msStop, width, msCurrent);
                    y = height * (128 - b) / 256;
                    Line2D shape = new Line2D.Double(x, height/2, x, y);
                    g.setColor(Color.red);
                    g.draw(shape);
                    
                    // Is the end?
                    if(isEnd(format, elapsed, i+1)){
                        break;
                    }
                }
            }else{
                for(int i=0; i<samplesLength; i++){
                    byte b = (byte)(samples[i] - 128);
                    
                    // Dessin
                    double positionInSeconds = (elapsed + i+1) / Math.max(1L, format.getFrameSize() * format.getFrameRate());
                    long msCurrent = Math.round(positionInSeconds * 1000L);
                    x = getX(msStart, msStop, width, msCurrent);
                    y = height * (128 - b) / 256;
                    Line2D shape = new Line2D.Double(x, height/2, x, y);
                    g.setColor(Color.red);
                    g.draw(shape);
                    
                    // Is the end?
                    if(isEnd(format, elapsed, i+1)){
                        break;
                    }
                }
            }
        }
    }
    
    private boolean isEnd(AudioFormat format, long bytesRead, long nowSample){
        // Seconds = bytesFromStart / frameSize * framerate        
        double positionInSeconds = (bytesRead + nowSample) / Math.max(1L, format.getFrameSize() * format.getFrameRate());
        long ms = Math.round(positionInSeconds * 1000L);
        return ms >= msStop;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    @Override
    public void run() {
        while(inExecution){
            if(execThread != null){
                try {
                    createImage();
                } catch (FFmpegFrameGrabber.Exception ex) {
                    Logger.getLogger(Waveform.class.getName()).log(Level.SEVERE, null, ex);
                }
                pleaseStop();
            }
        }
    }
    
    private final EventListenerList listeners = new EventListenerList();
    
    public void addWaveformListener(WaveformInterface listener){
        listeners.add(WaveformListener.class, (WaveformListener)listener);
    }
    
    public void removeWaveformListener(WaveformInterface listener){
        listeners.remove(WaveformListener.class, (WaveformListener)listener);
    }
    
    public Object[] getListeners(){
        return listeners.getListenerList();
    }
    
    protected void fireWaveformImage(WaveformImageEvent event){
        for(Object o : getListeners()){
            if(o instanceof WaveformListener listen){
                listen.getImage(event);
                break;
            }
        }
    }
    
}
