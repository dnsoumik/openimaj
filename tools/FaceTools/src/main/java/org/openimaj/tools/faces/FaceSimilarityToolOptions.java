/**
 * 
 */
package org.openimaj.tools.faces;

import org.kohsuke.args4j.Option;

/**
 *  @author David Dupplaw <dpd@ecs.soton.ac.uk>
 *	@version $Author$, $Revision$, $Date$
 *	@created 30 May 2011
 */
public class FaceSimilarityToolOptions extends FaceToolOptions
{
	@Option(name="-w",aliases="--withFirst",usage="Match against first rather than with all")
	public boolean withFirst = false;
	
	@Option(name="-bb",usage="Output bounding boxes of detected faces" )
	public boolean boundingBoxes = false;
}
