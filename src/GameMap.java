import java.awt.Color;
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
                    switch (c) {
                        case '1':
                            grid[y][x] = 1;
                            break;
                        case '2':
                            grid[y][x] = 2;
                            break;
                        case '3':
                            grid[y][x] = 3;
                            break;
                        case '4':
                            grid[y][x] = 4;
                            break;
                        case '5':
                            grid[y][x] = 5;
                            break;
                        case '6':
                            grid[y][x] = 6;
                            break;
                        case '7':
                            grid[y][x] = 7;
                            break;
                        case '8':
                            grid[y][x] = 8;
                            break;
                        case '9':
                            grid[y][x] = 9; //the end zone tile
                            break;
                        case '0':
                            grid[y][x] = 0; //the walkable tile
                            break;
                        default:
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
        return grid[tileY][tileX] == 0 || grid[tileY][tileX] == 9;
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
                    if (grid[y][x] >= 1)
                        engine.changeColor(Color.gray);
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
        return grid[y][x] >= 1 && grid[y][x] < 9;
    }

    public int getWallType(int x, int y)
    {
        if (isWall(x, y)) {
            if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT){
                return 1; //default points outside the map to be wall one
            }
            return grid[y][x];
        }
        return 1; //default to one
        
    }

    public boolean isEndTile(int x, int y)
    {
        return grid[y][x] == 9;
    }
}
