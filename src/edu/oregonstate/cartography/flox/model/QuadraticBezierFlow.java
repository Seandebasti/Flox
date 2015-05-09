package edu.oregonstate.cartography.flox.model;

import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class QuadraticBezierFlow extends Flow {

    /**
     * control point.
     */
    private Point cPt;

    /**
     * Construct a QuadraticBezierFlow from 2 points
     *
     * @param startPt Start point
     * @param endPt End point
     */
    public QuadraticBezierFlow(Point startPt, Point endPt) {

        this.startPt = startPt;
        this.endPt = endPt;

        // Angle between the straight line connecting start and end point and 
        // the line connecting the start/end point with the corresponding Bezier 
        // control point.
        double alpha = .5;

        // Distance between startPt and endPt
        double dist = getBaselineLength();
        double tangentLength = dist * .33;
        computeCtrlPt(alpha, tangentLength);
    }

    /**
     * Construct a QuadraticBezierFlow
     * @param startPt Start point
     * @param endPt End point
     * @param alpha Angle around the point between the start point and the end 
     * point, relative to the normal on the line connecting start point and end point.
     * 0 is perpendicular to this line. +/-PI/2 or is on the line.
     * @param distPerc The distance of the control point to the point between
     * the start and the end point (percentage).
     * @param value Value for line width.
     */
    public QuadraticBezierFlow(Point startPt, Point endPt, double alpha, int distPerc, double value) {
        this.startPt = startPt;
        this.endPt = endPt;
        this.value = value;
        double dist = getBaselineLength() * distPerc / 100d;
        double beta = getBaselineAzimuth();
        double dx = dist * Math.cos(Math.PI / 2 - alpha + beta);
        double dy = dist * Math.sin(Math.PI / 2 - alpha + beta);
        double x = (startPt.x + endPt.x) / 2 + dx;
        double y = (startPt.y + endPt.y) / 2 + dy;
        cPt = new Point(x, y);
    }

    /**
     * Compute first control point from orientation of base line
     *
     * @param alpha angle between the base line and the line connecting the
     * start point with the first control point.
     * @param dist Distance between start point and first control point.
     */
    private void computeCtrlPt(double alpha, double dist) {
        final double lineOrientation = getBaselineAzimuth();
        final double azimuth = lineOrientation + alpha;
        final double dx1 = Math.sin(azimuth) * dist;
        final double dy1 = Math.cos(azimuth) * dist;
        double cPt1X = startPt.x + dx1;
        double cPt1Y = startPt.y + dy1;
        cPt = new Point(cPt1X, cPt1Y);
    }

    /**
     * Returns a bounding box, which is usually larger than the actual curve.
     * Does not take the line width into account.
     *
     * @return Bounding box.
     */
    @Override
    public Rectangle2D.Double getBoundingBox() {
        // Bezier curve is guaranteed to be within the convex hull defined by 
        // the four points.
        Rectangle2D.Double bb = new Rectangle2D.Double(startPt.x, startPt.y, 0, 0);
        bb.add(endPt.x, endPt.y);
        bb.add(cPt.x, cPt.y);
        return bb;
    }

    /**
     * Returns the control point.
     *
     * @return the control point
     */
    public Point getcPt() {
        return cPt;
    }

    /**
     * Sets the control point.
     *
     * @param cPt the control point to set
     */
    public void setcPt(Point cPt) {
        this.cPt = cPt;
    }

    /**
    * Converts this Bezier curve to straight line segments.
    * @param flatness The maximum distance between the curve and the straight
    * line segments.
    * @return An list of points, including copies of the start point and the end point.
    */
    @Override
    public ArrayList<Point> toStraightLineSegments(double flatness) {
        ArrayList<Point> points = new ArrayList<>();
        GeneralPath path = new GeneralPath();
        path.moveTo(startPt.x, startPt.y);
        path.quadTo(cPt.x, cPt.y, endPt.x, endPt.y);
        PathIterator iter = path.getPathIterator(null, flatness);
        double[] coords = new double[6];
        while (!iter.isDone()) {
            iter.currentSegment(coords);
            points.add(new Point(coords[0], coords[1]));
            iter.next();
        }
        return points;
    }
    
    public static QuadraticBezierFlow bendQuadraticFlow (Flow flow, int angleDeg, int distPerc) {
        
        // Convert angleDeg into radians
        double radians = angleDeg * (Math.PI / 180);
        
        //get the start and end points
        Point startPt = flow.getStartPt();
        Point endPt = flow.getEndPt();
        double value = flow.getValue();
        
        return new QuadraticBezierFlow(startPt, endPt, radians, distPerc, value);
    }
}