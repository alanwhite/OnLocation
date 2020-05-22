package xyz.arwhite;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

@SuppressWarnings("serial")
class CameraPanel extends JPanel {

	private ImageIcon icon;
	private double zoom = 1.0f;
	private boolean fillPane = false;
	
	public CameraPanel(ImageIcon icon) {
		super();
		this.icon = icon;
	}

	public CameraPanel(ImageIcon icon, boolean isDoubleBuffered) {
		super(isDoubleBuffered);
		this.icon = icon;
	}

	public CameraPanel(ImageIcon icon, LayoutManager layout, boolean isDoubleBuffered) {
		super(layout, isDoubleBuffered);
		this.icon = icon;
	}

	public CameraPanel(ImageIcon icon, LayoutManager layout) {
		super(layout);
		this.icon = icon;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if ( icon == null )
			return;
		
		Graphics2D g2D = (Graphics2D) g;

		/*
		 * Determine the scaling the system has directed be applied and undo it for clean images
		 */
		var deviceTransform = g2D.getDeviceConfiguration().getDefaultTransform();
		var scaleFactorX = deviceTransform.getScaleX();
		var scaleFactorY = deviceTransform.getScaleY();
		
		var realWidth = getSize().getWidth() * scaleFactorX;
		var realHeight = getSize().getHeight() * scaleFactorY;
		
		var saveTransform = g2D.getTransform();
		AffineTransform ourTransform = g2D.getTransform();
		
		ourTransform.scale(1.0f/scaleFactorX, 1.0f/scaleFactorY);
		g2D.setTransform(ourTransform);

		if ( fillPane ) {

			double widthRatio = realWidth / (double)icon.getIconWidth();
		    double heightRatio = realHeight / (double)icon.getIconHeight();
		    zoom = Math.min(widthRatio, heightRatio);
			fillPane = false;
		
		} 
		
		if ( zoom != 1.0f ) {
			
			g2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			
			var targetWidth = icon.getIconWidth() * zoom;
			var targetHeight = icon.getIconHeight() * zoom;
			
			g2D.drawImage(
					icon.getImage(), 
					(int) ((realWidth - targetWidth) / 2.0f), 
					(int) ((realHeight - targetHeight) / 2.0f), 
					(int) targetWidth,
					(int) targetHeight,
					null);

		} else {

			g2D.drawImage(
					icon.getImage(), 
					(int) ((realWidth - icon.getIconWidth()) / 2.0f), 
					(int) ((realHeight - icon.getIconHeight()) / 2.0f), 
					null);
		}
		
		g2D.setTransform(saveTransform);
		
	}
	
	public void fill() {
		fillPane = true;
	}

	public double getZoom() {
		return zoom;
	}

	public void setZoom(double zoom) {
		this.zoom = zoom;
	}

}