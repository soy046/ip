package ui;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * Represents a dialog box consisting of an ImageView to represent the speaker's face
 * and a label containing text from the speaker.
 */
public class DialogBox extends HBox {
    @FXML
    private Label dialog;
    @FXML
    private ImageView displayPicture;

    private DialogBox(String text, Image img) {
        this(text, img, MainWindow.class.getResource("/view/DialogBox.fxml"));
    }

    /**
     * Loads a dialog from a supplied resource, allowing malformed-resource tests.
     */
    DialogBox(String text, Image img, URL resource) {
        if (resource == null) {
            throw new IllegalStateException("The dialog layout resource is missing.");
        }
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(resource);
            fxmlLoader.setController(this);
            fxmlLoader.setRoot(this);
            fxmlLoader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load the dialog layout.", e);
        }
        if (dialog == null || displayPicture == null) {
            throw new IllegalStateException("The dialog layout is missing required controls.");
        }

        dialog.setText(text);
        displayPicture.setImage(img);
    }

    /**
     * Flips the dialog box such that the ImageView is on the left and text on the right.
     */
    private void flip() {
        ObservableList<Node> tmp = FXCollections.observableArrayList(this.getChildren());
        Collections.reverse(tmp);
        getChildren().setAll(tmp);
        setAlignment(Pos.TOP_LEFT);
    }

    /**
     * Creates the dialog box for the user.
     *
     * @param text the text displayed in the user box
     * @param img the image for the user
     * @return a dialog box for user
     */
    public static DialogBox getUserDialog(String text, Image img) {
        return new DialogBox(text, img);
    }

    /**
     * Creates the dialog box for Tuesday.
     *
     * @param text the text displayed in the Tuesday box
     * @param img the image of Tuesday
     * @return a dialog box for Tuesday
     */
    public static DialogBox getTuesdayDialog(String text, Image img) {
        var db = new DialogBox(text, img);
        db.flip();
        return db;
    }
}
