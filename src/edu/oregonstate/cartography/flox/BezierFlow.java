package edu.oregonstate.cartography.flox;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class BezierFlow extends Flow {

    private Point cPt1;
    private Point cPt2;

    public BezierFlow(double startX, double startY, double c1x, double c1y,
            double c2x, double c2y, double endX, double endY) {
        this.startPt = new Point(startX, startY);
        cPt1 = new Point(c1x, c1y);
        cPt2 = new Point(c2x, c2y);
        this.endPt = new Point(endX, endY);
    }

    /**
     * @return the cPt1
     */
    public Point getcPt1() {
        return cPt1;
    }

    /**
     * @param cPt1 the cPt1 to set
     */
    public void setcPt1(Point cPt1) {
        this.cPt1 = cPt1;
    }

    /**
     * @return the cPt2
     */
    public Point getcPt2() {
        return cPt2;
    }

    /**
     * @param cPt2 the cPt2 to set
     */
    public void setcPt2(Point cPt2) {
        this.cPt2 = cPt2;
    }
}