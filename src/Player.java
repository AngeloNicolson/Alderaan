import java.util.ArrayList;
import java.util.List;

public class Player extends Entity
{
    private double angle;              // direction player is facing (in radians)
    private double verticalLookOffset; // Vertical offset for camera pitch
    private double speed = 77;
    private boolean left, right, up, down;

    private GameMap map;
    private int mapS;

    private int maxHealth = 100;
    private int currentHealth;

    private List<Weapon> weapons;
    private int currentWeapon;
    private boolean unlimitedAmmo; // True for Laser Pistol

    public Player(double x, double y, GameMap map, int mapS, List<Weapon> initialWeapons)
    {
        super(x, y);
        this.angle = 0;
        this.verticalLookOffset = 0;

        this.map = map;
        this.mapS = mapS;
        this.currentHealth = maxHealth;

        weapons = new ArrayList<>();
        this.weapons = new ArrayList<>(initialWeapons);
        currentWeapon = 0;
    }

    public void setDirection(boolean left, boolean right, boolean up, boolean down)
    {
        this.left = left;
        this.right = right;
        this.up = up;
        this.down = down;
    }

    public double getAngle()
    {
        return angle;
    }

    public double getSpeed()
    {
        return speed;
    }

    public double getVerticalLookOffset()
    {
        return verticalLookOffset;
    }

    public void rotate(double deltaAngle)
    {
        angle += deltaAngle;

        // Clamp angle to [0, 2pi] to keep it within a full circle
        if (angle < 0)
            angle += 2 * Math.PI;
        if (angle > 2 * Math.PI)
            angle -= 2 * Math.PI;
    }

    public void setVerticalLookOffset(double offset)
    {
        // Clamp the vertical look offset to a reasonable range
        // These values can be adjusted based on desired look limits
        this.verticalLookOffset = Math.max(-150, Math.min(150, offset));
    }

    @Override public void update(GameEngine engine, double dt)
    {
        double moveStep = speed * dt;
        double dx = 0;
        double dy = 0;

        // forward/backward movement
        if (up)
        {
            dx += Math.cos(angle);
            dy += Math.sin(angle);
        }
        if (down)
        {
            dx -= Math.cos(angle);
            dy -= Math.sin(angle);
        }

        // strafing movement
        if (left)
        {
            dx += Math.cos(angle - Math.PI / 2);
            dy += Math.sin(angle - Math.PI / 2);
        }
        if (right)
        {
            dx += Math.cos(angle + Math.PI / 2);
            dy += Math.sin(angle + Math.PI / 2);
        }

        // Normalize the combined movement vector to prevent faster diagonal movement
        double magnitude = Math.sqrt(dx * dx + dy * dy);
        if (magnitude > 0)
        {
            dx = (dx / magnitude) * moveStep;
            dy = (dy / magnitude) * moveStep;
        }
        else
        {
            // If no movement, set dx and dy to 0 to prevent NaN issues
            dx = 0;
            dy = 0;
        }

        double nextX = x + dx;
        double nextY = y + dy;

        // Collision detection for X movement
        // Check if moving to nextX would hit a wall, if not, update x
        if (!isWall(nextX, y))
            x = nextX;
        // Collision detection for Y movement
        // Check if moving to nextY would hit a wall, if not, update y
        if (!isWall(x, nextY))
            y = nextY;
    }

    public void draw(GameEngine engine)
    {
        engine.changeColor(engine.red);
        engine.drawSolidCircle(x, y, 3); // Draw player as a small red circle

        double lineLength = 20;
        // Calculate the end point of the direction line based on player's angle
        double endX = x + Math.cos(angle) * lineLength;
        double endY = y + Math.sin(angle) * lineLength;

        // Draw direction line from player's center
        engine.drawLine(x, y, endX, endY);
    }

    private boolean isWall(double px, double py)
    {
        int tileX = (int)(px / mapS);
        int tileY = (int)(py / mapS);

        return map.isWall(tileX, tileY);

        // if (tileX < 0 || tileY < 0 || tileX >= map.getWidth() || tileY >= map.getHeight())
        //     return true;

        // return map.getGrid()[tileY][tileX] >= 1 && map.getGrid()[tileY][tileX] < 9;
    }

    // Health Logic

    public void takeDamage(int amt)
    {
        currentHealth -= amt;
        if (currentHealth < 0)
        {
            currentHealth = 0;
        }
    }

    public void restoreHealth(int amt)
    {
        currentHealth += amt;
        if (currentHealth > maxHealth)
        {
            currentHealth = maxHealth;
        }
    }

    public boolean isAlive()
    {
        return currentHealth > 0;
    }

    public int getHealth()
    {
        return currentHealth;
    }

    public int getMaxHealth()
    {
        return maxHealth;
    }

    public float getHealthPercentage()
    {
        return (float)currentHealth / maxHealth;
    }

    // Weapon Management
    public void nextWeapon()
    {
        if (weapons.size() > 1)
        {
            currentWeapon = (currentWeapon + 1) % weapons.size();
        }
    }

    public void previousWeapon()
    {
        if (weapons.size() > 1)
        {
            currentWeapon = (currentWeapon - 1 + weapons.size()) % weapons.size();
        }
    }

    public Weapon getCurrentWeapon()
    {
        return weapons.get(currentWeapon);
    }

    public void pickupWeapon(String weaponName)
    {
        for (Weapon w : weapons)
        {
            if (w.getName().equals(weaponName))
            {
                if (!w.isUnlimitedAmmo())
                {
                    w.addAmmo(30); // Add 30 ammo if weapon exists
                }
                return;
            }
        }
        // Add new weapon if not found
        if (weaponName.equals("Laser Rifle"))
        {
            weapons.add(new Weapon("Laser Rifle", 15, 10, 30, 90, false, null, null));
        }
        else if (weaponName.equals("Laser Shotgun"))
        {
            weapons.add(new Weapon("Laser Shotgun", 25, 2, 8, 24, false, null, null));
        }
    }
}
