import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Enemy extends Entity
{
    // Map varible
    private double angle;
    private GameMap map;
    private int mapS;

    // Sprite varibles
    private String enemyType;
    private static Image spriteSheet;
    private static Image hitSheet;
    private static Image distanceSheet;
    private static Image[][] animations;    // [state][frame]
    private static Image[][] hitAnimations; // [state][frame]
    private static Image[][] distanceAnimations; // [state][frame]

    private int frameWidth, frameHeight;
    private int[] framesPerState = {5, 1, 6, 3, 5}; // IDLE=5, ALERTED=1, CHASING=6, ATTACKING=3, DEAD=5 frames
    private double frameTimer = 0;
    private double frameDuration = 0.2;
    private int currentFrame = 0;
    private static Map<Image, Image[]> spriteSlices = new HashMap<>();
    private static Map<Image, Image[]> distanceSlices = new HashMap<>();
    private static double maxDistance = 400;

    // Speed - AI
    private double speed = 50;
    private EnemyAI ai;

    // Walkback state variables
    private boolean isWalkingBack = false;
    private double walkBackDistance = 20;
    private double walkedBack = 0;
    private int walkBackDirection = 0; // -1 for left, +1 for right
    private double walkBackSpeed = 50;

    // Attack
    private double attackCooldown = 2.0;
    private double cooldownTimer = 0;
    private double deathTimer;
    private double hangAroundTime = 5;
    private int damage;
    // Health
    private int maxHealth = 100;
    private int currentHealth = maxHealth;
    private boolean hit = false;
    private double hitTimer = 0;
    Composite orig;

    public Enemy(double x, double y, String enemyType, GameMap map, int mapS, int damage)
    {
        super(x, y);
        this.enemyType = enemyType;
        this.map = map;
        this.mapS = mapS;
        this.damage = damage;
        this.ai = new EnemyAI(map, mapS);
        if (spriteSheet == null) {
            Enemy.spriteSheet = GameEngine.loadImage("assets/visual/StormZombieSpritesheet.png");
            Enemy.hitSheet = GameEngine.loadImage("assets/visual/StormZombieSpritesheetRedTint.png");
            Enemy.distanceSheet = GameEngine.loadImage("assets/visual/StormZombieSpritesheetShadow.png");
        }
        this.frameWidth = 432 / 6;   // 683 / 6 columns (approximate)
        this.frameHeight = 576 / 8;  // Updated: 9 rows instead of 8
        animations = new Image[5][]; // IDLE, ALERTED, CHASING, ATTACKING, DEAD
        hitAnimations = new Image[4][];
        distanceAnimations = new Image[5][];
        this.orig = null;

        // IDLE row 7 has 5 columns (directions)
        hitAnimations[0] = new Image[5];
        distanceAnimations[0] = new Image[5];
        animations[0] = new Image[5];
        for (int col = 0; col < 5; col++)
        {
            animations[0][col] =
                GameEngine.subImage(spriteSheet, col * frameWidth, 6 * frameHeight, frameWidth, frameHeight);
            hitAnimations[0][col] =
                GameEngine.subImage(hitSheet, col * frameWidth, 6 * frameHeight, frameWidth, frameHeight);
            distanceAnimations[0][col] =
                    GameEngine.subImage(distanceSheet, col * frameWidth, 6 * frameHeight, frameWidth, frameHeight);
        }

        // ALERTED uses a single static frame (row 0 col 0)
        animations[1] = new Image[] {GameEngine.subImage(spriteSheet, 0, 0, frameWidth, frameHeight)};
        hitAnimations[1] = new Image[] {GameEngine.subImage(hitSheet, 0, 0, frameWidth, frameHeight)};
        distanceAnimations[1] = new Image[] {GameEngine.subImage(distanceSheet, 0, 0, frameWidth, frameHeight)};

        // CHASING: 8 directions (rows 0-7), each with 6 frames (columns)
        animations[2] = new Image[8 * 6];
        hitAnimations[2] = new Image[8 * 6];
        distanceAnimations[2] = new Image[8 * 6];

        for (int dir = 0; dir < 8; dir++)
        {
            for (int frame = 0; frame < 6; frame++)
            {
                int index = dir * 6 + frame;
                animations[2][index] =
                    GameEngine.subImage(spriteSheet, frame * frameWidth, dir * frameHeight, frameWidth, frameHeight);
                hitAnimations[2][index] =
                        GameEngine.subImage(hitSheet, frame * frameWidth, dir * frameHeight, frameWidth, frameHeight);
                distanceAnimations[2][index] =
                        GameEngine.subImage(distanceSheet, frame * frameWidth, dir * frameHeight, frameWidth, frameHeight);

            }
        }
        animations[3] = new Image[3];
        hitAnimations[3] = new Image[3];
        distanceAnimations[3] = new Image[3];
        for (int att = 0; att < 3; att++)
        {
            animations[3][att] =
                GameEngine.subImage(spriteSheet, att * frameWidth, 7 * frameHeight, frameWidth, frameHeight);
            hitAnimations[3][att] =
                    GameEngine.subImage(hitSheet, att * frameWidth, 7 * frameHeight, frameWidth, frameHeight);
            distanceAnimations[3][att] =
                    GameEngine.subImage(distanceSheet, att * frameWidth, 7 * frameHeight, frameWidth, frameHeight);
        }
        animations[4] = new Image[5];
        distanceAnimations[4] = new Image[5];
        for (int dead = 0; dead < 5; dead++)
        {
            animations[4][dead] =
                    GameEngine.subImage(spriteSheet, dead * frameWidth, 5 * frameHeight, frameWidth, frameHeight);
            distanceAnimations[4][dead] =
                    GameEngine.subImage(distanceSheet, dead * frameWidth, 5 * frameHeight, frameWidth, frameHeight);
        }
    }

    @Override public void update(GameEngine engine, double dt, Player player)
    {
        // Every frame we decrease cooldown timer
        if (cooldownTimer > 0)
        {
            cooldownTimer -= dt;
        }
        if (hit)
        {
            hitTimer += dt;
            if (hitTimer > 0.1)
            {
                hit = false;
                hitTimer = 0;
            }
        }

        EnemyAI.AIState oldState = ai.getState();
        ai.update(this, player, dt);
        EnemyAI.AIState newState = ai.getState();
        if (oldState != EnemyAI.AIState.DEAD && newState == EnemyAI.AIState.DEAD)
        {
            deathTimer = 0;
            currentFrame = 0;
        }
        else if (oldState == EnemyAI.AIState.DEAD && newState == EnemyAI.AIState.DEAD)
        {
            deathTimer += dt;
        }

        // Detect flick from CHASING to ALERTED: trigger walkback
        if (oldState == EnemyAI.AIState.CHASING && newState == EnemyAI.AIState.ALERTED)
        {
            isWalkingBack = true;
            walkedBack = 0;

            // Choose left or right randomly relative to player
            double angleToPlayer = Math.atan2(player.getY() - y, player.getX() - x);
            walkBackDirection = (Math.random() < 0.5) ? -1 : 1;
            angle = angleToPlayer + walkBackDirection * (Math.PI / 2); // 90 degrees left or right
        }

        // If currently walking back, move accordingly and skip normal AI movement/frame update
        if (isWalkingBack)
        {
            double moveStep = walkBackSpeed * dt;
            if (walkedBack + moveStep >= walkBackDistance)
            {
                moveStep = walkBackDistance - walkedBack;
                isWalkingBack = false; // Done walking back
            }
            walkedBack += moveStep;

            double dx = Math.cos(angle) * moveStep;
            double dy = Math.sin(angle) * moveStep;

            double newX = x + dx;
            double newY = y + dy;

            if (map.isWalkableTile((int)(newX / mapS), (int)(y / mapS)))
                x = newX;
            if (map.isWalkableTile((int)(x / mapS), (int)(newY / mapS)))
                y = newY;

            // No frame update during walkback
            return;
        }

        if (oldState != EnemyAI.AIState.CHASING && newState == EnemyAI.AIState.CHASING)
        {
            smoothFacePlayer(player, 3.0, dt);
        }

        int stateIndex = newState.ordinal();
        if (framesPerState[stateIndex] > 1)
        {
            frameTimer += dt;
            if (frameTimer >= frameDuration)
            {
                frameTimer = 0;
                currentFrame = (currentFrame + 1) % framesPerState[stateIndex];
            }
        }
        else
        {
            currentFrame = 0;
        }
        // Apply melee damage
        if (newState == EnemyAI.AIState.ATTACKING)
        {
            double dx = player.getX() - x;
            double dy = player.getY() - y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            double stopDistance = 30;

            if (dist <= stopDistance && cooldownTimer <= 0)
            {
                player.takeDamage(damage); // will change with difficulty level now
                currentFrame = 1;
                // Play the injured sound
                Main main = (Main)engine;
                engine.playAudio(main.getSoundPlayerInjured());

                cooldownTimer = attackCooldown; // reset cooldown
            }
        }
        if (newState == EnemyAI.AIState.DEAD && deathTimer > frameDuration * 4)
        {
            currentFrame = 4;
        }
        if (orig == null)
        {
            orig = engine.mGraphics.getComposite();
        }
    }

    public EnemyAI.AIState getAIState()
    {
        return ai.getState();
    }

    public void render(GameEngine g, Player player, double[] rayDistances)
    {
        double dx = x - player.getX();
        double dy = y - player.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        double angleToEnemy = Math.atan2(dy, dx);
        double relativeAngle = normalizeAngle(angleToEnemy - player.getAngle());

        double halfFOV = Math.toRadians(30);
        if (Math.abs(relativeAngle) > halfFOV)
            return;

        int numRays = rayDistances.length;
        int screenWidth = g.width();
        int screenHeight = g.height();
        double stripWidth = (double)screenWidth / numRays;

        int stripIndex = (int)((relativeAngle + halfFOV) / (2 * halfFOV) * numRays);

        // Sprite scaling and vertical offset
        double scaleFactor = 1;
        double scale = ((mapS * 500) / distance) * scaleFactor;
        double spriteHeight = scale;
        double spriteWidth = scale;
        int verticalSpriteOffset = 7; // tweak to move sprite down

        // Vertical offset - compensate for camera vertical movement
        double verticalOffset = player.getVerticalLookOffset();
        int screenY = (int)((screenHeight - spriteHeight) / 2 - verticalOffset + verticalSpriteOffset);
        int screenX = (int)(stripIndex * stripWidth - spriteWidth / 2);

        int stateIndex = getAIState().ordinal();
        Image fullFrame;
        Image hitFrame = null;
        Image distFrame = null;
        boolean flipHorizontal = false;

        int dir8;

        // Handling AI states and assigning directions for sprite frames
        if (stateIndex == EnemyAI.AIState.CHASING.ordinal())
        {
            // Chasing animation: 8 directions with 6 frames each
            double angleToPlayer = Math.atan2(player.getY() - y, player.getX() - x);
            double relativeToEnemy = normalizeAngle(angleToPlayer - angle);

            dir8 = getDirectionIndexFromAngle(relativeToEnemy);

            int drawDir = dir8;
            if (dir8 >= 5)
            {
                drawDir = 8 - dir8;
                flipHorizontal = true;
            }

            int col = currentFrame % 6;
            fullFrame = animations[2][drawDir * 6 + col];
            distFrame = distanceAnimations[2][drawDir * 6 + col];
            hitFrame = hitAnimations[2][drawDir * 6 + col];
        }
        else if (stateIndex == EnemyAI.AIState.ATTACKING.ordinal())
        {
            if (attackCooldown - cooldownTimer < frameDuration * 2)
            {
                fullFrame = animations[3][currentFrame];
                distFrame = distanceAnimations[3][currentFrame];
                hitFrame = hitAnimations[3][currentFrame];
            }
            else
            {
                fullFrame = animations[3][0];
                distFrame = distanceAnimations[3][0];
                hitFrame = hitAnimations[3][0];
            }
        }
        else if (stateIndex == EnemyAI.AIState.DEAD.ordinal())
        {
            fullFrame = animations[4][currentFrame];
            distFrame = distanceAnimations[4][currentFrame];
        }
        else if (isWalkingBack)
        {
            // During walkback, face left or right according to walkBackDirection
            // Map left -> direction 3 (west), right -> direction 1 (east)
            int forcedDir = (walkBackDirection == -1) ? 3 : 1;

            int mappedDir = forcedDir;
            if (mappedDir >= 5)
            {
                mappedDir = 8 - mappedDir;
                flipHorizontal = true;
            }
            if (mappedDir > 4)
                mappedDir = 4;

            fullFrame = animations[0][mappedDir];
            hitFrame = hitAnimations[0][mappedDir];
        }
        else if (stateIndex == EnemyAI.AIState.IDLE.ordinal())
        {
            dir8 = getDirectionIndex(player);
            dir8 = (8 - dir8) % 8;

            int mappedDir = dir8;
            if (dir8 >= 5)
            {
                mappedDir = 8 - dir8;
                flipHorizontal = true;
            }

            if (mappedDir > 4)
                mappedDir = 4;

            fullFrame = animations[0][mappedDir];
            distFrame = distanceAnimations[0][mappedDir];
            hitFrame = hitAnimations[0][mappedDir];
        }
        else
        {
            fullFrame = animations[1][0];
            distFrame = distanceAnimations[1][0];
            hitFrame = hitAnimations[1][0];
        }

        int frameW = fullFrame.getWidth(null);
        int frameH = fullFrame.getHeight(null);
        Image[] slices = spriteSlices.get(fullFrame);
        Image[] distSlices = distanceSlices.get(distFrame);
        Image[] hitSlices = null;
        Map<Image, Image[]> hitSpriteSlices = new HashMap<Image, Image[]>();
        if (hit && hitFrame != null)
        {
            hitSlices = spriteSlices.get(hitFrame);
        }
        if (slices == null)
        {
            slices = new Image[frameW];
            for (int i = 0; i < frameW; i++)
            {
                slices[i] = GameEngine.subImage(fullFrame, i, 0, 1, frameH);
                if (hit && hitSlices != null)
                {
                    hitSlices[i] = GameEngine.subImage(hitFrame, i, 0, 1, frameH);
                }
            }
            spriteSlices.put(fullFrame, slices);
            if (hit && hitSlices != null)
            {
                hitSpriteSlices.put(hitFrame, hitSlices);
            }
        }
        if (distSlices == null) {
            distSlices = new Image[frameW];
            for (int i = 0; i < frameW; i++)
            {
                distSlices[i] = GameEngine.subImage(distFrame, i, 0, 1, frameH);
            }
            distanceSlices.put(distFrame, distSlices);
        }
        if (hitSlices == null && hit && hitFrame != null)
        {
            hitSlices = new Image[frameW];
            for (int i = 0; i < frameW; i++)
            {
                hitSlices[i] = GameEngine.subImage(hitFrame, i, 0, 1, frameH);
            }
            hitSpriteSlices.put(hitFrame, hitSlices);
        }
        float brightness = (float)Math.min(0.9, distance / maxDistance);
        for (int i = 0; i < (int)spriteWidth; i++)
        {
            int rayIndex = (int)((screenX + i) / stripWidth);
            if (rayIndex < 0 || rayIndex >= numRays)
                continue;

            if (distance < rayDistances[rayIndex])
            {
                int pixelX = (int)((double)i / spriteWidth * slices.length);
                if (pixelX < 0 || pixelX >= slices.length)
                    continue;

                if (flipHorizontal)
                {
                    int flippedX = slices.length - 1 - pixelX;
                    g.drawImage(slices[flippedX], screenX + i, screenY, 1, (int)spriteHeight);
                    if (hit && hitSlices != null && ai.getState() != EnemyAI.AIState.DEAD)
                    {
                        g.mGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                        g.drawImage(hitSlices[flippedX], screenX + i, screenY, 1, (int)spriteHeight);
                        g.mGraphics.setComposite(orig);
                    }
                    g.mGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, brightness));
                    g.drawImage(distSlices[flippedX], screenX + i, screenY, 1, (int)spriteHeight);
                    g.mGraphics.setComposite(orig);

                }
                else
                {
                    g.drawImage(slices[pixelX], screenX + i, screenY, 1, (int)spriteHeight);
                    if (hit && hitSlices != null && ai.getState() != EnemyAI.AIState.DEAD)
                    {
                        g.mGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                        g.drawImage(hitSlices[pixelX], screenX + i, screenY, 1, (int)spriteHeight);
                        g.mGraphics.setComposite(orig);
                    }
                    g.mGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, brightness));
                    g.drawImage(distSlices[pixelX], screenX + i, screenY, 1, (int)spriteHeight);
                    g.mGraphics.setComposite(orig);
                }
            }
        }
    }

    private double normalizeAngle(double angle)
    {
        while (angle < -Math.PI)
            angle += 2 * Math.PI;
        while (angle > Math.PI)
            angle -= 2 * Math.PI;
        return angle;
    }

    //-----------------------------------------
    //--------- FACING PLAYER METHODS ---------
    //-----------------------------------------
    public void facePlayer(Player player)
    {
        double dx = player.getX() - this.x;
        double dy = player.getY() - this.y;
        this.angle = Math.atan2(dy, dx);
    }
    public boolean toRemove()
    {
        return (deathTimer > hangAroundTime);
    }
    public void smoothFacePlayer(Player player, double maxTurnRate, double dt)
    {
        double dx = player.getX() - this.x;
        double dy = player.getY() - this.y;
        double targetAngle = Math.atan2(dy, dx);

        double angleDiff = normalizeAngle(targetAngle - this.angle);

        // Clamp the turning speed
        double maxTurn = maxTurnRate * dt;
        if (Math.abs(angleDiff) <= maxTurn)
        {
            this.angle = targetAngle;
        }
        else
        {
            this.angle += Math.signum(angleDiff) * maxTurn;
        }

        this.angle = normalizeAngle(this.angle);
    }

    public void moveToward(double targetX, double targetY, double dt)
    {
        double dx = targetX - x;
        double dy = targetY - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > 0.001)
        {
            double dirX = dx / dist;
            double dirY = dy / dist;

            double newX = x + dirX * speed * dt;
            double newY = y + dirY * speed * dt;

            if (map.isWalkableTile((int)(newX / mapS), (int)(y / mapS)))
                x = newX;
            if (map.isWalkableTile((int)(x / mapS), (int)(newY / mapS)))
                y = newY;
        }
    }

    private int getDirectionIndex(Player player)
    {
        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double angleToPlayer = Math.atan2(dy, dx);
        double relative = normalizeAngle(angleToPlayer - angle);
        return getDirectionIndexFromAngle(relative);
    }

    private int getDirectionIndexFromAngle(double angle)
    {
        // 8 directions: N=0, NE=1, E=2, SE=3, S=4, SW=5, W=6, NW=7
        double deg = Math.toDegrees(angle);
        if (deg < 0)
            deg += 360;
        int index = (int)((deg + 22.5) / 45) % 8;
        return index;
    }

    // -----------------------------------------
    // ----------- Place on minimap ------------
    // -----------------------------------------
    public void drawOnMinimap(GameEngine g, Player p)
    {
        double dx = (x / mapS) - (p.getX() / mapS);
        double dy = (y / mapS) - (p.getY() / mapS);
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > 5)
            return;
        final int MINI_MAP_SIZE = 128;
        int miniTileSize = MINI_MAP_SIZE / map.getWidth();

        int offsetX = g.width() - MINI_MAP_SIZE - 10;
        int offsetY = 10;

        double miniX = (x / mapS) * miniTileSize + offsetX;
        double miniY = (y / mapS) * miniTileSize + offsetY;

        g.changeColor(Color.RED);
        g.drawSolidCircle(miniX, miniY, 4);

        double lineLength = 10;
        double endX = miniX + Math.cos(angle) * lineLength;
        double endY = miniY + Math.sin(angle) * lineLength;

        g.drawLine(miniX, miniY, endX, endY);
    }

    // Health and Damage logic
    public void takeDamage(int amt)
    {
        hit = true;
        hitTimer = 0;
        currentHealth -= amt;
        if (currentHealth < 0)
        {
            currentHealth = 0;
        }
    }

    public boolean isAlive()
    {
        return currentHealth > 0;
    }
}
