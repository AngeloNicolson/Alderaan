public abstract class Entity
{
    protected double x, y;

    public Entity(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    // Updated method signature to include dt
    public abstract void update(GameEngine engine, double dt);

    public double getX()
    {
        return x;
    }
    public double getY()
    {
        return y;
    }
}
