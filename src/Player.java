// Player.java

import java.awt.event.KeyEvent;

public class Player extends Entity
{
    private double angle;
    private final double speed = 0.08;
    private final double PLAYER_RADIUS = 0.3; // This defines how far the player is from the center

    public Player(double x, double y, double angle)
    {
        super(x, y); // This correctly initializes the 'x' and 'y' inherited from Entity
        this.angle = angle;
    }

    @Override public void update(GameEngine engine)
    {
        double moveX = 0, moveY = 0;

        Main mainGame = null;
        int[][] map = null;
        int mapWidth = 0;
        int mapHeight = 0;

        if (engine instanceof Main)
        {
            mainGame = (Main)engine;
            map = mainGame.getMap();
            mapWidth = mainGame.getMapWidth();
            mapHeight = mainGame.getMapHeight();

            if (mainGame.isKeyUpPressed())
            {
                moveX += Math.cos(Math.toRadians(angle)) * speed;
                moveY += Math.sin(Math.toRadians(angle)) * speed;
            }
            if (mainGame.isKeyDownPressed())
            {
                moveX -= Math.cos(Math.toRadians(angle)) * speed;
                moveY -= Math.sin(Math.toRadians(angle)) * speed;
            }

            if (mainGame.isKeyLeftPressed())
            {
                moveX += Math.cos(Math.toRadians(angle - 90)) * speed;
                moveY += Math.sin(Math.toRadians(angle - 90)) * speed;
            }
            if (mainGame.isKeyRightPressed())
            {
                moveX += Math.cos(Math.toRadians(angle + 90)) * speed;
                moveY += Math.sin(Math.toRadians(angle + 90)) * speed;
            }
        }
        else
        {
            System.out.println("ERROR: 'engine' parameter in Player.update() is not an instance of Main.");
            return;
        }

        // --- Collision Detection and Response (UPDATED LOGIC) ---
        // Calculate potential new positions
        double newX_tentative = x + moveX;
        double newY_tentative = y + moveY;

        // Check X-axis collision first
        // Test the point that would be at the edge of the player's radius in the direction of X movement
        int testMapX_for_X_move = (int)(newX_tentative + Math.signum(moveX) * PLAYER_RADIUS);
        int testMapY_for_X_move = (int)y; // Keep current Y for X-axis check

        if (map != null)
        {
            // Check if the X-axis movement would place player into a wall or out of bounds
            if (testMapX_for_X_move < 0 || testMapX_for_X_move >= mapWidth ||
                map[testMapY_for_X_move][testMapX_for_X_move] == 1) // Using map[y][x]
            {
                // Collision detected in X direction. Snap player to the edge of the wall.
                if (moveX > 0)
                {                                                          // Moving right (hit left edge of wall cell)
                    this.x = testMapX_for_X_move - PLAYER_RADIUS - 0.0001; // Snap to left edge, slight epsilon
                }
                else
                { // Moving left (hit right edge of wall cell)
                    this.x = testMapX_for_X_move + 1 + PLAYER_RADIUS + 0.0001; // Snap to right edge, slight epsilon
                }
                moveX = 0; // Prevent further X movement for this frame
            }
        }

        // If no X collision, then apply the X movement
        this.x += moveX;

        // Check Y-axis collision (using possibly updated X)
        // Test the point that would be at the edge of the player's radius in the direction of Y movement
        int testMapX_for_Y_move = (int)x; // Use the (possibly updated) current X for Y-axis check
        int testMapY_for_Y_move = (int)(newY_tentative + Math.signum(moveY) * PLAYER_RADIUS);

        if (map != null)
        {
            // Check if the Y-axis movement would place player into a wall or out of bounds
            if (testMapY_for_Y_move < 0 || testMapY_for_Y_move >= mapHeight ||
                map[testMapY_for_Y_move][testMapX_for_Y_move] == 1) // Using map[y][x]
            {
                // Collision detected in Y direction. Snap player to the edge of the wall.
                if (moveY > 0)
                {                                                          // Moving down (hit top edge of wall cell)
                    this.y = testMapY_for_Y_move - PLAYER_RADIUS - 0.0001; // Snap to top edge, slight epsilon
                }
                else
                { // Moving up (hit bottom edge of wall cell)
                    this.y = testMapY_for_Y_move + 1 + PLAYER_RADIUS + 0.0001; // Snap to bottom edge, slight epsilon
                }
                moveY = 0; // Prevent further Y movement for this frame
            }
        }

        // Apply remaining Y movement
        this.y += moveY;
    }

    // These getX() and getY() methods will now correctly return the
    // 'x' and 'y' inherited from the Entity class.
    @Override public double getX()
    {
        return super.getX();
    }
    @Override public double getY()
    {
        return super.getY();
    }

    public double getAngle()
    {
        return angle;
    }

    public void setAngle(double angle)
    {
        this.angle = angle;
    }
}
