public class EnemyAI
{
    public enum AIState
    {
        IDLE,    // Not aware of the player
        ALERTED, // Possibly heard the player or sensed presence
        CHASING  // Has seen the player and is pursuing
    }

    private AIState state = AIState.IDLE;
    private GameMap map;
    private int tileSize;
    private double alertRadius = 300;      // How far enemy notices noise
    private double stopDistance = 50;      // Stops right near player
    private double maxChaseDistance = 200; // Gives up if player flees too far

    public EnemyAI(GameMap map, int tileSize)
    {
        this.map = map;
        this.tileSize = tileSize;
    }

    public void update(Enemy enemy, Player player, double dt)
    {
        double dx = player.getX() - enemy.getX();
        double dy = player.getY() - enemy.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);

        switch (state)
        {
        case IDLE:
            if (dist < alertRadius)
            {
                state = AIState.ALERTED;
            }
            break;

        case ALERTED:
            if (canSeePlayer(enemy.getX(), enemy.getY(), player.getX(), player.getY()))
            {
                state = AIState.CHASING;
            }
            else if (dist > alertRadius * 1.5)
            {
                state = AIState.IDLE; // Player moved away
            }
            break;

        case CHASING:
            if (dist > maxChaseDistance)
            {
                state = AIState.IDLE; // Player got away
            }
            else if (dist > stopDistance)
            {
                enemy.moveToward(player.getX(), player.getY(), dt);
            }
            else
            {
                enemy.facePlayer(player);
            }
            break;
        }
    }

    private boolean canSeePlayer(double ex, double ey, double px, double py)
    {
        double dx = px - ex;
        double dy = py - ey;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double steps = distance / tileSize;
        double stepX = dx / steps;
        double stepY = dy / steps;

        double testX = ex;
        double testY = ey;

        for (int i = 0; i < steps; i++)
        {
            int tileX = (int)(testX / tileSize);
            int tileY = (int)(testY / tileSize);
            if (!map.isWalkableTile(tileX, tileY))
            {
                return false;
            }
            testX += stepX;
            testY += stepY;
        }

        return true;
    }

    public AIState getState()
    {
        return state;
    }
}
