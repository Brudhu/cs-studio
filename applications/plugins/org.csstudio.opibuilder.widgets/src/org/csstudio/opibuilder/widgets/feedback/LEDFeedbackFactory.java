package org.csstudio.opibuilder.widgets.feedback;

import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.csstudio.opibuilder.widgets.model.LEDModel;

/**Feedback Factory for LED.
 * @author Xihui Chen
 *
 */
public class LEDFeedbackFactory extends AbstractSquareSizeFeedbackFactory {


	@Override
	public int getMinimumSize() {
		return LEDModel.MINIMUM_SIZE;
	}
	
	@Override
	public boolean isSquareSizeRequired(AbstractWidgetModel widgetModel) {
		return !((LEDModel)widgetModel).isSquareLED();
	}
	
}