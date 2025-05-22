import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class GameMap
{
    public static final int WIDTH = 32;
    public static final int HEIGHT = 32;
    private int[][] grid;

    public GameMap()
    {
        grid = new int[HEIGHT][WIDTH];
    }

    public boolean loadFromFile(String filename)
    {
        try (BufferedReader br = new BufferedReader(new FileReader(filename)))
        {
            for (int y = 0; y < HEIGHT; y++)
            {
                String line = br.readLine();
                if (line == null || line.length() < WIDTH)
                {
                    System.err.println("Map file too short or invalid at line " + y);
                    return false;
                }
                for (int x = 0; x < WIDTH; x++)
                {
                    char c = line.charAt(x);
                    if (c == '1')
                    {
                        grid[y][x] = 1;
                    }
                    else if (c == '0')
                    {
                        grid[y][x] = 0;
                    }
                    else
                    {
                        System.err.println("Invalid character '" + c + "' at " + y + "," + x);
                        return false;
                    }
                }
            }
            return true;
        }
        catch (IOException e)
        {
            System.err.println("Failed to load map: " + e.getMessage());
            return false;
        }
    }

    public int[][] getGrid()
    {
        return grid;
    }

    public int getWidth()
    {
        return WIDTH;
    }

    public int getHeight()
    {
        return HEIGHT;
    }

    public boolean isWalkableTile(int tileX, int tileY)
    {
        if (tileX < 0 || tileY < 0 || tileX >= WIDTH || tileY >= HEIGHT)
            return false;
        return grid[tileY][tileX] == 0;
    }

    public void draw(GameEngine engine, int miniTileSize, int offsetX, int offsetY, double playerX, double playerY,
                     int visionRadius, int worldTileSize)
    {
        int playerTileX = (int)(playerX / worldTileSize);
        int playerTileY = (int)(playerY / worldTileSize);

        for (int y = 0; y < HEIGHT; y++)
        {
            for (int x = 0; x < WIDTH; x++)
            {
                double dx = x - playerTileX;
                double dy = y - playerTileY;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance <= visionRadius)
                {
                    if (grid[y][x] == 1)
                        engine.changeColor(engine.red);
                    else
                        engine.changeColor(engine.black);
                }
                else
                {
                    engine.changeColor(engine.black);
                }

                engine.drawSolidRectangle(offsetX + x * miniTileSize, offsetY + y * miniTileSize, miniTileSize,
                                          miniTileSize);
            }
        }
    }

    public boolean isWall(int x, int y)
    {
        if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT)
            return true;
        return grid[y][x] == 1;
    }
}
