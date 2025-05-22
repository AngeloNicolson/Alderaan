import java.awt.Color;
import java.awt.Image;

public class Enemy extends Entity
{
    private double angle;
    private GameMap map;
    private int mapS;
    private String enemyType;
    private Image sprite;

    private double speed = 60; // pixels per second

    public Enemy(double x, double y, String enemyType, GameMap map, int mapS)
    {
        super(x, y);
        this.enemyType = enemyType;
        this.map = map;
        this.mapS = mapS;

        // Load different sprite based on type
        switch (enemyType)
        {
        case "Death Trooper" -> this.sprite = GameEngine.loadImage("assets/visual/Storm_Trooper_Placeholder.png");
        case "Do not use yet" -> this.sprite = GameEngine.loadImage("assets/placeholder.png");
        default -> this.sprite = GameEngine.loadImage("assets/visual/Storm_Trooper_Placeholder.png");
        }
    }

    @Override public void update(GameEngine engine, double dt, Player player)
    {
        // Vector from enemy to player
        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 1e-5)
        { // avoid division by zero
            // Normalize direction vector
            double dirX = dx / dist;
            double dirY = dy / dist;

            // Enemy speed = half the player speed
            double enemySpeed = player.getSpeed() / 2;

            // Calculate potential new position
            double newX = x + dirX * enemySpeed * dt;
            double newY = y + dirY * enemySpeed * dt;

            // Check if new tile is walkable
            int tileX = (int)(newX / mapS);
            int tileY = (int)(newY / mapS);

            if (map.isWalkableTile(tileX, tileY))
            {
                x = newX;
                y = newY;
            }

            // Update angle to face player
            angle = Math.atan2(dy, dx);
        }
    }

    public void render(GameEngine engine, Player player)
    {
        double dx = this.x - player.getX();
        double dy = this.y - player.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Angle between player and enemy
        double angleToEnemy = Math.atan2(dy, dx) - player.getAngle();

        // Normalize angle to (-PI, PI)
        while (angleToEnemy < -Math.PI)
            angleToEnemy += 2 * Math.PI;
        while (angleToEnemy > Math.PI)
            angleToEnemy -= 2 * Math.PI;

        // FOV and screen projection
        double fov = Math.toRadians(60);
        if (Math.abs(angleToEnemy) < fov / 2)
        {
            int screenWidth = engine.width();
            int screenHeight = engine.height();

            // Enemy on screen (center X)
            double projectionPlaneDist = screenWidth / (2 * Math.tan(fov / 2));
            double screenX = Math.tan(angleToEnemy) * projectionPlaneDist + screenWidth / 2;

            // Projected height based on distance
            double spriteHeight = (Main.TILE_SIZE * 320) / distance;

            // Adjust Y position by vertical look offset
            double yOffset = (screenHeight - spriteHeight) / 2 - player.getVerticalLookOffset();

            engine.changeColor(Color.RED); // or draw sprite here
            engine.drawSolidRectangle(screenX - spriteHeight / 2, yOffset, spriteHeight, spriteHeight);
        }
    }
}
