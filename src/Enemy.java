import java.awt.Color;
import java.awt.Image;

public class Enemy extends Entity
{
    private double angle;
    private GameMap map;
    private int mapS; // essentially the tile size
    private String enemyType;
    private Image sprite;

    private double speed = 50; // pixels per second
    private EnemyAI ai;

    public Enemy(double x, double y, String enemyType, GameMap map, int mapS)
    {
        super(x, y);
        this.enemyType = enemyType;
        this.map = map;
        this.mapS = mapS;

        this.ai = new EnemyAI(map, mapS);

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

        ai.update(this, player, dt);
    }
    // Purely for changing color during testing
    public EnemyAI.AIState getAIState()
    {
        return ai.getState();
    }

    // TODO: Add max height of enemy and add how close enemy can get to player so that we dont get zplane fighting
    public void render(GameEngine g, Player player, double[] rayDistances)
    {
        double dx = x - player.getX();
        double dy = y - player.getY();

        // Distance from player to enemy
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Angle between player direction and enemy
        double angleToEnemy = Math.atan2(dy, dx);
        double relativeAngle = normalizeAngle(angleToEnemy - player.getAngle());

        // If enemy is outside FOV, skip
        double halfFOV = Math.toRadians(30); // Assuming 60 degree FOV
        if (Math.abs(relativeAngle) > halfFOV)
            return;

        // Screen projection
        int numRays = rayDistances.length;
        int screenWidth = g.width();
        int screenHeight = g.height();

        int stripIndex = (int)((relativeAngle + halfFOV) / (2 * halfFOV) * numRays);
        double stripWidth = (double)screenWidth / numRays;

        double scale = (mapS * 320) / distance;
        double spriteHeight = scale;
        double spriteWidth = scale;

        int screenX = (int)(stripIndex * stripWidth - spriteWidth / 2);
        double verticalOffset = player.getVerticalLookOffset();
        int screenY = (int)((screenHeight - spriteHeight) / 2 - verticalOffset);

        // For easy state detection when building or altering AI.
        // DO NOT REMOVE YET
        Color enemyColor;
        switch (getAIState())
        {
        case IDLE:
            enemyColor = Color.GREEN; // Enemy unaware
            break;
        case ALERTED:
            enemyColor = Color.ORANGE; // Enemy suspicious
            break;
        case CHASING:
            enemyColor = Color.RED; // Enemy chasing player
            break;
        default:
            enemyColor = Color.WHITE; // fallback
            break;
        }

        g.changeColor(enemyColor);

        // Render each vertical slice of enemy
        for (int i = 0; i < spriteWidth; i++)
        {
            int rayIndex = (int)((screenX + i) / stripWidth);
            if (rayIndex < 0 || rayIndex >= numRays)
                continue;

            // Compare enemy distance to wall at this slice, if is less than distance then render, otherwise cull the
            // part of the enemy
            if (distance < rayDistances[rayIndex])
            {
                // Placeholder - use actual sprite slice
                g.drawLine(screenX + i, screenY, screenX + i, (int)(screenY + spriteHeight));
            }
        }
    }

    private double normalizeAngle(double angle)
    {
        while (angle < -Math.PI)
            angle += 2 * Math.PI;
        while (angle > Math.PI)
            angle -= 2 * Math.PI;
        return angle;
    }

    public void facePlayer(Player player)
    {
        double dx = player.getX() - this.x;
        double dy = player.getY() - this.y;
        this.angle = Math.atan2(dy, dx);
    }

    public void moveToward(double targetX, double targetY, double dt)
    {
        double dx = targetX - x;
        double dy = targetY - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > 0.001)
        {
            double dirX = dx / dist;
            double dirY = dy / dist;
            x += dirX * speed * dt;
            y += dirY * speed * dt;
            angle = Math.atan2(dy, dx);
        }
    }
}
