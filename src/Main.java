import javax.swing.*;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.Font;
import java.awt.FontMetrics;

enum GameState {
    PLAYING,
    GAME_OVER,
}


public class Main extends GameEngine
{
    public static final int TILE_SIZE = 32;
    private GameMap gameMap;
    private Player player;
    private RayCaster raycaster;
    private GameAsset gameAsset;
    private GameState currentState = GameState.PLAYING;

    // Window size
    private int width = 1024;
    private int height = 512;

    // Direction flags
    private boolean left, right, up, down;

    // Mouse handling
    private int lastMouseX, lastMouseY;
    private boolean firstMouseUpdate = true;
    private Robot robot;

    public static void main(String[] args)
    {
        Main main = new Main();
        try {
            SwingUtilities.invokeAndWait(() -> main.setupWindow(main.width, main.height));
        } catch (Exception e) {
            System.err.println("Error setting up window: " + e.getMessage());
            System.exit(1);
        }
        main.init();
        createGame(main, 60);
    }
    @Override public void init()
    {
        gameMap = new GameMap();

        // Load map from file
        if (!gameMap.loadFromFile("maps/map_1.txt"))
        {
            System.err.println("Error loading map.txt, exiting.");
            System.exit(1);
        }

        setWindowSize(width, height);
        initializePlayer();

        // Initalise ray caster and associated objects
        gameAsset = new GameAsset();

        raycaster = new RayCaster(gameMap, TILE_SIZE, gameAsset);

        // Initialize Robot for mouse control
        try
        {
            robot = new Robot();
        }
        catch (AWTException e)
        {
            System.err.println("Failed to initialize Robot: " + e.getMessage());
        }

        // Hide cursor with transparent image
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
        getFrame().setCursor(blankCursor);

        lastMouseX = width / 2;
        lastMouseY = height / 2;
    }

    @Override public void update(double dt)
    {
        if (currentState == GameState.PLAYING) {
            player.setDirection(left, right, up, down);
            player.update(this, dt);
            player.getCurrentWeapon().update(dt);
            if (!player.isAlive()) {
                currentState = GameState.GAME_OVER;
            }
        }
    }

    @Override public void paintComponent()
    {
        if (currentState == GameState.PLAYING) {
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

            // Health bar on bottom left
            changeColor(Color.gray);
            drawSolidRectangle(10, height() - 50, 200, 20);
            changeColor(green);
            float healthPercentage = player.getHealthPercentage();
            drawSolidRectangle(10, height() - 50, 200 * healthPercentage, 20);
            changeColor(white);
            drawText(220, height() - 30, player.getHealth() + "/" + player.getMaxHealth(), "Arial", 20);

            // Weapon name and ammo count on bottom right
            Weapon currentWeapon = player.getCurrentWeapon();
            String weaponName = currentWeapon.getName();
            changeColor(green);
            drawText(width() - 120, height() - 70, weaponName, "Arial", 16);
            if(currentWeapon.isUnlimitedAmmo()){
                drawText(width() - 100, height() - 10, "\u221E", "Arial", 60);
            }else{
                String ammoText = currentWeapon.getCurrentMagAmmo() + " / " + currentWeapon.getTotalAmmo();
                drawText(width() - 120, height() - 20, ammoText, "Arial", 30);
            }
        } else if (currentState == GameState.GAME_OVER) {
            changeBackgroundColor(black);
            clearBackground(width(), height());
            changeColor(red);
            drawCenteredText(height / 2, "GAME OVER", "Arial", 50);
            changeColor(white);
            drawCenteredText(height / 2 + 50, "Press Enter to restart", "Arial", 30);
        }
    }

    public void drawCenteredText(double y, String s, String font, int size) {
        mGraphics.setFont(new Font(font, Font.PLAIN, size));
        FontMetrics metrics = mGraphics.getFontMetrics();
        int textWidth = metrics.stringWidth(s);
        int x = (width - textWidth) / 2;
        mGraphics.drawString(s, x, (int) y);
    }

    @Override public void keyPressed(KeyEvent e) {
        if (currentState == GameState.PLAYING) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_Q:
                    player.previousWeapon();
                    break;
                case KeyEvent.VK_E:
                    player.nextWeapon();
                    break;
                case KeyEvent.VK_M:
                    player.takeDamage(10);  //Simulates Damage
                    break;
                case KeyEvent.VK_R:
                    player.getCurrentWeapon().reload();
                    break;
                case KeyEvent.VK_1:
                    player.pickupWeapon("Laser Rifle"); //Simulates Picking up the Laser Rifle
                    break;
                case KeyEvent.VK_2:
                    player.pickupWeapon("Laser Shotgun");  //Simulates Picking up the Shotgun
                    break;
                default:
                    updateDirection(e, true);
                    break;
            }
        } else if (currentState == GameState.GAME_OVER) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                restartGame();
            }
        }
    }

    @Override public void keyReleased(KeyEvent e)
    {
        if (currentState == GameState.PLAYING) {
            updateDirection(e, false);
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

    @Override public void mouseMoved(java.awt.event.MouseEvent e) {
        if (currentState == GameState.PLAYING) {
            // Camera/Player sensitivity
            double sensitivityX = 0.002;
            double sensitivityY = 0.5;

            int mouseX = e.getXOnScreen();
            int mouseY = e.getYOnScreen();

            int windowCenterX = getFrame().getLocationOnScreen().x + width / 2;
            int windowCenterY = getFrame().getLocationOnScreen().y + height / 2;

            if (firstMouseUpdate) {
                lastMouseX = mouseX;
                lastMouseY = mouseY;
                firstMouseUpdate = false;
            }

            int deltaX = mouseX - lastMouseX;
            int deltaY = mouseY - lastMouseY;

            player.rotate(deltaX * sensitivityX);
            player.setVerticalLookOffset(player.getVerticalLookOffset() + deltaY * sensitivityY);

            if (robot != null) {
                robot.mouseMove(windowCenterX, windowCenterY);
                lastMouseX = windowCenterX;
                lastMouseY = windowCenterY;
            } else {
                lastMouseX = mouseX;
                lastMouseY = mouseY;
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (currentState == GameState.PLAYING && e.getButton() == MouseEvent.BUTTON1) {
            if (player.getCurrentWeapon().tryFire()) {
                System.out.println("Fired " + player.getCurrentWeapon().getName());
                // Actual shooting logic coming soon ...
            }
        }
    }


    private void initializePlayer() {
        outer:
        for (int y = 0; y < GameMap.HEIGHT; y++) {
            for (int x = 0; x < GameMap.WIDTH; x++) {
                if (gameMap.isWalkableTile(x, y)) {
                    double px = x * TILE_SIZE + TILE_SIZE / 2.0;
                    double py = y * TILE_SIZE + TILE_SIZE / 2.0;
                    player = new Player(px, py, gameMap, TILE_SIZE);
                    break outer;
                }
            }
        }
    }

    private void restartGame(){
        initializePlayer();
        currentState = GameState.PLAYING;
        left = false;
        right = false;
        up = false;
        down = false;
    }
}