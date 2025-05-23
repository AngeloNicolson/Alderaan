public class EnemyAI
{
    public enum AIState
    {
        IDLE,    // ORDINAL 0 - Not aware of the player
        ALERTED, // ORDINAL 1 - Possibly heard the player or sensed presence
        CHASING  // ORDINAL 2 - Has seen the player and is pursuing
    }

    private AIState state = AIState.IDLE;
    private GameMap map;
    private int tileSize;
    private double alertRadius = 100;     // How far enemy notices noise
    private double stopDistance = 30;     // Stops right near player
    private double maxChaseDistance = 70; // Gives up if player flees too far

    private double targetX, targetY;

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
            targetX = enemy.getX();
            targetY = enemy.getY();
            // In IDLE, the enemy's angle is not explicitly set here;
            // The render method will make it appear to look towards the player if close enough.
            if (dist < alertRadius)
            {
                state = AIState.ALERTED;
            }
            break;

            // TODO: Somthing is wrong here. The sprite is flickering while slowly getting closer to player. I think
            // this is due to some soret of rapid stat change. Need ot have a deeper look
        case ALERTED:
            targetX = enemy.getX();
            targetY = enemy.getY();
            // In ALERTED, the enemy is static.
            if (canSeePlayer(enemy.getX(), enemy.getY(), player.getX(), player.getY()))
            {
                state = AIState.CHASING;
                // When transitioning to CHASING, immediately face the player
                enemy.facePlayer(player); // Ensure correct initial facing for movement
            }
            else if (dist > alertRadius * 1.5)
            {
                state = AIState.IDLE; // Player moved away
            }
            break;

        case CHASING:
            targetX = player.getX();
            targetY = player.getY();

            // When CHASING, the enemy's angle should reflect its movement direction (towards target)
            // or face the player if it's stopped very close.
            if (dist > stopDistance)
            {
                enemy.moveToward(targetX, targetY, dt); // This method sets enemy.angle based on movement.
            }
            else // If stopped (dist <= stopDistance), keep facing the player
            {
                enemy.facePlayer(player); // Keep facing player even when stopped
            }

            if (dist > maxChaseDistance)
            {
                state = AIState.IDLE; // Player got away
            }
            break;

        default:
            state = AIState.IDLE;
            targetX = enemy.getX();
            targetY = enemy.getY();
            break;
        }
    }

    private boolean canSeePlayer(double ex, double ey, double px, double py)
    {
        double dx = px - ex;
        double dy = py - ey;
        double distance = Math.sqrt(dx * dx + dy * dy);
        int steps = (int)Math.ceil(distance / tileSize);
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

        int playerTileX = (int)(px / tileSize);
        int playerTileY = (int)(py / tileSize);
        // Ensure the player's tile itself is walkable if the ray reaches it
        if (!map.isWalkableTile(playerTileX, playerTileY))
            return false;

        return true;
    }

    public AIState getState()
    {
        return state;
    }

    public double getTargetX()
    {
        return targetX;
    }

    public double getTargetY()
    {
        return targetY;
    }
}
