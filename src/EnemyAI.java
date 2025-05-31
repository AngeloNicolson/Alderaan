public class EnemyAI
{
    public enum AIState
    {
        IDLE,       // ORDINAL 0 - Not aware of the player
        ALERTED,    // ORDINAL 1 - Possibly heard the player or sensed presence
        CHASING,    // ORDINAL 2 - Has seen the player and is pursuing
        ATTACKING,  // ORDINAL 3 - In attacking range
        DEAD,      // ORDINAL 4
        RETREATING, // ORDINAL 5 - Retreating because player too close
        }

    private AIState state = AIState.IDLE; // Initial state
    private GameMap map;
    private int tileSize;
    private double alertRadius = 120;      // How far enemy notices noise
    private double stopDistance = 30;      // Stops right near player
    private double maxChaseDistance = 110; // Gives up if player flees too far
    private double retreatDistance = 20;   // player closer than this triggers retreat
    private double retreatSpeed = 70;      // pixels per second for retreating

    private static double targetX, targetY;

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
            if (dist < alertRadius)
            {
                state = AIState.ALERTED;
            }
            break;

        case ALERTED:
            targetX = enemy.getX();
            targetY = enemy.getY();
            if (canSeePlayer(enemy.getX(), enemy.getY(), player.getX(), player.getY()))
            {
                state = AIState.CHASING;
                enemy.facePlayer(player);
            }
            else if (dist > alertRadius * 1.5)
            {
                state = AIState.IDLE;
                enemy.facePlayer(player);
            }
            break;

        case CHASING:
            targetX = player.getX();
            targetY = player.getY();

            if (!canSeePlayer(enemy.getX(), enemy.getY(), player.getX(), player.getY()))
            {
                state = AIState.IDLE;
                break;
            }

            if (dist < retreatDistance)
            {
                state = AIState.RETREATING;
                break;
            }

            if (dist > stopDistance)
            {
                enemy.moveToward(targetX, targetY, dt);
            }
            else
            {
                state = AIState.ATTACKING;
            }

            enemy.smoothFacePlayer(player, Math.PI * 2, dt);

            if (dist > maxChaseDistance)
            {
                state = AIState.IDLE;
            }
            break;

        case RETREATING:
            // Calculate direction away from player
            double dirX = enemy.getX() - player.getX();
            double dirY = enemy.getY() - player.getY();
            double len2 = dirX * dirX + dirY * dirY;

            if (len2 > 0.001)
            {
                double len = Math.sqrt(len2);
                dirX /= len;
                dirY /= len;

                double moveX = dirX * retreatSpeed * dt;
                double moveY = dirY * retreatSpeed * dt;

                double newX = enemy.getX() + moveX;
                double newY = enemy.getY() + moveY;

                int tileX = (int)(newX / tileSize);
                int tileY = (int)(newY / tileSize);
                if (map.isWalkableTile(tileX, tileY))
                {
                    enemy.setX(newX);
                    enemy.setY(newY);
                }
            }

            enemy.smoothFacePlayer(player, Math.PI * 2, dt);

            // Recalculate distance after move
            double retreatDist = Math.sqrt((player.getX() - enemy.getX()) * (player.getX() - enemy.getX()) +
                                           (player.getY() - enemy.getY()) * (player.getY() - enemy.getY()));

            if (retreatDist > 30) // safe distance reached
            {
                state = AIState.ATTACKING;
            }
            break;

        case ATTACKING:
            if (dist < retreatDistance)
            {
                state = AIState.RETREATING;
                break;
            }

            if (dist > stopDistance)
            {
                state = AIState.CHASING;
            }

            enemy.smoothFacePlayer(player, Math.PI * 2, dt);
            break;

        case DEAD:
            break;

        default:
            state = AIState.IDLE;
            targetX = enemy.getX();
            targetY = enemy.getY();
            break;
        }

        if (!enemy.isAlive())
        {
            state = AIState.DEAD;
        }
    }

    public boolean canSeePlayer(double ex, double ey, double px, double py)
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
