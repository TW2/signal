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
package org.wingate.signal.spectrogram;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
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
import org.wingate.signal.SearchMode;

/**
 * The computing about spectrogram comes from spectrogram by ozielcarneiro
 * see his 'spectrogram' source code in github at see. 
 * @author util2
 * @author ozielcarneiro Oziel Carneiro
 * @see https://github.com/ozielcarneiro/spectrogram 
 */
public class Spectrogram implements Runnable {
        
    private String path;    
    private volatile Thread execThread;
    private volatile boolean inExecution = false;
    private FFmpegFrameGrabber grabber = null;
    private AudioFormat format = null;
    
    private long msLastStart;
    private long msStart, msStop;
    private int width, height;
    private float scale;
    
    private Complex complex = new Complex();
    private static final double NORM_FACT = 1d / 32768d;
    private static final int NS = 256;
    private static final int SIZE = 4;

    public Spectrogram(){
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
        long from = ms1;
        long to = ms2;
        
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
        g.setColor(Color.black);
        g.fillRect(0, 0, w, h);
        
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
            Logger.getLogger(Spectrogram.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        g.dispose();
        
        grabber.stop();
        grabber.release();
        
        // Send to user by event listener
        fireSpectrogramImage(new SpectrogramImageEvent(w, h, image));
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
        
        byte[] buffer = new byte[NS * 2];
        int iterSamples = 0;
        int iterBuffer;
        
        try{
            while(iterSamples < samples.length){
                iterBuffer = 0;
                
                // Buffer (smaller)
                for(int i=0; i<NS * 2; i++){
                    if(iterSamples >= samples.length) break;
                    buffer[i] = samples[iterSamples];
                    iterBuffer++;
                    iterSamples++;
                }
                
                if(iterSamples >= samples.length){
                    byte[] tmp = buffer;
                    buffer = new byte[iterBuffer];
                    for(int i=0; i<iterBuffer; i++){
                        buffer[i] = tmp[i];
                    }
                }
                
                // Draw
                // On tente une lecture
                if(format.getSampleSizeInBits() == 16){
                    // en stéréo (16)
                    // On obtient le nombre de données à traiter
                    int samplesLength = buffer.length / 2;
                    double[] audioData = new double[NS];
                    
                    // On traite le signal
                    if(format.isBigEndian()){
                        for(int i=0; i<samplesLength; i++){
                            int MSB = (int)buffer[2*i];
                            int LSB = (int)buffer[2*i+1];
                            audioData[i] = (MSB << 8 | (255 & LSB)) * NORM_FACT;
                        }
                    }else{
                        for(int i=0; i<samplesLength; i++){
                            int LSB = (int)buffer[2*i];
                            int MSB = (int)buffer[2*i+1];
                            audioData[i] = (MSB << 8 | (255 & LSB)) * NORM_FACT;
                        }
                    }
                    
                    double[] lineTrans = complex.absFFT(audioData);
                    
                    for(int i=0; i<NS; i++){
                        float ang = (float)(-lineTrans[i]+1)*240f/360f;
                        
                        // Dessin
                        double positionInSeconds = (elapsed + i+1) / Math.max(1L, format.getFrameSize() * format.getFrameRate());
                        long msCurrent = Math.round(positionInSeconds * 1000L);
                        x = getX(msStart, msStop, width, msCurrent);
                        Color spc = Color.getHSBColor(ang, 1, 1);
                        int avg = (spc.getRed()+spc.getGreen()+spc.getRed())/3;
                        Color grayScaled = new Color(avg, avg, avg); // Partially grayscaled
                        g.setColor(grayScaled);
                        
                        Rectangle2D r = new Rectangle2D.Double(x, height - SIZE * i, 1, SIZE);
                        g.fill(r);
                        
                        // Is the end?
                        if(isEnd(format, elapsed, i+1)){
                            break;
                        }
                    }
                }
            }            
        }catch(Exception exc){
            
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
                    Logger.getLogger(Spectrogram.class.getName()).log(Level.SEVERE, null, ex);
                }
                pleaseStop();
            }
        }
    }
    
    private final EventListenerList listeners = new EventListenerList();
    
    public void addSpectrogramListener(SpectrogramInterface listener){
        listeners.add(SpectrogramListener.class, (SpectrogramListener)listener);
    }
    
    public void removeSpectrogramListener(SpectrogramInterface listener){
        listeners.remove(SpectrogramListener.class, (SpectrogramListener)listener);
    }
    
    public Object[] getListeners(){
        return listeners.getListenerList();
    }
    
    protected void fireSpectrogramImage(SpectrogramImageEvent event){
        for(Object o : getListeners()){
            if(o instanceof SpectrogramListener listen){
                listen.getImage(event);
                break;
            }
        }
    }
    
    /**
     * @author ozielcarneiro Oziel Carneiro
     * @see https://github.com/ozielcarneiro/spectrogram
     */
    public class Complex {
        
        private double real;
        private double imag;

        public Complex() {
            real = 0;
            imag = 0;
        }

        public Complex(double real, double imag) {
            this.real = real;
            this.imag = imag;
        }

        public Complex multiply(Complex a, Complex b) {
            return new Complex((a.real * b.real - a.imag * b.imag), 
                            (a.real * b.imag + a.imag * b.real));
        }

        public Complex divide(Complex a, Complex b) {
            return new Complex((a.real * b.real + a.imag * b.imag) / (b.real * b.real + b.imag * b.imag), 
                                (-(a.real * b.imag) + a.imag * b.real) / (b.real * b.real + b.imag * b.imag));
        }

        public Complex add(Complex a, Complex b) {
            return new Complex(a.real + b.real, a.imag + b.imag);
        }

        public Complex subtract(Complex a, Complex b) {
            return new Complex(a.real - b.real, a.imag - b.imag);
        }

        /**
         * Implementation of Euler's Formula using radians for angle measurements
         * @param a complex number
         * @return resulting complex number
         */

        public Complex exp(Complex a){
            double real = Math.exp(a.real)*Math.cos(a.imag);
            double imag = Math.exp(a.real)*Math.sin(a.imag);
            return new Complex(real, imag);
        }

        public Complex[] fft(double[] data){
            int n = data.length;
            Complex[] transform = new Complex[n];
            if(n==1){
                transform[0] = new Complex(data[0],0);
            }else{
                double[] even = new double[n/2];
                double[] odd = new double[n/2];
                for (int i = 0; i < n/2; i++) {
                    even[i] = data[i*2];
                    odd[i]  = data[i*2+1];
                }
                Complex[] aux = fft(even);
                System.arraycopy(aux, 0, transform, 0, aux.length);
                aux = fft(odd);
                System.arraycopy(aux, 0, transform, n/2, aux.length);
                for (int i = 0; i < n/2; i++) {
                    Complex t1 = transform[i];
                    Complex t2 = multiply(exp(new Complex(0,-2*Math.PI*i/n)),transform[i+n/2]);
                    transform[i] = add(t1, t2);
                    transform[i+n/2] = subtract(t1, t2);
                }
            }
            return transform;
        }

        public void norm(double n){
            real = real*n;
            imag = imag*n;
        }

        public Complex[] norm(Complex[] data, double n){
            for (int i = 0; i < data.length; i++) {
                data[i].norm(n);
            }
            return data;
        }

        public double[] absFFT(double[] data){
            return abs(fftShift(fft(data)));
        }

        public Complex[] fftShift(Complex[] transform){
            int n = transform.length;
            Complex[] out = new Complex[n];
            for (int i = 0; i < n/2; i++) {
                out[i] = transform[i+n/2];
                out[i+n/2] = transform[i];
            }
            return out;
        }

        public double getAbs(){
            return Math.sqrt(real*real+imag*imag);
        }

        public double[] abs(Complex[] data){
            int n = data.length;
            double[] out = new double[n];
            for (int i = 0; i < n; i++) {
                out[i] = data[i].getAbs();
            }
            return out;
        }

        /**
         * @return the real
         */
        public double getReal() {
            return real;
        }

        /**
         * @param real the real to set
         */
        public void setReal(double real) {
            this.real = real;
        }

        /**
         * @return the imag
         */
        public double getImag() {
            return imag;
        }

        /**
         * @param imag the imag to set
         */
        public void setImag(double imag) {
            this.imag = imag;
        }

        @Override
        public String toString(){
            if(imag>=0) {
                return ""+real+" + "+imag+"i";
            }
            else {
                return ""+real+" "+imag+"i";
            }
        }
    }
    
}
