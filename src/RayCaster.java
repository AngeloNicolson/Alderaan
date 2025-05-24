import java.awt.Color;
import java.awt.Image;

public class RayCaster
{
    // declare fields
    private GameMap map;
    private int tileSize; // tile size
    private GameAsset gameAsset;

    private int numRays;   // number of rays
    private double fov;    // field of view in radians
    private double[] rayX; // array of final ray's X and Y end coordinates
    private double[] rayY;
    private double[] rayDistances;    // array of final ray's length
    private int[] wallType;           // array to determine the type of wall
    private Image[] imageWallSegment; // array of wall texture assigned to final ray

    public RayCaster(GameMap map, int tileSize, GameAsset gameAsset)
    {
        this.map = map;
        this.tileSize = tileSize;
        this.gameAsset = gameAsset;
        // initalise defaults
        numRays = 1024;           // default number of rays
        fov = Math.toRadians(60); // default field of view
        // initialise arrays for ray info
        rayX = new double[numRays];
        rayY = new double[numRays];
        rayDistances = new double[numRays];
        imageWallSegment = new Image[numRays];
        wallType = new int[numRays];
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
        int horWallType = 1;

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
            for (int i = 0; i < map.getHeight() + 1; i++)
            {
                int tileX = (int)(horX / tileSize);
                int tileY = (int)(horY / tileSize);

                if (map.isWall(tileX, tileY))
                {
                    horHit = true;
                    horDist = distance(px, py, horX, horY);
                    horWallType = map.getWallType(tileX, tileY);
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
        int vertWallType = 1;

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
            for (int i = 0; i < map.getWidth() + 1; i++)
            {
                int tileX = (int)(vertX / tileSize);
                int tileY = (int)(vertY / tileSize);

                if (map.isWall(tileX, tileY))
                {
                    vertHit = true;
                    vertDist = distance(px, py, vertX, vertY);
                    vertWallType = map.getWallType(tileX, tileY);
                    break;
                }

                vertX += vertStepX;
                vertY += vertStepY;
            }
        }

        // -- Save info of shorter ray (closest wall hit), to arrays --
        if (horHit && vertHit)
        {
            if (vertDist < horDist)
            { // indicates a vertical wall
                saveRay(rayIndex, vertX, vertY, vertDist, vertWallType, true);
            }
            else
            { // indicates (vertDist > hortDist), which is a horizontal wall
                saveRay(rayIndex, horX, horY, horDist, horWallType, false);
            }
        }
        else if (horHit)
        { // indicates
            saveRay(rayIndex, horX, horY, horDist, horWallType, false);
        }
        else if (vertHit)
        {
            saveRay(rayIndex, vertX, vertY, vertDist, vertWallType, true);
        }

        // System.out.println("DEBUG: ray"+rayIndex+", x:"+rayX[rayIndex]+" y:"+rayY[rayIndex]+"
        // wt:"+wallType[rayIndex]);
    }

    // method to cast the rays, and the ray info is stored in the local arrays
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

    // draws the output of the rays casted
    public void draw(GameEngine ge, double px, double py, double playerAngle, double verticalLookOffset)
    {

        // cast the rays, and the ray info is stored in the local arrays
        castRays(px, py, playerAngle);

        double stripWidth = (double)ge.width() / numRays; // full width for 2.5D view

        // loop to draw each ray
        for (int i = 0; i < numRays; i++)
        {
            double dist = rayDistances[i];
            // Correct fisheye
            double angleOffset = (i - numRays / 2.0) * (fov / numRays);
            dist *= Math.cos(angleOffset);

            double lineHeight = (tileSize * 640) / dist; // set the lineHeight of each display line
            double maxLineHeight = ge.height() * 8;      // set maxheight is 8 times the view
            if (lineHeight > maxLineHeight)
                lineHeight = maxLineHeight;
            double yOffset = (ge.height() - lineHeight) / 2 - verticalLookOffset;

            // draw textured walls
            ge.drawImage(imageWallSegment[i], i * stripWidth, yOffset, stripWidth, lineHeight);

            // draw fade overlay
            double maxDistance = 200;
            double brightness = Math.max(0.1, 1.0 - dist / maxDistance);
            int shade = (int)(brightness * 255);
            shade = 255 - shade;
            ge.changeColor(new Color(0, 0, 0, shade));

            ge.drawSolidRectangle(
                i * stripWidth, yOffset - 1, stripWidth,
                lineHeight + 1); // extended this by y-1 and h+1 to make sure it covers the wall image underneath
        }
    }

    // Helper method to calculate Euclidean distance
    private double distance(double x1, double y1, double x2, double y2)
    {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    // Helper method to save the ray info into ray arrays
    private void saveRay(int rayIndex, double finalX, double finalY, double finalDist, int finalWallType,
                         boolean isVert)
    {
        rayX[rayIndex] = finalX;
        rayY[rayIndex] = finalY;
        rayDistances[rayIndex] = finalDist;
        wallType[rayIndex] = finalWallType;
        if (isVert)
        {
            imageWallSegment[rayIndex] = matchWallTexture(wallType[rayIndex], rayY[rayIndex]);
        }
        else
        {
            imageWallSegment[rayIndex] = matchWallTexture(wallType[rayIndex], rayX[rayIndex]);
        }
    }

    // Helper method to determine wall image strip
    private Image matchWallTexture(int wallType, double rayCoord)
    {
        int wallImageX;                             // represents the local coordates of the side of the wall tile
        int maxSize = gameAsset.getWALLPIXELSIZE(); // this should return 128
        wallImageX = (int)((rayCoord % tileSize) / tileSize * maxSize); // convert the ray's coord to local coord of
                                                                        // tile
        wallImageX = wallImageX < 0 ? 0 : wallImageX;                   // checks wallImageX is within 0 to 127 bound
        wallImageX = wallImageX > maxSize - 1 ? maxSize - 1 : wallImageX;
        return gameAsset.getImageStripsScifiWall(wallType, wallImageX);
        // return gameAsset.getImageStripsTestWall(wallImageX); //gets the wall image strip
    }

    // Getters
    public double getRayX(int i)
    {
        if (0 <= i && i < rayX.length)
        {
            return rayX[i];
        }
        return 0.0;
    }

    public double getRayY(int i)
    {
        if (0 <= i && i < rayY.length)
        {
            return rayY[i];
        }
        return 0.0;
    }

    public double getRayDistances(int i)
    {
        if (0 <= i && i < rayDistances.length)
        {
            return rayDistances[i];
        }
        return 0.0;
    }
    public double[] getRayDistancesArray()
    {
        return rayDistances;
    }

    // Setters
    public void setMap(GameMap map)
    {
        this.map = map;
    }

    public void setNumRays(int numRays)
    {
        this.numRays = numRays;
    }

    public void setFov(double fov)
    {
        this.fov = fov;
    }
}
