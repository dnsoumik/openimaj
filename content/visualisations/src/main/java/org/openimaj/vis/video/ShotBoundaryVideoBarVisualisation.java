/**
 * 
 */
package org.openimaj.vis.video;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openimaj.audio.AudioStream;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.resize.ResizeProcessor;
import org.openimaj.time.Timecode;
import org.openimaj.video.Video;
import org.openimaj.video.processing.shotdetector.ShotBoundary;
import org.openimaj.video.processing.shotdetector.VideoShotDetector;

/**
 *	Will display a video in a timeline with shot detections marked on it.
 *
 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 *  @created 3 Jul 2012
 *	@version $Author$, $Revision$, $Date$
 */
public class ShotBoundaryVideoBarVisualisation extends VideoBarVisualisation
{
	/** Shot detector */
	private VideoShotDetector shotDetector = null;
	
	/** 
	 * 	To avoid constantly resampling, we cache the resampled images against
	 * 	the hash code of the original image.
	 */
	private HashMap<Integer, MBFImage> imageCache = new HashMap<Integer,MBFImage>();
	
	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 *	@param video
	 */
	public ShotBoundaryVideoBarVisualisation( Video<MBFImage> video )
	{
		this( video, null );
	}

	/**
	 * 
	 *	@param video
	 *	@param audio
	 */
	public ShotBoundaryVideoBarVisualisation( Video<MBFImage> video, AudioStream audio )
	{
		super( video, null );
		this.shotDetector = new VideoShotDetector( video );
		this.shotDetector.setFindKeyframes( true );
	}
	
	/** 
	 *	{@inheritDoc}
	 * 	@see org.openimaj.vis.video.VideoBarVisualisation#processFrame(org.openimaj.image.MBFImage, org.openimaj.time.Timecode)
	 */
	@Override
	public void processFrame( MBFImage frame, Timecode t )
	{
		this.shotDetector.processFrame( frame );
	}

	/** 
	 *	{@inheritDoc}
	 * 	@see org.openimaj.vis.video.VideoBarVisualisation#updateVis(org.openimaj.image.MBFImage)
	 */
	@Override
	public void updateVis( MBFImage vis )
	{
		List<ShotBoundary<MBFImage>> sbs = new ArrayList<ShotBoundary<MBFImage>>(
				this.shotDetector.getShotBoundaries() );
		for( ShotBoundary<MBFImage> sb : sbs )
		{
			int hash = sb.getKeyframe().imageAtBoundary.hashCode();
			MBFImage img = imageCache.get( hash );
			if( img == null )
				imageCache.put( hash, img = sb.getKeyframe().imageAtBoundary
					.process( new ResizeProcessor( 100, 100 ) ) );
			
			int x = (int)getTimePosition( sb.getTimecode() );
			vis.createRenderer().drawImage( img, x, 0 );
//			vis.drawLine( x, 0, x, vis.getHeight(), 2, RGBColour.BLACK );
		}
	}
}
