import java.awt.*;

public class WeaponItem {
    private double x, y;
    private Image sprite;
    private Weapon weapon;
    private boolean consumed;

    public WeaponItem(double x, double y, Image sprite, Weapon weapon){
        this.x = x;
        this.y = y;
        this.sprite = sprite;
        this.weapon = weapon;
        this.consumed = false;
    }

    public void render(GameEngine engine, Player player, double[] rayDistances) {
        if (consumed) return;

        double dx = x - player.getX();
        double dy = y - player.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        double angleToPlayer = Math.atan2(dy, dx) - player.getAngle();
        if (angleToPlayer < -Math.PI) angleToPlayer += 2 * Math.PI;
        if (angleToPlayer > Math.PI) angleToPlayer -= 2 * Math.PI;

        double screenWidth = engine.width();
        double fov = Math.toRadians(60);
        double halfFOV = fov / 2;
        double screenX = (angleToPlayer / halfFOV) * (screenWidth / 2) + (screenWidth / 2);

        if (screenX < 0 || screenX >= screenWidth) return;

        int numRays = rayDistances.length;
        double stripWidth = (double) screenWidth / numRays;
        int rayIndex = (int) (screenX / stripWidth);

        if (rayIndex < 0 || rayIndex >= numRays) return;
        if (distance >= rayDistances[rayIndex]) return;

        double spriteSize = (engine.height() * Main.TILE_SIZE / (distance * 2));
        double maxSpriteSize = engine.height() / 3; // Cap
        if (spriteSize > maxSpriteSize) spriteSize = maxSpriteSize;
        if (spriteSize < 1) spriteSize = 1;

        //Offset to change vertical placement (make it appear closer to the ground)
        double pseudoLineHeight = (Main.TILE_SIZE *640) /distance;
        double floorY= (engine.height() + pseudoLineHeight) / 2 - player.getVerticalLookOffset();
        double screenY = floorY - spriteSize;
        engine.drawImage(sprite, screenX - spriteSize / 2, screenY, spriteSize, spriteSize);
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void consume(Player player) {
        if (!consumed) {
            player.pickupWeapon(weapon);
            consumed = true;
        }
    }

    public double getX() {return x;}

    public double getY() {return y;}

}
