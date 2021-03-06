package edu.oregonstate.cartography.flox.gui;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryCollection;
import edu.oregonstate.cartography.flox.model.CSVFlowExporter;
import static edu.oregonstate.cartography.flox.model.CubicBezierFlow.bendCubicFlow;
import edu.oregonstate.cartography.flox.model.Flow;
import edu.oregonstate.cartography.flox.model.FlowImporter;
import edu.oregonstate.cartography.flox.model.ForceLayouter;
import edu.oregonstate.cartography.flox.model.Layer;
import edu.oregonstate.cartography.flox.model.LayoutGrader;
import edu.oregonstate.cartography.flox.model.Model;
import edu.oregonstate.cartography.flox.model.Model.FlowNodeDensity;
import edu.oregonstate.cartography.flox.model.Point;
import static edu.oregonstate.cartography.flox.model.QuadraticBezierFlow.bendQuadraticFlow;
import edu.oregonstate.cartography.flox.model.SVGFlowExporter;
import edu.oregonstate.cartography.flox.model.VectorSymbol;
import edu.oregonstate.cartography.map.AddFlowTool;
import edu.oregonstate.cartography.map.MeasureTool;
import edu.oregonstate.cartography.map.PanTool;
import edu.oregonstate.cartography.map.ScaleMoveSelectionTool;
import edu.oregonstate.cartography.map.ZoomInTool;
import edu.oregonstate.cartography.map.ZoomOutTool;
import edu.oregonstate.cartography.simplefeature.ShapeGeometryImporter;
import edu.oregonstate.cartography.utils.FileUtils;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.xml.bind.JAXBException;

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
     * timer for animated drawing of layout process
     */
    private Timer timer = null;

    /**
     * Undo/redo manager.
     */
    private final Undo undo;

    private boolean updatingGUI = false;

    /**
     * Creates new form MainWindow
     */
    public MainWindow() {

        initComponents();
        progressBar.setVisible(false);

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
        mapComponent.addMouseMotionListener(coordinateInfoPanel);

        mapComponent.requestFocusInWindow();

        updateClippingGUI();

        try {
            this.undo = new Undo(new Model().marshal());
            undo.registerUndoMenuItems(undoMenuItem, redoMenuItem);
        } catch (JAXBException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void byteArrayToModel(byte[] buf) {
        if (buf == null) {
            return;
        }
        try {
            setModel(Model.unmarshal(buf));
        } catch (JAXBException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void registerUndoMenuItems(JMenuItem undoMenuItem, JMenuItem redoMenuItem) {
        undo.registerUndoMenuItems(undoMenuItem, redoMenuItem);
    }

    protected void undo() {
        Object undoData = undo.getUndo();
        if (undoData != null) {
            byteArrayToModel((byte[]) undoData);
        }
    }

    protected void redo() {
        Object undoData = undo.getRedo();
        if (undoData != null) {
            byteArrayToModel((byte[]) undoData);
        }
    }

    private void addUndo(String message) {
        try {
            if (updatingGUI == false){
                undo.add(message, model.marshal());
            }
        } catch (JAXBException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Set the model for this application.
     *
     * @param model
     */
    public void setModel(Model model) {
        assert (model != null);
        this.model = model;
        mapComponent.setModel(model);
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
        writeModelToGUI();
    }

    /**
     * Write the data values from the model to the GUI elements
     */
    private void writeModelToGUI() {
        if (model == null) {
            return;
        }

        updatingGUI = true;
        try {
            // Arrow Settings
            flowDistanceFromEndPointFormattedTextField.setValue(model.getFlowDistanceFromEndPoint());
            addArrowsCheckbox.setSelected(model.isDrawArrows());
            arrowheadSizeSlider.setValue((int) (model.getArrowLengthScaleFactor() * 10));
            arrowheadWidthSlider.setValue((int) (model.getArrowWidthScaleFactor() * 10));
            arrowEdgeCtrlLengthSlider.setValue((int) (model.getArrowEdgeCtrlLength() * 100));
            arrowEdgeCtrlWidthSlider.setValue((int) (model.getArrowEdgeCtrlWidth() * 100));
            arrowCornerPositionSlider.setValue((int) (model.getArrowCornerPosition() * 100));
            arrowSizeRatioSlider.setValue((int) (model.getArrowSizeRatio() * 100));
            maximumFlowWidthSlider.setValue((int) model.getMaxFlowStrokeWidth());
            maximumNodeSizeSlider.setValue((int) model.getMaxNodeSize());

            // Force Settings
            selfForcesCheckBox.setSelected(model.isFlowExertingForcesOnItself());
            enforceRangeboxCheckbox.setSelected(model.isEnforceRangebox());
            longestFlowStiffnessSlider.setValue((int) (model.getMaxFlowLengthSpringConstant() * 100d));
            zeroLengthStiffnessSlider.setValue((int) (model.getMinFlowLengthSpringConstant() * 100d));
            exponentSlider.setValue((int) (model.getDistanceWeightExponent() * 10d));
            nodeWeightSlider.setValue((int) (model.getNodeWeightFactor() * 10d));
            antiTorsionSlider.setValue((int) (model.getAntiTorsionWeight() * 100d));
            peripheralStiffnessSlider.setValue((int) (model.getPeripheralStiffnessFactor() * 100));
            canvasSizeSlider.setValue((int) (model.getCanvasPadding() * 100));
            flowRangeboxSizeSlider.setValue((int) (model.getFlowRangeboxHeight() * 100));
            if (model.getFlowNodeDensity() == FlowNodeDensity.LOW) {
                flowNodeDensityComboBox.setSelectedIndex(0);
            } else if (model.getFlowNodeDensity() == FlowNodeDensity.MEDIUM) {
                flowNodeDensityComboBox.setSelectedIndex(1);
            } else {
                flowNodeDensityComboBox.setSelectedIndex(2);
            }
            updateClippingGUI();
        } finally {
            updatingGUI = false;
        }
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
        flowLayoutPanel = new javax.swing.JPanel();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        flowAngleSlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        flowLengthSlider = new javax.swing.JSlider();
        cubicCurvesRadioButton = new javax.swing.JRadioButton();
        quadraticCurvesRadioButton = new javax.swing.JRadioButton();
        mapToolsButtonGroup = new javax.swing.ButtonGroup();
        importPanel = new javax.swing.JPanel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        pointsFilePathLabel = new javax.swing.JLabel();
        flowsFilePathLabel = new javax.swing.JLabel();
        selectPointsFileButton = new javax.swing.JButton();
        selectFlowsFileButton = new javax.swing.JButton();
        importPanelOKButton = new javax.swing.JButton();
        importPanelCancelButton = new javax.swing.JButton();
        jToolBar1 = new javax.swing.JToolBar();
        jPanel2 = new javax.swing.JPanel();
        arrowToggleButton = new javax.swing.JToggleButton();
        addFlowToggleButton = new javax.swing.JToggleButton();
        zoomInToggleButton = new javax.swing.JToggleButton();
        zoomOutToggleButton = new javax.swing.JToggleButton();
        handToggleButton = new javax.swing.JToggleButton();
        distanceToggleButton = new javax.swing.JToggleButton();
        showAllButton = new javax.swing.JButton();
        coordinateInfoPanel = new edu.oregonstate.cartography.flox.gui.CoordinateInfoPanel();
        mapComponent = new edu.oregonstate.cartography.flox.gui.FloxMapComponent();
        rightPanel = new javax.swing.JPanel();
        controlsTabbedPane = new javax.swing.JTabbedPane();
        forcesPanel = new TransparentMacPanel();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        exponentSlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        longestFlowStiffnessSlider = new javax.swing.JSlider();
        zeroLengthStiffnessSlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
        selfForcesCheckBox = new javax.swing.JCheckBox();
        javax.swing.JLabel jLabel6 = new javax.swing.JLabel();
        nodeWeightSlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel7 = new javax.swing.JLabel();
        antiTorsionSlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel8 = new javax.swing.JLabel();
        peripheralStiffnessSlider = new javax.swing.JSlider();
        enforceRangeboxCheckbox = new javax.swing.JCheckBox();
        canvasSizeSlider = new javax.swing.JSlider();
        jLabel11 = new javax.swing.JLabel();
        flowRangeboxSizeSlider = new javax.swing.JSlider();
        jLabel13 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        lockSelectedFlowsButton = new javax.swing.JButton();
        keepForcesConstantToggleButton = new javax.swing.JToggleButton();
        flowNodeDensityComboBox = new javax.swing.JComboBox();
        mapPanel = new TransparentMacPanel();
        mapControlPanel = new TransparentMacPanel();
        drawControlPointsCheckBox = new javax.swing.JCheckBox();
        drawLineSegmentsCheckBox = new javax.swing.JCheckBox();
        drawReconstructedBezierCheckBox = new javax.swing.JCheckBox();
        javax.swing.JLabel jLabel9 = new javax.swing.JLabel();
        layerListScrollPane = new javax.swing.JScrollPane();
        layerList = new edu.oregonstate.cartography.flox.gui.DraggableList();
        symbolPanel = new TransparentMacPanel();
        fillCheckBox = new javax.swing.JCheckBox();
        strokeCheckBox = new javax.swing.JCheckBox();
        fillColorButton = new edu.oregonstate.cartography.flox.gui.ColorButton();
        strokeColorButton = new edu.oregonstate.cartography.flox.gui.ColorButton();
        drawingOrderComboBox = new javax.swing.JComboBox();
        javax.swing.JLabel jLabel12 = new javax.swing.JLabel();
        drawCanvasPaddingCheckbox = new javax.swing.JCheckBox();
        drawFlowRangeboxCheckbox = new javax.swing.JCheckBox();
        addLayerButton = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        deleteSelectedFeaturesButton = new javax.swing.JButton();
        editFlowValueButton = new javax.swing.JButton();
        reverseFlowDirectionButton = new javax.swing.JButton();
        straightenAllFlowsButton = new javax.swing.JButton();
        lockFlowWidthCheckbox = new javax.swing.JCheckBox();
        maximumFlowWidthSlider = new javax.swing.JSlider();
        jLabel26 = new javax.swing.JLabel();
        maximumNodeSizeSlider = new javax.swing.JSlider();
        jLabel27 = new javax.swing.JLabel();
        editSelectedNodeValueButton = new javax.swing.JButton();
        arrowHeadsPanel = new TransparentMacPanel();
        arrowHeadsControlPanel = new TransparentMacPanel();
        flowDistanceFromEndPointFormattedTextField = new javax.swing.JFormattedTextField();
        jLabel14 = new javax.swing.JLabel();
        addArrowsCheckbox = new javax.swing.JCheckBox();
        arrowheadSizeSlider = new javax.swing.JSlider();
        jLabel10 = new javax.swing.JLabel();
        arrowheadWidthSlider = new javax.swing.JSlider();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        arrowEdgeCtrlLengthSlider = new javax.swing.JSlider();
        arrowEdgeCtrlWidthSlider = new javax.swing.JSlider();
        arrowCornerPositionSlider = new javax.swing.JSlider();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        arrowSizeRatioSlider = new javax.swing.JSlider();
        clipAreaPanel = new TransparentMacPanel();
        clipAreaControlPanel = new TransparentMacPanel();
        javax.swing.JLabel jLabel20 = new javax.swing.JLabel();
        selectEndClipAreaButton = new javax.swing.JButton();
        javax.swing.JLabel jLabel21 = new javax.swing.JLabel();
        endAreasBufferDistanceFormattedTextField = new javax.swing.JFormattedTextField();
        clipWithEndAreasCheckBox = new javax.swing.JCheckBox();
        drawEndClipAreasCheckBox = new javax.swing.JCheckBox();
        javax.swing.JSeparator jSeparator5 = new javax.swing.JSeparator();
        javax.swing.JSeparator jSeparator6 = new javax.swing.JSeparator();
        javax.swing.JLabel jLabel24 = new javax.swing.JLabel();
        startAreasBufferDistanceFormattedTextField = new javax.swing.JFormattedTextField();
        drawStartClipAreasCheckBox = new javax.swing.JCheckBox();
        clipWithStartAreasCheckBox = new javax.swing.JCheckBox();
        progressBarPanel = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        importFlowsMenuItem = new javax.swing.JMenuItem();
        openPointsAndFlowsMenuItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator4 = new javax.swing.JPopupMenu.Separator();
        openSettingsMenuItem = new javax.swing.JMenuItem();
        saveSettingsMenuItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator7 = new javax.swing.JPopupMenu.Separator();
        openShapefileMenuItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator3 = new javax.swing.JPopupMenu.Separator();
        exportSVGMenuItem = new javax.swing.JMenuItem();
        exportCSVMenuItem = new javax.swing.JMenuItem();
        exportImageMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        undoMenuItem = new javax.swing.JMenuItem();
        redoMenuItem = new javax.swing.JMenuItem();
        mapMenu = new javax.swing.JMenu();
        removeAllLayersMenuItem = new javax.swing.JMenuItem();
        removeSelectedLayerMenuItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator1 = new javax.swing.JPopupMenu.Separator();
        geometricLayoutMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        showAllMenuItem = new javax.swing.JMenuItem();
        showAllMenuItem1 = new javax.swing.JMenuItem();
        zoomOnSelectedLayerMenuItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator viewSeparator = new javax.swing.JPopupMenu.Separator();
        viewZoomInMenuItem = new javax.swing.JMenuItem();
        viewZoomOutMenuItem = new javax.swing.JMenuItem();
        infoMenu = new javax.swing.JMenu();
        floxReportMenuItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator2 = new javax.swing.JPopupMenu.Separator();
        infoMenuItem = new javax.swing.JMenuItem();

        flowLayoutPanel.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Flow Angle");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        flowLayoutPanel.add(jLabel1, gridBagConstraints);

        flowAngleSlider.setMajorTickSpacing(45);
        flowAngleSlider.setMaximum(90);
        flowAngleSlider.setMinimum(-90);
        flowAngleSlider.setPaintLabels(true);
        flowAngleSlider.setPaintTicks(true);
        flowAngleSlider.setValue(30);
        flowAngleSlider.setPreferredSize(new java.awt.Dimension(250, 37));
        {
            java.util.Hashtable labels = flowAngleSlider.createStandardLabels(flowAngleSlider.getMajorTickSpacing());
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "\u00b0");
                }
            }
            flowAngleSlider.setLabelTable(labels);
        }
        flowAngleSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                flowAngleSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        flowLayoutPanel.add(flowAngleSlider, gridBagConstraints);

        jLabel2.setText("Flow Length");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        flowLayoutPanel.add(jLabel2, gridBagConstraints);

        flowLengthSlider.setMajorTickSpacing(25);
        flowLengthSlider.setPaintLabels(true);
        flowLengthSlider.setPaintTicks(true);
        flowLengthSlider.setValue(33);
        flowLengthSlider.setPreferredSize(new java.awt.Dimension(190, 37));
        {
            java.util.Hashtable labels = flowLengthSlider.createStandardLabels(flowLengthSlider.getMajorTickSpacing());
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            flowLengthSlider.setLabelTable(labels);
        }
        flowLengthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                flowLengthSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        flowLayoutPanel.add(flowLengthSlider, gridBagConstraints);

        curvesButtonGroup.add(cubicCurvesRadioButton);
        cubicCurvesRadioButton.setText("Cubic Curves");
        cubicCurvesRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cubicCurvesRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        flowLayoutPanel.add(cubicCurvesRadioButton, gridBagConstraints);

        curvesButtonGroup.add(quadraticCurvesRadioButton);
        quadraticCurvesRadioButton.setSelected(true);
        quadraticCurvesRadioButton.setText("Quadratic Curves");
        quadraticCurvesRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quadraticCurvesRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        flowLayoutPanel.add(quadraticCurvesRadioButton, gridBagConstraints);

        importPanel.setLayout(new java.awt.GridBagLayout());

        jLabel22.setText("Points File");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        importPanel.add(jLabel22, gridBagConstraints);

        jLabel23.setText("Flows File");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        importPanel.add(jLabel23, gridBagConstraints);

        pointsFilePathLabel.setFont(pointsFilePathLabel.getFont().deriveFont(pointsFilePathLabel.getFont().getSize()-3f));
        pointsFilePathLabel.setText("�");
        pointsFilePathLabel.setPreferredSize(new java.awt.Dimension(500, 13));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        importPanel.add(pointsFilePathLabel, gridBagConstraints);

        flowsFilePathLabel.setFont(flowsFilePathLabel.getFont().deriveFont(flowsFilePathLabel.getFont().getSize()-3f));
        flowsFilePathLabel.setText("�");
        flowsFilePathLabel.setPreferredSize(new java.awt.Dimension(500, 13));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        importPanel.add(flowsFilePathLabel, gridBagConstraints);

        selectPointsFileButton.setText("Select�");
        selectPointsFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectPointsFileButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        importPanel.add(selectPointsFileButton, gridBagConstraints);

        selectFlowsFileButton.setText("Select�");
        selectFlowsFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectFlowsFileButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        importPanel.add(selectFlowsFileButton, gridBagConstraints);

        importPanelOKButton.setText("OK");
        importPanelOKButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importPanelOKButtonActionPerformed(evt);
            }
        });

        importPanelCancelButton.setText("Cancel");
        importPanelCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importPanelCancelButtonActionPerformed(evt);
            }
        });

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jToolBar1.setRollover(true);

        mapToolsButtonGroup.add(arrowToggleButton);
        arrowToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/oregonstate/cartography/icons/Arrow16x16.gif"))); // NOI18N
        arrowToggleButton.setToolTipText("Select, move and scale objects.");
        arrowToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        arrowToggleButton.setPreferredSize(new java.awt.Dimension(24, 24));
        arrowToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        arrowToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                arrowToggleButtonActionPerformed(evt);
            }
        });
        jPanel2.add(arrowToggleButton);

        mapToolsButtonGroup.add(addFlowToggleButton);
        addFlowToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/oregonstate/cartography/icons/SetPoint16x16.gif"))); // NOI18N
        addFlowToggleButton.setToolTipText("Add Flow");
        addFlowToggleButton.setPreferredSize(new java.awt.Dimension(24, 24));
        addFlowToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFlowToggleButtonActionPerformed(evt);
            }
        });
        jPanel2.add(addFlowToggleButton);

        mapToolsButtonGroup.add(zoomInToggleButton);
        zoomInToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/oregonstate/cartography/icons/ZoomIn16x16.gif"))); // NOI18N
        zoomInToggleButton.setToolTipText("Zoom In");
        zoomInToggleButton.setFocusable(false);
        zoomInToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zoomInToggleButton.setPreferredSize(new java.awt.Dimension(24, 24));
        zoomInToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        zoomInToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomInToggleButtonActionPerformed(evt);
            }
        });
        jPanel2.add(zoomInToggleButton);

        mapToolsButtonGroup.add(zoomOutToggleButton);
        zoomOutToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/oregonstate/cartography/icons/ZoomOut16x16.gif"))); // NOI18N
        zoomOutToggleButton.setToolTipText("Zoom Out");
        zoomOutToggleButton.setFocusable(false);
        zoomOutToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zoomOutToggleButton.setPreferredSize(new java.awt.Dimension(24, 24));
        zoomOutToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        zoomOutToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomOutToggleButtonActionPerformed(evt);
            }
        });
        jPanel2.add(zoomOutToggleButton);

        mapToolsButtonGroup.add(handToggleButton);
        handToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/oregonstate/cartography/icons/Hand16x16.gif"))); // NOI18N
        handToggleButton.setToolTipText("Pan");
        handToggleButton.setFocusable(false);
        handToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        handToggleButton.setPreferredSize(new java.awt.Dimension(24, 24));
        handToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        handToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                handToggleButtonActionPerformed(evt);
            }
        });
        jPanel2.add(handToggleButton);

        mapToolsButtonGroup.add(distanceToggleButton);
        distanceToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/oregonstate/cartography/icons/Ruler16x16.gif"))); // NOI18N
        distanceToggleButton.setToolTipText("Measure Distance and Angle");
        distanceToggleButton.setFocusable(false);
        distanceToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        distanceToggleButton.setPreferredSize(new java.awt.Dimension(24, 24));
        distanceToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        distanceToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                distanceToggleButtonActionPerformed(evt);
            }
        });
        jPanel2.add(distanceToggleButton);

        showAllButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/oregonstate/cartography/icons/ShowAll20x14.png"))); // NOI18N
        showAllButton.setToolTipText("Show All");
        showAllButton.setBorderPainted(false);
        showAllButton.setContentAreaFilled(false);
        showAllButton.setFocusable(false);
        showAllButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        showAllButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        showAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAllButtonActionPerformed(evt);
            }
        });
        jPanel2.add(showAllButton);
        jPanel2.add(coordinateInfoPanel);

        jToolBar1.add(jPanel2);

        getContentPane().add(jToolBar1, java.awt.BorderLayout.NORTH);
        getContentPane().add(mapComponent, java.awt.BorderLayout.CENTER);

        rightPanel.setLayout(new java.awt.BorderLayout());

        forcesPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 10, 10, 10));
        forcesPanel.setLayout(new java.awt.GridBagLayout());

        jLabel3.setText("Stiffness of Longest Flow");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        forcesPanel.add(jLabel3, gridBagConstraints);

        exponentSlider.setMajorTickSpacing(100);
        exponentSlider.setMaximum(500);
        exponentSlider.setPaintLabels(true);
        exponentSlider.setPaintTicks(true);
        exponentSlider.setValue(0);
        exponentSlider.setPreferredSize(new java.awt.Dimension(190, 38));
        exponentSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                exponentSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        forcesPanel.add(exponentSlider, gridBagConstraints);

        jLabel4.setText("Weight Exponent");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        forcesPanel.add(jLabel4, gridBagConstraints);

        longestFlowStiffnessSlider.setMajorTickSpacing(20);
        longestFlowStiffnessSlider.setMinorTickSpacing(10);
        longestFlowStiffnessSlider.setPaintLabels(true);
        longestFlowStiffnessSlider.setPaintTicks(true);
        longestFlowStiffnessSlider.setValue(0);
        longestFlowStiffnessSlider.setPreferredSize(new java.awt.Dimension(190, 38));
        longestFlowStiffnessSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                longestFlowStiffnessSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        forcesPanel.add(longestFlowStiffnessSlider, gridBagConstraints);

        zeroLengthStiffnessSlider.setMajorTickSpacing(100);
        zeroLengthStiffnessSlider.setMaximum(500);
        zeroLengthStiffnessSlider.setMinorTickSpacing(50);
        zeroLengthStiffnessSlider.setPaintLabels(true);
        zeroLengthStiffnessSlider.setPaintTicks(true);
        zeroLengthStiffnessSlider.setValue(0);
        zeroLengthStiffnessSlider.setPreferredSize(new java.awt.Dimension(190, 38));
        zeroLengthStiffnessSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                zeroLengthStiffnessSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        forcesPanel.add(zeroLengthStiffnessSlider, gridBagConstraints);

        jLabel5.setText("Stiffness of Zero-Length Flow");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        forcesPanel.add(jLabel5, gridBagConstraints);

        selfForcesCheckBox.setText("Flows Exert Forces on Themselves");
        selfForcesCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selfForcesCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        forcesPanel.add(selfForcesCheckBox, gridBagConstraints);

        jLabel6.setText("Repulsion of Start and End Nodes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        forcesPanel.add(jLabel6, gridBagConstraints);

        nodeWeightSlider.setMajorTickSpacing(50);
        nodeWeightSlider.setMaximum(200);
        nodeWeightSlider.setMinorTickSpacing(10);
        nodeWeightSlider.setPaintLabels(true);
        nodeWeightSlider.setPaintTicks(true);
        nodeWeightSlider.setValue(0);
        nodeWeightSlider.setPreferredSize(new java.awt.Dimension(190, 38));
        nodeWeightSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                nodeWeightSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        forcesPanel.add(nodeWeightSlider, gridBagConstraints);

        jLabel7.setText("Anti-Torsion Forces");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        forcesPanel.add(jLabel7, gridBagConstraints);

        antiTorsionSlider.setMajorTickSpacing(10);
        antiTorsionSlider.setMinorTickSpacing(5);
        antiTorsionSlider.setPaintLabels(true);
        antiTorsionSlider.setPaintTicks(true);
        antiTorsionSlider.setValue(100);
        antiTorsionSlider.setPreferredSize(new java.awt.Dimension(190, 38));
        antiTorsionSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                antiTorsionSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        forcesPanel.add(antiTorsionSlider, gridBagConstraints);

        jLabel8.setText("Stiffness Factor for Peripheral Flows");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        forcesPanel.add(jLabel8, gridBagConstraints);

        peripheralStiffnessSlider.setMajorTickSpacing(250);
        peripheralStiffnessSlider.setMaximum(1000);
        peripheralStiffnessSlider.setMinorTickSpacing(50);
        peripheralStiffnessSlider.setPaintLabels(true);
        peripheralStiffnessSlider.setPaintTicks(true);
        peripheralStiffnessSlider.setValue(0);
        peripheralStiffnessSlider.setPreferredSize(new java.awt.Dimension(190, 38));
        peripheralStiffnessSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                peripheralStiffnessSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        forcesPanel.add(peripheralStiffnessSlider, gridBagConstraints);

        enforceRangeboxCheckbox.setSelected(true);
        enforceRangeboxCheckbox.setText("Enforce Control Point Range");
        enforceRangeboxCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enforceRangeboxCheckboxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        forcesPanel.add(enforceRangeboxCheckbox, gridBagConstraints);

        canvasSizeSlider.setMajorTickSpacing(10);
        canvasSizeSlider.setPaintLabels(true);
        canvasSizeSlider.setPaintTicks(true);
        canvasSizeSlider.setPreferredSize(new java.awt.Dimension(240, 38));
        canvasSizeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                canvasSizeSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        forcesPanel.add(canvasSizeSlider, gridBagConstraints);

        jLabel11.setText("Canvas Size");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        forcesPanel.add(jLabel11, gridBagConstraints);

        flowRangeboxSizeSlider.setMajorTickSpacing(10);
        flowRangeboxSizeSlider.setMaximum(50);
        flowRangeboxSizeSlider.setPaintLabels(true);
        flowRangeboxSizeSlider.setPaintTicks(true);
        flowRangeboxSizeSlider.setPreferredSize(new java.awt.Dimension(190, 38));
        flowRangeboxSizeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                flowRangeboxSizeSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        forcesPanel.add(flowRangeboxSizeSlider, gridBagConstraints);

        jLabel13.setText("Flow Rangebox Size");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        forcesPanel.add(jLabel13, gridBagConstraints);

        jLabel25.setText("Flow node density");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        forcesPanel.add(jLabel25, gridBagConstraints);

        lockSelectedFlowsButton.setText("Lock/unlock selected flows");
        lockSelectedFlowsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lockSelectedFlowsButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        forcesPanel.add(lockSelectedFlowsButton, gridBagConstraints);

        keepForcesConstantToggleButton.setText("Apply Constant Forces");
        keepForcesConstantToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keepForcesConstantToggleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 23;
        forcesPanel.add(keepForcesConstantToggleButton, gridBagConstraints);

        flowNodeDensityComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Low", "Medium", "High" }));
        flowNodeDensityComboBox.setSelectedIndex(1);
        flowNodeDensityComboBox.setPreferredSize(new java.awt.Dimension(200, 27));
        flowNodeDensityComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                flowNodeDensityComboBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 21;
        forcesPanel.add(flowNodeDensityComboBox, gridBagConstraints);

        controlsTabbedPane.addTab("Forces", forcesPanel);

        mapControlPanel.setLayout(new java.awt.GridBagLayout());

        drawControlPointsCheckBox.setText("Draw Control Points");
        drawControlPointsCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawControlPointsCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        mapControlPanel.add(drawControlPointsCheckBox, gridBagConstraints);

        drawLineSegmentsCheckBox.setText("Draw Line Segments");
        drawLineSegmentsCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawLineSegmentsCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        mapControlPanel.add(drawLineSegmentsCheckBox, gridBagConstraints);

        drawReconstructedBezierCheckBox.setText("Draw Reconstructed B�zier");
        drawReconstructedBezierCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawReconstructedBezierCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        mapControlPanel.add(drawReconstructedBezierCheckBox, gridBagConstraints);

        jLabel9.setText("Map Layers");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        mapControlPanel.add(jLabel9, gridBagConstraints);

        layerListScrollPane.setPreferredSize(new java.awt.Dimension(220, 80));

        layerList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                layerListValueChanged(evt);
            }
        });
        layerListScrollPane.setViewportView(layerList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        mapControlPanel.add(layerListScrollPane, gridBagConstraints);

        symbolPanel.setLayout(new java.awt.GridBagLayout());

        fillCheckBox.setText("Fill");
        fillCheckBox.setEnabled(false);
        fillCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fillCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        symbolPanel.add(fillCheckBox, gridBagConstraints);

        strokeCheckBox.setText("Stroke");
        strokeCheckBox.setEnabled(false);
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

        fillColorButton.setEnabled(false);
        fillColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fillColorButtonActionPerformed(evt);
            }
        });
        symbolPanel.add(fillColorButton, new java.awt.GridBagConstraints());

        strokeColorButton.setEnabled(false);
        strokeColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                strokeColorButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        symbolPanel.add(strokeColorButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        mapControlPanel.add(symbolPanel, gridBagConstraints);

        drawingOrderComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Largest First", "Smallest First", "Unordered" }));
        drawingOrderComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                drawingOrderComboBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        mapControlPanel.add(drawingOrderComboBox, gridBagConstraints);

        jLabel12.setText("Drawing Order");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        mapControlPanel.add(jLabel12, gridBagConstraints);

        drawCanvasPaddingCheckbox.setText("Draw Canvas Padding");
        drawCanvasPaddingCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawCanvasPaddingCheckboxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        mapControlPanel.add(drawCanvasPaddingCheckbox, gridBagConstraints);

        drawFlowRangeboxCheckbox.setText("Draw Flow Rangebox");
        drawFlowRangeboxCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawFlowRangeboxCheckboxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        mapControlPanel.add(drawFlowRangeboxCheckbox, gridBagConstraints);

        addLayerButton.setText("+");
        addLayerButton.setPreferredSize(new java.awt.Dimension(22, 22));
        addLayerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addLayerButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 3);
        mapControlPanel.add(addLayerButton, gridBagConstraints);

        jButton1.setText("-");
        jButton1.setPreferredSize(new java.awt.Dimension(22, 22));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        mapControlPanel.add(jButton1, gridBagConstraints);

        deleteSelectedFeaturesButton.setText("Delete Selected Features");
        deleteSelectedFeaturesButton.setPreferredSize(new java.awt.Dimension(195, 29));
        deleteSelectedFeaturesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteSelectedFeaturesButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.gridwidth = 3;
        mapControlPanel.add(deleteSelectedFeaturesButton, gridBagConstraints);

        editFlowValueButton.setText("Edit Selected Flow Value");
        editFlowValueButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editFlowValueButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 3;
        mapControlPanel.add(editFlowValueButton, gridBagConstraints);

        reverseFlowDirectionButton.setText("Reverse Flow Direction");
        reverseFlowDirectionButton.setPreferredSize(new java.awt.Dimension(195, 29));
        reverseFlowDirectionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reverseFlowDirectionButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 3;
        mapControlPanel.add(reverseFlowDirectionButton, gridBagConstraints);

        straightenAllFlowsButton.setText("Straighten All Flows");
        straightenAllFlowsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                straightenAllFlowsButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.gridwidth = 3;
        mapControlPanel.add(straightenAllFlowsButton, gridBagConstraints);

        lockFlowWidthCheckbox.setText("Lock Layout to Current Scale");
        lockFlowWidthCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lockFlowWidthCheckboxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        mapControlPanel.add(lockFlowWidthCheckbox, gridBagConstraints);

        maximumFlowWidthSlider.setMajorTickSpacing(20);
        maximumFlowWidthSlider.setMinorTickSpacing(10);
        maximumFlowWidthSlider.setPaintLabels(true);
        maximumFlowWidthSlider.setPaintTicks(true);
        maximumFlowWidthSlider.setPreferredSize(new java.awt.Dimension(220, 37));
        maximumFlowWidthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                maximumFlowWidthSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        mapControlPanel.add(maximumFlowWidthSlider, gridBagConstraints);

        jLabel26.setText("Maximum Flow Width");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        mapControlPanel.add(jLabel26, gridBagConstraints);

        maximumNodeSizeSlider.setMajorTickSpacing(1000);
        maximumNodeSizeSlider.setPaintTicks(true);
        maximumNodeSizeSlider.setPreferredSize(new java.awt.Dimension(220, 37));
        maximumNodeSizeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                maximumNodeSizeSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        mapControlPanel.add(maximumNodeSizeSlider, gridBagConstraints);

        jLabel27.setText("Maximum Node Size");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 0);
        mapControlPanel.add(jLabel27, gridBagConstraints);

        editSelectedNodeValueButton.setText("Edit Selected Node Value");
        editSelectedNodeValueButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editSelectedNodeValueButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 3;
        mapControlPanel.add(editSelectedNodeValueButton, gridBagConstraints);

        mapPanel.add(mapControlPanel);

        controlsTabbedPane.addTab("Map", mapPanel);

        arrowHeadsControlPanel.setLayout(new java.awt.GridBagLayout());

        flowDistanceFromEndPointFormattedTextField.setMinimumSize(new java.awt.Dimension(40, 30));
        flowDistanceFromEndPointFormattedTextField.setPreferredSize(new java.awt.Dimension(4, 30));
        flowDistanceFromEndPointFormattedTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                flowDistanceFromEndPointFormattedTextFieldActionPerformed(evt);
            }
        });
        flowDistanceFromEndPointFormattedTextField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                flowDistanceFromEndPointFormattedTextFieldPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 9, 0);
        arrowHeadsControlPanel.add(flowDistanceFromEndPointFormattedTextField, gridBagConstraints);

        jLabel14.setText("Flow Distance From End Point");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        arrowHeadsControlPanel.add(jLabel14, gridBagConstraints);

        addArrowsCheckbox.setText("Add Arrows");
        addArrowsCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addArrowsCheckboxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        arrowHeadsControlPanel.add(addArrowsCheckbox, gridBagConstraints);

        arrowheadSizeSlider.setMajorTickSpacing(50);
        arrowheadSizeSlider.setMaximum(200);
        arrowheadSizeSlider.setPaintLabels(true);
        arrowheadSizeSlider.setPaintTicks(true);
        arrowheadSizeSlider.setPreferredSize(new java.awt.Dimension(240, 43));
        arrowheadSizeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                arrowheadSizeSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        arrowHeadsControlPanel.add(arrowheadSizeSlider, gridBagConstraints);

        jLabel10.setText("Arrowhead Length");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        arrowHeadsControlPanel.add(jLabel10, gridBagConstraints);

        arrowheadWidthSlider.setMajorTickSpacing(25);
        arrowheadWidthSlider.setPaintLabels(true);
        arrowheadWidthSlider.setPaintTicks(true);
        arrowheadWidthSlider.setValue(25);
        arrowheadWidthSlider.setPreferredSize(new java.awt.Dimension(240, 43));
        arrowheadWidthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                arrowheadWidthSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        arrowHeadsControlPanel.add(arrowheadWidthSlider, gridBagConstraints);

        jLabel15.setText("Arrowhead Width");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        arrowHeadsControlPanel.add(jLabel15, gridBagConstraints);

        jLabel16.setText("Arrow Edge Ctrl Point Length");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        arrowHeadsControlPanel.add(jLabel16, gridBagConstraints);

        jLabel17.setText("Arrow Edge Ctrl Point Width");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        arrowHeadsControlPanel.add(jLabel17, gridBagConstraints);

        arrowEdgeCtrlLengthSlider.setMajorTickSpacing(25);
        arrowEdgeCtrlLengthSlider.setPaintLabels(true);
        arrowEdgeCtrlLengthSlider.setPaintTicks(true);
        arrowEdgeCtrlLengthSlider.setPreferredSize(new java.awt.Dimension(240, 43));
        arrowEdgeCtrlLengthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                arrowEdgeCtrlLengthSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        arrowHeadsControlPanel.add(arrowEdgeCtrlLengthSlider, gridBagConstraints);

        arrowEdgeCtrlWidthSlider.setMajorTickSpacing(50);
        arrowEdgeCtrlWidthSlider.setMaximum(200);
        arrowEdgeCtrlWidthSlider.setPaintLabels(true);
        arrowEdgeCtrlWidthSlider.setPaintTicks(true);
        arrowEdgeCtrlWidthSlider.setPreferredSize(new java.awt.Dimension(240, 43));
        arrowEdgeCtrlWidthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                arrowEdgeCtrlWidthSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        arrowHeadsControlPanel.add(arrowEdgeCtrlWidthSlider, gridBagConstraints);

        arrowCornerPositionSlider.setMajorTickSpacing(25);
        arrowCornerPositionSlider.setMaximum(50);
        arrowCornerPositionSlider.setMinimum(-50);
        arrowCornerPositionSlider.setPaintLabels(true);
        arrowCornerPositionSlider.setPaintTicks(true);
        arrowCornerPositionSlider.setValue(0);
        arrowCornerPositionSlider.setPreferredSize(new java.awt.Dimension(240, 43));
        arrowCornerPositionSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                arrowCornerPositionSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        arrowHeadsControlPanel.add(arrowCornerPositionSlider, gridBagConstraints);

        jLabel18.setText("Arrow Corner Position");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        arrowHeadsControlPanel.add(jLabel18, gridBagConstraints);

        jLabel19.setText("Arrow Size Ratio");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 0);
        arrowHeadsControlPanel.add(jLabel19, gridBagConstraints);

        arrowSizeRatioSlider.setMajorTickSpacing(10);
        arrowSizeRatioSlider.setPaintLabels(true);
        arrowSizeRatioSlider.setPaintTicks(true);
        arrowSizeRatioSlider.setValue(0);
        arrowSizeRatioSlider.setPreferredSize(new java.awt.Dimension(240, 43));
        arrowSizeRatioSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                arrowSizeRatioSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        arrowHeadsControlPanel.add(arrowSizeRatioSlider, gridBagConstraints);

        arrowHeadsPanel.add(arrowHeadsControlPanel);

        controlsTabbedPane.addTab("Arrows", arrowHeadsPanel);

        clipAreaControlPanel.setLayout(new java.awt.GridBagLayout());

        jLabel20.setText("Clip Areas");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        clipAreaControlPanel.add(jLabel20, gridBagConstraints);

        selectEndClipAreaButton.setText("Select Shapefile�");
        selectEndClipAreaButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectEndClipAreaButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        clipAreaControlPanel.add(selectEndClipAreaButton, gridBagConstraints);

        jLabel21.setText("End Areas Buffer Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        clipAreaControlPanel.add(jLabel21, gridBagConstraints);

        endAreasBufferDistanceFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.######"))));
        endAreasBufferDistanceFormattedTextField.setValue(0.);
        endAreasBufferDistanceFormattedTextField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                endAreasBufferDistanceFormattedTextFieldPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        clipAreaControlPanel.add(endAreasBufferDistanceFormattedTextField, gridBagConstraints);

        clipWithEndAreasCheckBox.setText("Clip with End Areas");
        clipWithEndAreasCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clipWithEndAreasCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        clipAreaControlPanel.add(clipWithEndAreasCheckBox, gridBagConstraints);

        drawEndClipAreasCheckBox.setText("Draw");
        drawEndClipAreasCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawEndClipAreasCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        clipAreaControlPanel.add(drawEndClipAreasCheckBox, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 12, 0);
        clipAreaControlPanel.add(jSeparator5, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 12, 0);
        clipAreaControlPanel.add(jSeparator6, gridBagConstraints);

        jLabel24.setText("Start Areas Buffer Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        clipAreaControlPanel.add(jLabel24, gridBagConstraints);

        startAreasBufferDistanceFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#,##0.######"))));
        startAreasBufferDistanceFormattedTextField.setValue(0.);
        startAreasBufferDistanceFormattedTextField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                startAreasBufferDistanceFormattedTextFieldPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        clipAreaControlPanel.add(startAreasBufferDistanceFormattedTextField, gridBagConstraints);

        drawStartClipAreasCheckBox.setText("Draw");
        drawStartClipAreasCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawStartClipAreasCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        clipAreaControlPanel.add(drawStartClipAreasCheckBox, gridBagConstraints);

        clipWithStartAreasCheckBox.setText("Clip with Start Areas");
        clipWithStartAreasCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clipWithStartAreasCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        clipAreaControlPanel.add(clipWithStartAreasCheckBox, gridBagConstraints);

        clipAreaPanel.add(clipAreaControlPanel);

        controlsTabbedPane.addTab("Clipping", clipAreaPanel);

        rightPanel.add(controlsTabbedPane, java.awt.BorderLayout.NORTH);

        progressBarPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 5, 10));
        progressBarPanel.setLayout(new javax.swing.BoxLayout(progressBarPanel, javax.swing.BoxLayout.LINE_AXIS));
        progressBarPanel.add(progressBar);

        rightPanel.add(progressBarPanel, java.awt.BorderLayout.SOUTH);

        getContentPane().add(rightPanel, java.awt.BorderLayout.EAST);

        fileMenu.setText("File");

        importFlowsMenuItem.setText("Open Flows�");
        importFlowsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importFlowsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(importFlowsMenuItem);

        openPointsAndFlowsMenuItem.setText("Open Points and Flows�");
        openPointsAndFlowsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openPointsAndFlowsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openPointsAndFlowsMenuItem);
        fileMenu.add(jSeparator4);

        openSettingsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        openSettingsMenuItem.setText("Open Settings�");
        openSettingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openSettingsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openSettingsMenuItem);

        saveSettingsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveSettingsMenuItem.setText("Save Settings�");
        saveSettingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveSettingsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveSettingsMenuItem);
        fileMenu.add(jSeparator7);

        openShapefileMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        openShapefileMenuItem.setText("Add Shapefile Layer�");
        openShapefileMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openShapefileMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openShapefileMenuItem);
        fileMenu.add(jSeparator3);

        exportSVGMenuItem.setText("Export SVG�");
        exportSVGMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSVGMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exportSVGMenuItem);

        exportCSVMenuItem.setText("Export CSV...");
        exportCSVMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportCSVMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exportCSVMenuItem);

        exportImageMenuItem.setText("Export Image�");
        exportImageMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportImageMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exportImageMenuItem);

        menuBar.add(fileMenu);

        editMenu.setText("Edit");

        undoMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        undoMenuItem.setText("Undo");
        undoMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undoMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(undoMenuItem);

        redoMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        redoMenuItem.setText("Redo");
        redoMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                redoMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(redoMenuItem);

        menuBar.add(editMenu);

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
        mapMenu.add(jSeparator1);

        geometricLayoutMenuItem.setText("Geometric Flow Layout�");
        geometricLayoutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                geometricLayoutMenuItemActionPerformed(evt);
            }
        });
        mapMenu.add(geometricLayoutMenuItem);

        menuBar.add(mapMenu);

        viewMenu.setText("View");

        showAllMenuItem.setText("Show All");
        showAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAllMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(showAllMenuItem);

        showAllMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        showAllMenuItem1.setText("Zoom on Flows");
        showAllMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAllMenuItem1ActionPerformed(evt);
            }
        });
        viewMenu.add(showAllMenuItem1);

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

        infoMenu.setText("Info");

        floxReportMenuItem.setText("Report�");
        floxReportMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                floxReportMenuItemActionPerformed(evt);
            }
        });
        infoMenu.add(floxReportMenuItem);
        infoMenu.add(jSeparator2);

        infoMenuItem.setText("Info�");
        infoMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                infoMenuItemActionPerformed(evt);
            }
        });
        infoMenu.add(infoMenuItem);

        menuBar.add(infoMenu);

        setJMenuBar(menuBar);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exportSVGMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSVGMenuItemActionPerformed

        OutputStream outputStream = null;
        try {
            // ask for export file
            String outFilePath = FileUtils.askFile(this, "SVG File", false);
            if (outFilePath == null) {
                // user canceled
                return;
            }
            outFilePath = FileUtils.forceFileNameExtension(outFilePath, "svg");
            SVGFlowExporter exporter = new SVGFlowExporter(model);
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
            String inFilePath = FileUtils.askFile(this, "Shapefile", true);
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
     * Passes flows to the model and initializes the GUI for the flows.
     *
     * @param flows
     * @param filePath
     */
    private void setFlows(ArrayList<Flow> flows, String filePath) {
        if (flows != null) {
            setTitle(FileUtils.getFileNameWithoutExtension(filePath));
            model.setFlows(flows);
            double maxFlowValue = model.getMaxFlowValue();
            flowDistanceFromEndPointFormattedTextField.setValue(model.getFlowDistanceFromEndPoint());
            layout("Load Flows");
            mapComponent.showAll();
        }
    }

    /**
     * Open a CSV file with flows
     */
    public void openFlowsCSVFile() {
        try {
            // ask for import file
            String inFilePath = FileUtils.askFile(this, "CSV Flows File", true);
            if (inFilePath == null) {
                // user canceled
                return;
            }
            ArrayList<Flow> flows = FlowImporter.readFlows(inFilePath);
            setFlows(flows, inFilePath);
        } catch (Exception ex) {
            ErrorDialog.showErrorDialog("The file could not be read.", "Flox Error", ex, null);
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
        mapComponent.eraseBufferImage();
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

    private void removeSelectedLayer() {
        int selectedLayerID = layerList.getSelectedIndex();
        if (selectedLayerID < 0) {
            return;
        }
        model.removeLayer(selectedLayerID);
        updateLayerList();
        layerList.setSelectedIndex(--selectedLayerID);
        writeSymbolGUI();
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
    }

    private void removeSelectedLayerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeSelectedLayerMenuItemActionPerformed
        removeSelectedLayer();
    }//GEN-LAST:event_removeSelectedLayerMenuItemActionPerformed

    private void importFlowsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importFlowsMenuItemActionPerformed
        openFlowsCSVFile();
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
        Rectangle2D.Double bbRect = new Rectangle2D.Double(bb.getMinX(), bb.getMinY(),
                bb.getWidth(), bb.getHeight());
        mapComponent.zoomOnRectangle(bbRect);
    }//GEN-LAST:event_zoomOnSelectedLayerMenuItemActionPerformed

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
                flow = bendCubicFlow(flow, angleDeg, distPerc);
            } else {
                flow = bendQuadraticFlow(flow, angleDeg, distPerc);
            }
            flows.add(flow);
        }

        model.setFlows(flows);

        // repaint the map
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
    }

    private void showReport() {
        ArrayList<Flow> flows = new ArrayList<>();
        Iterator<Flow> iter = model.flowIterator();
        while (iter.hasNext()) {
            Flow flow = iter.next();
            flows.add(flow);
        }
        int nbrIntersections = LayoutGrader.countFlowIntersections(model);
        int nbrFlows = model.getNbrFlows();
        int nbrNodes = model.getNbrNodes();

        StringBuilder sb = new StringBuilder();
        sb.append("Flows: ");
        sb.append(nbrFlows);
        sb.append("\nNodes: ");
        sb.append(nbrNodes);
        sb.append("\nIntersections: ");
        sb.append(nbrIntersections);
        JOptionPane.showMessageDialog(mapComponent, sb.toString(), "Flox", JOptionPane.INFORMATION_MESSAGE);
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

    private void drawControlPointsCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawControlPointsCheckBoxActionPerformed
        mapComponent.setDrawControlPoints(drawControlPointsCheckBox.isSelected());
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
    }//GEN-LAST:event_drawControlPointsCheckBoxActionPerformed

    private void exponentSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_exponentSliderStateChanged
        if (exponentSlider.getValueIsAdjusting() == false) {
            model.setDistanceWeightExponent((double) exponentSlider.getValue() / 10);
            layout("Exponent");
        }
    }//GEN-LAST:event_exponentSliderStateChanged

    private void longestFlowStiffnessSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_longestFlowStiffnessSliderStateChanged
        if (longestFlowStiffnessSlider.getValueIsAdjusting() == false && model != null) {
            model.setMaxFlowLengthSpringConstant(longestFlowStiffnessSlider.getValue() / 100d);
            layout("Flow Stiffness");
        }
    }//GEN-LAST:event_longestFlowStiffnessSliderStateChanged

    private void zeroLengthStiffnessSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_zeroLengthStiffnessSliderStateChanged
        if (zeroLengthStiffnessSlider.getValueIsAdjusting() == false && model != null) {
            model.setMinFlowLengthSpringConstant(zeroLengthStiffnessSlider.getValue() / 100d);
            layout("Zero Length Stiffness");
        }
    }//GEN-LAST:event_zeroLengthStiffnessSliderStateChanged

    private void drawLineSegmentsCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawLineSegmentsCheckBoxActionPerformed
        mapComponent.setDrawLineSegments(drawLineSegmentsCheckBox.isSelected());
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
    }//GEN-LAST:event_drawLineSegmentsCheckBoxActionPerformed

    private void drawReconstructedBezierCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawReconstructedBezierCheckBoxActionPerformed
        mapComponent.setDrawReconstructedBezier(drawReconstructedBezierCheckBox.isSelected());
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
    }//GEN-LAST:event_drawReconstructedBezierCheckBoxActionPerformed

    private void selfForcesCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selfForcesCheckBoxActionPerformed
        if (model != null) {
            model.setFlowExertingForcesOnItself(selfForcesCheckBox.isSelected());
            layout("Self-Forces");
        }

    }//GEN-LAST:event_selfForcesCheckBoxActionPerformed

    private void nodeWeightSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_nodeWeightSliderStateChanged
        if (nodeWeightSlider.getValueIsAdjusting() == false) {
            model.setNodeWeightFactor(nodeWeightSlider.getValue() / 10d + 1d);
            layout("Node Weight");
        }
    }//GEN-LAST:event_nodeWeightSliderStateChanged

    private void antiTorsionSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_antiTorsionSliderStateChanged
        if (antiTorsionSlider.getValueIsAdjusting() == false) {
            model.setAntiTorsionWeight(antiTorsionSlider.getValue() / 100d);
            layout("Anti-Torsion");
        }
    }//GEN-LAST:event_antiTorsionSliderStateChanged

    private void peripheralStiffnessSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_peripheralStiffnessSliderStateChanged
        if (peripheralStiffnessSlider.getValueIsAdjusting() == false) {
            model.setPeripheralStiffnessFactor(peripheralStiffnessSlider.getValue() / 100d);
            layout("Peripheral Stiffness");
        }
    }//GEN-LAST:event_peripheralStiffnessSliderStateChanged

    private void floxReportMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_floxReportMenuItemActionPerformed
        showReport();
    }//GEN-LAST:event_floxReportMenuItemActionPerformed

    private void enforceRangeboxCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enforceRangeboxCheckboxActionPerformed
        if (model != null) {
            model.setEnforceRangebox(enforceRangeboxCheckbox.isSelected());
            layout("Enforce Range Box");
        }

    }//GEN-LAST:event_enforceRangeboxCheckboxActionPerformed

    private void showAllMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showAllMenuItem1ActionPerformed
        mapComponent.zoomOnRectangle(model.getFlowsBoundingBox());
    }//GEN-LAST:event_showAllMenuItem1ActionPerformed

    private void infoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_infoMenuItemActionPerformed
        ProgramInfoPanel.showApplicationInfo(this);
    }//GEN-LAST:event_infoMenuItemActionPerformed

    private void exportImageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportImageMenuItemActionPerformed
        int size = Math.max(mapComponent.getWidth(), mapComponent.getHeight());
        String msg = "Length of longer side in pixels.";
        String input = JOptionPane.showInputDialog(this, msg, size);
        if (input == null) {
            return;
        }
        try {
            size = Math.abs(Integer.parseInt(input));
        } catch (NumberFormatException ex) {
            ErrorDialog.showErrorDialog("Invalid image size.", "Error", ex, this);
            return;
        }
        if (size > 5000) {
            ErrorDialog.showErrorDialog("The entered size is too large.");
            return;
        }
        BufferedImage image = FloxRenderer.renderToImage(model, size, true);
        String filePath = FileUtils.askFile(this, "PNG Image", null, false, "png");
        {
            if (filePath != null) {
                try {
                    filePath = FileUtils.forceFileNameExtension(filePath, "png");
                    ImageIO.write(image, "png", new File(filePath));
                } catch (IOException ex) {
                    msg = "Could not export the image.";
                    ErrorDialog.showErrorDialog(msg, "Error", ex, this);
                }
            }
        }
    }//GEN-LAST:event_exportImageMenuItemActionPerformed

    private void geometricLayoutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_geometricLayoutMenuItemActionPerformed
        String title = "Geometric Layout";
        JOptionPane.showOptionDialog(this, flowLayoutPanel, title,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
    }//GEN-LAST:event_geometricLayoutMenuItemActionPerformed

    private void drawingOrderComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_drawingOrderComboBoxItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            switch (drawingOrderComboBox.getSelectedIndex()) {
                case 0:
                    model.setFlowOrder(Model.FlowOrder.DECREASING);
                    break;
                case 1:
                    model.setFlowOrder(Model.FlowOrder.INCREASING);
                    break;
                case 2:
                    model.setFlowOrder(Model.FlowOrder.UNORDERED);
            }
        }
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
    }//GEN-LAST:event_drawingOrderComboBoxItemStateChanged

    private void drawCanvasPaddingCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawCanvasPaddingCheckboxActionPerformed
        mapComponent.setDrawCanvasPadding(drawCanvasPaddingCheckbox.isSelected());
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
    }//GEN-LAST:event_drawCanvasPaddingCheckboxActionPerformed

    private void canvasSizeSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_canvasSizeSliderStateChanged
        if (mapComponent.isDrawCanvasPadding()) {
            model.setCanvasPadding(canvasSizeSlider.getValue() / 100d);
            mapComponent.eraseBufferImage();
            mapComponent.repaint();
        }
        if (canvasSizeSlider.getValueIsAdjusting() == false) {
            layout("Canvas Size");
        }
    }//GEN-LAST:event_canvasSizeSliderStateChanged

    private void drawFlowRangeboxCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawFlowRangeboxCheckboxActionPerformed
        mapComponent.setDrawFlowRangebox(drawFlowRangeboxCheckbox.isSelected());
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
    }//GEN-LAST:event_drawFlowRangeboxCheckboxActionPerformed

    private void flowRangeboxSizeSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_flowRangeboxSizeSliderStateChanged
        model.setFlowRangeboxHeight(flowRangeboxSizeSlider.getValue() / 100d + 0.01);
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
        if (flowRangeboxSizeSlider.getValueIsAdjusting() == false) {
            layout("Flow Rangebox Size");
        }
    }//GEN-LAST:event_flowRangeboxSizeSliderStateChanged

    private void arrowToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_arrowToggleButtonActionPerformed
        mapComponent.setMapTool(new ScaleMoveSelectionTool(mapComponent, model));
    }//GEN-LAST:event_arrowToggleButtonActionPerformed

    private void zoomInToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomInToggleButtonActionPerformed
        mapComponent.setMapTool(new ZoomInTool(mapComponent));
    }//GEN-LAST:event_zoomInToggleButtonActionPerformed

    private void zoomOutToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomOutToggleButtonActionPerformed
        mapComponent.setMapTool(new ZoomOutTool(mapComponent));
    }//GEN-LAST:event_zoomOutToggleButtonActionPerformed

    private void handToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_handToggleButtonActionPerformed
        mapComponent.setMapTool(new PanTool(mapComponent));
    }//GEN-LAST:event_handToggleButtonActionPerformed

    private void distanceToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_distanceToggleButtonActionPerformed
        MeasureTool tool = new MeasureTool(this.mapComponent);
        tool.addMeasureToolListener(this.coordinateInfoPanel);
        this.mapComponent.setMapTool(tool);
    }//GEN-LAST:event_distanceToggleButtonActionPerformed

    private void showAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showAllButtonActionPerformed
        mapComponent.zoomOnRectangle(model.getFlowsBoundingBox());
    }//GEN-LAST:event_showAllButtonActionPerformed

    private void flowDistanceFromEndPointFormattedTextFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_flowDistanceFromEndPointFormattedTextFieldPropertyChange
        if ("value".equals(evt.getPropertyName()) && model != null) {
            double s = ((Number) flowDistanceFromEndPointFormattedTextField.getValue()).doubleValue();
            model.setFlowDistanceFromEndPoint(s);
            mapComponent.eraseBufferImage();
            mapComponent.repaint();
        }
    }//GEN-LAST:event_flowDistanceFromEndPointFormattedTextFieldPropertyChange

    private void flowDistanceFromEndPointFormattedTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_flowDistanceFromEndPointFormattedTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_flowDistanceFromEndPointFormattedTextFieldActionPerformed

    private void addArrowsCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addArrowsCheckboxActionPerformed
        if (model != null) {
            model.setAddArrows(addArrowsCheckbox.isSelected());
            mapComponent.eraseBufferImage();
            mapComponent.repaint();
        }

    }//GEN-LAST:event_addArrowsCheckboxActionPerformed

    private void arrowheadSizeSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_arrowheadSizeSliderStateChanged
        if (updatingGUI == false && model.isDrawArrows() && model != null) {
            model.setArrowLengthScaleFactor((arrowheadSizeSlider.getValue() + 1) / 10d);
            mapComponent.eraseBufferImage();
            mapComponent.repaint();
            if(!arrowheadSizeSlider.getValueIsAdjusting()) {
                addUndo("Arrow Head Size");
            }
            
        }
    }//GEN-LAST:event_arrowheadSizeSliderStateChanged

    private void arrowheadWidthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_arrowheadWidthSliderStateChanged
        if (model.isDrawArrows()) {
            model.setArrowWidthScaleFactor((arrowheadWidthSlider.getValue() + 1) / 10d);
            mapComponent.eraseBufferImage();
            mapComponent.repaint();
        }
    }//GEN-LAST:event_arrowheadWidthSliderStateChanged

    private void arrowEdgeCtrlLengthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_arrowEdgeCtrlLengthSliderStateChanged
        if (model.isDrawArrows() && model != null) {
            model.setArrowEdgeCtrlLength((arrowEdgeCtrlLengthSlider.getValue()) / 100d);
            mapComponent.eraseBufferImage();
            mapComponent.repaint();
        }
    }//GEN-LAST:event_arrowEdgeCtrlLengthSliderStateChanged

    private void arrowEdgeCtrlWidthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_arrowEdgeCtrlWidthSliderStateChanged
        if (model.isDrawArrows() && model != null) {
            model.setArrowEdgeCtrlWidth((arrowEdgeCtrlWidthSlider.getValue()) / 100d);
            mapComponent.eraseBufferImage();
            mapComponent.repaint();
        }
    }//GEN-LAST:event_arrowEdgeCtrlWidthSliderStateChanged

    private void arrowCornerPositionSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_arrowCornerPositionSliderStateChanged
        if (model.isDrawArrows() && model != null) {
            model.setArrowCornerPosition((arrowCornerPositionSlider.getValue()) / 100d);
            mapComponent.eraseBufferImage();
            mapComponent.repaint();
        }
    }//GEN-LAST:event_arrowCornerPositionSliderStateChanged

    private void updateClippingGUI() {
        boolean hasClipAreas = model != null && model.getFlows().size() > 0 && model.hasClipAreas();
        boolean clipStart = clipWithStartAreasCheckBox.isSelected();
        boolean clipEnd = clipWithEndAreasCheckBox.isSelected();
        clipWithEndAreasCheckBox.setEnabled(hasClipAreas);
        endAreasBufferDistanceFormattedTextField.setEnabled(hasClipAreas && clipEnd);
        drawEndClipAreasCheckBox.setEnabled(hasClipAreas && clipEnd);
        clipWithStartAreasCheckBox.setEnabled(hasClipAreas);
        startAreasBufferDistanceFormattedTextField.setEnabled(hasClipAreas && clipStart);
        drawStartClipAreasCheckBox.setEnabled(hasClipAreas && clipStart);
    }

    private void selectEndClipAreaButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectEndClipAreaButtonActionPerformed
        try {
            // ask for import file
            String inFilePath = FileUtils.askFile(this, "Shapefile", true);
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

            model.setClipAreas(collection);
            updateClippingGUI();
        } catch (IOException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            ErrorDialog.showErrorDialog("An error occured.", "Flox Error", ex, null);
        } finally {
            writeSymbolGUI();
        }
    }//GEN-LAST:event_selectEndClipAreaButtonActionPerformed

    private void clipWithEndAreasCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clipWithEndAreasCheckBoxActionPerformed
        if (clipWithEndAreasCheckBox.isSelected()) {
            model.updateEndClipAreas();
        } else {
            model.removeEndClipAreasFromFlows();
        }
        layout("Clip with End Areas");
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
        updateClippingGUI();
    }//GEN-LAST:event_clipWithEndAreasCheckBoxActionPerformed

    private void endAreasBufferDistanceFormattedTextFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_endAreasBufferDistanceFormattedTextFieldPropertyChange
        if ("value".equals(evt.getPropertyName())) {
            double d = ((Number) endAreasBufferDistanceFormattedTextField.getValue()).doubleValue();
            model.setEndClipAreaBufferDistance(d);
            layout("Buffered Distance");
            mapComponent.eraseBufferImage();
            mapComponent.repaint();
        }
    }//GEN-LAST:event_endAreasBufferDistanceFormattedTextFieldPropertyChange

    protected JOptionPane getOptionPane(JComponent parent) {
        JOptionPane pane = null;
        if (!(parent instanceof JOptionPane)) {
            pane = getOptionPane((JComponent) parent.getParent());
        } else {
            pane = (JOptionPane) parent;
        }
        return pane;
    }

    private void openPointsAndFlowsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openPointsAndFlowsMenuItemActionPerformed
        String title = "Open Points and Flows";
        importPanelOKButton.setEnabled(false);
        pointsFilePathLabel.setText("�");
        flowsFilePathLabel.setText("�");
        // http://stackoverflow.com/questions/14334931/disable-ok-button-on-joptionpane-dialog-until-user-gives-an-input/14335083#14335083
        int res = JOptionPane.showOptionDialog(
                this,
                importPanel,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{importPanelOKButton, importPanelCancelButton},
                importPanelOKButton);
        if (res != 0) {
            return;
        }
        try {
            String pointsFilePath = pointsFilePathLabel.getText();
            String flowsFilePath = flowsFilePathLabel.getText();
            ArrayList<Flow> flows = FlowImporter.readFlows(pointsFilePath, flowsFilePath);
            setFlows(flows, flowsFilePath);
        } catch (Exception ex) {
            ErrorDialog.showErrorDialog("The flows could not be imported.", "Flox Error", ex, this);
        }
    }//GEN-LAST:event_openPointsAndFlowsMenuItemActionPerformed

    private void selectPointsFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectPointsFileButtonActionPerformed
        String filePath = FileUtils.askFile(this, "Points File (CSV)", true);
        if (filePath == null) {
            // user canceled
            return;
        }
        // abusing JLabel to store user input
        pointsFilePathLabel.setText(filePath);
        importPanelOKButton.setEnabled(flowsFilePathLabel.getText().length() > 2);
    }//GEN-LAST:event_selectPointsFileButtonActionPerformed

    private void selectFlowsFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectFlowsFileButtonActionPerformed
        String filePath = FileUtils.askFile(this, "Flows File (CSV)", true);
        if (filePath == null) {
            // user canceled
            return;
        }
        // abusing JLabel to store user input
        flowsFilePathLabel.setText(filePath);
        importPanelOKButton.setEnabled(pointsFilePathLabel.getText().length() > 2);
    }//GEN-LAST:event_selectFlowsFileButtonActionPerformed

    private void importPanelOKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importPanelOKButtonActionPerformed
        JOptionPane pane = getOptionPane((JComponent) evt.getSource());
        pane.setValue(importPanelOKButton);
    }//GEN-LAST:event_importPanelOKButtonActionPerformed

    private void importPanelCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importPanelCancelButtonActionPerformed
        JOptionPane pane = getOptionPane((JComponent) evt.getSource());
        pane.setValue(importPanelCancelButton);
    }//GEN-LAST:event_importPanelCancelButtonActionPerformed

    private void drawEndClipAreasCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawEndClipAreasCheckBoxActionPerformed
        mapComponent.setDrawEndClipAreas(drawEndClipAreasCheckBox.isSelected());
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
    }//GEN-LAST:event_drawEndClipAreasCheckBoxActionPerformed

    private void startAreasBufferDistanceFormattedTextFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_startAreasBufferDistanceFormattedTextFieldPropertyChange
        if ("value".equals(evt.getPropertyName())) {
            double d = ((Number) startAreasBufferDistanceFormattedTextField.getValue()).doubleValue();
            model.setStartClipAreaBufferDistance(d);
            layout("Buffer Distance");
            mapComponent.eraseBufferImage();
            mapComponent.repaint();
        }
    }//GEN-LAST:event_startAreasBufferDistanceFormattedTextFieldPropertyChange

    private void drawStartClipAreasCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawStartClipAreasCheckBoxActionPerformed
        mapComponent.setDrawStartClipAreas(drawStartClipAreasCheckBox.isSelected());
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
    }//GEN-LAST:event_drawStartClipAreasCheckBoxActionPerformed

    private void clipWithStartAreasCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clipWithStartAreasCheckBoxActionPerformed
        if (clipWithStartAreasCheckBox.isSelected()) {
            model.updateStartClipAreas();
        } else {
            model.removeStartClipAreasFromFlows();
        }
        layout("Clipe with Start Areas");
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
        updateClippingGUI();
    }//GEN-LAST:event_clipWithStartAreasCheckBoxActionPerformed

    private void arrowSizeRatioSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_arrowSizeRatioSliderStateChanged
        if (model.isDrawArrows() && model != null) {
            model.setArrowSizeRatio((arrowSizeRatioSlider.getValue()) / 100d);
            mapComponent.eraseBufferImage();
            mapComponent.repaint();
        }
    }//GEN-LAST:event_arrowSizeRatioSliderStateChanged

    private void addLayerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addLayerButtonActionPerformed
        openShapefile();
    }//GEN-LAST:event_addLayerButtonActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        removeSelectedLayer();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void openSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openSettingsMenuItemActionPerformed
        String filePath = FileUtils.askFile(null, "Load XML Settings", null, true, "xml");
        if (filePath != null) {
            try {
                Model newModel = Model.unmarshal(filePath);
                model.copyTransientFields(newModel);
                setModel(newModel);
                mapComponent.zoomOnRectangle(model.getFlowsBoundingBox());
            } catch (JAXBException | FileNotFoundException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                String msg = "Could not load file";
                String title = "Flox Error";
                ErrorDialog.showErrorDialog(msg, title, ex, rootPane);
            }
        }
    }//GEN-LAST:event_openSettingsMenuItemActionPerformed

    private void saveSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveSettingsMenuItemActionPerformed
        String filePath = FileUtils.askFile(null, "Save XML Settings", null, false, "xml");
        if (filePath == null) {
            return;
        }
        File file = new File(filePath);
        try {
            model.marshal(file.getAbsolutePath());
        } catch (JAXBException | FileNotFoundException ex) {
            String msg = "Could not save file";
            String title = "Flox Error";
            ErrorDialog.showErrorDialog(msg, title, ex, rootPane);
        }
    }//GEN-LAST:event_saveSettingsMenuItemActionPerformed

    private void lockSelectedFlowsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lockSelectedFlowsButtonActionPerformed
        // iterate through flows in the model. If it is selected, lock it.
        Iterator flows = model.flowIterator();
        while (flows.hasNext()) {
            Flow flow = (Flow) flows.next();
            if (flow.isSelected()) {
                if (flow.isLocked()) {
                    flow.setLocked(false);
                } else {
                    flow.setLocked(true);
                }
            }
        }
        mapComponent.repaint();
    }//GEN-LAST:event_lockSelectedFlowsButtonActionPerformed

    private void exportCSVMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportCSVMenuItemActionPerformed
        try {
            // ask for export file
            String outFilePath = FileUtils.askFile(this, "CSV File", false);
            if (outFilePath == null) {
                // user canceled
                return;
            }
            outFilePath = FileUtils.forceFileNameExtension(outFilePath, "csv");
            CSVFlowExporter.export(outFilePath, model.flowIterator());
        } catch (IOException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_exportCSVMenuItemActionPerformed

    private void addFlowToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFlowToggleButtonActionPerformed
        mapComponent.setMapTool(new AddFlowTool(mapComponent, model));
    }//GEN-LAST:event_addFlowToggleButtonActionPerformed

    private void deleteSelectedFeaturesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteSelectedFeaturesButtonActionPerformed
        ArrayList<Flow> flowsToRemove = new ArrayList<>();
        ArrayList<Point> nodesToRemove = new ArrayList<>();

        Iterator flows = model.flowIterator();
        while (flows.hasNext()) {
            Flow flow = (Flow) flows.next();
            if (flow.isSelected()) {
                flowsToRemove.add(flow);
            }
        }

        Iterator nodes = model.nodeIterator();
        while (nodes.hasNext()) {
            Point node = (Point) nodes.next();
            if (node.isSelected()) {
                nodesToRemove.add(node);
            }
        }

        flowsToRemove.stream().forEach((flow) -> {
            model.deleteFlow(flow);
        });

        nodesToRemove.stream().forEach((node) -> {
            model.deleteNode(node);
        });

        mapComponent.eraseBufferImage();
        mapComponent.repaint();
    }//GEN-LAST:event_deleteSelectedFeaturesButtonActionPerformed

    private void editFlowValueButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editFlowValueButtonActionPerformed

        if (model.isFlowIsSelected()) {
            ArrayList<Flow> selectedFlows = new ArrayList<>(model.getSelectedFlows());
            double newValue = 0;
            if (selectedFlows.size() == 1) {
                double value = selectedFlows.get(0).getValue();
                while (newValue <= 0) {
                    String userInput = (String) JOptionPane.showInputDialog(
                            this,
                            "Current Flow Value: " + value + "\n"
                            + "Input a new numerical value over zero");
                    try {
                        newValue = Double.parseDouble(userInput);
                        selectedFlows.get(0).setValue(newValue);
                    } catch (NumberFormatException nfe) {
                        newValue = 0;
                    }
                }
                mapComponent.eraseBufferImage();
                mapComponent.repaint();
            } else {
                while (newValue <= 0) {
                    String userInput = (String) JOptionPane.showInputDialog(
                            this,
                            "Input a new numerical value over zero");
                    try {
                        newValue = Double.parseDouble(userInput);
                        for (Flow flow : selectedFlows) {
                            flow.setValue(newValue);
                        }
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(this, "Please enter a "
                                + "number over 0");
                    }
                }
                mapComponent.eraseBufferImage();
                mapComponent.repaint();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select one flow");
        }

    }//GEN-LAST:event_editFlowValueButtonActionPerformed

    private void reverseFlowDirectionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reverseFlowDirectionButtonActionPerformed
        ArrayList<Flow> flows = model.getSelectedFlows();
        for (Flow flow : flows) {
            flow.reverseFlow();
        }

        mapComponent.eraseBufferImage();
        mapComponent.repaint();
    }//GEN-LAST:event_reverseFlowDirectionButtonActionPerformed

    private void keepForcesConstantToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keepForcesConstantToggleButtonActionPerformed
        layout("Keep Forces Constant");
    }//GEN-LAST:event_keepForcesConstantToggleButtonActionPerformed

    private void straightenAllFlowsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_straightenAllFlowsButtonActionPerformed
        ForceLayouter layouter = new ForceLayouter(model);
        layouter.straightenFlows();

        mapComponent.eraseBufferImage();
        mapComponent.repaint();
    }//GEN-LAST:event_straightenAllFlowsButtonActionPerformed

    private void flowNodeDensityComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_flowNodeDensityComboBoxItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            switch (flowNodeDensityComboBox.getSelectedIndex()) {
                case 0:
                    model.setFlowNodeDensity(FlowNodeDensity.LOW);
                    break;
                case 1:
                    model.setFlowNodeDensity(FlowNodeDensity.MEDIUM);
                    break;
                case 2:
                    model.setFlowNodeDensity(FlowNodeDensity.HIGH);
            }
        }
        mapComponent.eraseBufferImage();
        mapComponent.repaint();
    }//GEN-LAST:event_flowNodeDensityComboBoxItemStateChanged

    private void undoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_undoMenuItemActionPerformed
        undo();
    }//GEN-LAST:event_undoMenuItemActionPerformed

    private void redoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_redoMenuItemActionPerformed
        redo();
    }//GEN-LAST:event_redoMenuItemActionPerformed

    private void lockFlowWidthCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lockFlowWidthCheckboxActionPerformed
        // Checking?
        if(lockFlowWidthCheckbox.isSelected()) {
            // Set locked to true, pass current scale to model
            model.setFlowWidthLocked(true);
            model.setLockedMapScale(mapComponent.getScale());
        } else {
            model.setFlowWidthLocked(false);
            mapComponent.eraseBufferImage();
            mapComponent.repaint();
        }
            
        // unchecking? 
            // Set locked to false, pass nothing!
    }//GEN-LAST:event_lockFlowWidthCheckboxActionPerformed

    private void maximumFlowWidthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_maximumFlowWidthSliderStateChanged
        if (updatingGUI == false && model != null) {
            model.setMaxFlowStrokeWidth(maximumFlowWidthSlider.getValue());
            mapComponent.eraseBufferImage();
            mapComponent.repaint();
            if(!maximumFlowWidthSlider.getValueIsAdjusting()) {
                addUndo("Flow Width");
            }
        }
    }//GEN-LAST:event_maximumFlowWidthSliderStateChanged

    private void maximumNodeSizeSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_maximumNodeSizeSliderStateChanged
        if (updatingGUI == false && model != null) {
            model.setMaxNodeSize(maximumNodeSizeSlider.getValue());
            mapComponent.eraseBufferImage();
            mapComponent.repaint();
            if(!maximumNodeSizeSlider.getValueIsAdjusting()) {
                addUndo("Node Size");
            }
        }
    }//GEN-LAST:event_maximumNodeSizeSliderStateChanged

    private void editSelectedNodeValueButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editSelectedNodeValueButtonActionPerformed
        if (model.isNodeIsSelected()) {
            ArrayList<Point> selectedNodes = new ArrayList<>(model.getSelectedNodes());
            double newValue = 0;
            if (selectedNodes.size() == 1) {
                double value = selectedNodes.get(0).getValue();
                while (newValue <= 0) {
                    String userInput = (String) JOptionPane.showInputDialog(
                            this,
                            "Current Node Value: " + value + "\n"
                            + "Input a new number over zero");
                    try {
                        newValue = Double.parseDouble(userInput);
                        selectedNodes.get(0).setValue(newValue);
                    } catch (NumberFormatException nfe) {
                        newValue = 0;
                    }
                }
                mapComponent.eraseBufferImage();
                mapComponent.repaint();
            } else {
                while (newValue <= 0) {
                    String userInput = (String) JOptionPane.showInputDialog(
                            this,
                            "Input a new numerical value over zero");
                    try {
                        newValue = Double.parseDouble(userInput);
                        for (Point node : selectedNodes) {
                            node.setValue(newValue);
                        }
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(this, "Please enter a "
                                + "number over 0");
                    }
                }
                mapComponent.eraseBufferImage();
                mapComponent.repaint();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select one node");
        }
    }//GEN-LAST:event_editSelectedNodeValueButtonActionPerformed

    private void layout(String undoString) {
        if (updatingGUI) {
            return;
        }
        addUndo(undoString);

        // If there are no flows, exit the method.
        if (model.getNbrFlows() == 0) {
            return;
        }

        // If the flows are not QuadraticBezierFlows, exit the method.
        if (model.getCurveType() != Model.CurveType.QUADRATIC) {
            String msg = "Please switch to quadratic B�zier curves first.\nUse Map > Geometric Flow Layout�";
            ErrorDialog.showErrorDialog(msg, "Flow Error", null, this);
            return;
        }

        ForceLayouter layouter = new ForceLayouter(model);

        if (!keepForcesConstantToggleButton.isSelected()) {
            layouter.straightenFlows();
        }

        model.setCanvas(model.getFlowsBoundingBox());

        if (timer != null) {
            timer.stop();
        }
        progressBar.setVisible(true);
        LayoutActionListener listener = new LayoutActionListener(layouter,
                keepForcesConstantToggleButton.isSelected());
        timer = new Timer(0, listener);
        timer.start();
    }

    class LayoutActionListener implements ActionListener {

        private int NBR_ITERATIONS;
        private final ForceLayouter layouter;
        private int counter = 0;
        private boolean constant = false;
        double weight;

        public LayoutActionListener(ForceLayouter layouter, boolean constant) {
            this.layouter = layouter;
            if (constant) {
                NBR_ITERATIONS = 100000;
                weight = 0.75;
            } else {
                NBR_ITERATIONS = 100;
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            mapComponent.eraseBufferImage();

            //layouter.layoutAllFlows(1 - counter / (double) NBR_ITERATIONS);
            if (constant) {
                layouter.layoutAllFlows(weight);
            } else {
                layouter.layoutAllFlows(1 - counter / (double) NBR_ITERATIONS);
            }

            mapComponent.repaint();
            if (++counter > NBR_ITERATIONS) {
                timer.stop();
                progressBar.setVisible(false);
            }
            progressBar.setValue(counter);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox addArrowsCheckbox;
    private javax.swing.JToggleButton addFlowToggleButton;
    private javax.swing.JButton addLayerButton;
    private javax.swing.JSlider antiTorsionSlider;
    private javax.swing.JSlider arrowCornerPositionSlider;
    private javax.swing.JSlider arrowEdgeCtrlLengthSlider;
    private javax.swing.JSlider arrowEdgeCtrlWidthSlider;
    private javax.swing.JPanel arrowHeadsControlPanel;
    private javax.swing.JPanel arrowHeadsPanel;
    private javax.swing.JSlider arrowSizeRatioSlider;
    private javax.swing.JToggleButton arrowToggleButton;
    private javax.swing.JSlider arrowheadSizeSlider;
    private javax.swing.JSlider arrowheadWidthSlider;
    private javax.swing.JSlider canvasSizeSlider;
    private javax.swing.JPanel clipAreaControlPanel;
    private javax.swing.JPanel clipAreaPanel;
    private javax.swing.JCheckBox clipWithEndAreasCheckBox;
    private javax.swing.JCheckBox clipWithStartAreasCheckBox;
    private javax.swing.JTabbedPane controlsTabbedPane;
    private edu.oregonstate.cartography.flox.gui.CoordinateInfoPanel coordinateInfoPanel;
    private javax.swing.JRadioButton cubicCurvesRadioButton;
    private javax.swing.ButtonGroup curvesButtonGroup;
    private javax.swing.JButton deleteSelectedFeaturesButton;
    private javax.swing.JToggleButton distanceToggleButton;
    private javax.swing.JCheckBox drawCanvasPaddingCheckbox;
    private javax.swing.JCheckBox drawControlPointsCheckBox;
    private javax.swing.JCheckBox drawEndClipAreasCheckBox;
    private javax.swing.JCheckBox drawFlowRangeboxCheckbox;
    private javax.swing.JCheckBox drawLineSegmentsCheckBox;
    private javax.swing.JCheckBox drawReconstructedBezierCheckBox;
    private javax.swing.JCheckBox drawStartClipAreasCheckBox;
    private javax.swing.JComboBox drawingOrderComboBox;
    private javax.swing.JButton editFlowValueButton;
    private javax.swing.JMenu editMenu;
    private javax.swing.JButton editSelectedNodeValueButton;
    private javax.swing.JFormattedTextField endAreasBufferDistanceFormattedTextField;
    private javax.swing.JCheckBox enforceRangeboxCheckbox;
    private javax.swing.JSlider exponentSlider;
    private javax.swing.JMenuItem exportCSVMenuItem;
    private javax.swing.JMenuItem exportImageMenuItem;
    private javax.swing.JMenuItem exportSVGMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JCheckBox fillCheckBox;
    private edu.oregonstate.cartography.flox.gui.ColorButton fillColorButton;
    private javax.swing.JSlider flowAngleSlider;
    private javax.swing.JFormattedTextField flowDistanceFromEndPointFormattedTextField;
    private javax.swing.JPanel flowLayoutPanel;
    private javax.swing.JSlider flowLengthSlider;
    private javax.swing.JComboBox flowNodeDensityComboBox;
    private javax.swing.JSlider flowRangeboxSizeSlider;
    private javax.swing.JLabel flowsFilePathLabel;
    private javax.swing.JMenuItem floxReportMenuItem;
    private javax.swing.JPanel forcesPanel;
    private javax.swing.JMenuItem geometricLayoutMenuItem;
    private javax.swing.JToggleButton handToggleButton;
    private javax.swing.JMenuItem importFlowsMenuItem;
    private javax.swing.JPanel importPanel;
    private javax.swing.JButton importPanelCancelButton;
    private javax.swing.JButton importPanelOKButton;
    private javax.swing.JMenu infoMenu;
    private javax.swing.JMenuItem infoMenuItem;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToggleButton keepForcesConstantToggleButton;
    private edu.oregonstate.cartography.flox.gui.DraggableList layerList;
    private javax.swing.JScrollPane layerListScrollPane;
    private javax.swing.JCheckBox lockFlowWidthCheckbox;
    private javax.swing.JButton lockSelectedFlowsButton;
    private javax.swing.JSlider longestFlowStiffnessSlider;
    private edu.oregonstate.cartography.flox.gui.FloxMapComponent mapComponent;
    private javax.swing.JPanel mapControlPanel;
    private javax.swing.JMenu mapMenu;
    private javax.swing.JPanel mapPanel;
    private javax.swing.ButtonGroup mapToolsButtonGroup;
    private javax.swing.JSlider maximumFlowWidthSlider;
    private javax.swing.JSlider maximumNodeSizeSlider;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JSlider nodeWeightSlider;
    private javax.swing.JMenuItem openPointsAndFlowsMenuItem;
    private javax.swing.JMenuItem openSettingsMenuItem;
    private javax.swing.JMenuItem openShapefileMenuItem;
    private javax.swing.JSlider peripheralStiffnessSlider;
    private javax.swing.JLabel pointsFilePathLabel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JPanel progressBarPanel;
    private javax.swing.JRadioButton quadraticCurvesRadioButton;
    private javax.swing.JMenuItem redoMenuItem;
    private javax.swing.JMenuItem removeAllLayersMenuItem;
    private javax.swing.JMenuItem removeSelectedLayerMenuItem;
    private javax.swing.JButton reverseFlowDirectionButton;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JMenuItem saveSettingsMenuItem;
    private javax.swing.JButton selectEndClipAreaButton;
    private javax.swing.JButton selectFlowsFileButton;
    private javax.swing.JButton selectPointsFileButton;
    private javax.swing.JCheckBox selfForcesCheckBox;
    private javax.swing.JButton showAllButton;
    private javax.swing.JMenuItem showAllMenuItem;
    private javax.swing.JMenuItem showAllMenuItem1;
    private javax.swing.JFormattedTextField startAreasBufferDistanceFormattedTextField;
    private javax.swing.JButton straightenAllFlowsButton;
    private javax.swing.JCheckBox strokeCheckBox;
    private edu.oregonstate.cartography.flox.gui.ColorButton strokeColorButton;
    private javax.swing.JPanel symbolPanel;
    private javax.swing.JMenuItem undoMenuItem;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JMenuItem viewZoomInMenuItem;
    private javax.swing.JMenuItem viewZoomOutMenuItem;
    private javax.swing.JSlider zeroLengthStiffnessSlider;
    private javax.swing.JToggleButton zoomInToggleButton;
    private javax.swing.JMenuItem zoomOnSelectedLayerMenuItem;
    private javax.swing.JToggleButton zoomOutToggleButton;
    // End of variables declaration//GEN-END:variables

}
