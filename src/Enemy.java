import java.awt.Color;

public class Enemy extends Entity
{
    private String name;
    private GameMap map;
    private int tileSize;

    public Enemy(double x, double y, String name, GameMap map, int tileSize)
    {
        super(x, y);
        this.name = name;
        this.map = map;
        this.tileSize = tileSize;
    }

    @Override public void update(GameEngine game, double dt)
    {
    }

    public void render(GameEngine g, Player player)
    {
        // Draw enemy on 2.5D screen view — simplified example
        double dx = getX() - player.getX();
        double dy = getY() - player.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        double angleToEnemy = Math.atan2(dy, dx);
        double relativeAngle = angleToEnemy - player.getAngle();

        while (relativeAngle > Math.PI)
            relativeAngle -= 2 * Math.PI;
        while (relativeAngle < -Math.PI)
            relativeAngle += 2 * Math.PI;

        double fov = Math.toRadians(60);

        if (Math.abs(relativeAngle) < fov / 2 && dist > 0.1)
        {
            int screenWidth = g.width();
            int screenHeight = g.height();

            double screenX = (0.5 + relativeAngle / fov) * screenWidth;

            // Size inversely proportional to distance
            double size = 64 * tileSize / dist;

            g.changeColor(Color.RED);
            g.drawSolidRectangle(screenX - size / 2, (screenHeight - size) / 2, size, size);
        }

        // Draw on minimap
        final int MINI_MAP_SIZE = 128;
        int miniTileSize = MINI_MAP_SIZE / map.getWidth();
        int offsetX = g.width() - MINI_MAP_SIZE - 10;
        int offsetY = 10;

        double miniX = getX() / tileSize * miniTileSize + offsetX;
        double miniY = getY() / tileSize * miniTileSize + offsetY;

        g.changeColor(Color.RED);
        g.drawSolidCircle(miniX, miniY, 4);
    }
}
