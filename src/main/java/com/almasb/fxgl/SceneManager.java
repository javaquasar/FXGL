/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.almasb.fxgl;

import java.io.Serializable;
import java.util.logging.Logger;

import com.almasb.fxgl.asset.AssetManager;
import com.almasb.fxgl.asset.SaveLoadManager;
import com.almasb.fxgl.entity.v2.GameScene;
import com.almasb.fxgl.event.MenuEvent;
import com.almasb.fxgl.settings.SceneSettings;
import com.almasb.fxgl.ui.FXGLDialogBox;
import com.almasb.fxgl.ui.FXGLScene;
import com.almasb.fxgl.ui.IntroScene;
import com.almasb.fxgl.ui.MenuFactory;
import com.almasb.fxgl.ui.UIFactory;
import com.almasb.fxgl.util.FXGLLogger;
import com.almasb.fxgl.util.UpdateTickListener;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Handles everything to do with modifying the scene.
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 *
 */
public final class SceneManager implements UpdateTickListener {

    private static final Logger log = FXGLLogger.getLogger("FXGL.SceneManager");

    private GameScene gameScene;

    /**
     * Main menu, this is the menu shown at the start of game
     */
    private FXGLScene mainMenuScene;

    /**
     * In-game menu, this is shown when menu key pressed during the game
     */
    private FXGLScene gameMenuScene;

    private FXGLScene currentScene;

    /**
     * Is menu enabled in settings
     */
    private boolean isMenuEnabled;

    /**
     * The key that triggers opening/closing game menu
     */
    private KeyCode menuKey = KeyCode.ESCAPE;

    /**
     * The dialog box used to communicate with the user.
     */
    private FXGLDialogBox dialogBox;

    /**
     * Game application instance.
     */
    private GameApplication app;

    /**
     * Main stage.
     */
    private Stage stage;
    private Scene scene;

    private SceneSettings sceneSettings;

    /**
     * Constructs scene manager.
     *
     * @param app
     *            instance of game application
     * @param stage
     *            main stage
     */
    /* package-private */ SceneManager(GameApplication app, Stage stage, Scene scene) {
        this.app = app;
        this.stage = stage;
        this.scene = scene;

        scene.addEventFilter(EventType.ROOT, event -> {
            Event copy = event.copyFor(null, null);
            currentScene.fireEvent(copy);
        });

        sceneSettings = computeSceneSettings(app.getWidth(), app.getHeight());
        gameScene = new GameScene(sceneSettings);


        dialogBox = UIFactory.getDialogBox();
        dialogBox.setOnShown(e -> {
            if (!menuOpenProperty().get())
                app.pause();

            app.getInputManager().clearAllInput();
        });
        dialogBox.setOnHidden(e -> {
            if (!menuOpenProperty().get())
                app.resume();
        });

        isMenuEnabled = app.getSettings().isMenuEnabled();
        menuOpen = new ReadOnlyBooleanWrapper(isMenuEnabled);
    }

    /**
     * Set preferred size to game scene root and stage. Computes
     * {@link #sizeRatio} and scales the root if necessary
     *
     * @param width
     * @param height
     */
    private SceneSettings computeSceneSettings(double width, double height) {
        Rectangle2D bounds = app.getSettings().isFullScreen()
                ? Screen.getPrimary().getBounds()
                : Screen.getPrimary().getVisualBounds();

        double newW = width;
        double newH = height;
        double newScale = 1.0;

        if (width > bounds.getWidth() || height > bounds.getHeight()) {
            log.finer("App size > screen size");

            double ratio = width / height;

            for (int newWidth = (int) bounds.getWidth(); newWidth > 0; newWidth--) {
                if (newWidth / ratio <= bounds.getHeight()) {
                    newW = newWidth;
                    newH = newWidth / ratio;
                    newScale = newWidth / width;
                    break;
                }
            }

            log.finer("Target size: " + width + "x" + height + "@" + 1.0);
            log.finer("New size:    " + newW  + "x" + newH   + "@" + newScale);
        }

        String css = AssetManager.INSTANCE.loadCSS("fxgl_dark.css");

        return new SceneSettings(width, height, newW, newH, css);
    }

    private boolean canSwitchGameMenu = true;

    /**
     * Applies FXGL CSS to menu roots. Scales menu roots appropriately based on
     * {@link #sizeRatio}. Registers event handlers to menus.
     */
    private void configureMenu() {
        menuOpenProperty().addListener((obs, oldState, newState) -> {
            if (newState.booleanValue()) {
                log.finer("Playing State -> Menu State");
                app.onMenuOpen();
            }
            else {
                log.finer("Menu State -> Playing State");
                app.onMenuClose();
            }
        });

        MenuFactory menuFactory = app.initMenuFactory();

        mainMenuScene = menuFactory.newMainMenu(app, sceneSettings);
        gameMenuScene = menuFactory.newGameMenu(app, sceneSettings);

        gameScene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (isMenuEnabled && event.getCode() == menuKey
                    && canSwitchGameMenu) {
                openGameMenu();
                canSwitchGameMenu = false;
            }
        });
        gameScene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == menuKey)
                canSwitchGameMenu = true;
        });

        gameMenuScene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == menuKey && canSwitchGameMenu) {
                closeGameMenu();
                canSwitchGameMenu = false;
            }
        });
        gameMenuScene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == menuKey)
                canSwitchGameMenu = true;
        });

        mainMenuScene.addEventHandler(MenuEvent.NEW_GAME, event -> {
            app.startNewGame();
            setScene(gameScene);
        });
        mainMenuScene.addEventHandler(MenuEvent.LOAD, this::handleMenuEventLoad);

        mainMenuScene.addEventHandler(MenuEvent.EXIT, event -> {
            app.exit();
        });

        gameMenuScene.addEventHandler(MenuEvent.RESUME, event -> {
            this.closeGameMenu();
        });
        gameMenuScene.addEventHandler(MenuEvent.SAVE, event -> {
            String saveFileName = event.getData().map(name -> (String) name).orElse("");
            if (!saveFileName.isEmpty()) {
                try {
                    SaveLoadManager.INSTANCE.save(app.saveState(), saveFileName);
                }
                catch (Exception e) {
                    log.warning("Failed to save game data: " + e.getMessage());
                    //showMessageBox("Failed to save game data: " + e.getMessage());
                }
            }
        });
        gameMenuScene.addEventHandler(MenuEvent.LOAD, this::handleMenuEventLoad);
        gameMenuScene.addEventHandler(MenuEvent.EXIT, event -> {
            this.exitToMainMenu();
        });
    }

    private void handleMenuEventLoad(MenuEvent event) {
        String saveFileName = event.getData().map(name -> (String) name)
                .orElse("");
        if (!saveFileName.isEmpty()) {
            try {
                Serializable data = SaveLoadManager.INSTANCE.load(saveFileName);
                gameScene.reset();
                app.loadState(data);
                app.startNewGame();
                setScene(gameScene);
            }
            catch (Exception e) {
                log.warning("Failed to load save data: " + e.getMessage());
                //showMessageBox("Failed to load save data: " + e.getMessage());
            }
        }
        else {
            SaveLoadManager.INSTANCE.loadLastModifiedFile().ifPresent(data -> {
                gameScene.reset();
                app.loadState((Serializable) data);
                app.startNewGame();
                setScene(gameScene);
            });
        }
    }

    /**
     * Called right before the main stage is shown.
     */
    /* package-private */ void onStageShow() {
        if (isMenuEnabled)
            configureMenu();

        if (app.getSettings().isIntroEnabled()) {
            IntroScene intro = app.initIntroFactory().newIntro(sceneSettings);
            intro.setOnFinished(() -> {
                if (isMenuEnabled) {
                    setScene(mainMenuScene);
                }
                else {
                    app.startNewGame();
                    setScene(gameScene);
                }
            });

            setScene(intro);
            intro.startIntro();
        }
        else {
            if (isMenuEnabled) {
                setScene(mainMenuScene);
            }
            else {
                app.startNewGame();
                setScene(gameScene);
            }
        }

        stage.sizeToScene();
    }

    public GameScene getGameScene() {
        return gameScene;
    }

    /**
     * Changes current scene to given scene.
     *
     * @param scene
     */
    private void setScene(FXGLScene scene) {
        currentScene = scene;

        menuOpen.set(scene == mainMenuScene || scene == gameMenuScene);

        this.scene.setRoot(scene.getRoot());
    }

    private ReadOnlyBooleanWrapper menuOpen;

    /**
     *
     * @return property tracking if any is open
     */
    public ReadOnlyBooleanProperty menuOpenProperty() {
        return menuOpen.getReadOnlyProperty();
    }

    /**
     *
     * @return true if any menu is open
     */
    public boolean isMenuOpen() {
        return menuOpen.get();
    }

    /**
     * Set the key which will open/close game menu.
     *
     * @param key
     * @defaultValue KeyCode.ESCAPE
     */
    public void setMenuKey(KeyCode key) {
        menuKey = key;
    }

    /**
     * Pauses the game and opens in-game menu. Does nothing if menu is disabled
     * in settings
     */
    private void openGameMenu() {
        app.pause();
        setScene(gameMenuScene);
    }

    /**
     * Closes the game menu and resumes the game. Does nothing if menu is
     * disabled in settings
     */
    private void closeGameMenu() {
        setScene(gameScene);
        app.resume();
    }

    /**
     * Exits the current game and opens main menu. Does nothing if menu is
     * disabled in settings
     */
    private void exitToMainMenu() {
        app.pause();
        app.getTimerManager().clearActions();

        gameScene.reset();

        setScene(mainMenuScene);
    }

//    /**
//     * Shows given dialog and blocks execution of the game until the dialog is
//     * dismissed. The provided callback will be called with the dialog result as
//     * parameter when the dialog closes.
//     *
//     * @param dialog
//     * @param resultCallback
//     */
//    public <T> void showDialog(Dialog<T> dialog, Consumer<T> resultCallback) {
//        boolean paused = menuOpenProperty().get();
//
//        if (!paused)
//            app.pause();
//
//        app.getInputManager().clearAllInput();
//
//        dialog.initOwner(gameScene.getWindow());
//        dialog.setOnCloseRequest(e -> {
//            if (!paused)
//                app.resume();
//
//            resultCallback.accept(dialog.getResult());
//        });
//        dialog.show();
//    }
//
//    /**
//     * Shows a blocking (stops game execution) message box with OK button. On
//     * button press, the message box will be dismissed.
//     *
//     * @param message
//     *            the message to show
//     */
//    public void showMessageBox(String message) {
//        dialogBox.showMessageBox(message);
//    }
//
//    /**
//     * Shows a blocking message box with YES and NO buttons. The callback is
//     * invoked with the user answer as parameter.
//     *
//     * @param message
//     * @param resultCallback
//     */
//    public void showConfirmationBox(String message,
//            Consumer<Boolean> resultCallback) {
//        dialogBox.showConfirmationBox(message, resultCallback);
//    }
//
//    /**
//     * Shows a blocking message box with OK button and input field. The callback
//     * is invoked with the field text as parameter.
//     *
//     * @param message
//     * @param resultCallback
//     */
//    public void showInputBox(String message, Consumer<String> resultCallback) {
//        dialogBox.showInputBox(message, resultCallback);
//    }

    /**
     * Saves a screenshot of the current main scene into a ".png" file
     *
     * @return true if the screenshot was saved successfully, false otherwise
     */
//    public boolean saveScreenshot() {
//        Image fxImage = currentScene.getFXScene().snapshot(null);
//        BufferedImage img = SwingFXUtils.fromFXImage(fxImage, null);
//
//        String fileName = "./" + app.getSettings().getTitle()
//                + app.getSettings().getVersion() + LocalDateTime.now() + ".png";
//
//        fileName = fileName.replace(":", "_");
//
//        try (OutputStream os = Files.newOutputStream(Paths.get(fileName))) {
//            return ImageIO.write(img, "png", os);
//        }
//        catch (Exception e) {
//            log.finer(
//                    "Exception occurred during saveScreenshot() - "
//                            + e.getMessage());
//        }
//
//        return false;
//    }

    @Override
    public void onUpdate() {
        // TODO Auto-generated method stub

    }
}
