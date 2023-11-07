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
package org.wingate.signal.video;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import javax.sound.sampled.AudioFormat;

/**
 * Do not use this class as is.
 * @author util2
 */
public class ScrollingWaveformView extends javax.swing.JPanel {
    
    private byte[] samples = null;
    private AudioFormat format = null;
    private BufferedImage image = null;
    
    private BufferedImage past = null;
    private boolean isPast = false;
    
    private long bytesRead = 0L;
    private double positionInSeconds = 0d;
    
    private Color backgroundColor = Color.white;
    private Color waveColor = Color.blue.brighter();
    private Color baselineColor = Color.red;
    private Color momentColor = Color.red;

    public ScrollingWaveformView() {
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setWaveColor(Color waveColor) {
        this.waveColor = waveColor;
    }

    public void setBaselineColor(Color baselineColor) {
        this.baselineColor = baselineColor;
    }

    public void setMomentColor(Color momentColor) {
        this.momentColor = momentColor;
    }
    
    public void resetPosition(){
        bytesRead = 0L;
    }
    
    public void updateSamples(AudioFormat audioFormat, byte[] samples){
        format = audioFormat;
        this.samples = samples;
        
        // Seconds = bytesFromStart / frameSize * framerate        
        positionInSeconds = bytesRead / Math.max(1L, format.getFrameSize() * format.getFrameRate());
        bytesRead += samples.length;
        
        repaint();
    }
    
    public void draw(Graphics2D ctx, byte b){
        ctx.setColor(waveColor);
        
        if(isPast == false){
            // On dessine la dernière image
            // (travelling de la droite vers la gauche)
            AffineTransform affineTransform = new AffineTransform();
            affineTransform.setToTranslation(-1d, 0d);
            ctx.drawImage(past, affineTransform, null);
            isPast = true;
            past = image;
        }
        
        // Représentation sur x et y
        double x = getWidth() - 1d;
        double y = getHeight() * (128 - b) / 256;

        // Dessin
        Line2D shape = new Line2D.Double(x, getHeight()/2, x, y);
        ctx.draw(shape);
    }
    
    private String timeFormat(double seconds){
        long h = Math.round(Math.floor(seconds / 3600d));
        long m = Math.round(Math.floor((seconds % 3600d) / 60d));
        long s = Math.round(Math.floor(seconds % 60d));
        
        return String.format("%02dh%02dm%02ds", h, m, s);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        g.setColor(backgroundColor);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        if(samples == null) return;
        if(format == null) return;
        
        image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        if(past == null) past = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D ctx = image.createGraphics();
        
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
                    draw(ctx, b);
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
                    draw(ctx, b);
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
                    draw(ctx, b);
                }
            }else{
                for(int i=0; i<samplesLength; i++){
                    byte b = (byte)(samples[i] - 128);
                    
                    // Dessin
                    draw(ctx, b);
                }
            }
        }
        
        ctx.setColor(baselineColor);
        ctx.drawLine(0, getHeight()/2, getWidth(), getHeight()/2);
        
        ctx.dispose();
        
        g.drawImage(image, 0, 0, null);
        
        g.setColor(momentColor);
        Font oldFont = g.getFont();
        g.setFont(g.getFont().deriveFont(Font.BOLD, 12));
        g.drawString(timeFormat(positionInSeconds), 10, getHeight() - 12);
        g.setFont(oldFont);
        
        isPast = false;
    }
}
