import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class Main extends GameEngine
{
    public static final int TILE_SIZE = 32;
    private GameMap gameMap;
    private Player player;
    private RayCaster raycaster;
    private GameAsset gameAsset;

    // Window size
    private int width = 1024;
    private int height = 512;

    // Direction flags
    private boolean left, right, up, down;

    // Mouse handling
    private int lastMouseX, lastMouseY;
    private boolean firstMouseUpdate = true;
    private Robot robot;

    // Weapon Drawing
    private double weaponX;
    private double weaponY;

    //declare audio assets
    private AudioClip soundLazer1;
    private AudioClip soundLazer2;
    private AudioClip soundLazer3;
    private AudioClip soundLazerHit;
    private AudioClip soundPlayerInjured;
    private AudioClip soundPlayerWalking;
    private AudioClip soundWinDoorOpen;
    private AudioClip soundWinLaunch;
    private AudioClip soundZombieDeath;
    private AudioClip soundZombieNeutral;

    public static void main(String[] args)
    {
        Main main = new Main();
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

        // Initialise Weapon Default Position
        weaponX = width()/2+200;
        weaponY = height()-200;
        
        // Initialise Audio Assets
        soundLazer1 = loadAudio("assets/audio/SoundLazer1.wav");
        soundLazer2 = loadAudio("assets/audio/SoundLazer2.wav");
        soundLazer3 = loadAudio("assets/audio/SoundLazer3.wav");
        soundLazerHit = loadAudio("assets/audio/SoundLazerHit.wav");
        soundPlayerInjured = loadAudio("assets/audio/SoundPlayerInjured.wav");
        soundPlayerWalking = loadAudio("assets/audio/SoundPlayerWalking.wav");
        soundWinDoorOpen = loadAudio("assets/audio/SoundWinDoorOpen.wav");
        soundWinLaunch = loadAudio("assets/audio/SoundWinLaunch.wav");
        soundZombieDeath = loadAudio("assets/audio/SoundZombieDeath.wav");
        soundZombieNeutral = loadAudio("assets/audio/SoundZombieNeutral.wav");

    }

    @Override public void update(double dt)
    {
        player.setDirection(left, right, up, down);
        player.update(this, dt);
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

        raycaster.draw(this, playerX, playerY, playerAngle, verticalLookOffset);

        // --- Weapon ---

        drawImage(gameAsset.getLazerPistol(), weaponX, weaponY, 512, 512);

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

    // Method for when mouse button is pressed
    public void mousePressed(java.awt.event.MouseEvent e){
        playAudio(soundLazer1);
    }

    // Method for when mouse button is released
    public void mouseReleased(java.awt.event.MouseEvent e){

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
