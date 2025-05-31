import java.awt.Color;
import java.awt.Image;

public class RayCaster
{
    private GameMap map;
    private int tileSize;
    private GameAsset gameAsset;

    private int numRays;
    private double fov;
    private double[] rayX;
    private double[] rayY;
    private double[] rayDistances;
    private int[] wallType;
    private Image[] imageWallSegment;

    public RayCaster(GameMap map, int tileSize, GameAsset gameAsset)
    {
        this.map = map;
        this.tileSize = tileSize;
        this.gameAsset = gameAsset;

        numRays = 1024;
        fov = Math.toRadians(60);

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

        // Horizontal intersections
        double horX = 0, horY = 0;
        double horStepX = 0, horStepY = 0;
        boolean horHit = false;
        double horDist = 1000;
        int horWallType = 1;

        if (Math.abs(sinA) > 0.0001)
        {
            if (sinA > 0)
            {
                horY = ((int)(py / tileSize) + 1) * tileSize;
                horStepY = tileSize;
            }
            else
            {
                horY = ((int)(py / tileSize)) * tileSize - 0.0001;
                horStepY = -tileSize;
            }
            horX = px + (horY - py) / sinA * cosA;
            horStepX = horStepY / sinA * cosA;

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

        // Vertical intersections
        double vertX = 0, vertY = 0;
        double vertStepX = 0, vertStepY = 0;
        boolean vertHit = false;
        double vertDist = 1000;
        int vertWallType = 1;

        if (Math.abs(cosA) > 0.0001)
        {
            if (cosA > 0)
            {
                vertX = ((int)(px / tileSize) + 1) * tileSize;
                vertStepX = tileSize;
            }
            else
            {
                vertX = ((int)(px / tileSize)) * tileSize - 0.0001;
                vertStepX = -tileSize;
            }
            vertY = py + (vertX - px) / cosA * sinA;
            vertStepY = vertStepX / cosA * sinA;

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

        // Save shortest ray hit
        if (horHit && vertHit)
        {
            if (vertDist < horDist)
                saveRay(rayIndex, vertX, vertY, vertDist, vertWallType, true);
            else
                saveRay(rayIndex, horX, horY, horDist, horWallType, false);
        }
        else if (horHit)
            saveRay(rayIndex, horX, horY, horDist, horWallType, false);
        else if (vertHit)
            saveRay(rayIndex, vertX, vertY, vertDist, vertWallType, true);
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

    // verticalLookOffset moves view UP or DOWN by shifting drawn walls vertically
    public void draw(GameEngine ge, double px, double py, double playerAngle, double verticalLookOffset)
    {
        // Cast all rays before drawing
        castRays(px, py, playerAngle);

        //ge.drawSolidRectangle(0, 0, ge.width(), ge.height() / 2);               // Ceiling
        //ge.drawSolidRectangle(0, ge.height() / 2, ge.width(), ge.height() / 2); // Floor

        double stripWidth = (double)ge.width() / numRays;

        for (int i = 0; i < numRays; i++)
        {
            double dist = rayDistances[i];

            // Correct fisheye distortion
            double angleOffset = (i - numRays / 2.0) * (fov / numRays);
            dist *= Math.cos(angleOffset);

            double lineHeight = (tileSize * 640) / dist;
            double maxLineHeight = ge.height() * 8;
            if (lineHeight > maxLineHeight)
                lineHeight = maxLineHeight;

            double yOffset = (ge.height() - lineHeight) / 2 - verticalLookOffset;

            ge.drawImage(imageWallSegment[i], i * stripWidth, yOffset, stripWidth, lineHeight);

            // Depth shading
            double maxDistance = 200;
            double brightness = Math.max(0.1, 1.0 - dist / maxDistance);
            int shade = 255 - (int)(brightness * 255);
            ge.changeColor(new Color(0, 0, 0, shade));
            ge.drawSolidRectangle(i * stripWidth, yOffset - 1, stripWidth, lineHeight + 1);
        }
    }

    private double distance(double x1, double y1, double x2, double y2)
    {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    private void saveRay(int rayIndex, double finalX, double finalY, double finalDist, int finalWallType,
                         boolean isVert)
    {
        rayX[rayIndex] = finalX;
        rayY[rayIndex] = finalY;
        rayDistances[rayIndex] = finalDist;
        wallType[rayIndex] = finalWallType;

        if (isVert)
            imageWallSegment[rayIndex] = matchWallTexture(wallType[rayIndex], rayY[rayIndex]);
        else
            imageWallSegment[rayIndex] = matchWallTexture(wallType[rayIndex], rayX[rayIndex]);
    }

    private Image matchWallTexture(int wallType, double rayCoord)
    {
        int maxSize = gameAsset.getWALLPIXELSIZE();
        int wallImageX = (int)((rayCoord % tileSize) / tileSize * maxSize);

        if (wallImageX < 0)
            wallImageX = 0;
        if (wallImageX >= maxSize)
            wallImageX = maxSize - 1;

        return gameAsset.getImageStripsScifiWall(wallType, wallImageX);
    }

    // Getters
    public double getRayX(int i)
    {
        if (0 <= i && i < rayX.length)
            return rayX[i];
        return 0.0;
    }
    public double getRayY(int i)
    {
        if (0 <= i && i < rayY.length)
            return rayY[i];
        return 0.0;
    }
    public double getRayDistances(int i)
    {
        if (0 <= i && i < rayDistances.length)
            return rayDistances[i];
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
    public int getNumRays()
    {
        return numRays;
    }
}
