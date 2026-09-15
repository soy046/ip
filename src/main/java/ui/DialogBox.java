package ui;

import java.io.IOException;
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
    private static final double AVATAR_SIZE_USER = 50.0;
    private static final double AVATAR_SIZE_TUESDAY = 75.0;

    @FXML
    private Label dialog;
    @FXML
    private ImageView displayPicture;

    /**
     * Loads the shared layout and applies the speaker's appearance.
     *
     * @param text The message to display.
     * @param img The speaker's avatar.
     * @param speakerStyleClass The CSS class controlling the speaker's text and padding.
     * @param avatarSize The maximum width and height of the avatar, in pixels.
     */
    private DialogBox(String text, Image img, String speakerStyleClass, double avatarSize) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainWindow.class.getResource("/view/DialogBox.fxml"));
            fxmlLoader.setController(this);
            fxmlLoader.setRoot(this);
            fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        dialog.setText(text);
        displayPicture.setImage(img);
        displayPicture.setFitWidth(avatarSize);
        displayPicture.setFitHeight(avatarSize);
        getStyleClass().add(speakerStyleClass);
    }

    /**
     * Flips the dialog box such that the ImageView is on the left and text on the right.
     */
    private void flip() {
        ObservableList<Node> children = FXCollections.observableArrayList(getChildren());
        Collections.reverse(children);
        getChildren().setAll(children);
        setAlignment(Pos.TOP_LEFT);
    }

    /**
     * Creates a compact, right-aligned dialog box for the user.
     *
     * @param text The text displayed in the user box.
     * @param img The image for the user.
     * @return A dialog box for the user.
     */
    public static DialogBox getUserDialog(String text, Image img) {
        return new DialogBox(text, img, "user-dialog", AVATAR_SIZE_USER);
    }

    /**
     * Creates a left-aligned dialog box that emphasizes Tuesday's response.
     *
     * @param text The text displayed in the Tuesday box.
     * @param img The image of Tuesday.
     * @return A dialog box for Tuesday.
     */
    public static DialogBox getTuesdayDialog(String text, Image img) {
        DialogBox dialogBox = new DialogBox(text, img, "tuesday-dialog", AVATAR_SIZE_TUESDAY);
        dialogBox.flip();
        return dialogBox;
    }
}
