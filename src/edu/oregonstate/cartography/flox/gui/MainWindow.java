package edu.oregonstate.cartography.flox.gui;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryCollection;
import edu.oregonstate.cartography.flox.model.BezierFlow;
import edu.oregonstate.cartography.flox.model.Flow;
import edu.oregonstate.cartography.flox.model.FlowImporter;
import edu.oregonstate.cartography.flox.model.FlowLayouter;
import edu.oregonstate.cartography.flox.model.Layer;
import edu.oregonstate.cartography.flox.model.Model;
import edu.oregonstate.cartography.flox.model.VectorSymbol;
import edu.oregonstate.cartography.simplefeature.SVGExporter;
import edu.oregonstate.cartography.simplefeature.ShapeGeometryImporter;
import edu.oregonstate.cartography.utils.FileUtils;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class MainWindow extends javax.swing.JFrame {

    /**
     * the model of this application
     */
    private Model model;

    /**
     * Creates new form MainWindow
     */
    public MainWindow() {
        initComponents();

        // change the name of a layer
        new ListAction(layerList, new EditListAction() {
            @Override
            protected void applyValueToModel(String value, ListModel model, int row) {
                DnDListModel m = (DnDListModel) model;
                Layer layer = (Layer) m.get(row);
                layer.setName(value);
            }
        });

        // reorder layers
        layerList.addPropertyChangeListener(DraggableList.MODEL_PROPERTY,
                new PropertyChangeListener() {

                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        model.removeAllLayers();
                        DnDListModel m = (DnDListModel) layerList.getModel();
                        int n = m.getSize();
                        for (int i = 0; i < n; i++) {
                            model.addLayer((Layer) m.get(i));
                        }
                        mapComponent.repaint();
                    }
                });
    }

    /**
     * Set the model for this application.
     *
     * @param model
     */
    public void setModel(Model model) {
        this.model = model;
        mapComponent.setModel(model);
    }

    private void updateLayerList() {
        assert SwingUtilities.isEventDispatchThread();
        int selectedID = layerList.getSelectedIndex();
        layerList.setListData(model.getLayers().toArray());
        layerList.setSelectedIndex(selectedID);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        curvesButtonGroup = new javax.swing.ButtonGroup();
        mapComponent = new edu.oregonstate.cartography.flox.gui.FloxMapComponent();
        leftPanel = new javax.swing.JPanel();
        layerListScrollPane = new javax.swing.JScrollPane();
        layerList = new edu.oregonstate.cartography.flox.gui.DraggableList();
        symbolPanel = new javax.swing.JPanel();
        fillCheckBox = new javax.swing.JCheckBox();
        strokeCheckBox = new javax.swing.JCheckBox();
        fillColorButton = new edu.oregonstate.cartography.flox.gui.ColorButton();
        strokeColorButton = new edu.oregonstate.cartography.flox.gui.ColorButton();
        rightPanel = new javax.swing.JPanel();
        flowWidthPanel = new javax.swing.JPanel();
        javax.swing.JLabel flowWidthLabel = new javax.swing.JLabel();
        flowScaleFormattedTextField = new javax.swing.JFormattedTextField();
        jLabel1 = new javax.swing.JLabel();
        flowAngleSlider = new javax.swing.JSlider();
        jLabel2 = new javax.swing.JLabel();
        flowLengthSlider = new javax.swing.JSlider();
        cubicCurvesRadioButton = new javax.swing.JRadioButton();
        quadraticCurvesRadioButton = new javax.swing.JRadioButton();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openShapefileMenuItem = new javax.swing.JMenuItem();
        exportSVGMenuItem = new javax.swing.JMenuItem();
        importFlowsMenuItem = new javax.swing.JMenuItem();
        mapMenu = new javax.swing.JMenu();
        removeAllLayersMenuItem = new javax.swing.JMenuItem();
        removeSelectedLayerMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        showAllMenuItem = new javax.swing.JMenuItem();
        zoomOnSelectedLayerMenuItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator viewSeparator = new javax.swing.JPopupMenu.Separator();
        viewZoomInMenuItem = new javax.swing.JMenuItem();
        viewZoomOutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().add(mapComponent, java.awt.BorderLayout.CENTER);

        leftPanel.setPreferredSize(new java.awt.Dimension(120, 100));
        leftPanel.setLayout(new java.awt.GridLayout(2, 1));

        layerList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                layerListValueChanged(evt);
            }
        });
        layerListScrollPane.setViewportView(layerList);

        leftPanel.add(layerListScrollPane);

        symbolPanel.setLayout(new java.awt.GridBagLayout());

        fillCheckBox.setText("Fill");
        fillCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fillCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        symbolPanel.add(fillCheckBox, gridBagConstraints);

        strokeCheckBox.setText("Stroke");
        strokeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                strokeCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        symbolPanel.add(strokeCheckBox, gridBagConstraints);

        fillColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fillColorButtonActionPerformed(evt);
            }
        });
        symbolPanel.add(fillColorButton, new java.awt.GridBagConstraints());

        strokeColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                strokeColorButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        symbolPanel.add(strokeColorButton, gridBagConstraints);

        leftPanel.add(symbolPanel);

        getContentPane().add(leftPanel, java.awt.BorderLayout.WEST);

        flowWidthPanel.setLayout(new java.awt.GridBagLayout());

        flowWidthLabel.setText("Flow Width Scale");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        flowWidthPanel.add(flowWidthLabel, gridBagConstraints);

        flowScaleFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter()));
        flowScaleFormattedTextField.setPreferredSize(new java.awt.Dimension(120, 28));
        flowScaleFormattedTextField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                flowScaleFormattedTextFieldPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        flowWidthPanel.add(flowScaleFormattedTextField, gridBagConstraints);

        jLabel1.setText("Flow Angle");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 3, 0, 3);
        flowWidthPanel.add(jLabel1, gridBagConstraints);

        flowAngleSlider.setMajorTickSpacing(45);
        flowAngleSlider.setMaximum(90);
        flowAngleSlider.setMinimum(-90);
        flowAngleSlider.setPaintLabels(true);
        flowAngleSlider.setPaintTicks(true);
        flowAngleSlider.setValue(30);
        flowAngleSlider.setPreferredSize(new java.awt.Dimension(190, 37));
        flowAngleSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                flowAngleSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        flowWidthPanel.add(flowAngleSlider, gridBagConstraints);

        jLabel2.setText("Flow Length");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 3, 0, 3);
        flowWidthPanel.add(jLabel2, gridBagConstraints);

        flowLengthSlider.setMajorTickSpacing(25);
        flowLengthSlider.setPaintLabels(true);
        flowLengthSlider.setPaintTicks(true);
        flowLengthSlider.setValue(33);
        flowLengthSlider.setPreferredSize(new java.awt.Dimension(190, 37));
        flowLengthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                flowLengthSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        flowWidthPanel.add(flowLengthSlider, gridBagConstraints);

        curvesButtonGroup.add(cubicCurvesRadioButton);
        cubicCurvesRadioButton.setSelected(true);
        cubicCurvesRadioButton.setText("Cubic Curves");
        cubicCurvesRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cubicCurvesRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        flowWidthPanel.add(cubicCurvesRadioButton, gridBagConstraints);

        curvesButtonGroup.add(quadraticCurvesRadioButton);
        quadraticCurvesRadioButton.setText("Quadratic Curves");
        quadraticCurvesRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quadraticCurvesRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        flowWidthPanel.add(quadraticCurvesRadioButton, gridBagConstraints);

        rightPanel.add(flowWidthPanel);

        getContentPane().add(rightPanel, java.awt.BorderLayout.EAST);

        fileMenu.setText("File");

        openShapefileMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        openShapefileMenuItem.setText("Add Shapefile Layer�");
        openShapefileMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openShapefileMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openShapefileMenuItem);

        exportSVGMenuItem.setText("Export SVG�");
        exportSVGMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSVGMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exportSVGMenuItem);

        importFlowsMenuItem.setText("Open Flows�");
        importFlowsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importFlowsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(importFlowsMenuItem);

        menuBar.add(fileMenu);

        mapMenu.setText("Map");

        removeAllLayersMenuItem.setText("Remove All Layers");
        removeAllLayersMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAllLayersMenuItemActionPerformed(evt);
            }
        });
        mapMenu.add(removeAllLayersMenuItem);

        removeSelectedLayerMenuItem.setText("Remove Selected Layer");
        removeSelectedLayerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeSelectedLayerMenuItemActionPerformed(evt);
            }
        });
        mapMenu.add(removeSelectedLayerMenuItem);

        menuBar.add(mapMenu);

        viewMenu.setText("View");

        showAllMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        showAllMenuItem.setText("Show All");
        showAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAllMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(showAllMenuItem);

        zoomOnSelectedLayerMenuItem.setText("Zoom on Selected Layer");
        zoomOnSelectedLayerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomOnSelectedLayerMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(zoomOnSelectedLayerMenuItem);
        viewMenu.add(viewSeparator);

        viewZoomInMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PLUS,    java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        viewZoomInMenuItem.setText("Zoom In");
        viewZoomInMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewZoomInMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(viewZoomInMenuItem);

        viewZoomOutMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_MINUS,    java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        viewZoomOutMenuItem.setText("Zoom Out");
        viewZoomOutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewZoomOutMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(viewZoomOutMenuItem);

        menuBar.add(viewMenu);

        setJMenuBar(menuBar);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exportSVGMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSVGMenuItemActionPerformed

        OutputStream outputStream = null;
        try {
            // ask for export file
            String outFilePath = FileUtils.askFile("SVG File", false);
            if (outFilePath == null) {
                // user canceled
                return;
            }
            outFilePath = FileUtils.forceFileNameExtension(outFilePath, "svg");
            SVGExporter exporter = new SVGExporter(model.getGeometryCollection(),
                    "OSU Cartography Group", "Flox");
            exporter.setSVGCanvasSize(800, 550);
            outputStream = new FileOutputStream(outFilePath);
            exporter.export(outputStream);
        } catch (IOException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            ErrorDialog.showErrorDialog("An error occured.", "Flox Error", ex, null);
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }//GEN-LAST:event_exportSVGMenuItemActionPerformed

    /**
     * Open an Esri shapefile
     */
    public void openShapefile() {
        try {
            // ask for import file
            String inFilePath = FileUtils.askFile("Shapefile", true);
            if (inFilePath == null) {
                // user canceled
                return;
            }

            // read shapefile
            GeometryCollection collection = new ShapeGeometryImporter().read(inFilePath);
            if (collection == null) {
                ErrorDialog.showErrorDialog("The selected file is not a shapefile.", "Flox Error");
                return;
            }
            Layer layer = model.addLayer(collection);
            layer.setName(FileUtils.getFileNameWithoutExtension(inFilePath));
            mapComponent.showAll();
            updateLayerList();
            layerList.setSelectedIndex(0);
        } catch (IOException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            ErrorDialog.showErrorDialog("An error occured.", "Flox Error", ex, null);
        } finally {
            writeSymbolGUI();
        }
    }

    /**
     * Returns the layer currently selected by the user.
     *
     * @return The selected map layer or null if none is selected.
     */
    private Layer getSelectedMapLayer() {
        assert SwingUtilities.isEventDispatchThread();
        int index = layerList.getSelectedIndex();
        return index == -1 ? null : model.getLayer(index);
    }

    private VectorSymbol getSelectedVectorSymbol() {
        Layer selectedLayer = getSelectedMapLayer();
        VectorSymbol vectorSymbol = null;
        if (selectedLayer != null) {
            vectorSymbol = selectedLayer.getVectorSymbol();
        }
        return vectorSymbol;
    }

    private void writeSymbolGUI() {
        VectorSymbol vectorSymbol = getSelectedVectorSymbol();

        boolean enable = vectorSymbol != null;
        fillCheckBox.setEnabled(enable);
        strokeCheckBox.setEnabled(enable);
        fillColorButton.setEnabled(enable);
        strokeColorButton.setEnabled(enable);

        if (vectorSymbol != null) {
            fillCheckBox.setSelected(vectorSymbol.isFilled());
            strokeCheckBox.setSelected(vectorSymbol.isStroked());
            fillColorButton.setColor(vectorSymbol.getFillColor());
            strokeColorButton.setColor(vectorSymbol.getStrokeColor());
        }
    }

    private void readSymbolGUI() {
        VectorSymbol vectorSymbol = getSelectedVectorSymbol();
        if (vectorSymbol == null) {
            return;
        }
        vectorSymbol.setFilled(fillCheckBox.isSelected());
        vectorSymbol.setStroked(strokeCheckBox.isSelected());
        vectorSymbol.setFillColor(fillColorButton.getColor());
        vectorSymbol.setStrokeColor(strokeColorButton.getColor());
    }

    private void openShapefileMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openShapefileMenuItemActionPerformed
        openShapefile();
    }//GEN-LAST:event_openShapefileMenuItemActionPerformed

    private void removeAllLayersMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeAllLayersMenuItemActionPerformed
        model.removeAllLayers();
        mapComponent.showAll();
        mapComponent.repaint();
        updateLayerList();
    }//GEN-LAST:event_removeAllLayersMenuItemActionPerformed

    private void layerListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_layerListValueChanged
        if (!evt.getValueIsAdjusting()) {
            writeSymbolGUI();
        }
    }//GEN-LAST:event_layerListValueChanged

    private void fillCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fillCheckBoxActionPerformed
        readSymbolGUI();
        mapComponent.repaint();
    }//GEN-LAST:event_fillCheckBoxActionPerformed

    private void strokeCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_strokeCheckBoxActionPerformed
        readSymbolGUI();
        mapComponent.repaint();
    }//GEN-LAST:event_strokeCheckBoxActionPerformed

    private void fillColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fillColorButtonActionPerformed
        if (!fillCheckBox.isSelected()) {
            fillCheckBox.setSelected(true);
        }
        readSymbolGUI();
        mapComponent.repaint();
    }//GEN-LAST:event_fillColorButtonActionPerformed

    private void strokeColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_strokeColorButtonActionPerformed
        if (!strokeCheckBox.isSelected()) {
            strokeCheckBox.setSelected(true);
        }
        readSymbolGUI();
        mapComponent.repaint();
    }//GEN-LAST:event_strokeColorButtonActionPerformed

    private void removeSelectedLayerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeSelectedLayerMenuItemActionPerformed
        int selectedLayerID = layerList.getSelectedIndex();
        if (selectedLayerID < 0) {
            return;
        }
        model.removeLayer(selectedLayerID);
        updateLayerList();
        layerList.setSelectedIndex(--selectedLayerID);
        writeSymbolGUI();
        mapComponent.repaint();
    }//GEN-LAST:event_removeSelectedLayerMenuItemActionPerformed

    private void importFlowsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importFlowsMenuItemActionPerformed
        try {
            // ask for import file
            String inFilePath = FileUtils.askFile("Shapefile", true);
            if (inFilePath == null) {
                // user canceled
                return;
            }

            ArrayList<Flow> flows = FlowImporter.readFlows(inFilePath);
            if (flows != null) {
                model.setFlows(flows);
                double maxFlowValue = model.getMaxFlowValue();
                model.setFlowWidthScale(20 / maxFlowValue);
                flowScaleFormattedTextField.setValue(model.getFlowWidthScale());
                mapComponent.showAll();
            }
        } catch (IOException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            ErrorDialog.showErrorDialog("An error occured.", "Flox Error", ex, null);
        }
    }//GEN-LAST:event_importFlowsMenuItemActionPerformed

    private void showAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showAllMenuItemActionPerformed
        mapComponent.showAll();
    }//GEN-LAST:event_showAllMenuItemActionPerformed

    private void viewZoomInMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewZoomInMenuItemActionPerformed
        mapComponent.zoomIn();
    }//GEN-LAST:event_viewZoomInMenuItemActionPerformed

    private void viewZoomOutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewZoomOutMenuItemActionPerformed
        mapComponent.zoomOut();
    }//GEN-LAST:event_viewZoomOutMenuItemActionPerformed

    private void zoomOnSelectedLayerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomOnSelectedLayerMenuItemActionPerformed
        Layer layer = getSelectedMapLayer();
        if (layer == null) {
            return;
        }
        Envelope bb = layer.getGeometry().getEnvelopeInternal();
        Rectangle2D bbRect = new Rectangle2D.Double(bb.getMinX(), bb.getMinY(),
                bb.getWidth(), bb.getHeight());
        mapComponent.zoomOnRectangle(bbRect);
    }//GEN-LAST:event_zoomOnSelectedLayerMenuItemActionPerformed

    private void flowScaleFormattedTextFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_flowScaleFormattedTextFieldPropertyChange
        if ("value".equals(evt.getPropertyName())) {
            double s = ((Number) flowScaleFormattedTextField.getValue()).doubleValue();
            model.setFlowWidthScale(s);
            mapComponent.repaint();
        }
    }//GEN-LAST:event_flowScaleFormattedTextFieldPropertyChange

    /**
     * Change the flow layout based on current GUI parameters
     */
    private void layoutFlows() {
        int angleDeg = flowAngleSlider.getValue();
        int distPerc = flowLengthSlider.getValue();
        ArrayList<Flow> flows = new ArrayList<>();
        Model.CurveType curveType = model.getCurveType();
        
        Iterator<Flow> iter = model.flowIterator();
        while (iter.hasNext()) {
            Flow flow = iter.next();
            if (curveType == Model.CurveType.CUBIC) {
                flow = FlowLayouter.bendCubicFlow(flow, angleDeg, distPerc);
            } else {
                flow = FlowLayouter.bendQuadraticFlow(flow, angleDeg, distPerc);
            }
            flows.add(flow);
        }

        model.setFlows(flows);

        // repaint the map
        mapComponent.repaint();
    }

    private void flowAngleSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_flowAngleSliderStateChanged
        layoutFlows();
    }//GEN-LAST:event_flowAngleSliderStateChanged

    private void flowLengthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_flowLengthSliderStateChanged
        layoutFlows();
    }//GEN-LAST:event_flowLengthSliderStateChanged

    private void cubicCurvesRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cubicCurvesRadioButtonActionPerformed
        model.setCurveType(Model.CurveType.CUBIC);
        layoutFlows();
    }//GEN-LAST:event_cubicCurvesRadioButtonActionPerformed

    private void quadraticCurvesRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quadraticCurvesRadioButtonActionPerformed
        model.setCurveType(Model.CurveType.QUADRATIC);
        layoutFlows();
    }//GEN-LAST:event_quadraticCurvesRadioButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton cubicCurvesRadioButton;
    private javax.swing.ButtonGroup curvesButtonGroup;
    private javax.swing.JMenuItem exportSVGMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JCheckBox fillCheckBox;
    private edu.oregonstate.cartography.flox.gui.ColorButton fillColorButton;
    private javax.swing.JSlider flowAngleSlider;
    private javax.swing.JSlider flowLengthSlider;
    private javax.swing.JFormattedTextField flowScaleFormattedTextField;
    private javax.swing.JPanel flowWidthPanel;
    private javax.swing.JMenuItem importFlowsMenuItem;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private edu.oregonstate.cartography.flox.gui.DraggableList layerList;
    private javax.swing.JScrollPane layerListScrollPane;
    private javax.swing.JPanel leftPanel;
    private edu.oregonstate.cartography.flox.gui.FloxMapComponent mapComponent;
    private javax.swing.JMenu mapMenu;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem openShapefileMenuItem;
    private javax.swing.JRadioButton quadraticCurvesRadioButton;
    private javax.swing.JMenuItem removeAllLayersMenuItem;
    private javax.swing.JMenuItem removeSelectedLayerMenuItem;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JMenuItem showAllMenuItem;
    private javax.swing.JCheckBox strokeCheckBox;
    private edu.oregonstate.cartography.flox.gui.ColorButton strokeColorButton;
    private javax.swing.JPanel symbolPanel;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JMenuItem viewZoomInMenuItem;
    private javax.swing.JMenuItem viewZoomOutMenuItem;
    private javax.swing.JMenuItem zoomOnSelectedLayerMenuItem;
    // End of variables declaration//GEN-END:variables

}
