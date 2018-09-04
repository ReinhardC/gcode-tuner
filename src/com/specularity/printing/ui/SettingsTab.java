package com.specularity.printing.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
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
        cbPreventLayerChangeOnOuterPerimeter.setTooltip(new Tooltip("Prevent layer change on outer perimeter.\n\nSome slicers perform a layer change on the outer perimeter which might create a blob. This setting can prevent that.\n\nDefault is ON."));
        cbPreventLayerChangeOnOuterPerimeter.setSelected(preferences.get("preventLayerChangeOnOuterPerimeter", "on").equals("on"));
        cbPreventLayerChangeOnOuterPerimeter.selectedProperty().addListener((v, ov, nv) -> preferences.put("preventLayerChangeOnOuterPerimeter", nv ? "on" : "off"));
        cbPreventLayerChangeOnOuterPerimeter.setPadding(new Insets(12, 12, 5, 12));

        CheckBox cbHideOuterPerimeterStartPointInCavity = new CheckBox("Hide outer perimeter start point in cavity");
        Tooltip ttHideOuter = new Tooltip("Shift outer perimeter so the start point is hidden inside a cavity.\n\nIn case a suitable cavity was found, this removes the need to tweak that perimeter\n\nDefault is ON.");
        cbHideOuterPerimeterStartPointInCavity.setTooltip(ttHideOuter);
        cbHideOuterPerimeterStartPointInCavity.setSelected(preferences.get("hideOuterPerimeterStartPointInCavity", "on").equals("on"));
        cbHideOuterPerimeterStartPointInCavity.selectedProperty().addListener((v, ov, nv) -> preferences.put("hideOuterPerimeterStartPointInCavity", nv ? "on" : "off"));
        cbHideOuterPerimeterStartPointInCavity.setPadding(new Insets(12, 12, 5, 12));

        CheckBox cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint = new CheckBox("Align inner perimeter start points to outer perimeter start point");
        cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint.setTooltip(new Tooltip("Shift inner perimeters so their start points align with the outer perimeter start point.\n\nIf all perimeters align well so will the applied tweaks.\n\nDefault is ON."));
        cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint.getTooltip().setAutoHide(false);
        cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint.setSelected(preferences.get("alignInnerPerimeterStartPointsToOuterPerimeterStartPoint", "on").equals("on"));
        cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint.selectedProperty().addListener((v, ov, nv) -> preferences.put("alignInnerPerimeterStartPointsToOuterPerimeterStartPoint", nv ? "on" : "off"));
        cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint.setPadding(new Insets(12, 12, 5, 12));

        CheckBox cbOnlyIfCavityFound = new CheckBox("only if cavity found");
        cbOnlyIfCavityFound.setSelected(preferences.get("onlyIfCavityFound", "off").equals("on"));
        cbOnlyIfCavityFound.setTooltip(new Tooltip("Shift inner perimeters only if outer perimeter start point was shifted into a cavity\n\nSome slicers have the start points of all perimeters aligned aleady which could deliver a better result than the built-in shifting.\nIn that case it is better to turn aligning off unless the outer perimeter has been shifted anyway.\n\nDefault is OFF."));
        cbOnlyIfCavityFound.selectedProperty().addListener((v, ov, nv) -> preferences.put("onlyIfCavityFound", nv ? "on" : "off"));
        cbOnlyIfCavityFound.setPadding(new Insets(16, 12, 5, 12));
        HBox hbOnlyIfCavityFound = new HBox(new Label("\t"), cbOnlyIfCavityFound);

        VBox btp1content = new VBox(cbPreventLayerChangeOnOuterPerimeter, cbHideOuterPerimeterStartPointInCavity, cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint, hbOnlyIfCavityFound);
        BorderedTitledPane btpGeneral = new BorderedTitledPane("General settings", btp1content);

        
        

        TextField txtMinExtrusionDistance = new TextField();
        HBox hbMinDistance = new HBox(new Label("Minimum extrusion distance:\t\t\t"), txtMinExtrusionDistance);
        txtMinExtrusionDistance.setText(df.format(preferences.getDouble("minExtrusionDistance", 0.0001)));
        txtMinExtrusionDistance.setTooltip(new Tooltip("Remove points from computed paths would extrude less than given amount.\n\nDefault is 0.0001"));
        txtMinExtrusionDistance.textProperty().addListener((v, ov, nv) -> preferences.putDouble("minExtrusionDistance", Double.valueOf(nv)));
        ((Label) hbMinDistance.getChildren().get(0)).setPadding(new Insets(5, 12, 2, 12));
        ((Label) hbMinDistance.getChildren().get(0)).setTooltip(txtMinExtrusionDistance.getTooltip());
        hbMinDistance.setPadding(new Insets(5, 0, 0, 0));
        
        TextField txtMinPerimeterLength = new TextField();
        HBox hbMinPerimeterLen = new HBox(new Label("Minimum perimeter length (mm):\t\t\t\t\t"), txtMinPerimeterLength);
        txtMinPerimeterLength.setText(df.format(preferences.getDouble("minPerimeterLength", 4.0)));
        txtMinPerimeterLength.setTooltip(new Tooltip("Don't apply tweaks to perimeter if it's shorter than given value.\n\nTweaking might interfere with very short paths and produces unexpected results.\n\nDefault is 4."));
        txtMinPerimeterLength.textProperty().addListener((v, ov, nv) -> preferences.putDouble("minPerimeterLength", Double.valueOf(nv)));
        ((Label) hbMinPerimeterLen.getChildren().get(0)).setPadding(new Insets(5, 12, 2, 12));
        ((Label) hbMinPerimeterLen.getChildren().get(0)).setTooltip(txtMinPerimeterLength.getTooltip());
        hbMinPerimeterLen.setPadding(new Insets(5, 0, 0, 0));

        TextField txtMinZ = new TextField();
        HBox hbMinZ = new HBox(new Label("Minimum perimeter Z height (mm):\t\t\t\t"), txtMinZ);
        txtMinZ.setText(df.format(preferences.getDouble("minZ", 1.0)));
        txtMinZ.setTooltip(new Tooltip("Don't apply tweaks to perimeter if it is below a certain Z height.\n\nDefault is 1"));
        txtMinZ.textProperty().addListener((v, ov, nv) -> preferences.putDouble("minZ", Double.valueOf(nv)));
        ((Label) hbMinZ.getChildren().get(0)).setPadding(new Insets(5, 12, 2, 12));
        ((Label) hbMinZ.getChildren().get(0)).setTooltip(txtMinZ.getTooltip());
        hbMinZ.setPadding(new Insets(5, 0, 0, 0));

        TextField txtMaxAngleBetweenSegments = new TextField();
        HBox hbMaxAngle = new HBox(new Label("Maximum angle between segments at start point (°):\t"), txtMaxAngleBetweenSegments);
        txtMaxAngleBetweenSegments.setText(df.format(preferences.getDouble("maxAngleBetweenSegments", 25.0)));
        txtMaxAngleBetweenSegments.setTooltip(new Tooltip("Don't apply tweaks to perimeter if angle at start point is larger then given value.\n\nTweaking might not be neccessary if the angle is high enough.\n\nDefault is 25."));
        txtMaxAngleBetweenSegments.textProperty().addListener((v, ov, nv) -> preferences.putDouble("maxAngleBetweenSegments", Double.valueOf(nv)));
        ((Label) hbMaxAngle.getChildren().get(0)).setPadding(new Insets(5, 12, 2, 12));
        ((Label) hbMaxAngle.getChildren().get(0)).setTooltip(txtMaxAngleBetweenSegments.getTooltip());
        hbMaxAngle.setPadding(new Insets(5, 0, 0, 0));

        CheckBox cbIgnoreSinglePerimeters = new CheckBox("Ignore single perimeters");
        cbIgnoreSinglePerimeters.setTooltip(new Tooltip("If checked, only perimeters of 2+ thick walls will be tweaked.\n\nDefault is OFF."));
        cbIgnoreSinglePerimeters.setSelected(preferences.get("ignoreSinglePerimeters", "off").equals("on"));
        cbIgnoreSinglePerimeters.selectedProperty().addListener((v, ov, nv) -> preferences.put("ignoreSinglePerimeters", nv ? "on" : "off"));
        cbIgnoreSinglePerimeters.setPadding(new Insets(12, 12, 5, 12));

        VBox btp2content = new VBox(hbMinPerimeterLen, hbMinZ, hbMaxAngle, cbIgnoreSinglePerimeters);
        BorderedTitledPane btpTweaks = new BorderedTitledPane("Perimeter path start/end tweak settings", btp2content);

        
        
        box.getChildren().addAll(btpGeneral, btpTweaks);

        
        cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint.selectedProperty().addListener((observable, oldValue, newValue) -> cbOnlyIfCavityFound.setDisable(!newValue || !cbHideOuterPerimeterStartPointInCavity.isSelected()));
        cbHideOuterPerimeterStartPointInCavity.selectedProperty().addListener((observable, oldValue, newValue) -> cbOnlyIfCavityFound.setDisable(!newValue || !cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint.isSelected()));

        cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint.setSelected(!cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint.isSelected());
        cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint.setSelected(!cbAlignInnerPerimeterStartPointsToOuterPerimeterStartPoint.isSelected());
        cbHideOuterPerimeterStartPointInCavity.setSelected(!cbHideOuterPerimeterStartPointInCavity.isSelected());
        cbHideOuterPerimeterStartPointInCavity.setSelected(!cbHideOuterPerimeterStartPointInCavity.isSelected());

        setContent(box);
    }
}
