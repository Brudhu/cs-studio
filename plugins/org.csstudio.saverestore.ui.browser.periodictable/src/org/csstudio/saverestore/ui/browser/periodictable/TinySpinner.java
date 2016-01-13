package org.csstudio.saverestore.ui.browser.periodictable;

import com.sun.javafx.scene.control.skin.SpinnerSkin;

import javafx.geometry.Insets;
import javafx.scene.control.Skin;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.Region;

/**
 *
 * <code>TinySpinner</code> is a spinner that has narrower buttons, which take less space than the regular Java FX
 * spinner. In addition it also implements some common functionality required by the periodic table, such as applying
 * value as you type, editing etc.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class TinySpinner extends Spinner<Integer> {

    /**
     *
     * <code>TinySpinnerSkin</code> is a skin for the spinner with reduced increment and decrement buttons widths.
     *
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     *
     */
    private static class TinySpinnerSkin extends SpinnerSkin<Integer> {

        /**
         * Constructs a new spinner skin for the given spinner.
         *
         * @param spinner the spinner for which the skin is required
         */
        public TinySpinnerSkin(Spinner<Integer> spinner) {
            super(spinner);
            ((Region) getChildren().get(1)).setPadding(new Insets(0, 3, 0, 3));
            ((Region) getChildren().get(2)).setPadding(new Insets(0, 3, 0, 3));
        }
    }

    /**
     * Constructs a new editable spinner with the given value factory.
     *
     * @param factory the value factory to set on the spinner
     */
    TinySpinner(SpinnerValueFactory<Integer> factory) {
        super(factory);
        setEditable(true);
        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);
        getEditor().setOnKeyReleased(e -> {
            int pos = getEditor().getCaretPosition();
            try {
                int val = Integer.parseInt(getEditor().getText());
                getValueFactory().setValue(val);
            } catch (NumberFormatException ex) {
                //ignore
            }
            getEditor().positionCaret(pos);
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see javafx.scene.control.Spinner#createDefaultSkin()
     */
    @Override
    protected Skin<?> createDefaultSkin() {
        return new TinySpinnerSkin(this);
    }
}
