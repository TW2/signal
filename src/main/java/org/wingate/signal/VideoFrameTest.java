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

import org.wingate.signal.video.Media;
import java.awt.BorderLayout;
import javax.swing.JFileChooser;

/**
 *
 * @author util2
 */
public class VideoFrameTest extends javax.swing.JFrame {
    
    private final Media media;

    /**
     * Creates new form VideoFrameTest
     */
    public VideoFrameTest() {
        initComponents();
        
        media = new Media();
        
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(media.getView(), BorderLayout.CENTER);
        
        scrollingWavePanel.setLayout(new BorderLayout());
        scrollingWavePanel.add(media.getScrollingWave(), BorderLayout.CENTER);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fcMedia = new javax.swing.JFileChooser();
        menuPanel = new javax.swing.JPanel();
        jToolBar1 = new javax.swing.JToolBar();
        btnOpen = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        btnPlay = new javax.swing.JButton();
        btnPause = new javax.swing.JButton();
        btnStop = new javax.swing.JButton();
        jToolBar2 = new javax.swing.JToolBar();
        btnQuit = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel();
        signalPanel = new javax.swing.JPanel();
        scrollingWavePanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        menuPanel.setLayout(new java.awt.BorderLayout());

        btnOpen.setText("Open");
        btnOpen.setFocusable(false);
        btnOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });
        jToolBar1.add(btnOpen);
        jToolBar1.add(jSeparator1);

        btnPlay.setText("Play");
        btnPlay.setFocusable(false);
        btnPlay.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnPlay.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPlayActionPerformed(evt);
            }
        });
        jToolBar1.add(btnPlay);

        btnPause.setText("Pause");
        btnPause.setFocusable(false);
        btnPause.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnPause.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPauseActionPerformed(evt);
            }
        });
        jToolBar1.add(btnPause);

        btnStop.setText("Stop");
        btnStop.setFocusable(false);
        btnStop.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnStop.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });
        jToolBar1.add(btnStop);

        menuPanel.add(jToolBar1, java.awt.BorderLayout.CENTER);

        btnQuit.setText("Exit");
        btnQuit.setFocusable(false);
        btnQuit.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnQuit.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnQuitActionPerformed(evt);
            }
        });
        jToolBar2.add(btnQuit);

        menuPanel.add(jToolBar2, java.awt.BorderLayout.EAST);

        getContentPane().add(menuPanel, java.awt.BorderLayout.NORTH);

        mainPanel.setBackground(new java.awt.Color(0, 102, 204));

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 924, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 522, Short.MAX_VALUE)
        );

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        signalPanel.setPreferredSize(new java.awt.Dimension(836, 200));
        signalPanel.setLayout(new javax.swing.BoxLayout(signalPanel, javax.swing.BoxLayout.Y_AXIS));

        scrollingWavePanel.setBackground(new java.awt.Color(0, 102, 255));

        javax.swing.GroupLayout scrollingWavePanelLayout = new javax.swing.GroupLayout(scrollingWavePanel);
        scrollingWavePanel.setLayout(scrollingWavePanelLayout);
        scrollingWavePanelLayout.setHorizontalGroup(
            scrollingWavePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 924, Short.MAX_VALUE)
        );
        scrollingWavePanelLayout.setVerticalGroup(
            scrollingWavePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 200, Short.MAX_VALUE)
        );

        signalPanel.add(scrollingWavePanel);

        getContentPane().add(signalPanel, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
        // Open media in FFmpeg (no filter)
        int z = fcMedia.showOpenDialog(this);
        if(z == JFileChooser.APPROVE_OPTION){
            String path = fcMedia.getSelectedFile().getPath();
            media.setMediaFile(path);
        }
    }//GEN-LAST:event_btnOpenActionPerformed

    private void btnPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPlayActionPerformed
        // Play
        media.play();
    }//GEN-LAST:event_btnPlayActionPerformed

    private void btnPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPauseActionPerformed
        // Pause
        media.pause();
    }//GEN-LAST:event_btnPauseActionPerformed

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        // Stop
        media.stop();
    }//GEN-LAST:event_btnStopActionPerformed

    private void btnQuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnQuitActionPerformed
        // Exit
        System.exit(0);
    }//GEN-LAST:event_btnQuitActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnOpen;
    private javax.swing.JButton btnPause;
    private javax.swing.JButton btnPlay;
    private javax.swing.JButton btnQuit;
    private javax.swing.JButton btnStop;
    private javax.swing.JFileChooser fcMedia;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel menuPanel;
    private javax.swing.JPanel scrollingWavePanel;
    private javax.swing.JPanel signalPanel;
    // End of variables declaration//GEN-END:variables
}