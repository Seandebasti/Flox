/*
 * ColorUtils.java
 *
 * Created on May 16, 2005, 8:47 PM
 */

package edu.oregonstate.cartography.utils;

import java.awt.*;

/**
 * Utility methods for color related stuff.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ColorUtils {
    
    public static final Color highlightColor = new Color (75, 123, 181);
    
    /**
     * Converts a Color to a CSS string of the format "rgb(12,34,56)"
     * @param color The color to convert.
     * @return The CSS string.
     */
    public static String colorToCSSString(Color color) {
        StringBuilder str = new StringBuilder();
        str.append("rgb(");
        str.append(color.getRed());
        str.append(",");
        str.append(color.getGreen());
        str.append(",");
        str.append(color.getBlue());
        str.append(")");
        return str.toString();
    }
    
    /**
     * Converts a Color to a hexadecimal string of the format "#773300"
     * @param color The color to convert.
     * @return The hexadecimal string with a leading '#'.
     */
    public static String colorToRGBHexString (Color color) {
        
        String rgb = Integer.toHexString(color.getRGB());
        // cut the leading alpha value (2 characters) and add '#'
        return "#" + rgb.substring(2, rgb.length());
    }
    
    /**
     * Returns a highlight-color that can be used to draw selected objects.
     * @return The color to use for selected objects.
     */
    public static final java.awt.Color getSelectionColor() {
        return java.awt.Color.red;
    }
    
    /**
     * Returns a highlight-color that can be used to draw selected objects.
     * @return The color to use for selected objects.
     */
    public static final java.awt.Color getHighlightColor() {
        return highlightColor;
    }
    
}