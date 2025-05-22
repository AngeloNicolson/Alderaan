public class Enemy extends Entity
{
    private double angle;
    private GameMap map;
    private int mapS;

    public Enemy(double x, double y, GameMap map, int mapS)
    {
        super(x, y);
        this.angle = 0;

        this.map = map;
        this.mapS = mapS;
    }

    public void update(GameEngine engine, double dt)
    {
    }
}
