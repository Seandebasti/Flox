package edu.oregonstate.cartography.flox.gui;

import edu.oregonstate.cartography.flox.model.BezierFlow;
import edu.oregonstate.cartography.flox.model.Model;
import edu.oregonstate.cartography.flox.model.Point;
import edu.oregonstate.cartography.simplefeature.AbstractSimpleFeatureMapComponent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class FloxMapComponent extends AbstractSimpleFeatureMapComponent {

    /**
     * The model to draw.
     */
    private Model model;

    public FloxMapComponent() {
    }

    /**
     * Returns the bounding box of the geometry that is drawn by the map.
     *
     * @return The bounding box.
     */
    @Override
    protected Rectangle2D getBoundingBox() {
        return model.getBoundingBox();
    }

    /**
     * Override paintComponent of JComponent for custom drawing.
     *
     * @param g The destination to draw to.
     */
    @Override
    protected void paintComponent(Graphics g) {
        if (model == null) {
            return;
        }

        Graphics2D g2d = getGraphics2DBuffer();
        g2d.setColor(Color.LIGHT_GRAY);
        draw(model.getGeometry(), g2d);
        g2d.setColor(Color.BLACK);
        drawFlows(g2d);

        // copy double buffer image to JComponent
        Insets insets = getInsets();
        ((Graphics2D) g).drawImage(bufferImage, insets.left, insets.top, this);
    }

    /**
     * Set the model with data to draw.
     *
     * @param model the model to set
     */
    public void setModel(Model model) {
        this.model = model;
    }

    /**
     * Draw all flow lines to a Graphics2D context.
     *
     * @param g2d The graphics context.
     */
    private void drawFlows(Graphics2D g2d) {
        Iterator<BezierFlow> iter = model.flowIterator();
        while (iter.hasNext()) {
            BezierFlow flow = iter.next();
            GeneralPath path = new GeneralPath();
            Point startPt = flow.getStartPt();
            path.moveTo(xToPx(startPt.x), yToPx(startPt.y));
            Point cPt1 = flow.getcPt1();
            Point cPt2 = flow.getcPt2();
            Point endPt = flow.getEndPt();
            path.curveTo(xToPx(cPt1.x), yToPx(cPt1.y),
                    xToPx(cPt2.x), yToPx(cPt2.y),
                    xToPx(endPt.x), yToPx(endPt.y));
            g2d.draw(path);
        }
    }
}