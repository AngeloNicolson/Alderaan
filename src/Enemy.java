import java.awt.Image;
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
    private Image spriteSheet;
    private Image[][] animations; // [state][frame]

    private int frameWidth, frameHeight;
    private int[] framesPerState = {5, 1, 6}; // IDLE=5, ALERTED=1, CHASING=6 frames
    private double frameTimer = 0;
    private double frameDuration = 0.2;
    private int currentFrame = 0;
    private Map<Image, Image[]> spriteSlices = new HashMap<>();

    // Speed - AI
    private double speed = 50;
    private EnemyAI ai;

    // Walkback state variables
    private boolean isWalkingBack = false;
    private double walkBackDistance = 20;
    private double walkedBack = 0;
    private int walkBackDirection = 0; // -1 for left, +1 for right
    private double walkBackSpeed = 50;

    public Enemy(double x, double y, String enemyType, GameMap map, int mapS)
    {
        super(x, y);
        this.enemyType = enemyType;
        this.map = map;
        this.mapS = mapS;

        this.ai = new EnemyAI(map, mapS);
        this.spriteSheet = GameEngine.loadImage("assets/visual/pixil-frame-death.png");

        // Updated frame size for new spritesheet dimensions: 683 x 1024
        this.frameWidth = 113;       // 683 / 6 columns (approximate)
        this.frameHeight = 1024 / 9; // Updated: 9 rows instead of 8
        animations = new Image[3][]; // IDLE, ALERTED, CHASING

        // IDLE row 7 has 5 columns (directions)
        animations[0] = new Image[5];
        for (int col = 0; col < 5; col++)
        {
            animations[0][col] =
                GameEngine.subImage(spriteSheet, col * frameWidth, 7 * frameHeight, frameWidth, frameHeight);
        }

        // ALERTED uses a single static frame (row 0 col 0)
        animations[1] = new Image[] {GameEngine.subImage(spriteSheet, 0, 0, frameWidth, frameHeight)};

        // CHASING: 8 directions (rows 0-7), each with 6 frames (columns)
        animations[2] = new Image[8 * 6];
        for (int dir = 0; dir < 8; dir++)
        {
            for (int frame = 0; frame < 6; frame++)
            {
                int index = dir * 6 + frame;
                animations[2][index] =
                    GameEngine.subImage(spriteSheet, frame * frameWidth, dir * frameHeight, frameWidth, frameHeight);
            }
        }
    }

    @Override public void update(GameEngine engine, double dt, Player player)
    {
        EnemyAI.AIState oldState = ai.getState();
        ai.update(this, player, dt);
        EnemyAI.AIState newState = ai.getState();

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
        double scaleFactor = 0.77;
        double scale = ((mapS * 320) / distance) * scaleFactor;
        double spriteHeight = scale;
        double spriteWidth = scale;
        int verticalSpriteOffset = 5; // tweak to move sprite down

        // Vertical offset - compensate for camera vertical movement
        double verticalOffset = player.getVerticalLookOffset();
        int screenY = (int)((screenHeight - spriteHeight) / 2 - verticalOffset + verticalSpriteOffset);
        int screenX = (int)(stripIndex * stripWidth - spriteWidth / 2);

        int stateIndex = getAIState().ordinal();
        Image fullFrame;
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
        }
        else
        {
            fullFrame = animations[1][0];
        }

        int frameW = fullFrame.getWidth(null);
        int frameH = fullFrame.getHeight(null);
        Image[] slices = spriteSlices.get(fullFrame);
        if (slices == null)
        {
            slices = new Image[frameW];
            for (int i = 0; i < frameW; i++)
            {
                slices[i] = GameEngine.subImage(fullFrame, i, 0, 1, frameH);
            }
            spriteSlices.put(fullFrame, slices);
        }

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
                }
                else
                {
                    g.drawImage(slices[pixelX], screenX + i, screenY, 1, (int)spriteHeight);
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
    public void drawOnMinimap(GameEngine g)
    {
        final int MINI_MAP_SIZE = 128;
        int miniTileSize = MINI_MAP_SIZE / map.getWidth();

        int offsetX = g.width() - MINI_MAP_SIZE - 10;
        int offsetY = 10;

        double miniX = (x / mapS) * miniTileSize + offsetX;
        double miniY = (y / mapS) * miniTileSize + offsetY;

        g.changeColor(java.awt.Color.RED);
        g.drawSolidCircle(miniX, miniY, 4);

        double lineLength = 10;
        double endX = miniX + Math.cos(angle) * lineLength;
        double endY = miniY + Math.sin(angle) * lineLength;

        g.drawLine(miniX, miniY, endX, endY);
    }
}
