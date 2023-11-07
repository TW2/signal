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
import java.awt.Graphics;
import java.awt.Image;

/**
 * Do not use this class as is!
 * @author util2
 */
public class VideoView extends javax.swing.JPanel {
    
    private Image image = null;
    
    public VideoView(){
        
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        if(image != null){
            float scale = Math.min(
                    (float)getWidth()/(float)image.getWidth(null),
                    (float)getHeight()/(float)image.getHeight(null)
            );
            int w = (int)(scale * image.getWidth(null));
            int h = (int)(scale * image.getHeight(null));
            int x = (getWidth() - w) / 2;
            int y = (getHeight() - h) / 2;
            g.drawImage(image, x, y, w, h, null);
        }
    }

    public void setImage(Image image) {
        this.image = image;
        repaint();
    }
    
}
