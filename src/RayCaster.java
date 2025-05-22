public class RayCaster
{
    private GameMap map;
    private int mapS; // tile size

    public RayCaster(GameMap map, int mapS)
    {
        this.map = map;
        this.mapS = mapS;
    }

    public double castSingleRay(double px, double py, double rayAngle)
    {
        double sinA = Math.sin(rayAngle);
        double cosA = Math.cos(rayAngle);

        // --- Horizontal intersections ---
        double horX = 0, horY = 0;
        double horStepX = 0, horStepY = 0;
        boolean horHit = false;
        double horDist = 1000;

        if (Math.abs(sinA) > 0.0001)
        { // ray not perfectly horizontal
            if (sinA > 0)
            {
                // Looking down, find first horizontal grid line below player
                horY = ((int)(py / mapS) + 1) * mapS;
                horStepY = mapS;
            }
            else
            {
                // Looking up, find first horizontal grid line above player
                horY = ((int)(py / mapS)) * mapS - 0.0001;
                horStepY = -mapS;
            }
            horX = px + (horY - py) / sinA * cosA;
            horStepX = horStepY / sinA * cosA;

            // Step along horizontal lines until hit or max distance
            for (int i = 0; i < 100; i++)
            {
                int tileX = (int)(horX / mapS);
                int tileY = (int)(horY / mapS);

                if (map.isWall(tileX, tileY))
                {
                    horHit = true;
                    horDist = distance(px, py, horX, horY);
                    break;
                }

                horX += horStepX;
                horY += horStepY;
            }
        }

        // --- Vertical intersections ---
        double vertX = 0, vertY = 0;
        double vertStepX = 0, vertStepY = 0;
        boolean vertHit = false;
        double vertDist = 1000;

        if (Math.abs(cosA) > 0.0001)
        { // ray not perfectly vertical
            if (cosA > 0)
            {
                // Looking right, find first vertical grid line to the right of player
                vertX = ((int)(px / mapS) + 1) * mapS;
                vertStepX = mapS;
            }
            else
            {
                // Looking left, find first vertical grid line to the left of player
                vertX = ((int)(px / mapS)) * mapS - 0.0001;
                vertStepX = -mapS;
            }
            vertY = py + (vertX - px) / cosA * sinA;
            vertStepY = vertStepX / cosA * sinA;

            // Step along vertical lines until hit or max distance
            for (int i = 0; i < 100; i++)
            {
                int tileX = (int)(vertX / mapS);
                int tileY = (int)(vertY / mapS);

                if (map.isWall(tileX, tileY))
                {
                    vertHit = true;
                    vertDist = distance(px, py, vertX, vertY);
                    break;
                }

                vertX += vertStepX;
                vertY += vertStepY;
            }
        }

        // Return the smaller ray between the vertical or horizontal (closest wall hit)
        if (horHit && vertHit)
            return Math.min(horDist, vertDist);
        else if (horHit)
            return horDist;
        else if (vertHit)
            return vertDist;
        else
            return 1000; // no hit found
    }

    public double[] castRays(double px, double py, double playerAngle, int numRays, double fov)
    {
        double[] distances = new double[numRays];
        double startAngle = playerAngle - fov / 2.0;
        double angleStep = fov / (numRays - 1);

        for (int i = 0; i < numRays; i++)
        {
            double rayAngle = startAngle + i * angleStep;
            distances[i] = castSingleRay(px, py, rayAngle);
        }
        return distances;
    }

    // Helper method to calculate Euclidean distance
    private double distance(double x1, double y1, double x2, double y2)
    {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }
}
