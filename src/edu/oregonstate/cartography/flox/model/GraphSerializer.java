package edu.oregonstate.cartography.flox.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.StringTokenizer;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class GraphSerializer extends XmlAdapter<String, Graph> {

    @Override
    public Graph unmarshal(String s) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(s));

        HashMap<String, Point> points = new HashMap<>();
        ArrayList<Flow> flows = new ArrayList<>();
        Graph graph = new Graph();
        
        int numberOfNodes = Integer.parseInt(reader.readLine());

        if(numberOfNodes == 0) {
            return graph;
        }
        
        String l;

        // Read node data
        for (int i = 0; i < numberOfNodes; i++) {
            l = reader.readLine();
            StringTokenizer tokenizer = new StringTokenizer(l, " ,\t");
            String id = tokenizer.nextToken();
            double x = Double.parseDouble(tokenizer.nextToken());
            double y = Double.parseDouble(tokenizer.nextToken());
            double nodeValue = Double.parseDouble(tokenizer.nextToken());
            Point point = new Point(x, y);
            point.setValue(nodeValue);
            points.put(id, point);

        }

        // Read flow data
        while ((l = reader.readLine()) != null) {

            StringTokenizer tokenizer = new StringTokenizer(l, " ,\t");
            boolean locked = false;

            String startPtID = tokenizer.nextToken();
            String endPtID = tokenizer.nextToken();
            double cPtX = Double.parseDouble(tokenizer.nextToken());
            double cPtY = Double.parseDouble(tokenizer.nextToken());
            double flowValue = Double.parseDouble(tokenizer.nextToken());

            if (Double.parseDouble(tokenizer.nextToken()) == 1) {
                locked = true;
            }

            Point startPoint = points.get(startPtID);
            Point endPoint = points.get(endPtID);
            Point cPoint = new Point(cPtX, cPtY);
            QuadraticBezierFlow flow = new QuadraticBezierFlow(startPoint, endPoint);
            flow.setValue(flowValue);
            flow.setControlPoint(cPoint);
            flow.setLocked(locked);
            flows.add(flow);
        }

        for (Flow flow : flows) {
            graph.addFlow(flow);
        }

        return graph;
    }

    @Override
    public String marshal(Graph graph) {

        // A map of the nodes, with the node itself as the key.
        // The string will be a new ID number.
        HashMap<Point, String> nodeMap = new HashMap<>();

        // Get the flows
        Iterator flows = graph.flowIterator();

        StringBuilder nodeStr = new StringBuilder();
        StringBuilder flowStr = new StringBuilder();

        int key = 0;
        while (flows.hasNext()) {

            QuadraticBezierFlow flow = (QuadraticBezierFlow) flows.next();

            // Check to see if flow.stPoint is in nodeMap
            if (nodeMap.containsKey(flow.getStartPt())) {
                // Append the key and a comma to flowStr
                flowStr.append(nodeMap.get(flow.getStartPt()));
                flowStr.append(",");
            } else {
                // Add startPt to nodeMap, give it a value of key.
                nodeMap.put(flow.getStartPt(), Integer.toString(key));
                flowStr.append(Integer.toString(key));
                flowStr.append(",");
                key += 1;
            }

            // Same thing for endPoint
            // Check to see if flow.stPoint is in nodeMap
            if (nodeMap.containsKey(flow.getEndPt())) {
                // Append the key and a comma to flowStr
                flowStr.append(nodeMap.get(flow.getEndPt()));
                flowStr.append(",");
            } else {
                // Add startPt to nodeMap, give it a value of key.
                nodeMap.put(flow.getEndPt(), Integer.toString(key));
                flowStr.append(Integer.toString(key));
                flowStr.append(",");
                key += 1;
            }

            // Now the control point coordinates
            flowStr.append(flow.getCtrlPt().x);
            flowStr.append(",");
            flowStr.append(flow.getCtrlPt().y);
            flowStr.append(",");

            flowStr.append(flow.getValue());
            flowStr.append(",");

            if (flow.isLocked()) {
                flowStr.append(1);
            } else {
                flowStr.append(0);
            }
            flowStr.append("\n");
        }

        // Make a string of the nodes in nodeMap
        // Key first, then coordinates
        nodeStr.append(nodeMap.size());
        nodeStr.append("\n");
        for (Map.Entry<Point, String> entry : nodeMap.entrySet()) {
            String id = entry.getValue();
            double x = entry.getKey().x;
            double y = entry.getKey().y;
            double val = entry.getKey().getValue();

            nodeStr.append(id);
            nodeStr.append(",");
            nodeStr.append(x);
            nodeStr.append(",");
            nodeStr.append(y);
            nodeStr.append(",");
            nodeStr.append(val);
            nodeStr.append("\n");

        }

        return nodeStr.append(flowStr.toString()).toString();

    }

}
