package com.specularity.printing.ui;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static com.specularity.printing.tuner.preferences;

public class SettingsTab extends Tab {

    private static DecimalFormat df = new DecimalFormat("#");

    public SettingsTab() {
        super("Other Settings");

        df.setMaximumFractionDigits(8);
        df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));

        VBox box = new VBox();

        CheckBox cbPreventLayerChangeOnOuterPerimeter = new CheckBox("Prevent layer change on outer perimeter");
        cbPreventLayerChangeOnOuterPerimeter.setSelected(preferences.get("preventLayerChangeOnOuterPerimeter", "on").equals("on"));
        cbPreventLayerChangeOnOuterPerimeter.selectedProperty().addListener((v, ov, nv) -> preferences.put("preventLayerChangeOnOuterPerimeter", nv ? "on" : "off"));
        cbPreventLayerChangeOnOuterPerimeter.setPadding(new Insets(12,12,5,12));

        CheckBox cbHideOuterPerimeterStartPointInCavity = new CheckBox("Hide outer perimeter start point in cavitiy");
        cbHideOuterPerimeterStartPointInCavity.setSelected(preferences.get("hideOuterPerimeterStartPointInCavity", "on").equals("on"));
        cbHideOuterPerimeterStartPointInCavity.selectedProperty().addListener((v, ov, nv) -> preferences.put("hideOuterPerimeterStartPointInCavity", nv ? "on" : "off"));
        cbHideOuterPerimeterStartPointInCavity.setPadding(new Insets(12,12,5,12));

        CheckBox cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint = new CheckBox("Align inner perimeter start points to outer perimeter start point");
        cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint.setSelected(preferences.get("alignInnerPerimeterStartPointsToOuterPerimeterStartPoint", "on").equals("on"));
        cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint.selectedProperty().addListener((v, ov, nv) -> preferences.put("alignInnerPerimeterStartPointsToOuterPerimeterStartPoint", nv ? "on" : "off"));
        cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint.setPadding(new Insets(12,12,5,12));

        TextField txtMaxAngleBetweenSegments = new TextField();
        HBox hb1 = new HBox(new Label("Maximum angle between segments:\t"), txtMaxAngleBetweenSegments);
        txtMaxAngleBetweenSegments.setText(df.format(preferences.getDouble("maxAngleBetweenSegments", 25.0)));
        txtMaxAngleBetweenSegments.textProperty().addListener((v, ov, nv) -> preferences.putDouble("maxAngleBetweenSegments", Double.valueOf(nv)));
        ((Label) hb1.getChildren().get(0)).setPadding(new Insets(5, 12, 2, 12));
        hb1.setPadding(new Insets(5,0,0,0));

        TextField txtMinExtrusionDistance = new TextField();
        HBox hb2 = new HBox(new Label("Minimum extrusion distance:\t\t\t"), txtMinExtrusionDistance);
        txtMinExtrusionDistance.setText(df.format(preferences.getDouble("minExtrusionDistance", 0.0001)));
        txtMinExtrusionDistance.textProperty().addListener((v, ov, nv) -> preferences.putDouble("minExtrusionDistance", Double.valueOf(nv)));
        ((Label) hb2.getChildren().get(0)).setPadding(new Insets(5, 12, 2, 12));
        hb2.setPadding(new Insets(5,0,0,0));

        box.getChildren().addAll(cbPreventLayerChangeOnOuterPerimeter, cbHideOuterPerimeterStartPointInCavity, cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint, hb1, hb2);

        setContent(box);
    }
}
