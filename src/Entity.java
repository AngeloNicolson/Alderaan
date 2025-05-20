public abstract class Entity
{
    protected double x, y;

    public Entity(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public abstract void update(GameEngine engine);

    public double getX()
    {
        return x;
    }
    public double getY()
    {
        return y;
    }
}
