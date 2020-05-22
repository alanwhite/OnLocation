package xyz.arwhite;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

@SuppressWarnings("serial")
public class OnLocation extends JFrame {

	private int chosenCamera = 0;

	final int windowWidth = 1320;
	final int windowHeight = 800;
	final int captureWidth = 1280;
	final int captureHeight = 760;

	CameraDriver viewingCamera;

	BufferedImage image = new BufferedImage(captureWidth, captureHeight, BufferedImage.TYPE_INT_RGB);
	private ImageIcon icon;

	public OnLocation() {
		
		setTitle("Making Movies ... on location ... she don't know what it means");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		var panel = new JPanel(new BorderLayout());
		add(panel);

		var cameras = cameraChooser();
		panel.add(cameras, BorderLayout.WEST);

		var buttons = new JPanel();
		panel.add(buttons,BorderLayout.NORTH);

		var config = new JToggleButton("Config");
		config.addItemListener(e -> {
			cameras.setVisible(e.getStateChange() == ItemEvent.SELECTED);

			if ( e.getStateChange() == ItemEvent.DESELECTED ) 
				viewingCamera.setCamera(chosenCamera, captureWidth, captureHeight);
			else 
				viewingCamera.releaseCamera();
		});
		buttons.add(config);

		icon = new ImageIcon(image);
		var cameraPanel = new CameraPanel(icon, new BorderLayout());
		cameraPanel.setBackground(Color.GRAY);
		panel.add(cameraPanel, BorderLayout.CENTER);
		
		viewingCamera = new CameraDriver(icon, cameraPanel);
		viewingCamera.execute();
		viewingCamera.setCamera(0, captureWidth, captureHeight);

		new CameraPanelController(cameraPanel, viewingCamera);

		setSize(windowWidth, windowHeight);
		setLocationRelativeTo(null);
		setVisible(true);
		
	}
	
	private JPanel cameraChooser() {

		var panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.PAGE_AXIS));

		ImageIcon icon0 = new ImageIcon(new BufferedImage(160,120,BufferedImage.TYPE_INT_RGB));
		ImageIcon icon1 = new ImageIcon(new BufferedImage(160,120,BufferedImage.TYPE_INT_RGB));
		ImageIcon icon2 = new ImageIcon(new BufferedImage(160,120,BufferedImage.TYPE_INT_RGB));

		var camera0 = new JRadioButton("0", icon0);
		var camera1 = new JRadioButton("1", icon1);
		var camera2 = new JRadioButton("2", icon2);

		var cameras = new ButtonGroup();
		cameras.add(camera0);
		cameras.add(camera1);
		cameras.add(camera2);

		panel.add(camera0);
		panel.add(camera1);
		panel.add(camera2);

		Action aa = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				chosenCamera = Integer.parseInt(e.getActionCommand());
			} 
		};

		camera0.addActionListener(aa);
		camera1.addActionListener(aa);
		camera2.addActionListener(aa);
		camera0.setSelected(true);

		panel.setVisible(false);

		panel.addComponentListener(new ComponentAdapter() {
			MiniCameraReader c0;
			MiniCameraReader c1;
			MiniCameraReader c2;

			@Override
			public void componentShown(ComponentEvent e) {
				c0 = new MiniCameraReader(0,icon0, camera0);
				c1 = new MiniCameraReader(1,icon1, camera1);
				c2 = new MiniCameraReader(2,icon2, camera2);
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				c0.cancel(true);
				c1.cancel(true);
				c2.cancel(true);
			}

		});
		return panel;
	}
	
	class MiniCameraReader extends SwingWorker<Frame, Frame> {

		OpenCVFrameGrabber miniGrabber;
		Java2DFrameConverter converter = new Java2DFrameConverter();
		ImageIcon miniIcon;
		JComponent parent;

		public MiniCameraReader(int device, ImageIcon icon, JComponent comp) {
			miniGrabber = new OpenCVFrameGrabber(device);
			miniIcon = icon;
			parent = comp;

			miniGrabber.setImageWidth(miniIcon.getIconWidth());
			miniGrabber.setImageHeight(miniIcon.getIconHeight());

			execute();
		}

		@Override
		protected void process(List<Frame> chunks) {
			miniIcon.setImage(converter.getBufferedImage(chunks.get(chunks.size() - 1))
					.getScaledInstance(miniIcon.getIconWidth(), miniIcon.getIconHeight(), Image.SCALE_FAST));
			parent.repaint();
		}

		@Override
		protected Frame doInBackground() throws java.lang.Exception {
			System.out.println("Start mini grabber");
			miniGrabber.start();
			System.out.println("Mini "+miniGrabber.getImageWidth()+"x"+miniGrabber.getImageHeight());
			Frame capturedFrame = null;
			while ((capturedFrame = miniGrabber.grab()) != null) {

				publish(capturedFrame);

				if ( this.isCancelled() )
					break;
			}

			miniGrabber.release();
			miniGrabber.close();
			miniGrabber.stop();

			return null;
		}

	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.invokeLater(() -> new OnLocation());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}
}
