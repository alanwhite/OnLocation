package xyz.arwhite;

import java.awt.Dimension;
import java.io.File;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SwingWorker;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

public class CameraDriver extends SwingWorker<Frame, Frame> {

	final public static String DEVICE_RESOLUTION_PROPERTY = "deviceResolution";
	final public static String DEVICE_FPS_PROPERTY = "deviceFPS";
	final public static String FRAME_FPS_PROPERTY = "frameFPS";
	final public static String SCREEN_FPS_PROPERTY = "screenFPS";
	
	long lastFPSUpdate = 0;
	double frameCount = 0.0f;
	double screenCount = 0.0f;
			
	ImageIcon icon;
	JComponent component;
	Java2DFrameConverter converter = new Java2DFrameConverter();
	ArrayBlockingQueue<CameraCommand> commandQueue = new ArrayBlockingQueue<CameraCommand>(5);
	
	/*
	 * E D T
	 */
	
	public CameraDriver(ImageIcon icon, JComponent refreshComponent) {
		this.icon = icon;
		this.component = refreshComponent;
		
		execute();
	}
	
	@Override
	protected void process(List<Frame> chunks) {
		long now = System.currentTimeMillis();
		
		frameCount += chunks.size();
		screenCount++;
		
		if ( lastFPSUpdate == 0 ) 
			lastFPSUpdate = now;
		else {
			double diff = now - lastFPSUpdate;
			if ( diff >= 10000 ) {
				diff /= 1000.0f;
				
				this.firePropertyChange(CameraDriver.FRAME_FPS_PROPERTY, "", (float) frameCount / diff);
				this.firePropertyChange(CameraDriver.SCREEN_FPS_PROPERTY, "", (float) screenCount / diff);
				
				frameCount = 0;
				screenCount = 0;
			}
		}
		
		icon.setImage(converter.getBufferedImage(chunks.get(chunks.size() - 1)));
		component.repaint();
	}
	
	/*
	 * C O N T R O L  C O M M A N D S  ( E D T )
	 */

	// puts the filename on the queue
	public void openFile(String filename) {
		sendCommand(CameraCommand.OPENFILE, filename);
	}
	
	// send command to thread to start writing a file
	public void startFileCapture() {
		sendCommand(CameraCommand.STARTCAPTURE, null);
	}
	
	// send command to stop writing a file
	public void stopFileCapture() {
		sendCommand(CameraCommand.CLOSEFILE, null);
	}
	
	// send command to switch cameras
	public void setCamera(int deviceIndex, int width, int height) {
		this.firePropertyChange(CameraDriver.FRAME_FPS_PROPERTY, "", (float) 0.0f);
		this.firePropertyChange(CameraDriver.SCREEN_FPS_PROPERTY, "", (float) 0.0f);
		sendCommand(CameraCommand.SETCAMERA, new CameraSetup(deviceIndex,width,height));
	}
	
	// send command to release camera
	public void releaseCamera() {
		sendCommand(CameraCommand.RELEASECAMERA, null);
	}
	
	private void sendCommand(int command, Object parameter) {
		try {
			commandQueue.put(new CameraCommand(command,parameter));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * B A C K G R O U N D   T H R E A D
	 */
	
	@Override
	protected Frame doInBackground() throws Exception {
		OpenCVFrameGrabber grabber = null;
		FFmpegFrameRecorder recorder = null;
		
		var filename = "";
		var writeFile = false;
		Frame capturedFrame = null;
		
		while(true) {
			
			if ( grabber == null ) {
				// this will wait for a command to be sent to us
				var cmd = commandQueue.take();
				switch(cmd.command) {
				case CameraCommand.SETCAMERA:
					grabber = initCamera((CameraSetup) cmd.parameter);
					break;
				case CameraCommand.RELEASECAMERA:
					break;
				default:
					System.out.println("Use of CameraControl is borked, need to send a camera before anything else");
				} 
				
			} else {
				// grab a frame and publish it
				capturedFrame = grabber.grab();
				publish(capturedFrame);
				
				if ( writeFile ) {
					recorder.record(capturedFrame, avutil.AV_PIX_FMT_ABGR);
				}
				
				// check for commands
				var cmd = commandQueue.poll(); 
				if ( cmd != null ) {
					switch(cmd.command) {
					case CameraCommand.SETCAMERA:
						grabber.close();
						grabber.stop();
						grabber = initCamera((CameraSetup) cmd.parameter);
						break;
					case CameraCommand.RELEASECAMERA:
						grabber.close();
						grabber.stop();
						grabber = null;
						break;
					case CameraCommand.OPENFILE:
						filename = (String) cmd.parameter;
						recorder = new FFmpegFrameRecorder(
								new File(filename), 
								grabber.getImageWidth(), 
								grabber.getImageHeight(), 
								0);
						recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
						recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
						
						recorder.setFormat("mp4");
						recorder.setFrameRate(grabber.getFrameRate()); 
						recorder.start();
						break;
					case CameraCommand.STARTCAPTURE:
						writeFile = true;
						break;
					case CameraCommand.CLOSEFILE:
						writeFile = false;
						recorder.stop();
						recorder.close();
						break;
						
					}
				}
			} 
			
			if ( this.isCancelled() )
				break;
			
		} // while true loop
		
		if ( grabber != null )
			grabber.close();
		
		return null;

	}
	
	/**
	 * DRY helper for configuring and opening camera connection
	 * @param cam
	 * @return the started grabber to use
	 * @throws org.bytedeco.javacv.FrameGrabber.Exception
	 */
	private OpenCVFrameGrabber initCamera(CameraSetup cam) throws org.bytedeco.javacv.FrameGrabber.Exception {
		OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(cam.device);
		grabber.setImageWidth(cam.width);
		grabber.setImageHeight(cam.height);
		grabber.start();
		System.out.println("Sending");
		this.firePropertyChange(CameraDriver.DEVICE_RESOLUTION_PROPERTY, "", new Dimension(grabber.getImageWidth(), grabber.getImageHeight()));
		this.firePropertyChange(CameraDriver.DEVICE_FPS_PROPERTY, "", grabber.getFrameRate());
		return grabber;
	}
}

/*
 * Data Structures needed, hoping Records will help with this in JDK14 onwards
 */
abstract class CameraListener {
	abstract void fileOpened();
}

class CameraCommand {
	final static int OPENFILE = 100;
	final static int CLOSEFILE = 200;
	final static int STARTCAPTURE = 300;
	final static int RELEASECAMERA = 350;
	final static int SETCAMERA = 400;
	
	int command;
	Object parameter;
	public CameraCommand(int command, Object parameter) {
		this.command = command;
		this.parameter = parameter;
	}
}

class CameraSetup extends Object {
	int device;
	int width;
	int height;
	public CameraSetup(int device, int width, int height) {
		this.device = device;
		this.width = width;
		this.height = height;
	}
}