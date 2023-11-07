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

import java.awt.Image;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.wingate.signal.video.ScrollingWaveformView;
import org.wingate.signal.video.VideoView;

/**
 *
 * @author util2
 */
public class Media {
    
    private final VideoView view = new VideoView();
    private final ScrollingWaveformView scrollingWave = new ScrollingWaveformView();
    private File media = null;
    private boolean grabberIsSet = false;
    private volatile boolean inLOOP = false;
    private boolean inMEDIA = false;
    private AudioFormat format = null; // Shortcut role play
    
    private FFmpegFrameGrabber grabber = null;
    private static final Logger LOG = Logger.getLogger(Media.class.getName());
    private volatile Thread playThread;
    private PlaybackTimer playbackTimer;
    private SourceDataLine soundLine;

    public Media() {
    }
    
    public void setMediaFile(String path){
        File file = new File(path);
        if(file.exists() && file.isFile()){
            media = file;
            setupGrabber();
        }
    }

    public VideoView getView() {
        return view;
    }

    public ScrollingWaveformView getScrollingWave() {
        return scrollingWave;
    }
    
    private void setupGrabber(){
        grabber = new FFmpegFrameGrabber(media);
        grabberIsSet = true;
    }
    
    public void play(long microsStart, long microsEnd){
        if(media == null) return;
        if(grabberIsSet == false) return;
        
        if(inMEDIA == false){
            if(grabber != null){
                inMEDIA = true;
                inLOOP = false;
                whileRunning(microsStart, microsEnd);
            }
        }else{
            //inMEDIA = true
            inLOOP = false;
        }
        
    }
    
    public void play(){
        play(-1L, -1L);
    }
    
    public void pause(){
        if(media == null) return;
        if(grabberIsSet == false) return;
        
        inLOOP = !inLOOP;
    }
    
    public void stop(){
        if(media == null) return;
        if(grabberIsSet == false) return;
        
        inLOOP = false;
        inMEDIA = false;
        playThread.interrupt();
        scrollingWave.resetPosition();
    }
    
    @SuppressWarnings("SleepWhileInLoop")
    private void whileRunning(long microsStart, long microsEnd){
        playThread = new Thread(() -> {
            try {
                grabber.start();

                if (grabber.getAudioChannels() > 0) {
                    final AudioFormat audioFormat = new AudioFormat(grabber.getSampleRate(), 16, grabber.getAudioChannels(), true, true);
                    format = audioFormat;

                    final DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                    soundLine = (SourceDataLine) AudioSystem.getLine(info);
                    soundLine.open(audioFormat);
                    soundLine.start();
                    playbackTimer = new PlaybackTimer(soundLine);
                } else {
                    soundLine = null;
                    playbackTimer = new PlaybackTimer();
                }

                final Java2DFrameConverter converter = new Java2DFrameConverter();

                final ExecutorService audioExecutor = Executors.newSingleThreadExecutor();
                final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
                final ExecutorService waveformExecutor = Executors.newSingleThreadExecutor();

                final long maxReadAheadBufferMicros = 1000 * 1000L;

                long lastTimeStamp = -1L;
                
                if(microsStart != -1L) grabber.setAudioTimestamp(microsStart);

                while (!Thread.interrupted()) {
                    while(inLOOP){
                        // The media is paused!
                    }
                    final Frame frame = grabber.grab();
                    if (frame == null) {
                        break;
                    }
                    if (lastTimeStamp < 0) {
                        playbackTimer.start();
                    }
                    lastTimeStamp = frame.timestamp;
                    if (frame.image != null) {
                        final Frame imageFrame = frame.clone();

                        imageExecutor.submit(() -> {
                            final Image image = converter.convert(imageFrame);
                            final long timeStampDeltaMicros = imageFrame.timestamp - playbackTimer.elapsedMicros();
                            imageFrame.close();
                            if (timeStampDeltaMicros > 0) {
                                final long delayMillis = timeStampDeltaMicros / 1000L;
                                try {
                                    Thread.sleep(delayMillis);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                            view.setImage(image);
                        });
                    } else if (frame.samples != null) {
                        if (soundLine == null) {
                            throw new IllegalStateException("Internal error: sound playback not initialized");
                        }
                        final ShortBuffer channelSamplesShortBuffer = (ShortBuffer) frame.samples[0];
                        channelSamplesShortBuffer.rewind();

                        final ByteBuffer outBuffer = ByteBuffer.allocate(channelSamplesShortBuffer.capacity() * 2);

                        for (int i = 0; i < channelSamplesShortBuffer.capacity(); i++) {
                            short val = channelSamplesShortBuffer.get(i);
                            outBuffer.putShort(val);
                        }
                        
                        waveformExecutor.submit(() -> {
                            scrollingWave.updateSamples(format, outBuffer.array());
                        });

                        audioExecutor.submit(() -> {
                            soundLine.write(outBuffer.array(), 0, outBuffer.capacity());
                            outBuffer.clear();
                        });
                    }
                    final long timeStampDeltaMicros = frame.timestamp - playbackTimer.elapsedMicros();
                    if (timeStampDeltaMicros > maxReadAheadBufferMicros) {
                        Thread.sleep((timeStampDeltaMicros - maxReadAheadBufferMicros) / 1000);
                    }
                    
                    if(microsEnd != -1L && frame.timestamp >= microsEnd) break;
                }

                //==================================================================
                // AT THE END
                //==================================================================
                if (!Thread.interrupted()) {
                    long delay = (lastTimeStamp - playbackTimer.elapsedMicros()) / 1000 +
                           Math.round(1 / grabber.getFrameRate() * 1000);
                    Thread.sleep(Math.max(0, delay));
                }

                grabber.stop();
                grabber.release();
                if (soundLine != null) {
                    soundLine.stop();
                }
                
                waveformExecutor.shutdownNow(); // Added
                waveformExecutor.awaitTermination(10, TimeUnit.SECONDS); // Added
                
                audioExecutor.shutdownNow();
                audioExecutor.awaitTermination(10, TimeUnit.SECONDS);
                imageExecutor.shutdownNow();
                imageExecutor.awaitTermination(10, TimeUnit.SECONDS);

            } catch (IllegalStateException | InterruptedException | FFmpegFrameGrabber.Exception exception) {
                LOG.log(Level.SEVERE, null, exception);
                System.exit(1);
            } catch (LineUnavailableException ex) {
                Logger.getLogger(Media.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        playThread.start();
    }
    
    private class PlaybackTimer {
        private long startTime = -1L;
        private final DataLine soundLine;

        public PlaybackTimer(DataLine soundLine) {
            this.soundLine = soundLine;
        }

        public PlaybackTimer() {
            this.soundLine = null;
        }
        
        public void start(){
            if(soundLine == null){
                startTime = System.nanoTime();
            }
        }
        
        public long elapsedMicros(){
            if(soundLine == null){
                if(startTime < 0L){
                    throw new IllegalStateException("PlaybackTimer not initialized.");
                }
                return (System.nanoTime() - startTime) / 1000L;
            }else{
                return soundLine.getMicrosecondPosition();
            }
        }
    }
}
