package ui;

import java.io.FileNotFoundException;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import processor.Command;
import processor.Parser;
import processor.Storage;
import task.TaskList;

/**
 * Controls commands and preserves the visible conversation when the window is resized.
 */
public class MainWindow extends AnchorPane {
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox dialogContainer;
    @FXML
    private TextField userInput;
    @FXML
    private Button sendButton;

    private Image tuesdayImage = new Image(this.getClass().getResourceAsStream("/images/Tuesday.png"));
    private Image userImage = new Image(this.getClass().getResourceAsStream("/images/TonyStark.png"));
    private TaskList tasks = new TaskList();
    private Parser parser = new Parser();

    /**
     * The first visible message before a resize, or {@code null} when no message is visible.
     */
    private Node readingAnchor = null;

    /**
     * The distance in pixels from the top of {@link #readingAnchor} to the top of the viewport.
     */
    private double readingOffset = 0.0;

    /**
     * Whether a scene size change is waiting to be handled in the next layout pulse.
     */
    private boolean isResizePending = false;

    /**
     * Whether the next layout pulse should reveal the latest response.
     */
    private boolean isScrollToBottomPending = false;

    /**
     * Prevents recursive layout handling while the reading position is being restored.
     */
    private boolean isHandlingResize = false;

    /**
     * Marks scene size changes for handling and retains its identity for removal when the scene changes.
     */
    private final InvalidationListener resizeListener = observable -> isResizePending = true;

    /**
     * Handles pending layout changes and retains its identity for removal when the scene changes.
     */
    private final Runnable resizePulseListener = this::handlePendingResize;

    /**
     * Initializes conversation sizing and resize tracking, then loads saved tasks.
     */
    @FXML
    public void initialize() {
        initializeConversationBackground();
        initializeResizeTracking();

        try {
            Storage.loadData(Storage.FILE_PATH, this.tasks);
        } catch (FileNotFoundException e) {
            // A missing save file is expected when the application runs for the first time.
        }
    }

    /**
     * Sets the minimum conversation height to fill the scroll viewport.
     * Allows longer conversations to grow beyond the viewport and scroll normally.
     */
    private void initializeConversationBackground() {
        dialogContainer.minHeightProperty().bind(
                Bindings.createDoubleBinding(() -> scrollPane.getViewportBounds().getHeight(),
                        scrollPane.viewportBoundsProperty()));
    }

    /**
     * Registers resize tracking on the scene containing {@link #scrollPane}.
     * Removes listeners from the previous scene when the scroll pane moves to another scene.
     */
    private void initializeResizeTracking() {
        scrollPane.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.widthProperty().removeListener(resizeListener);
                oldScene.heightProperty().removeListener(resizeListener);
                oldScene.removePreLayoutPulseListener(resizePulseListener);
            }

            if (newScene != null) {
                newScene.widthProperty().addListener(resizeListener);
                newScene.heightProperty().addListener(resizeListener);
                newScene.addPreLayoutPulseListener(resizePulseListener);
                isResizePending = true;
            }
        });
    }

    /**
     * Saves the first visible message and the distance scrolled into it.
     * A message reference preserves the reading position when text wrapping changes the content height.
     */
    private void captureReadingPosition() {
        readingAnchor = null;

        double scrollRange = Math.max(0,
                dialogContainer.getHeight() - scrollPane.getViewportBounds().getHeight());
        double valueRange = scrollPane.getVmax() - scrollPane.getVmin();
        double scrollOffset = (valueRange <= 0)
                ? 0
                : (scrollPane.getVvalue() - scrollPane.getVmin()) / valueRange * scrollRange;

        for (Node message : dialogContainer.getChildren()) {
            if (message.getBoundsInParent().getMaxY() > scrollOffset) {
                readingAnchor = message;
                readingOffset = Math.max(0, scrollOffset - message.getBoundsInParent().getMinY());
                break;
            }
        }
    }

    /**
     * Applies pending layout changes and restores the scroll position.
     * Runs before the normal scene layout pass so width and height changes are handled together.
     * Restores the reading anchor after layout, or reveals the latest response after a submission.
     */
    private void handlePendingResize() {
        Scene scene = scrollPane.getScene();
        if (scene == null || isHandlingResize || (!isResizePending && !isScrollToBottomPending)) {
            return;
        }

        isHandlingResize = true;
        try {
            captureReadingPosition();
            isResizePending = false;

            Parent root = scene.getRoot();
            root.applyCss();
            root.layout();

            // A scrollbar appearing or disappearing can require another pass to settle the text width.
            root.layout();

            if (isScrollToBottomPending) {
                scrollPane.setVvalue(scrollPane.getVmax());
                isScrollToBottomPending = false;
            } else {
                restoreReadingPosition();
            }
        } finally {
            isHandlingResize = false;
        }
    }

    /**
     * Restores the saved message and offset within the available scroll range.
     * Clamps the offset when the message becomes shorter and scrolls to the top when all content fits.
     */
    private void restoreReadingPosition() {
        double scrollRange = Math.max(0,
                dialogContainer.getHeight() - scrollPane.getViewportBounds().getHeight());
        if (scrollRange == 0 || readingAnchor == null) {
            scrollPane.setVvalue(scrollPane.getVmin());
            return;
        }

        double messageHeight = readingAnchor.getBoundsInParent().getHeight();
        double offset = Math.min(readingOffset, Math.max(0, messageHeight - 1));
        double targetOffset = readingAnchor.getBoundsInParent().getMinY() + offset;
        double fraction = Math.max(0, Math.min(1, targetOffset / scrollRange));
        scrollPane.setVvalue(scrollPane.getVmin() + fraction * (scrollPane.getVmax() - scrollPane.getVmin()));
    }

    /**
     * Requests scrolling to the latest response after the new messages have been laid out.
     */
    private void scrollToLatestMessage() {
        isScrollToBottomPending = true;
        Platform.requestNextPulse();
    }

    /**
     * Processes input, displays the conversation, and reveals the newest response.
     */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText();
        String response = parser.processCommand(input, tasks);

        dialogContainer.getChildren().addAll(
                DialogBox.getUserDialog(input, userImage),
                DialogBox.getTuesdayDialog(response, tuesdayImage));
        userInput.clear();
        scrollToLatestMessage();

        if (Parser.getCommand(input) == Command.BYE) {
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(event -> Platform.exit());
            pause.play();
        }
    }
}
