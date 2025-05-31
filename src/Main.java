import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import javax.swing.*;

enum GameState
{
    MAIN_MENU,
    HOW_TO_PLAY,
    CREDITS,
    SETTINGS,
    PLAYING,
    GAME_OVER,
    BETWEENLEVELS,
    VICTORY
}

enum Difficulty
{
    EASY,
    NORMAL,
    HARD
}

enum GraphicsQuality
{
    LOW,
    HIGH
}

public class Main extends GameEngine
{
    // Game state related variables
    public static final int TILE_SIZE = 32;
    private GameMap gameMap;
    private Player player;
    private RayCaster raycaster;
    private List<Enemy> enemies = new ArrayList<>();
    private List<HealthItem> healthItems = new ArrayList<>();
    private List<WeaponItem> weaponItems = new ArrayList<>();
    private GameAsset gameAsset;
    private GameState currentState;
    private boolean isAtEndTile;
    private int numOfLevels;
    private int currentLevel;
    private Difficulty difficulty = Difficulty.NORMAL;
    private GraphicsQuality quality = GraphicsQuality.LOW;

    // Window size
    private int width = 1024;
    private int height = 512;

    // Direction flags
    private boolean left, right, up, down;

    // Mouse handling
    private int lastMouseX, lastMouseY;
    private boolean firstMouseUpdate = true;
    private Robot robot;
    private boolean warpingMouse = false;

    // Key handling
    private boolean qPressed = false;
    private boolean ePressed = false;

    // Audio Clips
    private static final AudioClip soundLazer1 = loadAudio("assets/audio/SoundLazer1.wav");
    private static final AudioClip soundLazer2 = loadAudio("assets/audio/SoundLazer2.wav");
    private static final AudioClip soundLazer3 = loadAudio("assets/audio/SoundLazer3.wav");
    private static final AudioClip soundLazerHit = loadAudio("assets/audio/SoundLazerHit.wav");
    private static final AudioClip soundPlayerInjured = loadAudio("assets/audio/SoundPlayerInjured.wav");
    private static final AudioClip soundPlayerWalking = loadAudio("assets/audio/SoundPlayerWalking.wav");
    private static final AudioClip soundReloadRifle = loadAudio("assets/audio/SoundReloadRifle.wav");
    private static final AudioClip soundReloadShotgun = loadAudio("assets/audio/SoundReloadShotgun.wav");
    private static final AudioClip soundWinDoorOpen = loadAudio("assets/audio/SoundWinDoorOpen.wav");
    private static final AudioClip soundWinLaunch = loadAudio("assets/audio/SoundWinLaunch.wav");
    private static final AudioClip soundZombieDeath = loadAudio("assets/audio/SoundZombieDeath.wav");
    private static final AudioClip soundZombieNeutral = loadAudio("assets/audio/SoundZombieNeutral.wav");
    private static final AudioClip soundPickupItem = loadAudio("assets/audio/SoundPickupItem.wav");
    
    // Gameplay related variables
    private double weaponX = 400;
    private double weaponY = 300;
    private List<Weapon> initialWeapons;
    private Image lazerRifleSprite;
    private Image lazerShotgunSprite;
    private Image lazerRiflePickup;
    private Image lazerShotgunPickup;

    // Menu related variables
    private Image menuBackground;
    private List<Button> menuButtons = new ArrayList<>();
    private Cursor blankCursor;
    private Cursor defaultCursor;
    private Image gameOverBackground;
    private Button backButton;
    private List<Button> settingsButtons = new ArrayList<>();
    private Button easyButton, normalButton, hardButton, highButton, lowButton;
    private boolean gameStarted = false;
    private Button resumeButton;

    // Animation related variables
    private double betweenTimer;
    private double betweenLength;
    private boolean displayEnd;
    private double escapePodX, escapePodY;

    private class Button
    {
        int x, y, width, height;
        String text;
        Runnable action;
        boolean selected;

        Button(int x, int y, int width, int height, String text, Runnable action)
        {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
            this.action = action;
            this.selected = false;
        }

        boolean contains(int mx, int my)
        {
            return mx >= x && mx <= x + width && my >= y && my <= y + height;
        }

        void draw()
        {
            if (selected)
            {
                changeColor(new Color(60, 60, 60, 200));
            }
            else
            {
                changeColor(new Color(30, 30, 30, 150));
            }
            drawSolidRectangle(x, y, width, height);
            changeColor(white);
            mGraphics.setFont(new Font("Arial", Font.PLAIN, 20));
            FontMetrics metrics = mGraphics.getFontMetrics();
            int textWidth = metrics.stringWidth(text);
            int textHeight = metrics.getHeight();
            int textX = x + (width - textWidth) / 2;
            int textY = y + (height - textHeight) / 2 + metrics.getAscent();
            mGraphics.drawString(text, textX, textY);
        }
    }

    private static class RenderableObject
    {
        Object obj;
        double distance;
        RenderableObject(Object obj, double distance)
        {
            this.obj = obj;
            this.distance = distance;
        }
    }

    public static void main(String[] args)
    {
        Main main = new Main();
        try
        {
            SwingUtilities.invokeAndWait(() -> main.setupWindow(main.width, main.height));
        }
        catch (Exception e)
        {
            System.err.println("Error setting up window: " + e.getMessage());
            System.exit(1);
        }
        main.init();
        createGame(main, 60);
    }

    @Override public void init()
    {
        //Initialise gameAssets to hold all visual game assets
        this.gameAsset = new GameAsset();

        //initialise game menu
        currentState = GameState.MAIN_MENU;
        menuBackground = loadImage("assets/visual/menuWallpaper.png");
        gameOverBackground = loadImage("assets/visual/gameOverScreen.png");
        int buttonWidth = 200;
        int buttonHeight = 50;
        int buttonSpacing = 20;
        int startX = (width - buttonWidth) / 2;
        int startY = (height - (buttonHeight + 3 * buttonSpacing)) / 3;

        resumeButton = new Button(startX, startY, buttonWidth, buttonHeight, "Resume Game", () -> {
            currentState = GameState.PLAYING;
        });

        menuButtons.add(new Button(startX, startY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight, "Start New Game", () -> { startNewGame(); }));

        menuButtons.add(new Button(startX, startY + 2 * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight,
                                   "How to Play", () -> { currentState = GameState.HOW_TO_PLAY; }));
        menuButtons.add(new Button(startX, startY + 3 * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight,
                                   "Settings", () -> { currentState = GameState.SETTINGS; }));
        menuButtons.add(new Button(startX, startY + 4 * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight,
                                   "Credits", () -> { currentState = GameState.CREDITS; }));

        easyButton = new Button(startX / 2, startY, buttonWidth, buttonHeight, "Easy", () -> {
            difficulty = Difficulty.EASY;
            updateDifficultyButtons();
        });
        normalButton =
            new Button(startX / 2, startY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight, "Normal", () -> {
                difficulty = Difficulty.NORMAL;
                updateDifficultyButtons();
            });
        hardButton = new Button(startX / 2, startY + 2 * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight,
                                "Hard", () -> {
                                    difficulty = Difficulty.HARD;
                                    updateDifficultyButtons();
                                });
        highButton = new Button((startX / 2) * 3, startY + (buttonHeight + buttonSpacing), buttonWidth, buttonHeight,
                                "High", () -> {
                                    quality = GraphicsQuality.HIGH;
                                    updateGraphicsQuality();
                                    raycaster.setNumRays(1024);
                                });
        lowButton = new Button((startX / 2) * 3, startY, buttonWidth, buttonHeight, "Low", () -> {
            quality = GraphicsQuality.LOW;
            updateGraphicsQuality();
            raycaster.setNumRays(512);
        });

        settingsButtons.add(easyButton);
        settingsButtons.add(normalButton);
        settingsButtons.add(hardButton);
        settingsButtons.add(lowButton);
        settingsButtons.add(highButton);
        updateDifficultyButtons();
        updateGraphicsQuality();

        int backButtonWidth = 100;
        int backButtonHeight = 40;
        int backButtonX = (width - backButtonWidth) / 2;
        int backButtonY = height - 100;
        backButton = new Button(backButtonX, backButtonY, backButtonWidth, backButtonHeight, "Back",
                                () -> { currentState = GameState.MAIN_MENU; });

        // hide the mouse
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
        defaultCursor = Cursor.getDefaultCursor();

        // initialise the game map
        gameMap = new GameMap();
        numOfLevels = 3;
        currentLevel = 0; // start at 0, and will auto increment to level 1
        advanceLevel(); // sets up map
        isAtEndTile = false;

        setWindowSize(width, height);

        // Initialize ray caster and associated objects
        raycaster = new RayCaster(gameMap, TILE_SIZE, gameAsset);
        lazerRifleSprite = gameAsset.getLazerRifle();
        lazerShotgunSprite = gameAsset.getLazerShotgun();
        lazerRiflePickup = gameAsset.getLazerRiflePickup();
        lazerShotgunPickup = gameAsset.getLazerShotgunPickup();

        // Initialize Robot for mouse control
        try
        {
            robot = new Robot();
        }
        catch (AWTException e)
        {
            System.err.println("Failed to initialize Robot: " + e.getMessage());
        }

        lastMouseX = width / 2;
        lastMouseY = height / 2;

        // initialise animation var
        resetAnimation();
    }

    @Override public void update(double dt)
    {
        if (currentState == GameState.VICTORY) {
            betweenTimer += dt;
            if (betweenTimer > betweenLength+2) {
                betweenTimer = 0;
                displayEnd = true;
            }
            escapePodY += 15 * dt;
        }
        if (currentState == GameState.BETWEENLEVELS) {
            betweenTimer += dt;
            if (betweenTimer > betweenLength) {
                betweenTimer = 0;
                currentState = GameState.PLAYING;
            }
        }
        // Update Enemies
        if (currentState == GameState.PLAYING)
        {
            player.setDirection(left, right, up, down);
            player.update(this, dt);
            player.getCurrentWeapon().update(dt);
            Enemy toRemove = null;
            for (Enemy enemy : enemies)
            {
                enemy.update(this, dt, player);
                if (enemy.toRemove())
                {
                    toRemove = enemy;
                }
            }
            enemies.remove(toRemove);

            // Check health item pickup
            for (HealthItem healthItem : healthItems)
            {
                if (!healthItem.isConsumed())
                {
                    double dx = player.getX() - healthItem.getX();
                    double dy = player.getY() - healthItem.getY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance < TILE_SIZE / 2)
                    { // the pickup range
                        healthItem.consume(player);
                        playAudio(soundPickupItem);
                    }
                }
            }

            // Check Weapon pickup
            for (WeaponItem weaponItem : weaponItems)
            {
                if (!weaponItem.isConsumed())
                {
                    double dx = player.getX() - weaponItem.getX();
                    double dy = player.getY() - weaponItem.getY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance < TILE_SIZE / 2)
                    {
                        weaponItem.consume(player);
                        playAudio(soundPickupItem);
                    }
                }
            }
            if (!player.isAlive())
            {
                currentState = GameState.GAME_OVER;
            }

            // Check Standing on End Tile
            if (gameMap.isEndTile((int)player.getX() / TILE_SIZE, (int)player.getY() / TILE_SIZE))
            {
                isAtEndTile = true;
                // System.out.println("Standing on end tile");
            }
            else
            {
                isAtEndTile = false;
            }
        }
    }

    @Override public void paintComponent()
    {
        if (currentState == GameState.VICTORY) {
            getFrame().setCursor(defaultCursor);
            saveCurrentTransform();
            changeBackgroundColor(black);
            clearBackground(width, height);
            drawImage(menuBackground, 0, 0, width, height);
            drawImage(gameAsset.getEscapePodSprite(), escapePodX, escapePodY, 3, -5); // draw the escape pod
            changeColor(white);
            drawCenteredText( 100, "You have successfully escaped.", "Arial", 40, Font.BOLD);
            drawCenteredText( height-50, "Press ENTER to return to main menu.", "Arial", 14,Font.PLAIN);
            if (displayEnd) {
                drawCenteredText( 150, "Then you look to the nasty looking gash on your arm...", "Arial", 20, Font.BOLD);
            }
            restoreLastTransform();
        }
        else if(currentState == GameState.BETWEENLEVELS) {
            getFrame().setCursor(defaultCursor);
            saveCurrentTransform();
            changeBackgroundColor(black);
            clearBackground(width, height);
            changeColor(white);
            drawCenteredText( height / 2, "Level: " + currentLevel, "Arial", 40, Font.BOLD);
            restoreLastTransform();
        }
        else if (currentState == GameState.MAIN_MENU)
        {
            getFrame().setCursor(defaultCursor);
            drawMainMenu();
        }
        else if (currentState == GameState.HOW_TO_PLAY)
        {
            getFrame().setCursor(defaultCursor);
            drawImage(menuBackground, 0, 0, width, height);
            drawHowToPlay();
        }
        else if (currentState == GameState.SETTINGS)
        {
            getFrame().setCursor(defaultCursor);
            drawSettings();
        }
        else if (currentState == GameState.CREDITS)
        {
            getFrame().setCursor(defaultCursor);
            drawImage(menuBackground, 0, 0, width, height);
            drawCredits();
        }
        else if (currentState == GameState.PLAYING)
        {
            getFrame().setCursor(blankCursor);
            changeBackgroundColor(black);
            clearBackground(width(), height());

            // --- 2.5D RENDERING ---
            double playerX = player.getX();
            double playerY = player.getY();
            double playerAngle = player.getAngle();
            double verticalLookOffset = player.getVerticalLookOffset();

            raycaster.draw(this, playerX, playerY, playerAngle, verticalLookOffset);

            // --- MINIMAP OVERLAY ---
            final int MINI_MAP_SIZE = 128;
            int miniTileSize = MINI_MAP_SIZE / gameMap.getWidth();

            // Position minimap at top-right corner with some padding
            int offsetX = width() - MINI_MAP_SIZE - 10; // 10 px from right
            int offsetY = 10;                           // 10 px from top

            // Draw minimap border (outside blackout)
            changeColor(white);
            drawRectangle(offsetX - 1, offsetY - 1, MINI_MAP_SIZE + 2, MINI_MAP_SIZE + 2);

            // Optionally draw minimap background inside border
            changeColor(new Color(20, 20, 20, 180)); // semi-transparent dark fill
            drawSolidRectangle(offsetX, offsetY, MINI_MAP_SIZE, MINI_MAP_SIZE);

            // Draw the minimap tiles, blacking out those outside vision radius
            int visionRadius = 5;
            gameMap.draw(this, miniTileSize, offsetX, offsetY, player.getX(), player.getY(), visionRadius, TILE_SIZE);

            // Render our objects
            List<RenderableObject> toRender = new ArrayList<>();

            for (HealthItem healthItem : healthItems)
            {
                if (!healthItem.isConsumed())
                {
                    double dx = healthItem.getX() - player.getX();
                    double dy = healthItem.getY() - player.getY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    toRender.add(new RenderableObject(healthItem, distance));
                }
            }

            for (Enemy enemy : enemies)
            {
                double dx = enemy.getX() - player.getX();
                double dy = enemy.getY() - player.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);
                toRender.add(new RenderableObject(enemy, distance));
            }

            for (WeaponItem weaponItem : weaponItems)
            {
                if (!weaponItem.isConsumed())
                {
                    double dx = weaponItem.getX() - player.getX();
                    double dy = weaponItem.getY() - player.getY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    toRender.add(new RenderableObject(weaponItem, distance));
                }
            }

            // Sort by distance (we want farther objects spawning first)
            toRender.sort((a, b) -> Double.compare(b.distance, a.distance));

            // Render objects
            for (RenderableObject ro : toRender)
            {
                if (ro.obj instanceof HealthItem)
                {
                    ((HealthItem)ro.obj).render(this, player, raycaster.getRayDistancesArray());
                }
                else if (ro.obj instanceof Enemy)
                {
                    ((Enemy)ro.obj).render(this, player, raycaster.getRayDistancesArray());
                }
                else if (ro.obj instanceof WeaponItem)
                {
                    ((WeaponItem)ro.obj).render(this, player, raycaster.getRayDistancesArray());
                }
            }

            // Health bar on bottom left
            changeColor(Color.gray);
            drawSolidRectangle(10, height() - 50, 200, 20);
            changeColor(green);
            float healthPercentage = player.getHealthPercentage();
            drawSolidRectangle(10, height() - 50, 200 * healthPercentage, 20);
            changeColor(white);
            drawText(220, height() - 30, player.getHealth() + "/" + player.getMaxHealth(), "Arial", 20);

            // Weapon Sprite
            Weapon currentWeapon = player.getCurrentWeapon();
            if (currentWeapon.getSprite() != null)
            {
                if (currentWeapon.getFired())
                {
                    if (Objects.equals(currentWeapon.getName(), "Laser Rifle"))
                    {
                        drawImage(currentWeapon.getFlashFrame(), weaponX - (21 * 3.125), weaponY - (47 * 3.125), 400,
                                  400);
                    }
                    else if (Objects.equals(currentWeapon.getName(), "Laser ShotGun"))
                    {
                        drawImage(currentWeapon.getFlashFrame(), weaponX - (21 * 3.125), weaponY - (53 * 3.125), 400,
                                  400);
                    }
                    else
                    {
                        drawImage(currentWeapon.getFlashFrame(), weaponX - (19 * 3.125), weaponY - (54 * 3.125), 400,
                                  400);
                    }
                }
                drawImage(currentWeapon.getSprite(), weaponX, weaponY, 400, 400);
            }
            // Weapon name and ammo count on bottom right
            String weaponName = currentWeapon.getName();
            changeColor(green);
            drawText(width() - 120, height() - 70, weaponName, "Arial", 16);
            if (currentWeapon.isUnlimitedAmmo())
            {
                drawText(width() - 100, height() - 10, "∞", "Arial", 60);
            }
            else
            {
                String ammoText = currentWeapon.getCurrentMagAmmo() + " / " + currentWeapon.getTotalAmmo();
                drawText(width() - 120, height() - 20, ammoText, "Arial", 30);
            }

            if (isAtEndTile)
            {
                changeColor(white);
                drawCenteredText(height() - 100, "Press F to Activate", "Arial", 20, Font.PLAIN);
            }

            // Draw minimap border (outside blackout)
            changeColor(white);
            drawRectangle(offsetX - 1, offsetY - 1, MINI_MAP_SIZE + 2, MINI_MAP_SIZE + 2);

            // Optionally draw minimap background inside border
            changeColor(new Color(20, 20, 20, 180)); // semi-transparent dark fill
            drawSolidRectangle(offsetX, offsetY, MINI_MAP_SIZE, MINI_MAP_SIZE);

            // Draw the minimap tiles, blacking out those outside vision radius
            visionRadius = 7;
            gameMap.draw(this, miniTileSize, offsetX, offsetY, player.getX(), player.getY(), visionRadius, TILE_SIZE);

            // Draw player on minimap
            changeColor(white);
            double px = player.getX() / TILE_SIZE * miniTileSize + offsetX;
            double py = player.getY() / TILE_SIZE * miniTileSize + offsetY;
            drawSolidCircle(px, py, 4);

            // Draw player facing direction line
            double lineLength = 10;
            double endX = px + Math.cos(player.getAngle()) * lineLength;
            double endY = py + Math.sin(player.getAngle()) * lineLength;
            drawLine(px, py, endX, endY);

            for (Enemy enemy : enemies)
            {
                enemy.drawOnMinimap(this, player);
            }
            // Leave here so crosshair is drawn over everything
            player.draw(this);
        }
        else if (currentState == GameState.GAME_OVER)
        {
            getFrame().setCursor(defaultCursor);
            drawImage(gameOverBackground, 0, 0, width, height);
            changeColor(new Color(200, 200, 200));
            drawCenteredText(height / 2 + 100, "GAME OVER", "Arial", 50, Font.BOLD);
            drawCenteredText(height / 2 + 150, "Press Enter to restart", "Arial", 30, Font.PLAIN);
        }
    }

    public void drawCenteredText(double y, String s, String font, int size, int style)
    {
        mGraphics.setFont(new Font(font, style, size));
        FontMetrics metrics = mGraphics.getFontMetrics();
        int textWidth = metrics.stringWidth(s);
        int x = (width - textWidth) / 2;
        mGraphics.drawString(s, x, (int)y);
    }

    private void drawMainMenu()
    {
        drawImage(menuBackground, 0, 0, width, height);

        mGraphics.setFont(new Font("Arial", Font.BOLD, 60));
        FontMetrics metrics = mGraphics.getFontMetrics();
        String title = "Alderaan";
        int textWidth = metrics.stringWidth(title);
        int x = (width - textWidth) / 2;
        int y = 60;
        changeColor(white);
        drawBoldText(x, y, title, "Arial", 60);

        mGraphics.setFont(new Font("Arial", Font.PLAIN, 30));
        metrics = mGraphics.getFontMetrics();
        String subtitle = "The Last Man on Alderaan";
        textWidth = metrics.stringWidth(subtitle);
        x = (width - textWidth) / 2;
        y = 100;
        changeColor(new Color(200, 200, 200)); // Light gray
        mGraphics.drawString(subtitle, x, y);
        if (gameStarted) {
            resumeButton.draw();
        }
        for (Button button : menuButtons)
        {
            button.draw();
        }
    }

    private void drawHowToPlay()
    {
        changeColor(new Color(200, 200, 200));
        drawCenteredText(60, "How to Play", "Arial", 40, Font.BOLD);
        String[] lines = {"You need to get to a life pod, and get off this ship.",
                          " ",
                          "Controls:",
                          "ESC: Back to menu",
                          "WASD: Move",
                          "Mouse: Look around",
                          "Left Click: Shoot",
                          "Q/E: Switch weapons",
                          "R: Reload",
                          "Reach the life pod to win"};
        int lineHeight = 30;
        int startY = 100;
        drawCenteredText(startY, lines[0], "Arial", 20, Font.PLAIN);
        for (int i = 1; i < lines.length; i++)
        {
            drawText((double)this.mWidth / 8, startY + i * lineHeight, lines[i], "Arial", 20);
        }
        drawText((double)this.mWidth / 4 * 2, startY + 3 * lineHeight, "Press F at the doors like below:", "Arial", 20);
        drawImage(subImage(loadImage("assets/visual/ScifiWall.png"), 512, 0, 128, 128), (double)this.mWidth / 4 * 2,
                  startY + 4 * lineHeight);
        drawText((double)this.mWidth / 4 * 2, startY + 5 * lineHeight + 128, "To advance to the next level", "Arial",
                 20);
        backButton.draw();
    }

    private void drawCredits()
    {
        changeColor(new Color(200, 200, 200));
        drawCenteredText(60, "Credits", "Arial", 40, Font.BOLD);
        String[] lines = {"Angelo Nicolson", "Joshua Sim", "Kale Twist", "Johnny Chadwick-Watt"};
        int lineHeight = 30;
        int startY = 120;
        for (int i = 0; i < lines.length; i++)
        {
            drawCenteredText(startY + i * lineHeight, lines[i], "Arial", 20, Font.PLAIN);
        }
        backButton.draw();
    }

    private void drawSettings()
    {
        drawImage(menuBackground, 0, 0, width, height);
        changeColor(new Color(200, 200, 200));
        drawCenteredText(60, "Settings", "Arial", 40, Font.BOLD);
        // Draw difficulty label
        changeColor(white);
        drawText(this.mWidth / 4.0, 120.0, "Difficulty:", "Arial", 24);
        drawText((this.mWidth / 4.0) * 3 - 140, 120.0, "Graphics Quality:", "Arial", 24);

        // Draw setings buttons
        for (Button button : settingsButtons)
        {
            button.draw();
        }

        backButton.draw();
    }

    private void updateDifficultyButtons()
    {
        easyButton.selected = (difficulty == Difficulty.EASY);
        normalButton.selected = (difficulty == Difficulty.NORMAL);
        hardButton.selected = (difficulty == Difficulty.HARD);
    }
    private void updateGraphicsQuality()
    {
        highButton.selected = (quality == GraphicsQuality.HIGH);
        lowButton.selected = (quality == GraphicsQuality.LOW);
    }

    @Override public void keyPressed(KeyEvent e)
    {
        if (currentState == GameState.PLAYING)
        {
            switch (e.getKeyCode())
            {
                case KeyEvent.VK_ESCAPE:
                    currentState = GameState.MAIN_MENU;
                    break;
            case KeyEvent.VK_Q:
                if (!qPressed)
                {
                    player.previousWeapon();
                    qPressed = true;
                }
                break;
            case KeyEvent.VK_E:
                if (!ePressed)
                {
                    player.previousWeapon();
                    ePressed = true;
                }
                break;
            case KeyEvent.VK_R:
                player.getCurrentWeapon().reload();
                playAudio(soundReloadRifle);
                break;
            case KeyEvent.VK_F:
                if (isAtEndTile)
                {
                    
                    advanceLevel();
                    isAtEndTile = false;
                    resetPlayer();
                }
                break;
            default:
                updateDirection(e, true);
                break;
            }
        }
        else if (currentState == GameState.GAME_OVER)
        {
            if (e.getKeyCode() == KeyEvent.VK_ENTER)
            {
                restartGame();
            }
        }
        else if (currentState == GameState.VICTORY) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER)
            {
                resetAnimation();
                gameStarted = false;
                currentState = GameState.MAIN_MENU;
            }
        }
    }

    @Override public void keyReleased(KeyEvent e)
    {
        if (currentState == GameState.PLAYING)
        {
            switch (e.getKeyCode())
            {
            case KeyEvent.VK_Q:
                qPressed = false;
                break;
            case KeyEvent.VK_E:
                ePressed = false;
                break;
            default:
                updateDirection(e, false);
                break;
            }
        }
    }

    private void updateDirection(KeyEvent e, boolean pressed)
    {
        switch (e.getKeyCode())
        {
        case KeyEvent.VK_A -> left = pressed;
        case KeyEvent.VK_D -> right = pressed;
        case KeyEvent.VK_W -> up = pressed;
        case KeyEvent.VK_S -> down = pressed;
        }
    }

    public void mouseMoved(java.awt.event.MouseEvent e)
    {
        if (currentState == GameState.PLAYING && player != null)
        {
            if (warpingMouse)
            {
                // Ignore this event caused by the robot mouseMove call
                warpingMouse = false;
                return;
            }

            double sensitivityX = 0.002;
            double sensitivityY = 0.5;

            int mouseX = e.getXOnScreen();
            int mouseY = e.getYOnScreen();

            int windowCenterX = getFrame().getLocationOnScreen().x + width / 2;
            int windowCenterY = getFrame().getLocationOnScreen().y + height / 2;

            if (firstMouseUpdate)
            {
                lastMouseX = mouseX;
                lastMouseY = mouseY;
                firstMouseUpdate = false;
            }

            int deltaX = mouseX - lastMouseX;
            int deltaY = mouseY - lastMouseY;

            player.rotate(deltaX * sensitivityX);
            player.setVerticalLookOffset(player.getVerticalLookOffset() + deltaY * sensitivityY);

            if (robot != null)
            {
                warpingMouse = true; // Set flag before warping
                robot.mouseMove(windowCenterX, windowCenterY);
                lastMouseX = windowCenterX;
                lastMouseY = windowCenterY;
            }
            else
            {
                lastMouseX = mouseX;
                lastMouseY = mouseY;
            }
        }
    }
    @Override public void mouseClicked(MouseEvent e)
    {
        if (currentState == GameState.MAIN_MENU)
        {
            int mx = e.getX();
            int my = e.getY();
            if (gameStarted) {
                if (resumeButton.contains(mx, my)) {
                    resumeButton.action.run();
                    return;
                }
            }
            for (Button button : menuButtons)
            {
                if (button.contains(mx, my))
                {
                    button.action.run();
                    break;
                }
            }
        }

        else if (currentState == GameState.HOW_TO_PLAY || currentState == GameState.CREDITS)
        {
            int mx = e.getX();
            int my = e.getY();
            if (backButton.contains(mx, my))
            {
                backButton.action.run();
            }
        }

        else if (currentState == GameState.SETTINGS)
        {
            int mx = e.getX();
            int my = e.getY();
            for (Button button : settingsButtons)
            {
                if (button.contains(mx, my))
                {
                    button.action.run();
                    break;
                }
            }
            if (backButton.contains(mx, my))
            {
                backButton.action.run();
            }
        }

        else if (currentState == GameState.PLAYING && e.getButton() == MouseEvent.BUTTON1)
        {
            Weapon currentWeapon = player.getCurrentWeapon();
            if (player.getCurrentWeapon().tryFire())
            {
                if (currentWeapon.getFireSound() != null)
                {
                    playAudio(currentWeapon.getFireSound());
                }

                // Shooting logic
                int centralRayIndex = raycaster.getNumRays() / 2; // with numRays being 1024
                double wallDistance = raycaster.getRayDistances(centralRayIndex);
                double angleTolerance = Math.toRadians(1); // aiming tolerance = 1 degree
                Enemy hitEnemy = null;
                double minDistance = Double.MAX_VALUE;
                double playerAngle = player.getAngle();
                for (Enemy enemy : enemies)
                {
                    double dx = enemy.getX() - player.getX();
                    double dy = enemy.getY() - player.getY();
                    double enemyAngle = Math.atan2(dy, dx);
                    double angleDiff = normalizeAngle(enemyAngle - playerAngle);
                    if (Math.abs(angleDiff) < angleTolerance)
                    {
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        if (distance < minDistance && distance < wallDistance)
                        {
                            minDistance = distance;
                            hitEnemy = enemy;
                        }
                    }
                }

                if (hitEnemy != null)
                {
                    int damage = currentWeapon.getDamage();
                    hitEnemy.takeDamage(damage);
                    playAudio(soundLazerHit);
                    if (!hitEnemy.isAlive())
                    {
                        playAudio(soundZombieDeath);
                    }
                }
            }
        }
    }

    private void initializePlayer(List<Weapon> weapons)
    {
    outer:
        for (int y = 0; y < GameMap.HEIGHT; y++)
        {
            for (int x = 0; x < GameMap.WIDTH; x++)
            {
                if (gameMap.isWalkableTile(x, y))
                {
                    double px = x * TILE_SIZE + TILE_SIZE / 2.0;
                    double py = y * TILE_SIZE + TILE_SIZE / 2.0;
                    player = new Player(px, py, gameMap, TILE_SIZE, weapons);
                    break outer;
                }
            }
        }
    }

    private void startNewGame()
    {
        gameStarted = true;
        currentLevel = 0;
        advanceLevel();

        List<Weapon> initialWeapons = createInitialWeapons();
        initializePlayer(initialWeapons);
        left = false;
        right = false;
        up = false;
        down = false;
    }

    private void resetPlayer()
    {
    outer:
        for (int y = 0; y < GameMap.HEIGHT; y++)
        {
            for (int x = 0; x < GameMap.WIDTH; x++)
            {
                if (gameMap.isWalkableTile(x, y))
                {
                    double px = x * TILE_SIZE + TILE_SIZE / 2.0;
                    double py = y * TILE_SIZE + TILE_SIZE / 2.0;
                    player.setX(px);
                    player.setY(py);
                    player.setAngle(0.0);
                    player.setVerticalLookOffset(0.0);
                    break outer;
                }
            }
        }
    }

    private void restartGame()
    {
        startNewGame();
    }

    public AudioClip getSoundPlayerInjured()
    {
        return soundPlayerInjured;
    }

    private double normalizeAngle(double angle)
    {
        while (angle < -Math.PI)
            angle += 2 * Math.PI;
        while (angle > Math.PI)
            angle -= 2 * Math.PI;
        return angle;
    }

    private void spawnLevelEntities()
    {
        enemies.clear();
        healthItems.clear();
        weaponItems.clear();

        List<int[]> walkableTiles = new ArrayList<>();
        for (int y = 0; y < GameMap.HEIGHT; y++)
        {
            for (int x = 0; x < GameMap.WIDTH; x++)
            {
                if (gameMap.isWalkableTile(x, y))
                {
                    walkableTiles.add(new int[] {x, y});
                }
            }
        }

        // Enemy count we determine by level and difficulty
        int easyBase = 7 + (currentLevel - 1) * 2; // base enemies are - level 1: 7, Level 2: 9, Level 3: 11
        int adjustment = 0;
        if (difficulty == Difficulty.NORMAL)
        {
            adjustment = 2;
        }
        else if (difficulty == Difficulty.HARD)
        {
            adjustment = 4;
        }
        int enemyCount = easyBase + adjustment;

        int zombieDamage;
        switch (difficulty)
        {
        case EASY:
            zombieDamage = 5;
            break;
        case NORMAL:
            zombieDamage = 10;
            break;
        case HARD:
            zombieDamage = 20;
            break;
        default:
            zombieDamage = 10;
        }

        // Spawn enemies
        Random rand = new Random();
        for (int i = 0; i < enemyCount; i++)
        {
            if (walkableTiles.isEmpty())
                break;
            int index = rand.nextInt(walkableTiles.size());
            int[] tile = walkableTiles.get(index);
            double ex = tile[0] * TILE_SIZE + TILE_SIZE / 2.0;
            double ey = tile[1] * TILE_SIZE + TILE_SIZE / 2.0;
            enemies.add(new Enemy(ex, ey, "", gameMap, TILE_SIZE, zombieDamage));
        }

        int healthItemCount;
        if (difficulty == Difficulty.EASY)
        {
            healthItemCount = 6;
        }
        else if (difficulty == Difficulty.NORMAL)
        {
            healthItemCount = 4;
        }
        else
        { // HARD
            healthItemCount = 2;
        }

        // Spawn health items
        List<int[]> availableTiles = new ArrayList<>(walkableTiles);
        for (int i = 0; i < healthItemCount && !availableTiles.isEmpty(); i++)
        {
            int index = rand.nextInt(availableTiles.size());
            int[] tile = availableTiles.remove(index);
            double hx = tile[0] * TILE_SIZE + TILE_SIZE / 2.0;
            double hy = tile[1] * TILE_SIZE + TILE_SIZE / 2.0;
            healthItems.add(new HealthItem(hx, hy, gameAsset.getHealthItemSprite()));
        }

        int rifleDamage;
        int shotgunDamage;
        switch (difficulty)
        {
        case EASY:
            rifleDamage = 20;
            shotgunDamage = 30;
            break;
        case NORMAL:
            rifleDamage = 15;
            shotgunDamage = 25;
            break;
        case HARD:
            rifleDamage = 10;
            shotgunDamage = 20;
            break;
        default:
            rifleDamage = 15;
            shotgunDamage = 25;
        }

        // Spawn Laser Rifle
        if (!availableTiles.isEmpty())
        {
            int index = rand.nextInt(availableTiles.size());
            int[] tile = availableTiles.remove(index);
            double wx = tile[0] * TILE_SIZE + TILE_SIZE / 2.0;
            double wy = tile[1] * TILE_SIZE + TILE_SIZE / 2.0;
            Weapon lazerRifle = new Weapon("Laser Rifle", rifleDamage, 10, 30, 90, false, lazerRifleSprite, soundLazer3,
                                           loadImage("assets/visual/LazerRifleFlare.png"), 2);
            weaponItems.add(new WeaponItem(wx, wy, lazerRiflePickup, lazerRifle));
        }

        // SpawnShotgun
        if (!availableTiles.isEmpty())
        {
            int index = rand.nextInt(availableTiles.size());
            int[] tile = availableTiles.remove(index);
            double wx = tile[0] * TILE_SIZE + TILE_SIZE / 2.0;
            double wy = tile[1] * TILE_SIZE + TILE_SIZE / 2.0;
            Weapon lazerShotgun = new Weapon("Laser Shotgun", shotgunDamage, 2, 8, 24, false, lazerShotgunSprite,
                                             soundLazer2, loadImage("assets/visual/LazerShotGunFlare.png"), 4);
            weaponItems.add(new WeaponItem(wx, wy, lazerShotgunPickup, lazerShotgun));
        }
        left = false;
        right = false;
        up = false;
        down = false;
    }

    private List<Weapon> createInitialWeapons()
    {
        int pistolDamage;
        switch (difficulty)
        {
        case EASY:
            pistolDamage = 15;
            break;
        case NORMAL:
            pistolDamage = 10;
            break;
        case HARD:
            pistolDamage = 5;
            break;
        default:
            pistolDamage = 10;
        }
        Image laserPistolSprite = gameAsset.getLazerPistol();
        Weapon laserPistol =
            new Weapon("Laser Pistol", pistolDamage, 5, 10, 0, true, laserPistolSprite, soundLazer1, loadImage("assets/visual/LazerPistolFlare.png"), 3);
        List<Weapon> weapons = new ArrayList<>();
        weapons.add(laserPistol);
        return weapons;
    }

    private void advanceLevel()
    {
        currentLevel++; // advance to the next level
        
        //if the current level is more than then number of levels, then player has won
        if(gameStarted && currentLevel > numOfLevels){
            currentState = GameState.VICTORY;
            playAudio(soundWinLaunch);
            //doesn't return immediately and continues to load the map into gameMap, this is to prevent any errors
        }

        if (gameStarted && currentLevel <= numOfLevels) {
            currentState = GameState.BETWEENLEVELS;
            playAudio(soundWinDoorOpen);
        }

        String mapFileName = "maps/Level01.txt";

        switch (currentLevel)
        {
        case 1:
            mapFileName = "maps/Level01.txt";
            break;
        case 2:
            mapFileName = "maps/Level02.txt";
            break;
        case 3:
            mapFileName = "maps/Level03.txt";
            break;
        case 4:
            //System.out.println("WIN WIN WIN");
            break;
        default:
            mapFileName = "maps/Level01.txt";
        }

        // Load map from file name
        if (!gameMap.loadFromFile(mapFileName))
        {
            System.err.println("Error loading map.txt, exiting.");
            System.exit(1);
        }

        spawnLevelEntities();
    }

    public void resetAnimation(){
        betweenTimer = 0;
        betweenLength = 1;
        displayEnd = false;
        escapePodX = width-100;
        escapePodY = 150;
    }
}
