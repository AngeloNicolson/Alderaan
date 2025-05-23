import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Main extends GameEngine
{
    public static final int TILE_SIZE = 32;
    private GameMap gameMap;
    private Player player;
    private RayCaster raycaster;
    private List<Enemy> enemies = new ArrayList<>();

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
        createGame(main, 60);
    }

    @Override public void init()
    {
        gameMap = new GameMap();

        // Load map from file
        if (!gameMap.loadFromFile("maps/PlayGround_Map.txt"))
        {
            System.err.println("Error loading map.txt, exiting.");
            System.exit(1);
        }

        setWindowSize(width, height);

    // Find a walkable spawn tile
    outer:
        for (int y = 0; y < GameMap.HEIGHT; y++)
        {
            for (int x = 0; x < GameMap.WIDTH; x++)
            {
                if (gameMap.isWalkableTile(x, y))
                {
                    double px = x * TILE_SIZE + TILE_SIZE / 2.0;
                    double py = y * TILE_SIZE + TILE_SIZE / 2.0;
                    player = new Player(px, py, gameMap, TILE_SIZE);
                    break outer;
                }
            }
        }
        enemies.add(
            new Enemy(5 * TILE_SIZE + TILE_SIZE / 2.0, 5 * TILE_SIZE + TILE_SIZE / 2.0, "", gameMap, TILE_SIZE));

        raycaster = new RayCaster(gameMap, TILE_SIZE);

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
        player.setDirection(left, right, up, down);
        player.update(this, dt);
        // Update Enemies
        for (Enemy enemy : enemies)
        {
            enemy.update(this, dt, player);
        }
    }

    @Override public void paintComponent()
    {
        changeBackgroundColor(black);
        clearBackground(width(), height());

        // --- 2.5D RENDERING ---
        double playerX = player.getX();
        double playerY = player.getY();
        double playerAngle = player.getAngle();
        double verticalLookOffset = player.getVerticalLookOffset();

        int numRays = 256;
        double fov = Math.toRadians(60);

        double[] rayDistances = raycaster.castRays(playerX, playerY, playerAngle, numRays, fov);

        double stripWidth = (double)width / numRays; // full width for 2.5D view

        for (int i = 0; i < numRays; i++)
        {
            double dist = rayDistances[i];
            // Correct fisheye
            double angleOffset = (i - numRays / 2.0) * (fov / numRays);
            dist *= Math.cos(angleOffset);

            double lineHeight = (TILE_SIZE * 320) / dist;
            double yOffset = (height - lineHeight) / 2 - verticalLookOffset;

            double maxDistance = 500;
            double brightness = Math.max(0.2, 1.0 - dist / maxDistance);
            int shade = (int)(brightness * 255);

            // Limit the max height, so it doesnt become huge, rendering off screen
            double maxLineHeight = height * 2;
            if (lineHeight > maxLineHeight)
                lineHeight = maxLineHeight;
            changeColor(new Color(shade, shade, shade));

            drawSolidRectangle(i * stripWidth, yOffset, stripWidth + 1, lineHeight);
        }
        // --- RENDER ENEMIES ---
        for (Enemy enemy : enemies)
        {
            enemy.render(this, player, rayDistances);
        }

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
        int visionRadius = 7;
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
    }

    @Override public void keyPressed(KeyEvent e)
    {
        updateDirection(e, true);
    }

    @Override public void keyReleased(KeyEvent e)
    {
        updateDirection(e, false);
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

    @Override public void mouseMoved(java.awt.event.MouseEvent e)
    {

        // Camera/Player sensitivity
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
