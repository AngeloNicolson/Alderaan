import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

public class Main extends GameEngine
{
    private static final int SCREEN_WIDTH = 640;
    private static final int SCREEN_HEIGHT = 480;

    // Constants for Raycasting
    private static final double FOV_DEGREES = 66.0; // Common FOV for Wolfenstein-like games
    // The camera plane's length, derived from half the FOV.
    // For a FOV of 66 degrees, tan(33 degrees) is approximately 0.649.
    // Many raycasters use 0.66 as a rounded value for ~66 degrees.
    private static final double CAMERA_PLANE_FACTOR = Math.tan(Math.toRadians(FOV_DEGREES / 2.0));
    private static final double MAX_RENDER_DISTANCE = 20.0; // Max distance for rays to travel

    Player player; // Your Player object

    // Define a size for each map cell in pixels for the 2D view (used only for 2D debug drawing)
    private static final int TILE_SIZE = 20;

    // Declare a 24x24 map
    int[][] map = new int[24][24];

    private int mapWidth;
    private int mapHeight;

    // Declare Robot for mouse warping
    private Robot robot;

    // Declare the keyboard input flags directly in Main
    private boolean keyLeft;
    private boolean keyRight;
    private boolean keyUp;
    private boolean keyDown;
    private boolean isGameOver = false;

    // Offset for 2D map drawing
    private int twoDMapOffsetX;

    public Main()
    {
        // Initialize map dimensions based on the declared map array
        mapHeight = map.length;
        mapWidth = map[0].length;

        // Populate the 24x24 map programmatically for a simple maze
        // Fill with zeros (open space)
        for (int y = 0; y < mapHeight; y++)
        {
            for (int x = 0; x < mapWidth; x++)
            {
                map[y][x] = 0;
            }
        }

        // Create outer border walls
        for (int y = 0; y < mapHeight; y++)
        {
            map[y][0] = 1;            // Left border
            map[y][mapWidth - 1] = 1; // Right border
        }
        for (int x = 0; x < mapWidth; x++)
        {
            map[0][x] = 1;             // Top border
            map[mapHeight - 1][x] = 1; // Bottom border
        }

        // Add some internal walls for testing ray casting
        for (int i = 5; i < 19; i++)
        {
            map[11][i] = 1;
            map[i][11] = 1;
            map[i][18] = 1;
        }
        // Create some openings or additional blocks
        map[12][12] = 0;
        map[11][12] = 0;
        map[12][11] = 0;
        map[11][13] = 0;
        map[13][11] = 0;
        map[10][10] = 1;
        map[10][14] = 1;
        map[14][10] = 1;
        map[14][14] = 1;
        map[7][7] = 1;
        map[7][15] = 1;
        map[15][7] = 1;
        map[15][15] = 1;

        // Initialize Robot in the constructor
        try
        {
            robot = new Robot();
        }
        catch (AWTException e)
        {
            System.err.println("Robot class not available, cannot hide or warp mouse: " + e.getMessage());
            robot = null; // Set to null if unavailable
        }
    }

    @Override public void init()
    {
        // Calculate the new total window width to accommodate 3D view and 2D map
        int twoDMapDisplayWidth = mapWidth * TILE_SIZE;
        int twoDMapDisplayHeight = mapHeight * TILE_SIZE;

        // Set the window size to accommodate both 3D view and the full 2D map height
        int totalHeight = Math.max(SCREEN_HEIGHT, twoDMapDisplayHeight);
        setWindowSize(SCREEN_WIDTH + twoDMapDisplayWidth, totalHeight);
        twoDMapOffsetX = SCREEN_WIDTH;

        // Player starts more centrally in the larger map, and slightly in from the walls
        // (X, Y, Angle)
        player = new Player(12.5, 12.5, 90); // Player position in map units

        // Reset key flags when game initializes/restarts
        keyLeft = false;
        keyRight = false;
        keyUp = false;
        keyDown = false;
        isGameOver = false; // Reset game over state

        // Use SwingUtilities.invokeLater to ensure UI updates are on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            if (mPanel != null)
            {
                // Register Main as MouseMotionListener to the game panel
                mPanel.addMouseMotionListener(this);

                // Register Main as KeyListener to the panel
                mPanel.addKeyListener(this);
                // Ensure the panel can receive keyboard focus
                mPanel.setFocusable(true);
                // Request focus immediately so it starts listening for key events
                mPanel.requestFocusInWindow();
                // Add a permanent focus listener to re-request focus if lost
                mPanel.addFocusListener(new java.awt.event.FocusAdapter() {
                    @Override public void focusLost(java.awt.event.FocusEvent e)
                    {
                        System.out.println("Focus lost, re-requesting..."); // Debug
                        SwingUtilities.invokeLater(() -> mPanel.requestFocusInWindow());
                    }
                });

                // Hide the mouse cursor by using a blank cursor image
                BufferedImage cursorImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                Cursor blankCursor =
                    Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blankCursor");
                mPanel.setCursor(blankCursor);
            }
            else
            {
                System.err.println("Error: mPanel is null in Main.init() for listener setup.");
            }
        });
    }

    @Override public void update(double dt)
    {
        // Update the player's position and angle based on input
        player.update(this);
    }

    @Override public void paintComponent()
    {
        // ROOF
        changeColor(new Color(0x777777));
        drawSolidRectangle(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT / 2); // Only clear 3D view area
        // FLOOR
        changeColor(new Color(0x777777));
        drawSolidRectangle(0, SCREEN_HEIGHT / 2, SCREEN_WIDTH, SCREEN_HEIGHT / 2); // Only clear 3D view area

        // Get player's current position and angle
        double playerX = player.getX();
        double playerY = player.getY();
        double playerAngle = player.getAngle();

        // Calculate player's direction vector (pre-calculated outside loop for efficiency)
        double playerAngleRad = Math.toRadians(playerAngle);
        double playerDirX = Math.cos(playerAngleRad);
        double playerDirY = Math.sin(playerAngleRad);

        // Calculate camera plane vector (perpendicular to player direction)
        double cameraPlaneX = -playerDirY * CAMERA_PLANE_FACTOR;
        double cameraPlaneY = playerDirX * CAMERA_PLANE_FACTOR;

        /*
         * ---------------------------------------
         * --------- Raycasting Loop -------------
         * ---------------------------------------
         */
        for (int x_screen = 0; x_screen < SCREEN_WIDTH; x_screen++)
        {
            // Calculate x-coordinate on the camera plane (-1 to 1) for the current ray
            double cameraX = 2 * x_screen / (double)SCREEN_WIDTH - 1;

            // Calculate ray direction vector for this specific screen column
            double rayDirX = playerDirX + cameraPlaneX * cameraX;
            double rayDirY = playerDirY + cameraPlaneY * cameraX;

            // Current map cell of the ray (integer coordinates)
            int mapX = (int)playerX;
            int mapY = (int)playerY;

            // Length of ray from current position to next x or y-side
            double sideDistX;
            double sideDistY;

            // Length of ray from one x or y-side to next x or y-side
            double deltaDistX = (rayDirX == 0) ? 1e30 : Math.abs(1 / rayDirX);
            double deltaDistY = (rayDirY == 0) ? 1e30 : Math.abs(1 / rayDirY);

            double perpWallDist = 0; // Initialize perpWallDist
            int stepX;
            int stepY;
            int side = 0; // 0 for x-side (vertical wall), 1 for y-side (horizontal wall)

            boolean hit = false;

            // Calculate step and initial sideDist
            if (rayDirX < 0)
            {
                stepX = -1;
                sideDistX = (playerX - mapX) * deltaDistX;
            }
            else
            {
                stepX = 1;
                sideDistX = (mapX + 1.0 - playerX) * deltaDistX;
            }
            if (rayDirY < 0)
            {
                stepY = -1;
                sideDistY = (playerY - mapY) * deltaDistY;
            }
            else
            {
                stepY = 1;
                sideDistY = (mapY + 1.0 - playerY) * deltaDistY;
            }

            // --- START OF OPTIMIZED DDA LOOP ---
            int maxDdaSteps = 100; // Maximum steps the DDA will take for a single ray
            int currentDdaStep = 0;

            // Perform DDA (Digital Differential Analysis) algorithm
            while (!hit && currentDdaStep < maxDdaSteps) // Added maxDdaSteps limit
            {
                currentDdaStep++; // Increment the step counter

                if (sideDistX < sideDistY)
                {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0; // Hit a vertical wall
                }
                else
                {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = 1; // Hit a horizontal wall
                }

                // Check if ray has hit a wall or gone out of bounds
                if (mapX >= 0 && mapX < mapWidth && mapY >= 0 && mapY < mapHeight)
                {
                    if (map[mapY][mapX] == 1)
                    {
                        hit = true;
                    }
                }
                else
                {
                    hit = true; // Ray went out of map bounds
                    // No need to set perpWallDist here; it's handled after the loop
                }
            }

            // Calculate perpendicular distance to wall based on which side was hit
            if (hit)
            {
                if (side == 0) // Hit a vertical wall (x-side)
                {
                    perpWallDist = (mapX - playerX + (1 - stepX) / 2) / rayDirX;
                }
                else // Hit a horizontal wall (y-side)
                {
                    perpWallDist = (mapY - playerY + (1 - stepY) / 2) / rayDirY;
                }
            }
            else
            {
                // If the ray didn't hit a wall within maxDdaSteps, treat it as hitting max distance
                perpWallDist = MAX_RENDER_DISTANCE;
            }

            // --- Important: Clamp perpWallDist to prevent division by zero or extremely large values ---
            // This prevents rendering errors and massive performance drops when very close to a wall.
            if (perpWallDist < 0.001)
            {                         // Use a small positive number instead of 0 or negative
                perpWallDist = 0.001; // Clamp to a tiny positive value
            }
            // Also ensure it doesn't exceed the max render distance
            if (perpWallDist > MAX_RENDER_DISTANCE)
            {
                perpWallDist = MAX_RENDER_DISTANCE;
            }
            // --- END OF OPTIMIZED DDA LOOP ---

            // Calculate height of the wall slice to draw on screen
            int lineHeight = (int)(SCREEN_HEIGHT / perpWallDist);

            // Calculate lowest and highest pixel to fill current stripe (centered on screen)
            int drawStart = -lineHeight / 2 + SCREEN_HEIGHT / 2;
            if (drawStart < 0)
                drawStart = 0;
            int drawEnd = lineHeight / 2 + SCREEN_HEIGHT / 2;
            if (drawEnd >= SCREEN_HEIGHT)
                drawEnd = SCREEN_HEIGHT - 1;

            // Choose wall color based on which side was hit for basic shading
            Color wallColor;
            if (side == 1)
            {
                wallColor = new Color(0x990000); // Darker red
            }
            else
            {
                wallColor = new Color(0xFF0000); // Brighter red
            }
            changeColor(wallColor);

            // Draw the vertical line (slice) for the current screen column
            drawLine(x_screen, drawStart, x_screen, drawEnd, 1.0);
        }

        // --- Draw the 2D Top-Down Map for Debugging ---
        // Offset the drawing by twoDMapOffsetX to place it next to the 3D view
        int mapPixelWidth = mapWidth * TILE_SIZE;
        int mapPixelHeight = mapHeight * TILE_SIZE;

        // Clear the background for the 2D map
        changeColor(Color.BLACK); // Background for the 2D map area
        drawSolidRectangle(twoDMapOffsetX, 0, mapPixelWidth, mapPixelHeight);

        // 2. Draw the 2D Map itself
        for (int y_map = 0; y_map < mapHeight; y_map++)
        {
            for (int x_map = 0; x_map < mapWidth; x_map++)
            {
                if (map[y_map][x_map] == 1) // It's a wall
                {
                    changeColor(Color.GRAY); // Draw walls in gray
                }
                else // It's an empty space
                {
                    changeColor(Color.DARK_GRAY); // Draw empty space in dark gray
                }
                // Draw the map cell, scaled by TILE_SIZE, offset by twoDMapOffsetX
                drawSolidRectangle(x_map * TILE_SIZE + twoDMapOffsetX, y_map * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        // 3. Draw the Player as a small blue circle on the 2D map
        changeColor(Color.BLUE); // Player color
        // Convert player's map coordinates to screen pixel coordinates, with offset
        int playerScreenX_2D = (int)(playerX * TILE_SIZE) + twoDMapOffsetX;
        int playerScreenY_2D = (int)(playerY * TILE_SIZE);
        int playerRadius = TILE_SIZE / 4; // Make player circle size proportional to tile size
        drawSolidCircle(playerScreenX_2D - playerRadius, playerScreenY_2D - playerRadius, playerRadius * 2);

        // 4. Draw Player's direction line (Cyan line extending from player) on the 2D map
        changeColor(Color.CYAN);             // Direction line color
        double lineLength = TILE_SIZE * 0.7; // Length of the direction line
        // Calculate end point of direction line based on player's angle, with offset
        int lineEndX_2D = (int)(playerScreenX_2D + Math.cos(Math.toRadians(playerAngle)) * lineLength);
        int lineEndY_2D = (int)(playerScreenY_2D + Math.sin(Math.toRadians(playerAngle)) * lineLength);
        drawLine(playerScreenX_2D, playerScreenY_2D, lineEndX_2D, lineEndY_2D, 2.0); // Draw a thicker line

        // 5. Draw Rays from the player's perspective (Red lines) on the 2D map
        changeColor(Color.RED);
        int numRays = 60;             // Fewer rays for 2D debug to reduce clutter
        final double STEP_SIZE = 0.5; // Larger step size for 2D rays for visibility

        for (int i = 0; i < numRays; i++)
        {
            double rayAngle = (playerAngle - FOV_DEGREES / 2.0) + ((double)i / numRays) * FOV_DEGREES;
            rayAngle = (rayAngle + 360) % 360; // Normalize angle

            double rayXDir = Math.cos(Math.toRadians(rayAngle));
            double rayYDir = Math.sin(Math.toRadians(rayAngle));

            double currentRayDistance = 0;
            boolean hitWall = false;

            int rayLineEndX_2D = playerScreenX_2D;
            int rayLineEndY_2D = playerScreenY_2D;

            while (!hitWall && currentRayDistance < MAX_RENDER_DISTANCE)
            {
                currentRayDistance += STEP_SIZE;

                double testMapX = playerX + rayXDir * currentRayDistance;
                double testMapY = playerY + rayYDir * currentRayDistance;

                int testCellX = (int)testMapX;
                int testCellY = (int)testMapY;

                // Ensure testCellX and testCellY are within map bounds before checking map array
                if (testCellX >= 0 && testCellX < mapWidth && testCellY >= 0 && testCellY < mapHeight)
                {
                    if (map[testCellY][testCellX] == 1)
                    {
                        hitWall = true;
                    }
                }
                else
                {
                    hitWall = true; // Ray went out of map bounds
                }

                // Convert ray end point to screen coordinates, with offset
                rayLineEndX_2D = (int)(testMapX * TILE_SIZE) + twoDMapOffsetX;
                rayLineEndY_2D = (int)(testMapY * TILE_SIZE);
            }
            drawLine(playerScreenX_2D, playerScreenY_2D, rayLineEndX_2D, rayLineEndY_2D, 1.0);
        }
    }
    // -------------------------------------------
    // ----------- USER INPUT FUNCTIONS ----------
    // -------------------------------------------

    // -- Keyboard Inputs --

    @Override public void keyPressed(KeyEvent e)
    {
        if (isGameOver)
        {
            if (e.getKeyCode() == KeyEvent.VK_SPACE)
            {
                init();
                isGameOver = false;
            }
        }
        else
        {
            if (e.getKeyCode() == KeyEvent.VK_A)
            {
                keyLeft = true;
            }
            if (e.getKeyCode() == KeyEvent.VK_D)
            {
                keyRight = true;
            }
            if (e.getKeyCode() == KeyEvent.VK_W)
            {
                keyUp = true;
            }
            if (e.getKeyCode() == KeyEvent.VK_S)
            {
                keyDown = true;
            }
        }
    }

    @Override public void keyReleased(KeyEvent e)
    {
        if (!isGameOver)
        {
            if (e.getKeyCode() == KeyEvent.VK_A)
            {
                keyLeft = false;
            }
            if (e.getKeyCode() == KeyEvent.VK_D)
            {
                keyRight = false;
            }
            if (e.getKeyCode() == KeyEvent.VK_W)
            {
                keyUp = false;
            }
            if (e.getKeyCode() == KeyEvent.VK_S)
            {
                keyDown = false;
            }
        }
    }

    // Public getter methods for keyboard input flags, for Player to use
    public boolean isKeyLeftPressed()
    {
        return keyLeft;
    }
    public boolean isKeyRightPressed()
    {
        return keyRight;
    }
    public boolean isKeyUpPressed()
    {
        return keyUp;
    }
    public boolean isKeyDownPressed()
    {
        return keyDown;
    }

    // Getter methods for map data, for collision detection in Player class
    public int[][] getMap()
    {
        return map;
    }
    public int getMapWidth()
    {
        return mapWidth;
    }
    public int getMapHeight()
    {
        return mapHeight;
    }

    // -- Mouse Movements --
    @Override public void mouseMoved(MouseEvent e)
    {
        if (!isGameOver)
        {
            if (player == null || mPanel == null)
            {
                return;
            }
            int centerX = SCREEN_WIDTH / 2; // Only consider the 3D view width for mouse centering
            int mouseX = e.getX();
            double sensitivity = 0.1;

            // Only respond to mouse movement within the 3D view area
            if (mouseX < SCREEN_WIDTH)
            { // Check if mouse is in the 3D view section
                int deltaX = mouseX - centerX;
                player.setAngle(player.getAngle() + deltaX * sensitivity);
                player.setAngle((player.getAngle() + 360) % 360);

                // Reset cursor to center of 3D view
                if (robot != null)
                {
                    try
                    {
                        Point panelLocation = mPanel.getLocationOnScreen();
                        robot.mouseMove(panelLocation.x + centerX, panelLocation.y + mPanel.getHeight() / 2);
                    }
                    catch (Exception ex)
                    {
                        System.err.println("Error warping mouse: " + ex.getMessage());
                    }
                }
            }
        }
    }
    @Override public void mouseDragged(MouseEvent e)
    {
        mouseMoved(e);
    }

    public static void main(String[] args)
    {
        Main game = new Main();
        createGame(game, 60);
    }
}
