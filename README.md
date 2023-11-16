# signal
Video and waveform using bytedeco JavaCV API working with FFmpeg.
Signal is a library that have ready to use components.

---

For video and audio in a player (a simple panel), use Media with VideoView.

To generate piece of waveform use this class and listener like this:
```java
SignalData signalData = new SignalData();
signalData.addSignalListener(new SignalListener(){
    @Override
    public void getSignal(SignalImageEvent event) {
        try {
            if(event.hasWaveform()){
                ImageIO.write(event.getWaveformImage(), "png", new File("your/path/to/png"));
            }
        } catch (IOException ex) {
            Logger.getLogger(Signal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
});
signalData.setMediaFile("your/path/to/media/to/view/in/the/waveform");
signalData.get(0L, 10000L, 800, 200, ImageMode.WaveformOnly, SearchMode.Absolute);
```
The last function 'get' has the following features:

| ABSOLUTE | RELATIVE |
| ---- | ---- |
| msStart, msStop, imageWidth, imageHeight | msStartRel, msDuration, imageWidth, imageHeight |

If RELATIVE, 'msStartRel' is calculated from last 'msStart' position.

To generate piece of spectrogram use this class and listener like this:
```java
SignalData signalData = new SignalData();
signalData.addSignalListener(new SignalListener(){
    @Override
    public void getSignal(SignalImageEvent event) {
        try {
            if(event.hasSpectrogram()){
                ImageIO.write(event.getSpectrogramImage(), "png", new File("your/path/to/png"));
            }
        } catch (IOException ex) {
            Logger.getLogger(Signal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
});
signalData.setMediaFile("your/path/to/media/to/view/in/the/spectrogram");
signalData.get(0L, 10000L, 800, 260, ImageMode.SpectrogramOnly, SearchMode.Absolute);
```

| Example: (Gin & Juice.m4a from 0s to 10s) |
| ---- |
| ![Gin & Juice.m4a from 0s to 10s.](https://github.com/TW2/signal/blob/main/data/essai.png) |
| ![Gin & Juice.m4a from 0s to 10s.](https://github.com/TW2/signal/blob/main/data/essai2.png) |

