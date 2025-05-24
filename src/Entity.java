public class Entity
{
    protected double x, y;

    public Entity(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    // Default update: no player context
    public void update(GameEngine engine, double dt)
    {
    }

    // Default update: with player context
    public void update(GameEngine engine, double dt, Player player)
    {
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }
}
