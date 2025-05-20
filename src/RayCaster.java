public class RayCaster
{
    private int[][] map;
    private int mapWidth, mapHeight;

    public RayCaster(int[][] map)
    {
        this.map = map;
        this.mapHeight = map.length;
        this.mapWidth = map[0].length; // Should maybe be clamped to a smaller size
    }

    // Normalize angle to [0, 2*PI)
    private double fixAngle(double angle)
    {
        while (angle < 0)
            angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI)
            angle -= 2 * Math.PI;
        return angle;
    }

    public void render(GameEngine engine, double playerX, double playerY, double playerAngleDegrees)
    {
        int screenWidth = engine.getWidth();
        int screenHeight = engine.getHeight();
        double fovDegrees = 60.0;
        int numRays = screenWidth;

        double halfFov = fovDegrees / 2.0;
        double playerAngle = Math.toRadians(playerAngleDegrees);

        final int TILE_SIZE = 64;

        for (int x = 0; x < numRays; x++)
        {
            // Calculate ray angle in degrees, then radians, normalize between 0 and 2PI
            double rayAngleDeg = (playerAngleDegrees - halfFov) + ((double)x / numRays) * fovDegrees;
            rayAngleDeg = (rayAngleDeg + 360) % 360;
            double rayAngle = Math.toRadians(rayAngleDeg);

            rayAngle = fixAngle(rayAngle);

            /* -----------------------------
             *  --- VERTICAL RAY CHECK ---
             * -----------------------------
             */

            double vx = 0, vy = 0;
            double distV = Double.MAX_VALUE;

            double tan = Math.tan(rayAngle);

            int dof = 0;
            double rayX = 0, rayY = 0;
            double xo = 0, yo = 0;

            if (Math.cos(rayAngle) > 0.0001)
            {
                rayX = ((int)playerX >> 6 << 6) + TILE_SIZE; // grid aligned + one tile right
                rayY = playerY + (rayX - playerX) * tan;
                xo = TILE_SIZE;
                yo = xo * tan;
            }
            else if (Math.cos(rayAngle) < -0.0001)
            {
                rayX = ((int)playerX >> 6 << 6) - 0.0001; // grid aligned one tile left minus epsilon
                rayY = playerY + (rayX - playerX) * tan;
                xo = -TILE_SIZE;
                yo = xo * tan;
            }
            else
            {
                rayX = playerX;
                rayY = playerY;
                dof = mapWidth; // no vertical check needed
            }

            while (dof < mapWidth)
            {
                int mx = (int)(rayX) >> 6;
                int my = (int)(rayY) >> 6;

                if (mx >= 0 && mx < mapWidth && my >= 0 && my < mapHeight)
                {
                    if (map[my][mx] == 1)
                    {
                        vx = rayX;
                        vy = rayY;
                        distV = Math.hypot(vx - playerX, vy - playerY);
                        break; // wall hit, stop stepping
                    }
                    else
                    {
                        rayX += xo; // move to next vertical grid intersection
                        rayY += yo;
                        dof++; // increment depth of field
                    }
                }
                else
                {
                    break; // outside map bounds, stop stepping
                }
            }

            /* ------------------------------
             *  --- HORIZONTAL RAY CHECK ---
             * ------------------------------
             */
            double hx = 0, hy = 0;
            double distH = Double.MAX_VALUE;

            dof = 0;
            double atan = -1.0 / tan;

            rayX = 0;
            rayY = 0;
            xo = 0;
            yo = 0;

            if (Math.sin(rayAngle) > 0.0001)
            {
                rayY = ((int)playerY >> 6 << 6) - 0.0001; // grid aligned one tile up minus epsilon
                rayX = playerX + (playerY - rayY) * atan;
                yo = -TILE_SIZE;
                xo = -yo * atan;
            }
            else if (Math.sin(rayAngle) < -0.0001)
            {
                rayY = ((int)playerY >> 6 << 6) + TILE_SIZE; // grid aligned one tile down
                rayX = playerX + (playerY - rayY) * atan;
                yo = TILE_SIZE;
                xo = -yo * atan;
            }
            else
            {
                rayX = playerX;
                rayY = playerY;
                dof = mapHeight; // no horizontal check needed
            }

            while (dof < mapHeight)
            {
                int mx = (int)(rayX) >> 6;
                int my = (int)(rayY) >> 6;

                if (mx >= 0 && mx < mapWidth && my >= 0 && my < mapHeight)
                {
                    if (map[my][mx] == 1)
                    {
                        hx = rayX;
                        hy = rayY;
                        distH = Math.hypot(hx - playerX, hy - playerY);
                        break;
                    }
                    else
                    {
                        rayX += xo;
                        rayY += yo;
                        dof++;
                    }
                }
                else
                {
                    break;
                }
            }

            // Choose closest hit
            double finalDist;
            boolean hitVertical;
            if (distV < distH)
            {
                finalDist = distV;
                hitVertical = true;
            }
            else
            {
                finalDist = distH;
                hitVertical = false;
            }

            // Fix fisheye effect by projecting distance onto player's viewing direction
            double ca = playerAngle - rayAngle;
            finalDist = finalDist * Math.cos(ca);

            // Calculate wall height slice on screen
            int lineHeight = (int)(screenHeight * TILE_SIZE / finalDist);

            int drawStart = -lineHeight / 2 + screenHeight / 2;
            if (drawStart < 0)
                drawStart = 0;

            int drawEnd = lineHeight / 2 + screenHeight / 2;
            if (drawEnd >= screenHeight)
                drawEnd = screenHeight - 1;

            // Shade walls differently based on vertical/horizontal hit
            // You can add color support in your engine and use that instead
            // int color = hitVertical ? 0xFFFFFF : 0xAAAAAA;

            // Draw vertical line for wall slice at column x
            engine.drawLine(x, drawStart, x, drawEnd);
        }
    }
}
