# signal
Video and waveform using bytedeco JavaCV API working with FFmpeg.
Signal is a library that have ready to use components.

---

For video and audio in a player (a simple panel), use Media with VideoView.

To generate piece of waveform use Waveform class and listener like this:
```java
Waveform wf = new Waveform();
wf.addWaveformListener(new WaveformListener(){
    @Override
    public void getImage(ImageEvent event) {
        try {
            ImageIO.write(event.getImage(), "png", new File("your/path/to/png"));
        } catch (IOException ex) {
            Logger.getLogger(Signal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }        
});
wf.setMediaFile("your/path/to/media/to/view/in/the/waveform");
wf.get(0L, 10000L, 800, 200, Waveform.SearchMode.Absolute);
```
The last function 'get' has the following features:

| ABSOLUTE | RELATIVE |
| ---- | ---- |
| msStart, msStop, imageWidth, imageHeight | msStartRel, msDuration, imageWidth, imageHeight |

If RELATIVE, 'msStartRel' is calculated from last 'msStart' position.

| Example: (Gin & Juice.m4a from 0s to 10s) |
| ---- |
| ![Gin & Juice.m4a from 0s to 10s.](https://github.com/TW2/signal/blob/main/data/essai.png) |

