import java.awt.Color;
import java.awt.Image;

public class RayCaster
{
    //declare fields
    private GameMap map;
    private int tileSize; // tile size
    private GameAsset gameAsset;

    private int numRays; //number of rays
    private double fov; //field of view in radians
    private double [] rayX; //array of final ray's X and Y end coordinates
    private double [] rayY;
    private double [] rayDistances; //array of final ray's length
    private Image [] imageWallSegment; //array of wall texture assigned to final ray


    public RayCaster(GameMap map, int tileSize, GameAsset gameAsset)
    {
        this.map = map;
        this.tileSize = tileSize;
        this.gameAsset = gameAsset;
        //initalise defaults
        numRays = 256; //default number of rays
        fov = Math.toRadians(60); //default field of view
        //initialise arrays for ray info
        rayX = new double[numRays];
        rayY = new double[numRays];
        rayDistances = new double[numRays];
        imageWallSegment = new Image[numRays];

    }

    public void castSingleRay(double px, double py, double rayAngle, int rayIndex)
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
                horY = ((int)(py / tileSize) + 1) * tileSize;
                horStepY = tileSize;
            }
            else
            {
                // Looking up, find first horizontal grid line above player
                horY = ((int)(py / tileSize)) * tileSize - 0.0001;
                horStepY = -tileSize;
            }
            horX = px + (horY - py) / sinA * cosA;
            horStepX = horStepY / sinA * cosA;

            // Step along horizontal lines until hit or max distance
            for (int i = 0; i < 100; i++)
            {
                int tileX = (int)(horX / tileSize);
                int tileY = (int)(horY / tileSize);

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
                vertX = ((int)(px / tileSize) + 1) * tileSize;
                vertStepX = tileSize;
            }
            else
            {
                // Looking left, find first vertical grid line to the left of player
                vertX = ((int)(px / tileSize)) * tileSize - 0.0001;
                vertStepX = -tileSize;
            }
            vertY = py + (vertX - px) / cosA * sinA;
            vertStepY = vertStepX / cosA * sinA;

            // Step along vertical lines until hit or max distance
            for (int i = 0; i < 100; i++)
            {
                int tileX = (int)(vertX / tileSize);
                int tileY = (int)(vertY / tileSize);

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
        if (horHit && vertHit){
            if (vertDist < horDist) { //indicates a vertical wall
                rayX[rayIndex] = vertX;
                rayY[rayIndex] = vertY;
                rayDistances[rayIndex] = vertDist;
            } else { //indicates (vertDist > hortDist), which is a horizontal wall
                rayX[rayIndex] = horX;
                rayY[rayIndex] = horY;
                rayDistances[rayIndex] = horDist;
            }
        }  
        else if (horHit){
            rayX[rayIndex] = horX;
            rayY[rayIndex] = horY;
            rayDistances[rayIndex] = horDist;
        }
        else if (vertHit){
            rayX[rayIndex] = vertX;
            rayY[rayIndex] = vertY;
            rayDistances[rayIndex] = vertDist;
        }

        //System.out.println("DEBUG: ray"+rayIndex+", x:"+rayX[rayIndex]+" y:"+rayY[rayIndex]);
    }

    public void castRays(double px, double py, double playerAngle)
    {
        double startAngle = playerAngle - fov / 2.0;
        double angleStep = fov / (numRays - 1);

        for (int i = 0; i < numRays; i++)
        {
            double rayAngle = startAngle + i * angleStep;
            castSingleRay(px, py, rayAngle, i);
        }
    }

    //draws the output of the rays casted
    public void draw(GameEngine ge, double px, double py, double playerAngle, double verticalLookOffset){
        
        //cast the rays, and the ray info is stored in the local arrays
        castRays(px, py, playerAngle);

        double stripWidth = (double) ge.width() / numRays; // full width for 2.5D view

        //loop to draw each ray
        for (int i = 0; i < numRays; i++)
        {
            double dist = rayDistances[i];
            // Correct fisheye
            double angleOffset = (i - numRays / 2.0) * (fov / numRays);
            dist *= Math.cos(angleOffset);

            double lineHeight = (tileSize * 320) / dist;
            double yOffset = (ge.height() - lineHeight) / 2 - verticalLookOffset;

            double maxDistance = 500;
            double brightness = Math.max(0.2, 1.0 - dist / maxDistance);
            int shade = (int)(brightness * 255);
            ge.changeColor(new Color(shade, shade, shade));

            ge.drawSolidRectangle(i * stripWidth, yOffset, stripWidth, lineHeight);
        }
    }

    // Helper method to calculate Euclidean distance
    private double distance(double x1, double y1, double x2, double y2)
    {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }
}
