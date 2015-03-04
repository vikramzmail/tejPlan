package com.tejas.client;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;

import com.mxgraph.util.mxCellRenderer;

public class ImageWriter extends MyCanvas {

	public  void createImage() {

		BufferedImage image = mxCellRenderer.createBufferedImage(graph, null, 1, Color.WHITE, true, null);
		try {
			ImageIO.write(image, "PNG", new File("tempfiles/graph.png"));
		} catch(IOException e){
			e.printStackTrace();
		}
		Date date = new Date();
		System.out.println("Image created"+" at "+date.toString());

	}
}
