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
package org.wingate.signal;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 *
 * @author util2
 */
public class SignalImageEvent {
    private BufferedImage waveformImage = null;
    private BufferedImage spectrogramImage = null;
    private long waveformMsStart = 0L;
    private long waveformMsEnd = 0L;
    private long spectrogramMsStart = 0L;
    private long spectrogramMsEnd = 0L;

    public SignalImageEvent() {
    }

    public BufferedImage getWaveformImage() {
        return waveformImage;
    }

    public void setWaveformImage(BufferedImage waveformImage) {
        this.waveformImage = waveformImage;
    }

    public BufferedImage getSpectrogramImage() {
        return spectrogramImage;
    }

    public void setSpectrogramImage(BufferedImage spectrogramImage) {
        this.spectrogramImage = spectrogramImage;
    }

    public long getWaveformMsStart() {
        return waveformMsStart;
    }

    public void setWaveformMsStart(long waveformMsStart) {
        this.waveformMsStart = waveformMsStart;
    }

    public long getWaveformMsEnd() {
        return waveformMsEnd;
    }

    public void setWaveformMsEnd(long waveformMsEnd) {
        this.waveformMsEnd = waveformMsEnd;
    }

    public long getSpectrogramMsStart() {
        return spectrogramMsStart;
    }

    public void setSpectrogramMsStart(long spectrogramMsStart) {
        this.spectrogramMsStart = spectrogramMsStart;
    }

    public long getSpectrogramMsEnd() {
        return spectrogramMsEnd;
    }

    public void setSpectrogramMsEnd(long spectrogramMsEnd) {
        this.spectrogramMsEnd = spectrogramMsEnd;
    }

    public boolean hasWaveform() {
        return waveformImage != null;
    }

    public boolean hasSpectrogram() {
        return spectrogramImage != null;
    }
    
    public Dimension getWaveformDimension(){
        Dimension dim = new Dimension(
                hasWaveform() ? waveformImage.getWidth() : 0,
                hasWaveform() ? waveformImage.getHeight() : 0
        );
        
        return dim;
    }
    
    public Dimension getSpectrogramDimension(){
        Dimension dim = new Dimension(
                hasSpectrogram() ? spectrogramImage.getWidth() : 0,
                hasSpectrogram() ? spectrogramImage.getHeight() : 0
        );
        
        return dim;
    }
}
