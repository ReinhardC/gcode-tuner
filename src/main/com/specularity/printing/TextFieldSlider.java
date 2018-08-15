package main.com.specularity.printing;

import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class TextFieldSlider extends VBox {
    Slider slider = new Slider();
    TextField textField = new TextField();

    public TextFieldSlider(double min, double max) {
        slider.setMax(max);
        slider.setMin(min);

        this.getChildren().addAll(slider, textField);

        slider.valueProperty().addListener((observable, oldValue, newValue) -> textField.setText(Double.toString(newValue.doubleValue())));

        textField.textProperty().addListener((observable, oldValue, newValue) -> slider.setValue(Double.parseDouble(newValue)));
    }
}
