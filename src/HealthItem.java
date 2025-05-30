import java.awt.*;

public class HealthItem
{
    private double x, y;
    private Image sprite;
    private boolean consumed;

    public HealthItem(double x, double y, Image sprite)
    {
        this.x = x;
        this.y = y;
        this.sprite = sprite;
        this.consumed = false;
    }

    public void render(GameEngine engine, Player player, double[] rayDistances)
    {
        if (consumed)
            return; // No render if consumed

        // Calculate distance and angle from player to health item
        double dx = x - player.getX();
        double dy = y - player.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        double angleToPlayer = Math.atan2(dy, dx) - player.getAngle();
        // Normalize angle
        if (angleToPlayer < -Math.PI)
            angleToPlayer += 2 * Math.PI;
        if (angleToPlayer > Math.PI)
            angleToPlayer -= 2 * Math.PI;

        double screenWidth = engine.width();
        double fov = Math.toRadians(60);
        double halfFOV = fov / 2;
        double screenX = (angleToPlayer / halfFOV) * (screenWidth / 2) + (screenWidth / 2);

        if (screenX < 0 || screenX >= screenWidth)
            return;

        int numRays = rayDistances.length;
        double stripWidth = (double)screenWidth / numRays;
        int rayIndex = (int)(screenX / stripWidth);

        // Ensure within bounds
        if (rayIndex < 0 || rayIndex >= numRays)
            return;

        // only render if the item is closer than the wall
        if (distance >= rayDistances[rayIndex])
            return;

        // Scale sprite based on distance
        double spriteSize = (engine.height() * Main.TILE_SIZE / (distance * 2));
        double maxSpriteSize = engine.height() / 3; // Cap
        if (spriteSize > maxSpriteSize)
            spriteSize = maxSpriteSize;
        if (spriteSize < 1)
            spriteSize = 1;

        // Offset to change vertical placement (make it appear closer to the ground)
        double pseudoLineHeight = (Main.TILE_SIZE * 640) / distance;
        double floorY = (engine.height() + pseudoLineHeight) / 2 - player.getVerticalLookOffset();
        double screenY = floorY - spriteSize;

        engine.drawImage(sprite, screenX - spriteSize / 2, screenY, spriteSize, spriteSize);
    }

    public boolean isConsumed()
    {
        return consumed;
    }

    // Consume item and increase player health
    public void consume(Player player)
    {
        if (!consumed)
        {
            player.restoreHealth(20);
            consumed = true;
        }
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }
}
