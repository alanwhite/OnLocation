package xyz.arwhite;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.MemoryImageSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

public class CameraPanelController {

	public CameraPanelController(CameraPanel cameraPanel, CameraDriver cameraDriver) {
		final var iconSize = 24;
		final var iconVGap = 10;
		
		Image recordIcon = loadNewMRImage("icons8-record",iconSize);
		Image stopIcon = loadNewMRImage("icons8-stop",iconSize);
		
		var enlargeButton = new JButton(new ImageIcon(loadNewMRImage("icons8-enlarge",iconSize)));
		var compressButton = new JButton(new ImageIcon(loadNewMRImage("icons8-compress",iconSize)));
		var expandButton = new JButton(new ImageIcon(loadNewMRImage("icons8-expand",iconSize)));
		var originalButton = new JButton(new ImageIcon(loadNewMRImage("icons8-original-size",iconSize)));
		
		var controlsPanel = new JPanel(new BorderLayout());
		controlsPanel.setOpaque(false);
		cameraPanel.add(controlsPanel);
		
		var rightButtonPanel = new JPanel();
		rightButtonPanel.setOpaque(false);
		rightButtonPanel.setLayout(new BoxLayout(rightButtonPanel,BoxLayout.Y_AXIS));
		rightButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, iconVGap, iconVGap));
		rightButtonPanel.add(Box.createVerticalGlue());
		rightButtonPanel.add(enlargeButton);
		rightButtonPanel.add(Box.createVerticalStrut(iconVGap));
		rightButtonPanel.add(compressButton);
		rightButtonPanel.add(Box.createVerticalStrut(iconVGap));
		rightButtonPanel.add(expandButton);
		rightButtonPanel.add(Box.createVerticalStrut(iconVGap));
		rightButtonPanel.add(originalButton);
		controlsPanel.add(rightButtonPanel, BorderLayout.EAST);
		
		enlargeButton.addActionListener(l -> cameraPanel.setZoom(cameraPanel.getZoom() + 0.025f));
		compressButton.addActionListener(l -> cameraPanel.setZoom(cameraPanel.getZoom() - 0.025f));
		originalButton.addActionListener(l -> cameraPanel.setZoom(1.0f));
		expandButton.addActionListener(l -> cameraPanel.fill());

		rightButtonPanel.setVisible(false);
		
		final JLabel cameraTextOverlay = new JLabel("");
		cameraTextOverlay.setBorder(BorderFactory.createEmptyBorder(5,5,0,0));
		controlsPanel.add(cameraTextOverlay, BorderLayout.NORTH);
		
		final StringBuilder cameraResolution = new StringBuilder();
		final StringBuilder cameraFPS = new StringBuilder();
		final StringBuilder frameFPS = new StringBuilder();
		final StringBuilder screenFPS = new StringBuilder();
		
		cameraDriver.addPropertyChangeListener(l -> {
			switch(l.getPropertyName()) {
			case CameraDriver.DEVICE_RESOLUTION_PROPERTY:
				Dimension res = (Dimension) l.getNewValue();
				cameraResolution.setLength(0);
				cameraResolution.append(res.width);
				cameraResolution.append("x");
				cameraResolution.append(res.height);
				break;
			case CameraDriver.DEVICE_FPS_PROPERTY:
				cameraFPS.setLength(0);
				cameraFPS.append(l.getNewValue());
				break;
			case CameraDriver.FRAME_FPS_PROPERTY:
				frameFPS.setLength(0);
				frameFPS.append(String.format("%.1f", (float) l.getNewValue()));
				break;
			case CameraDriver.SCREEN_FPS_PROPERTY:
				screenFPS.setLength(0);
				screenFPS.append(String.format("%.1f", (float) l.getNewValue()));
				break;
			}

			cameraTextOverlay.setText(cameraResolution+" @ "+screenFPS+"/"+frameFPS+"/"+cameraFPS+"fps");
		});
		cameraTextOverlay.setVisible(false);
		
		int[] pixels = new int[16 * 16];
		Image image = Toolkit.getDefaultToolkit().createImage(
		        new MemoryImageSource(16, 16, pixels, 0, 16));
		Cursor transparentCursor =
		        Toolkit.getDefaultToolkit().createCustomCursor
		             (image, new Point(0, 0), "invisibleCursor");

		cameraPanel.addMouseMotionListener(new MouseAdapter() {
			
			Timer showTimer;
			
			{
				showTimer = new Timer(5000, l -> {
					rightButtonPanel.setVisible(false);
					cameraTextOverlay.setVisible(false);
					cameraPanel.setCursor(transparentCursor);
					showTimer.stop(); 
				});
			}
			
			@Override
			public void mouseEntered(MouseEvent e) { 
				manageVisibility();
			};
			
			
			@Override
			public void mouseMoved(MouseEvent e) {
				manageVisibility();
			}
			
			private void manageVisibility() {
				if ( !rightButtonPanel.isVisible() ) {
					rightButtonPanel.setVisible(true);
					cameraTextOverlay.setVisible(true);
					cameraPanel.setCursor(Cursor.getDefaultCursor());
					showTimer.start();
				} else
					showTimer.restart();
			}
		});
		

	
	}
	/**
	 * WARNING if you use a multiresolution image as the icon to a Swing JButton, there is a nasty bug related to disabled buttons.
	 * 
	 * Creates a MultiResolutionImage from resources using the provided name, suffixed by a "-" then the provided size, and also
	 * at sizes 1.25, 1.5 and 2.0 times larger. 

	 * The filenames must be structured as per name-24.png, name-30.png, name-36.png and name-48.png
	 * for a given name of 'name' and size of '32'. 
	 * 
	 * @param name the name of the image
	 * @param size the preferred starting size of the image
	 * @return a Multi-Resolution Image
	 */
	@SuppressWarnings("unused")
	private BaseMultiResolutionImage loadNewMRImage(String name, int size) {
		List<Image> imgList = new ArrayList<Image>();
		int[] sizes = { size, size+(size/4), size+(size/2), size*2 };
		final MediaTracker mt = new MediaTracker(new Canvas());

		try {
			for ( int i = 0; i < sizes.length; i++ ) {
				Image img = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(name+"-"+sizes[i]+".png"));
				imgList.add(img);
				mt.addImage(img, i);
			}

			if ( imgList.isEmpty() )
				return null;

			mt.waitForAll();

		} catch (Exception e) {
			System.out.println(e.getLocalizedMessage());
			return null;
		}

		if (mt.isErrorAny()) {
			System.out.println("Unexpected MediaTracker error " +
					Arrays.toString(mt.getErrorsAny()));
			return null;
		}

		BaseMultiResolutionImage mrImage = new BaseMultiResolutionImage(imgList.toArray(new Image[0]));
		return mrImage;
	}
}
