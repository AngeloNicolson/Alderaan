import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;

enum GameState
{
    PLAYING,
    GAME_OVER,
}

public class Main extends GameEngine
{
    public static final int TILE_SIZE = 32;
    private GameMap gameMap;
    private Player player;
    private RayCaster raycaster;
    private List<Enemy> enemies = new ArrayList<>();
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
    private boolean warpingMouse = false;

    private double weaponX = 300;
    private double weaponY = 300;
    private List<Weapon> initialWeapons;
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
        gameMap = new GameMap();

        // Load map from file
        if (!gameMap.loadFromFile("maps/map_1.txt"))
        {
            System.err.println("Error loading map.txt, exiting.");
            System.exit(1);
        }

        setWindowSize(width, height);

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

        // Collect all walkable tiles for enemy spawning
        List<int[]> walkableTiles = new ArrayList<>();
        for (int y = 0; y < GameMap.HEIGHT; y++)
        {
            for (int x = 0; x < GameMap.WIDTH; x++)
            {
                if (gameMap.isWalkableTile(x, y))
                    walkableTiles.add(new int[] {x, y});
            }
        }

        // Adds the enemies
        Random rand = new Random();
        int enemyCount = 7;
        enemies.clear(); // clear existing enemies if any
        for (int i = 0; i < enemyCount; i++)
        {
            // Pick a random tile index
            int[] tile = walkableTiles.get(rand.nextInt(walkableTiles.size()));

            double ex = tile[0] * TILE_SIZE + TILE_SIZE / 2.0;
            double ey = tile[1] * TILE_SIZE + TILE_SIZE / 2.0;

            enemies.add(new Enemy(ex, ey, "", gameMap, TILE_SIZE));
        }

        // Initialize ray caster and associated objects
        gameAsset = new GameAsset();

        Image laserPistolSprite = gameAsset.getLazerPistol();
        AudioClip laserPistolSound = soundLazer1;
        Weapon laserPistol = new Weapon("Laser Pistol", 10, 5, 10, 0, true, laserPistolSprite, laserPistolSound);
        List<Weapon> initialWeapons = new ArrayList<>();
        initialWeapons.add(laserPistol);
        this.initialWeapons = initialWeapons;

        // Initialize player once using your method
        initializePlayer(initialWeapons);


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
        player.setDirection(left, right, up, down);
        player.update(this, dt);
        // Update Enemies
        if (currentState == GameState.PLAYING)
        {
            player.setDirection(left, right, up, down);
            player.update(this, dt);
            player.getCurrentWeapon().update(dt);
            for (Enemy enemy : enemies)
            {
                enemy.update(this, dt, player);
            }
            if (!player.isAlive())
            {
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



            // Health bar on bottom left
            changeColor(Color.gray);
            drawSolidRectangle(10, height() - 50, 200, 20);
            changeColor(green);
            float healthPercentage = player.getHealthPercentage();
            drawSolidRectangle(10, height() - 50, 200 * healthPercentage, 20);
            changeColor(white);
            drawText(220, height() - 30, player.getHealth() + "/" + player.getMaxHealth(), "Arial", 20);

            //Weapon Sprite
            Weapon currentWeapon = player.getCurrentWeapon();
            if (currentWeapon.getSprite() != null) {
                drawImage(currentWeapon.getSprite(), weaponX, weaponY, 512, 512);
            }
            // Weapon name and ammo count on bottom right
            String weaponName = currentWeapon.getName();
            changeColor(green);
            drawText(width() - 120, height() - 70, weaponName, "Arial", 16);
            if (currentWeapon.isUnlimitedAmmo()) {
                drawText(width() - 100, height() - 10, "\u221E", "Arial", 60);
            } else {
                String ammoText = currentWeapon.getCurrentMagAmmo() + " / " + currentWeapon.getTotalAmmo();
                drawText(width() - 120, height() - 20, ammoText, "Arial", 30);
            }
            // --- RENDER ENEMIES ---
            for (Enemy enemy : enemies) {
                enemy.render(this, player, raycaster.getRayDistancesArray());
                enemy.drawOnMinimap(this);
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

            for (Enemy enemy : enemies) {
                enemy.drawOnMinimap(this);
            }
        }
        else if (currentState == GameState.GAME_OVER)
        {
            changeBackgroundColor(black);
            clearBackground(width(), height());
            changeColor(red);
            drawCenteredText(height / 2, "GAME OVER", "Arial", 50);
            changeColor(white);
            drawCenteredText(height / 2 + 50, "Press Enter to restart", "Arial", 30);
        }
    }

    public void drawCenteredText(double y, String s, String font, int size)
    {
        mGraphics.setFont(new Font(font, Font.PLAIN, size));
        FontMetrics metrics = mGraphics.getFontMetrics();
        int textWidth = metrics.stringWidth(s);
        int x = (width - textWidth) / 2;
        mGraphics.drawString(s, x, (int)y);
    }

    @Override public void keyPressed(KeyEvent e)
    {
        if (currentState == GameState.PLAYING)
        {
            switch (e.getKeyCode())
            {
            case KeyEvent.VK_Q:
                player.previousWeapon();
                break;
            case KeyEvent.VK_E:
                player.nextWeapon();
                break;
            case KeyEvent.VK_M:
                player.takeDamage(10); // Simulates Damage
                break;
            case KeyEvent.VK_R:
                player.getCurrentWeapon().reload();
                break;
            case KeyEvent.VK_1:
                player.pickupWeapon("Laser Rifle"); // Simulates Picking up the Laser Rifle
                break;
            case KeyEvent.VK_2:
                player.pickupWeapon("Laser Shotgun"); // Simulates Picking up the Shotgun
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
    }

    @Override public void keyReleased(KeyEvent e)
    {
        if (currentState == GameState.PLAYING)
        {
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
        if (currentState == GameState.PLAYING && e.getButton() == MouseEvent.BUTTON1)
        {
            Weapon currentWeapon = player.getCurrentWeapon();
            if (player.getCurrentWeapon().tryFire())
            {
                System.out.println("Fired " + player.getCurrentWeapon().getName());
                if(currentWeapon.getFireSound() != null){
                    playAudio(currentWeapon.getFireSound());
                }
                // Actual shooting logic coming soon ...
            }
        }
    }

    private void initializePlayer(List<Weapon> weapons) {
        outer:
        for (int y = 0; y < GameMap.HEIGHT; y++) {
            for (int x = 0; x < GameMap.WIDTH; x++) {
                if (gameMap.isWalkableTile(x, y)) {
                    double px = x * TILE_SIZE + TILE_SIZE / 2.0;
                    double py = y * TILE_SIZE + TILE_SIZE / 2.0;
                    player = new Player(px, py, gameMap, TILE_SIZE, weapons);
                    break outer;
                }
            }
        }
    }

    private void restartGame()
    {
        initializePlayer(initialWeapons);
        currentState = GameState.PLAYING;
        left = false;
        right = false;
        up = false;
        down = false;
    }
}
